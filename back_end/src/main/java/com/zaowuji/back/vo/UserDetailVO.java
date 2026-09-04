package com.zaowuji.back.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户详情 VO（管理后台用户详情用）：基本信息 + 名下订单 + 名下激活码
 */
@Data
public class UserDetailVO {
    private UserVO user;
    private List<OrderVO> orders;
    private List<LicenseVO> licenses;
}
