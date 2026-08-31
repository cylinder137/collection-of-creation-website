package com.zaowuji.back.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 买家用户表
 */
@Data
public class User {
    private Long id;
    private String openid;
    private String unionid;
    private String nickname;
    /** AES 加密后存储 */
    private String phone;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
