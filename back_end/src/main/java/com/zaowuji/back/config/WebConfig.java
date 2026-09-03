package com.zaowuji.back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置：
 * - 注册管理员接口鉴权拦截器（/api/admin/**，登录接口除外）
 * - 上传文件静态映射：/uploads/** -> {zaowuji.upload-dir} 目录（封面图/安装包公网可访问）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final Path uploadRoot;

    public WebConfig(AdminAuthInterceptor adminAuthInterceptor,
                     @Value("${zaowuji.upload-dir:./uploads}") String uploadDir) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                // 登录接口本身公开（换取令牌）
                .excludePathPatterns("/api/admin/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 上传产物（cover 封面图 / package 安装包）对外静态访问：/uploads/** -> 磁盘目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadRoot.toUri().toString());
    }
}
