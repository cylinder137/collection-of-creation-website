package com.zaowuji.back.controller;

import com.zaowuji.back.common.ApiResponse;
import com.zaowuji.back.dto.CreateOrderParams;
import com.zaowuji.back.service.OrderService;
import com.zaowuji.back.vo.OrderVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 创建订单（人工核验支付模式） */
    @PostMapping
    public ApiResponse<OrderVO> create(@Valid @RequestBody CreateOrderParams params) {
        return ApiResponse.ok(orderService.create(params.getProductId(), params.getContact()));
    }

    /** 查询订单 */
    @GetMapping("/{orderNo}")
    public ApiResponse<OrderVO> detail(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.detail(orderNo));
    }

    /** 订单列表（管理后台，新 → 旧） */
    @GetMapping
    public ApiResponse<java.util.List<OrderVO>> list() {
        return ApiResponse.ok(orderService.list());
    }

    /** 人工核验通过：待支付 → 已支付（管理员确认收款后调用） */
    @PostMapping("/{orderNo}/review-pass")
    public ApiResponse<OrderVO> reviewPass(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.reviewPass(orderNo));
    }
}
