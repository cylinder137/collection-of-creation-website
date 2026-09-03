package com.zaowuji.back.config;

import com.zaowuji.back.common.ApiResponse;
import com.zaowuji.back.entity.AdminUser;
import com.zaowuji.back.mapper.AdminUserMapper;
import com.zaowuji.back.util.AdminTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员接口鉴权拦截器：/api/admin/** 除登录外，每次请求都执行两步核验：
 * 1. 验签 Authorization: Bearer &lt;token&gt;（无状态令牌，HMAC 签名 + 过期时间）
 * 2. 回库确认管理员仍然存在且未被禁用（防止令牌有效期内账号被停用后继续操作）
 * <p>
 * 前端不保存任何会话，令牌仅是身份凭据；身份的最终裁决权始终在服务端。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ADMIN_ID = "adminId";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AdminUserMapper adminUserMapper;

    @Value("${zaowuji.admin-secret}")
    private String adminSecret;

    public AdminAuthInterceptor(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(response, 401, "未登录或登录已过期");
        }
        Long adminId = AdminTokenUtil.verify(auth.substring(7), adminSecret);
        if (adminId == null) {
            return reject(response, 401, "登录已过期，请重新登录");
        }
        // 回库核验：管理员必须存在且启用（每次请求都查，账号被删/禁用后旧令牌立即失效）
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            return reject(response, 401, "管理员账号不存在");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            return reject(response, 401, "管理员账号已被禁用");
        }
        request.setAttribute(ATTR_ADMIN_ID, adminId);
        return true;
    }

    private boolean reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(status, message)));
        return false;
    }
}
