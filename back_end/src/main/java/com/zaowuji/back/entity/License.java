package com.zaowuji.back.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 激活码表
 */
@Data
public class License {
    private Long id;
    /** 激活码内容（payload） */
    private String licenseKey;
    /** 绑定机器码 SHA-256 哈希 */
    private String machineCode;
    private Long productId;
    private Long orderId;
    private Long userId;
    private Long deviceId;
    /** RSA 签名 */
    private String sign;
    /** 1永久 2订阅 */
    private Integer licenseType;
    /** 0未激活 1已激活 2已吊销 3已过期 */
    private Integer status;
    private LocalDateTime issuedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
