package com.zaowuji.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 买家用户展示 VO（管理后台用户列表/详情用）
 * <p>phone 为 AES 密文，不对外返回；联系方式以 contact 为准。
 */
@Data
public class UserVO {
    private Long id;
    /** 联系方式（手机/邮箱），用户唯一标识 */
    private String contact;
    private String nickname;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 累计订单数 */
    private Integer orderCount;
    /** 累计激活码数 */
    private Integer licenseCount;
}
