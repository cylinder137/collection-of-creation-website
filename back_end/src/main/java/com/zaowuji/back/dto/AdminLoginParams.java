package com.zaowuji.back.dto;

import lombok.Data;

/**
 * 管理员登录入参
 */
@Data
public class AdminLoginParams {
    private String username;
    private String password;
}
