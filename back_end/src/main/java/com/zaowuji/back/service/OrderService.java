package com.zaowuji.back.service;

import com.zaowuji.back.common.BizException;
import com.zaowuji.back.entity.License;
import com.zaowuji.back.entity.Orders;
import com.zaowuji.back.entity.Product;
import com.zaowuji.back.entity.User;
import com.zaowuji.back.mapper.LicenseMapper;
import com.zaowuji.back.mapper.OrdersMapper;
import com.zaowuji.back.mapper.UserMapper;
import com.zaowuji.back.vo.OrderVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务：创建订单 / 查询订单 / 订单列表 / 人工核验通过 / 人工核验拒收
 * <p>
 * 微信支付 API 暂未开通，支付采用「人工核验」模式：
 * 用户下单(待支付) → 扫码转账 → 管理员后台核验通过(置已支付) → 客户端提交机器码签发激活码。
 * 微信认证登录已废除：下单联系方式（手机/QQ等）作为买家用户（user.contact）唯一标识。
 * 管理员核验账单不符时可「拒收」订单（置为已取消），若该订单已签发激活码则一并吊销。
 */
@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final OrdersMapper ordersMapper;
    private final UserMapper userMapper;
    private final LicenseMapper licenseMapper;
    private final ProductService productService;

    public OrderService(OrdersMapper ordersMapper, UserMapper userMapper,
                        LicenseMapper licenseMapper, ProductService productService) {
        this.ordersMapper = ordersMapper;
        this.userMapper = userMapper;
        this.licenseMapper = licenseMapper;
        this.productService = productService;
    }

    /**
     * 创建订单
     *
     * @param productId 产品 ID
     * @param contact   联系方式（手机/QQ等），买家用户唯一标识
     * @return 订单 VO
     */
    @Transactional
    public OrderVO create(Long productId, String contact) {
        Product product = productService.getById(productId);
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BizException("产品已下架");
        }

        // 联系方式可选：空则 anonymous 兜底；按 contact 查找/建档买家用户
        String effectiveContact = (contact == null || contact.isBlank()) ? "anonymous" : contact.trim();
        User user = findOrCreateUser(effectiveContact);

        Orders orders = new Orders();
        orders.setOrderNo(generateOrderNo());
        orders.setUserId(user.getId());
        orders.setProductId(product.getId());
        orders.setProductName(product.getName());
        orders.setAmount(product.getPrice());
        orders.setStatus(0); // 待支付
        orders.setPayType(1); // 微信
        ordersMapper.insert(orders);

        return toVO(orders);
    }

    /**
     * 按联系方式查找买家用户，不存在则建档。
     * 并发下单同号触发唯一键冲突时，回查已建档用户（幂等）。
     */
    private User findOrCreateUser(String contact) {
        User user = userMapper.selectByContact(contact);
        if (user != null) {
            return user;
        }
        user = new User();
        user.setContact(contact);
        user.setNickname(contact);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            User existing = userMapper.selectByContact(contact);
            if (existing == null) {
                throw e;
            }
            return existing;
        }
        return user;
    }

    /**
     * 按订单号查询
     */
    public OrderVO detail(String orderNo) {
        Orders orders = ordersMapper.selectByOrderNo(orderNo);
        if (orders == null) {
            throw new BizException(404, "订单不存在");
        }
        return toVO(orders);
    }

    /**
     * 订单列表（新 → 旧，JOIN user 带下单人联系方式），供管理后台展示与人工核验
     */
    public List<OrderVO> list() {
        return ordersMapper.selectAllWithUser().stream()
                .peek(this::fillLicenseStatus)
                .toList();
    }

    /**
     * 某买家用户的订单列表（新 → 旧），供管理后台用户详情展示
     */
    public List<OrderVO> listByUser(Long userId) {
        return ordersMapper.selectByUserId(userId).stream().map(this::toVO).toList();
    }

    /**
     * 人工核验通过：管理员确认收款后，待支付(0) → 已支付(1)
     *
     * @param orderNo 订单号
     * @return 更新后的订单 VO
     */
    @Transactional
    public OrderVO reviewPass(String orderNo) {
        Orders orders = ordersMapper.selectByOrderNo(orderNo);
        if (orders == null) {
            throw new BizException(404, "订单不存在：" + orderNo);
        }
        Integer status = orders.getStatus();
        if (status != null && status == 2) {
            throw new BizException("订单已取消（含拒收），无法核验通过");
        }
        if (status != null && status == 3) {
            throw new BizException("订单已退款，无法核验通过");
        }
        if (status != null && (status == 1 || status == 4)) {
            // 幂等：已通过/已签发直接返回
            return toVO(orders);
        }
        LocalDateTime now = LocalDateTime.now();
        ordersMapper.updateStatus(orders.getId(), 1, now);
        orders.setStatus(1);
        orders.setPaidAt(now);
        return toVO(orders);
    }

    /**
     * 人工核验拒收：管理员核对账单不符时拒收该订单（置为已取消）。
     * 若该订单此前已签发激活码（异常场景：先通过后发现问题），则一并吊销，激活码立即失效。
     *
     * @param orderNo 订单号
     * @return 更新后的订单 VO（licenseStatus 反映吊销结果）
     */
    @Transactional
    public OrderVO reviewReject(String orderNo) {
        Orders orders = ordersMapper.selectByOrderNo(orderNo);
        if (orders == null) {
            throw new BizException(404, "订单不存在：" + orderNo);
        }
        Integer status = orders.getStatus();
        if (status != null && status == 3) {
            throw new BizException("订单已退款，不能再拒收");
        }
        if (status != null && status == 2) {
            // 幂等：已取消/已拒收直接返回
            return toVO(orders);
        }
        LocalDateTime now = LocalDateTime.now();
        // 置为已取消(2)；保留原支付时间（拒收≠退款，若需退款另走流程）
        ordersMapper.updateStatus(orders.getId(), 2, orders.getPaidAt());
        orders.setStatus(2);

        // 吊销该订单名下未吊销的激活码（若有）
        List<License> licenses = licenseMapper.selectByOrderId(orders.getId());
        for (License license : licenses) {
            if (license.getStatus() != null && license.getStatus() == 2) {
                continue;
            }
            licenseMapper.updateStatus(license.getId(), 2, license.getActivatedAt(), now);
        }
        return toVO(orders);
    }

    private String generateOrderNo() {
        // 时间戳 + 4 位随机数（约 32 位唯一）
        return LocalDateTime.now().format(ORDER_NO_FMT) + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    /** 补充该订单的激活码状态（列表用：JOIN 查询不带该字段） */
    private void fillLicenseStatus(OrderVO vo) {
        List<License> licenses = licenseMapper.selectByOrderId(vo.getId());
        if (licenses != null && !licenses.isEmpty()) {
            vo.setLicenseStatus(licenses.get(0).getStatus());
        }
    }

    private OrderVO toVO(Orders o) {
        OrderVO vo = new OrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setUserId(o.getUserId());
        vo.setProductId(o.getProductId());
        vo.setProductName(o.getProductName());
        vo.setAmount(BigDecimal.valueOf(o.getAmount(), 2));
        vo.setStatus(o.getStatus());
        vo.setPaidAt(o.getPaidAt());
        vo.setCreatedAt(o.getCreatedAt());
        // 核验辅助信息：联系方式（user.contact）+ 该订单激活码状态
        if (o.getUserId() != null) {
            User user = userMapper.selectById(o.getUserId());
            if (user != null) {
                vo.setContact(user.getContact());
            }
        }
        fillLicenseStatus(vo);
        return vo;
    }
}
