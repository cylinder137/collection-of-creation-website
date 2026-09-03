package com.zaowuji.back.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付流水表
 */
@Data
public class Payment {
    private Long id;
    private Long orderId;
    /** 订单号（唯一，防微信重复回调） */
    private String orderNo;
    /** 微信支付流水号 */
    private String transactionId;
    /** 支付金额（分） */
    private Integer amount;
    /** 0待支付 1成功 2失败 */
    private Integer status;
    /** 回调原始报文（AES 加密存储） */
    private String notifyRaw;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
