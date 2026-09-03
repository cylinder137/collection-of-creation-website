package com.zaowuji.back.controller;

import com.zaowuji.back.common.ApiResponse;
import com.zaowuji.back.entity.Product;
import com.zaowuji.back.service.ProductService;
import com.zaowuji.back.vo.ProductVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * 产品接口（对外公开）
 * - GET /api/products          上架产品列表（含 downloadUrl 安装包直链 / payQrUrl 收款码地址）
 * - GET /api/products/{id}     产品详情
 * - GET /api/products/{id}/pay-qr  收款码图片（二进制，供桌面端/前端展示）
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final Path uploadRoot;

    public ProductController(ProductService productService,
                             @Value("${zaowuji.upload-dir:./uploads}") String uploadDir) {
        this.productService = productService;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** 获取全部上架产品（含安装包直链 downloadUrl / 收款码地址 payQrUrl） */
    @GetMapping
    public ApiResponse<List<ProductVO>> list() {
        return ApiResponse.ok(productService.listOnSale());
    }

    /** 获取产品详情 */
    @GetMapping("/{id}")
    public ApiResponse<ProductVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(productService.detail(id));
    }

    /**
     * 获取产品收款码图片（无鉴权，桌面端/前端展示用）
     * - payQrUrl 为 /uploads/... 相对路径：直接回读本地文件（image 流）
     * - payQrUrl 为 http(s) 绝对地址：302 跳转
     * - 未配置：404
     */
    @GetMapping("/{id}/pay-qr")
    public ResponseEntity<Resource> payQr(@PathVariable Long id) {
        Product p = productService.getById(id); // 不存在抛 404
        String url = p.getPayQrUrl();
        if (url == null || url.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, url).build();
        }
        if (url.startsWith("/uploads/")) {
            Path file = uploadRoot.resolve(url.substring("/uploads/".length())).normalize();
            if (!file.startsWith(uploadRoot) || !Files.isRegularFile(file)) {
                return ResponseEntity.notFound().build();
            }
            String ext = extOf(file.getFileName().toString());
            MediaType mt = mediaTypeOf(ext);
            return ResponseEntity.ok()
                    .contentType(mt)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                    .body(new FileSystemResource(file));
        }
        return ResponseEntity.notFound().build();
    }

    private String extOf(String filename) {
        int i = filename.lastIndexOf('.');
        return i < 0 ? "" : filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private MediaType mediaTypeOf(String ext) {
        return switch (ext) {
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "svg" -> MediaType.parseMediaType("image/svg+xml");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
