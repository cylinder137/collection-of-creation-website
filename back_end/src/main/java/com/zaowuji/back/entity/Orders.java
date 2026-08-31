package com.zaowuji.back.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单表
 */
@Data
public class Orders {
    private Long id;
    /** 订单号（业务唯一） */
    private String orderNo;
    private Long userId;
    private Long productId;
    /** 产品名快照 */
    private String productName;
    /** 订单金额（分） */
    private Integer amount;
    /** 0待支付 1已支付 2已取消 3已退款 4已签发 */
    private Integer status;
    /** 1微信 */
    private Integer payType;
    /** 微信支付流水号 */
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
