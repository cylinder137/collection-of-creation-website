package com.zaowuji.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 激活码管理 VO（管理后台展示用）
 */
@Data
public class LicenseVO {
    private Long id;
    /** license_key：机器码哈希-产品ID */
    private String licenseKey;
    /** RSA 签名（base64url） */
    private String sign;
    private Long productId;
    private String productName;
    private Long orderId;
    /** 0永久 1试用... */
    private Integer licenseType;
    /** 0未激活 1已激活 2已吊销 3已过期 */
    private Integer status;
    private LocalDateTime issuedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime createdAt;
}
