package com.zaowuji.back.controller;

import com.zaowuji.back.common.ApiResponse;
import com.zaowuji.back.common.BizException;
import com.zaowuji.back.config.AdminAuthInterceptor;
import com.zaowuji.back.dto.AdminLoginParams;
import com.zaowuji.back.dto.ProductSaveParams;
import com.zaowuji.back.entity.AdminUser;
import com.zaowuji.back.entity.License;
import com.zaowuji.back.entity.Product;
import com.zaowuji.back.mapper.AdminUserMapper;
import com.zaowuji.back.mapper.LicenseMapper;
import com.zaowuji.back.service.OrderService;
import com.zaowuji.back.service.ProductService;
import com.zaowuji.back.util.AdminTokenUtil;
import com.zaowuji.back.vo.AdminLoginVO;
import com.zaowuji.back.vo.LicenseVO;
import com.zaowuji.back.vo.OrderVO;
import com.zaowuji.back.vo.ProductVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理后台接口（RESTful 无状态）：
 * - POST /api/admin/login 公开（换取令牌）
 * - 其余 /api/admin/** 均需 Authorization: Bearer &lt;token&gt;
 *   拦截器每次请求都会验签并回库核验管理员状态，前端只需缓存令牌即可
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminUserMapper adminUserMapper;
    private final LicenseMapper licenseMapper;
    private final OrderService orderService;
    private final ProductService productService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${zaowuji.admin-secret}")
    private String adminSecret;

    public AdminController(AdminUserMapper adminUserMapper, LicenseMapper licenseMapper,
                           OrderService orderService, ProductService productService) {
        this.adminUserMapper = adminUserMapper;
        this.licenseMapper = licenseMapper;
        this.orderService = orderService;
        this.productService = productService;
    }

    /** 管理员登录：校验用户名密码 → 签发无状态令牌 */
    @PostMapping("/login")
    public ApiResponse<AdminLoginVO> login(@RequestBody AdminLoginParams params) {
        String username = params.getUsername() == null ? "" : params.getUsername().trim();
        String password = params.getPassword() == null ? "" : params.getPassword();
        if (username.isEmpty() || password.isEmpty()) {
            throw new BizException("用户名和密码不能为空");
        }
        AdminUser admin = adminUserMapper.selectByUsername(username);
        if (admin == null || !passwordEncoder.matches(password, admin.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BizException("账号已禁用，请联系管理员");
        }
        String token = AdminTokenUtil.issue(admin.getId(), adminSecret);
        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setNickname(admin.getNickname());
        vo.setRole(admin.getRole());
        return ApiResponse.ok(vo);
    }

    /** 当前登录管理员信息（顺带校验令牌有效性） */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_ID);
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(404, "管理员不存在");
        }
        return ApiResponse.ok(Map.of(
                "id", admin.getId(),
                "username", admin.getUsername(),
                "nickname", admin.getNickname(),
                "role", admin.getRole()));
    }

    // ==================== 订单 ====================

    /** 订单列表（新 → 旧，含待人工核验订单） */
    @GetMapping("/orders")
    public ApiResponse<List<OrderVO>> orders() {
        return ApiResponse.ok(orderService.list());
    }

    /** 人工核验通过：待支付(0) → 已支付(1) */
    @PostMapping("/orders/{orderNo}/review-pass")
    public ApiResponse<OrderVO> reviewPass(@PathVariable String orderNo) {
        return ApiResponse.ok(orderService.reviewPass(orderNo));
    }

    // ==================== 激活码 ====================

    /** 激活码签发记录（新 → 旧） */
    @GetMapping("/licenses")
    public ApiResponse<List<LicenseVO>> licenses() {
        return ApiResponse.ok(licenseMapper.selectAll().stream()
                .map(this::toLicenseVO)
                .toList());
    }

    /** 吊销激活码（status → 2，记录吊销时间；客户端在线核验将立即失败） */
    @PostMapping("/licenses/{id}/revoke")
    public ApiResponse<Map<String, Object>> revokeLicense(@PathVariable Long id) {
        License license = licenseMapper.selectById(id);
        if (license == null) {
            throw new BizException(404, "激活码记录不存在");
        }
        if (license.getStatus() != null && license.getStatus() == 2) {
            throw new BizException("激活码已处于吊销状态");
        }
        licenseMapper.updateStatus(id, 2, null, LocalDateTime.now());
        return ApiResponse.ok(Map.of("id", id, "status", 2));
    }

    // ==================== 产品 ====================

    /** 产品列表（含下架，管理用） */
    @GetMapping("/products")
    public ApiResponse<List<ProductVO>> products() {
        return ApiResponse.ok(productService.listAll());
    }

    /** 新建产品 */
    @PostMapping("/products")
    public ApiResponse<ProductVO> createProduct(@RequestBody ProductSaveParams params) {
        return ApiResponse.ok(productService.create(toEntity(params)));
    }

    /** 更新产品（全量字段） */
    @PutMapping("/products/{id}")
    public ApiResponse<ProductVO> updateProduct(@PathVariable Long id,
                                                @RequestBody ProductSaveParams params) {
        return ApiResponse.ok(productService.update(id, toEntity(params)));
    }

    /** 上架 / 下架 */
    @PatchMapping("/products/{id}/status")
    public ApiResponse<ProductVO> updateProductStatus(@PathVariable Long id,
                                                      @RequestBody Map<String, Integer> body) {
        Integer status = body == null ? null : body.get("status");
        return ApiResponse.ok(productService.updateStatus(id, status));
    }

    /** 删除产品（无订单/激活码关联时允许物理删除） */
    @DeleteMapping("/products/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.ok();
    }

    private Product toEntity(ProductSaveParams params) {
        Product p = new Product();
        p.setName(params.getName());
        p.setCode(params.getCode() == null ? null : params.getCode().trim());
        p.setDescription(params.getDescription());
        p.setVersion(params.getVersion());
        p.setCoverUrl(params.getCoverUrl());
        p.setPayQrUrl(params.getPayQrUrl());
        p.setDownloadUrl(params.getDownloadUrl());
        // 元 → 分（四舍五入，避免浮点误差）
        if (params.getPrice() != null) {
            p.setPrice((int) Math.round(params.getPrice() * 100));
        }
        p.setStatus(params.getStatus());
        p.setSort(params.getSort());
        return p;
    }

    private LicenseVO toLicenseVO(License l) {
        LicenseVO vo = new LicenseVO();
        vo.setId(l.getId());
        vo.setLicenseKey(l.getLicenseKey());
        vo.setSign(l.getSign());
        vo.setProductId(l.getProductId());
        vo.setProductName(productService.getById(l.getProductId()).getName());
        vo.setOrderId(l.getOrderId());
        vo.setLicenseType(l.getLicenseType());
        vo.setStatus(l.getStatus());
        vo.setIssuedAt(l.getIssuedAt());
        vo.setActivatedAt(l.getActivatedAt());
        vo.setCreatedAt(l.getCreatedAt());
        return vo;
    }
}
