package com.zaowuji.back.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单展示 VO
 */
@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long productId;
    private String productName;
    /** 金额（元） */
    private BigDecimal amount;
    /** 0待支付 1已支付 2已取消 3已退款 4已签发 */
    private Integer status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
