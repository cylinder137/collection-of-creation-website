package com.zaowuji.back.controller;

import com.zaowuji.back.common.ApiResponse;
import com.zaowuji.back.service.UserService;
import com.zaowuji.back.vo.UserDetailVO;
import com.zaowuji.back.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 买家用户接口（管理后台，/api/admin/** 统一走 Bearer 鉴权）：
 * - GET /api/admin/users        用户列表（含订单数/激活码数）
 * - GET /api/admin/users/{id}   用户详情（基本信息 + 名下订单 + 名下激活码）
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 用户列表（新 → 旧） */
    @GetMapping
    public ApiResponse<List<UserVO>> list() {
        return ApiResponse.ok(userService.list());
    }

    /** 用户详情 */
    @GetMapping("/{id}")
    public ApiResponse<UserDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(userService.detail(id));
    }
}
