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
    /** 下单用户 ID（管理端列表返回） */
    private Long userId;
    /** 下单人联系方式（管理端列表返回，人工核验收款时核对用） */
    private String contact;
    private Long productId;
    private String productName;
    /** 金额（元） */
    private BigDecimal amount;
    /** 0待支付 1已支付 2已取消 3已退款 4已签发 */
    private Integer status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
