package com.zaowuji.back.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台管理员表
 */
@Data
public class AdminUser {
    private Long id;
    private String username;
    /** BCrypt 加密密码 */
    private String password;
    private String nickname;
    /** 1超管 2普通 */
    private Integer role;
    /** 1启用 0禁用 */
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
