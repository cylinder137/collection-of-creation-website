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
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务：创建订单 / 查询订单
 * <p>
 * 注：微信支付回调（支付成功 → 订单状态流转 → 签发激活码）为后续迭代，
 * 当前版本先打通「下单 → 订单落库 → 查询」链路。
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

        // 按联系方式找/建用户（openid 暂用 contact 的 SHA-256 前缀占位，后续接微信登录替换）
        User user = userMapper.selectByOpenid("contact_" + contact);
        if (user == null) {
            user = new User();
            user.setOpenid("contact_" + contact);
            user.setNickname(contact);
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
        vo.setCreatedAt(o.getCreatedAt());
        return vo;
    }
}
