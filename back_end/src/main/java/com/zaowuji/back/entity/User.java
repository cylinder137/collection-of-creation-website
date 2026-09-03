package com.zaowuji.back.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 买家用户表
 * <p>微信认证登录已废除：以联系方式 contact（手机/邮箱）为唯一标识。
 */
@Data
public class User {
    private Long id;
    /** 联系方式（手机/邮箱），用户唯一标识 */
    private String contact;
    private String nickname;
    /** AES 加密后存储 */
    private String phone;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
