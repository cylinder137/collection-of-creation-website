package com.zaowuji.back.service;

import com.zaowuji.back.common.BizException;
import com.zaowuji.back.entity.Orders;
import com.zaowuji.back.entity.Product;
import com.zaowuji.back.entity.User;
import com.zaowuji.back.mapper.OrdersMapper;
import com.zaowuji.back.mapper.UserMapper;
import com.zaowuji.back.vo.OrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务：创建订单 / 查询订单 / 订单列表 / 人工核验通过
 * <p>
 * 微信支付 API 暂未开通，支付采用「人工核验」模式：
 * 用户下单(待支付) → 扫码转账 → 管理员后台一键通过(置已支付) → 用户激活页签发激活码。
 */
@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final OrdersMapper ordersMapper;
    private final UserMapper userMapper;
    private final ProductService productService;

    public OrderService(OrdersMapper ordersMapper, UserMapper userMapper, ProductService productService) {
        this.ordersMapper = ordersMapper;
        this.userMapper = userMapper;
        this.productService = productService;
    }

    /**
     * 创建订单
     *
     * @param productId 产品 ID
     * @param contact   联系方式（手机/邮箱），当前作为用户标识
     * @return 订单 VO
     */
    @Transactional
    public OrderVO create(Long productId, String contact) {
        Product product = productService.getById(productId);
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BizException("产品已下架");
        }

        // 联系方式可选：空则用 anonymous 兜底（openid 暂用 contact 的 SHA-256 前缀占位，后续接微信登录替换）
        String effectiveContact = (contact == null || contact.isBlank()) ? "anonymous" : contact;
        User user = userMapper.selectByOpenid("contact_" + effectiveContact);
        if (user == null) {
            user = new User();
            user.setOpenid("contact_" + effectiveContact);
            user.setNickname(effectiveContact);
            userMapper.insert(user);
        }

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
     * 订单列表（新 → 旧），供管理后台展示与人工核验
     */
    public List<OrderVO> list() {
        return ordersMapper.selectAll().stream().map(this::toVO).toList();
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
            throw new BizException("订单已取消，无法通过审核");
        }
        if (status != null && status == 3) {
            throw new BizException("订单已退款，无法通过审核");
        }
        if (status != null && status == 1) {
            // 幂等：已通过审核直接返回，避免重复操作报错
            return toVO(orders);
        }
        if (status != null && status == 4) {
            return toVO(orders);
        }
        LocalDateTime now = LocalDateTime.now();
        ordersMapper.updateStatus(orders.getId(), 1, now);
        orders.setStatus(1);
        orders.setPaidAt(now);
        return toVO(orders);
    }

    private String generateOrderNo() {
        // 时间戳 + 4 位随机数，32 位内唯一
        return LocalDateTime.now().format(ORDER_NO_FMT) + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private OrderVO toVO(Orders o) {
        OrderVO vo = new OrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setProductId(o.getProductId());
        vo.setProductName(o.getProductName());
        vo.setAmount(BigDecimal.valueOf(o.getAmount(), 2));
        vo.setStatus(o.getStatus());
        vo.setPaidAt(o.getPaidAt());
        vo.setCreatedAt(o.getCreatedAt());
        return vo;
    }
}
