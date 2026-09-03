package com.zaowuji.back.controller;

import com.zaowuji.back.common.ApiResponse;
import com.zaowuji.back.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 管理后台文件上传（路径 /api/admin/**，已被鉴权拦截器保护，需 Bearer 令牌）
 *
 * 上传产物落盘 {zaowuji.upload-dir}/{cover|package}/ 目录：
 * - cover   封面图（png/jpg/jpeg/webp/gif/svg）
 * - package 安装包（exe/msi/zip/7z）
 * 返回相对 URL（/uploads/...），由 WebConfig 静态映射对外提供，可直接存入产品字段。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminFileController {

    private static final Set<String> COVER_EXTS = Set.of("png", "jpg", "jpeg", "webp", "gif", "svg");
    private static final Set<String> PACKAGE_EXTS = Set.of("exe", "msi", "zip", "7z");

    private final Path uploadRoot;

    public AdminFileController(@Value("${zaowuji.upload-dir:./uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 上传文件
     *
     * @param file 文件内容（multipart）
     * @param kind cover=封面图；qr=收款码图片；package/exe=安装包
     * @return { url: "/uploads/{kind}/{文件名}" }
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "kind", defaultValue = "cover") String kind) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        boolean isPackage = "package".equalsIgnoreCase(kind) || "exe".equalsIgnoreCase(kind);
        boolean isQr = "qr".equalsIgnoreCase(kind) || "payqr".equalsIgnoreCase(kind);
        Set<String> allowExts = isPackage ? PACKAGE_EXTS : COVER_EXTS;
        String ext = extOf(file.getOriginalFilename());
        if (!allowExts.contains(ext)) {
            throw new BizException("不支持的文件类型 ." + ext + "，仅允许：" + String.join("/", allowExts));
        }
        String dir = isPackage ? "package" : (isQr ? "qr" : "cover");
        try {
            Path targetDir = uploadRoot.resolve(dir);
            Files.createDirectories(targetDir);
            String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(targetDir.resolve(name).toFile());
            return ApiResponse.ok(Map.of("url", "/uploads/" + dir + "/" + name));
        } catch (IOException e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
    }

    private String extOf(String filename) {
        if (filename == null) {
            return "";
        }
        int i = filename.lastIndexOf('.');
        return i < 0 ? "" : filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}
