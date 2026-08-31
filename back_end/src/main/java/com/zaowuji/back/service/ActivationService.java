package com.zaowuji.back.service;

import com.zaowuji.back.common.BizException;
import com.zaowuji.back.entity.Device;
import com.zaowuji.back.entity.License;
import com.zaowuji.back.entity.Product;
import com.zaowuji.back.mapper.DeviceMapper;
import com.zaowuji.back.mapper.LicenseMapper;
import com.zaowuji.back.util.SecurityUtils;
import com.zaowuji.back.vo.ActivationCodeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private final ProductService productService;

    public ActivationService(LicenseMapper licenseMapper, DeviceMapper deviceMapper, ProductService productService) {
        this.licenseMapper = licenseMapper;
        this.deviceMapper = deviceMapper;
        this.productService = productService;
    }

    /**
     * 提交机器码，申请签发激活码
     * 已存在则直接返回（幂等）
     */
    @Transactional
    public ActivationCodeVO activate(Long productId, String machineCode) {
        Product product = productService.getById(productId);
        String hash = SecurityUtils.sha256Hex(machineCode);

        // 幂等：同一产品 + 同一机器码已有激活码则直接返回
        License existing = licenseMapper.selectByLicenseKey(hash + "-" + productId);
        if (existing != null) {
            return toVO(existing, product, machineCode);
        }

        // 登记设备（幂等）
        Device device = deviceMapper.selectByMachineCode(hash);
        if (device == null) {
            device = new Device();
            device.setMachineCode(hash);
            device.setProductId(productId);
            deviceMapper.insert(device);
        }

        // 签发激活码（payload = 机器码哈希 + 产品ID，sign 占位后续接 RSA）
        License license = new License();
        license.setLicenseKey(hash + "-" + productId);
        license.setMachineCode(hash);
        license.setProductId(productId);
        license.setDeviceId(device.getId());
        license.setSign("RSA_PENDING_" + UUID.randomUUID()); // TODO: 接入 RSA 私钥签发
        license.setLicenseType(1); // 永久
        license.setStatus(0);      // 未激活
        license.setIssuedAt(LocalDateTime.now());
        licenseMapper.insert(license);

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
        vo.setProductId(l.getProductId());
        vo.setProductName(product.getName());
        vo.setMachineCode(machineCode);
        vo.setStatus(l.getStatus());
        vo.setCreatedAt(l.getCreatedAt());
        return vo;
    }
}
