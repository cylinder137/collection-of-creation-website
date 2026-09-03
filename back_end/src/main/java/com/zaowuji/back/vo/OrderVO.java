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
    /** 0待支付 1已支付 2已取消(含管理员拒收) 3已退款 4已签发 */
    private Integer status;
    /** 付款时间 */
    private LocalDateTime paidAt;
    /** 下单时间 */
    private LocalDateTime createdAt;
    /** 用户联系方式（下单时填写，管理员核验用） */
    private String contact;
    /** 该订单激活码状态（0未激活 1已激活 2已吊销 3已过期；未签发为 null） */
    private Integer licenseStatus;
}
