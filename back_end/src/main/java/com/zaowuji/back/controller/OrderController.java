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

    /** 创建订单（微信支付） */
    @PostMapping
    public ApiResponse<OrderVO> create(@Valid @RequestBody CreateOrderParams params) {
        return ApiResponse.ok(orderService.create(params.getProductId(), params.getContact()));
    }

    /** 查询订单 */
    @GetMapping("/{orderNo}")
    public ApiResponse<OrderVO> detail(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.detail(orderNo));
    }
}
