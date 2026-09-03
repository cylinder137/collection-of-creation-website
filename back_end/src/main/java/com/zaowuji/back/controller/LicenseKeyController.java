package com.zaowuji.back.controller;

import com.zaowuji.back.common.ApiResponse;
import com.zaowuji.back.mapper.LicenseMapper;
import com.zaowuji.back.util.RsaUtils;
import com.zaowuji.back.util.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 激活码公钥 / 在线核验接口（客户端专用）
 * - GET /api/license-key/public-key  获取 RSA 公钥（本地验签用；也可用于安装器离线验签）
 * - GET /api/license-key/verify      在线核验激活码（吊销状态 / 验签 / 机器码绑定）
 */
@RestController
@RequestMapping("/api/license-key")
public class LicenseKeyController {

    private final LicenseMapper licenseMapper;

    public LicenseKeyController(LicenseMapper licenseMapper) {
        this.licenseMapper = licenseMapper;
    }

    /** RSA 公钥（PEM） */
    @GetMapping("/public-key")
    public ApiResponse<Map<String, String>> publicKey() {
        String pem = RsaUtils.publicKeyPem(RsaUtils.ensureKeyPair().getPublic());
        return ApiResponse.ok(Map.of("pem", pem, "algorithm", "SHA256withRSA"));
    }

    /**
     * 在线核验激活码：
     * - code 必填：查库（不存在 / 已吊销 → valid=false）
     * - sign 选填：提供则做 RSA 验签
     * - machineCode 选填：提供则校验激活码是否绑定本机
     * <p>
     * 客户端启动核验只传 code 即可获知吊销状态；完整核验可三参齐传。
     */
    @GetMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(@RequestParam String code,
                                                   @RequestParam(required = false) String sign,
                                                   @RequestParam(required = false) String machineCode) {
        var license = licenseMapper.selectByLicenseKey(code);
        if (license == null) {
            return ApiResponse.ok(Map.of("valid", false, "reason", "激活码不存在，请检查是否为本系统签发"));
        }
        if (sign != null && !sign.isBlank()) {
            boolean signatureOk = RsaUtils.verify(RsaUtils.ensureKeyPair().getPublic(), code, sign);
            if (!signatureOk) {
                return ApiResponse.ok(Map.of("valid", false, "reason", "激活码签名无效"));
            }
        }
        if (machineCode != null && !machineCode.isBlank()) {
            String hash = SecurityUtils.sha256Hex(machineCode);
            if (!code.startsWith(hash + "-")) {
                return ApiResponse.ok(Map.of("valid", false, "reason", "激活码与本机机器码不匹配"));
            }
        }
        if (license.getStatus() != null && license.getStatus() == 2) {
            return ApiResponse.ok(Map.of("valid", false, "reason", "激活码已被吊销"));
        }
        return ApiResponse.ok(Map.of("valid", true,
                "productId", license.getProductId(),
                "status", license.getStatus()));
    }
}
