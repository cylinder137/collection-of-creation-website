package com.zaowuji.back.service;

import com.zaowuji.back.common.BizException;
import com.zaowuji.back.entity.Device;
import com.zaowuji.back.entity.License;
import com.zaowuji.back.entity.Orders;
import com.zaowuji.back.entity.Product;
import com.zaowuji.back.mapper.DeviceMapper;
import com.zaowuji.back.mapper.LicenseMapper;
import com.zaowuji.back.mapper.OrdersMapper;
import com.zaowuji.back.util.RsaUtils;
import com.zaowuji.back.util.SecurityUtils;
import com.zaowuji.back.vo.ActivationCodeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 激活码服务：提交机器码申请激活码 / 查询激活记录
 * <p>
 * 安全约定（与 schema.sql 一致）：
 * - 机器码只存 SHA-256 哈希，不存明文
 * - license_key 生成 payload，sign 由服务端 RSA 私钥签发（后续迭代接入）
 * - 激活时必须验签 + 比对机器码哈希 + 校验状态，严禁只按"激活码存在"放行
 */
@Service
public class ActivationService {

    private final LicenseMapper licenseMapper;
    private final DeviceMapper deviceMapper;
    private final OrdersMapper ordersMapper;
    private final ProductService productService;

    public ActivationService(LicenseMapper licenseMapper, DeviceMapper deviceMapper,
                             OrdersMapper ordersMapper, ProductService productService) {
        this.licenseMapper = licenseMapper;
        this.deviceMapper = deviceMapper;
        this.ordersMapper = ordersMapper;
        this.productService = productService;
    }

    /**
     * 提交机器码，申请签发激活码
     * 已存在则直接返回（幂等）
     *
     * @param orderNo 可选：下单后激活时携带，服务端校验订单并绑定到激活码
     */
    @Transactional
    public ActivationCodeVO activate(Long productId, String machineCode, String orderNo) {
        Product product = productService.getById(productId);
        String hash = SecurityUtils.sha256Hex(machineCode);

        // 幂等：同一产品 + 同一机器码已有激活码则直接返回
        License existing = licenseMapper.selectByLicenseKey(hash + "-" + productId);
        if (existing != null) {
            return toVO(existing, product, machineCode);
        }

        // 可选订单校验：订单存在、已支付（人工核验）、产品匹配，激活码绑定该订单
        Long orderId = null;
        Long orderUserId = null;
        LocalDateTime orderPaidAt = null;
        if (orderNo != null && !orderNo.isBlank()) {
            Orders order = ordersMapper.selectByOrderNo(orderNo);
            if (order == null) {
                throw new BizException("订单不存在：" + orderNo);
            }
            if (order.getStatus() != null && order.getStatus() == 2) {
                throw new BizException("订单已取消，无法签发激活码");
            }
            if (order.getStatus() != null && order.getStatus() == 3) {
                throw new BizException("订单已退款，无法签发激活码");
            }
            // 人工核验模式：待支付(0) 不允许签发，须管理员后台确认收款
            if (order.getStatus() != null && order.getStatus() == 0) {
                throw new BizException("订单待人工审核，管理员确认收款后即可激活");
            }
            if (!order.getProductId().equals(productId)) {
                throw new BizException("订单与所选产品不匹配");
            }
            orderId = order.getId();
            orderUserId = order.getUserId();
            orderPaidAt = order.getPaidAt();
        }

        // 登记设备（幂等；带订单时把设备归属到下单用户，便于后台用户维度追溯）
        Device device = deviceMapper.selectByMachineCode(hash);
        if (device == null) {
            device = new Device();
            device.setMachineCode(hash);
            device.setProductId(productId);
            device.setUserId(orderUserId);
            deviceMapper.insert(device);
        }

        // 签发激活码（license_key = 机器码哈希-产品ID；sign = RSA 私钥签名，客户端/官网可用公钥验签）
        License license = new License();
        license.setLicenseKey(hash + "-" + productId);
        license.setMachineCode(hash);
        license.setProductId(productId);
        license.setOrderId(orderId);
        license.setUserId(orderUserId);
        license.setDeviceId(device.getId());
        license.setSign(RsaUtils.sign(RsaUtils.ensureKeyPair().getPrivate(),
                hash + "-" + productId));
        license.setLicenseType(1); // 永久
        license.setStatus(0);      // 未激活
        license.setIssuedAt(LocalDateTime.now());
        licenseMapper.insert(license);

        // 绑定订单的激活码签发成功 → 订单置「已签发(4)」，保留原支付时间
        if (orderId != null) {
            ordersMapper.updateStatus(orderId, 4, orderPaidAt);
        }

        return toVO(license, product, machineCode);
    }

    /**
     * 查询激活记录（按机器码哈希匹配）
     */
    public List<ActivationCodeVO> list(String machineCode) {
        String hash = SecurityUtils.sha256Hex(machineCode == null ? "" : machineCode);
        return licenseMapper.selectByMachineCode(hash).stream()
                .map(l -> toVO(l, productService.getById(l.getProductId()), machineCode))
                .toList();
    }

    private ActivationCodeVO toVO(License l, Product product, String machineCode) {
        ActivationCodeVO vo = new ActivationCodeVO();
        vo.setId(l.getId());
        vo.setCode(l.getLicenseKey());
        vo.setSign(l.getSign());
        vo.setProductId(l.getProductId());
        vo.setProductName(product.getName());
        vo.setMachineCode(machineCode);
        vo.setStatus(l.getStatus());
        vo.setCreatedAt(l.getCreatedAt());
        return vo;
    }
}
