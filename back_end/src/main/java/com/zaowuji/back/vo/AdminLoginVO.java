package com.zaowuji.back.vo;

import lombok.Data;

/**
 * 管理员登录返回
 */
@Data
public class AdminLoginVO {
    /** 无状态令牌：后续请求放 Authorization: Bearer <token> */
    private String token;
    private String nickname;
    /** 1超级 2普通 */
    private Integer role;
}
