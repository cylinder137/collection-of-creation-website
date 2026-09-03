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
 * 激活码公钥 / 在线核验接口（桌面端接入用）：
 * - GET /api/license-key/public-key  拉取 RSA 公钥（验签用；也可内置到安装包离线验签）
 * - GET /api/license-key/verify      在线核验激活码（吊销检查 / 防伪）
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
     * 1) 格式合法（machineHash-productId，machineHash 与提供的机器码哈希一致）
     * 2) 签名有效（RSA 公钥验签）
     * 3) 服务端存在且未吊销
     *
     * @param code       激活码（license_key）
     * @param sign       激活码签名（license.sign）
     * @param machineCode 本机机器码（明文，用于哈希比对）
     */
    @GetMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(@RequestParam String code,
                                                   @RequestParam String sign,
                                                   @RequestParam String machineCode) {
        String hash = SecurityUtils.sha256Hex(machineCode == null ? "" : machineCode);
        String productPart = code == null ? "" : code.substring(code.lastIndexOf('-') + 1);
        boolean formatOk = code != null && code.startsWith(hash + "-")
                && productPart.matches("\\d+");
        if (!formatOk) {
            return ApiResponse.ok(Map.of("valid", false, "reason", "激活码与本机机器码不匹配"));
        }
        boolean signatureOk = RsaUtils.verify(RsaUtils.ensureKeyPair().getPublic(), code, sign);
        if (!signatureOk) {
            return ApiResponse.ok(Map.of("valid", false, "reason", "激活码签名无效"));
        }
        var license = licenseMapper.selectByLicenseKey(code);
        if (license == null) {
            return ApiResponse.ok(Map.of("valid", false, "reason", "激活码不存在（请检查是否为本机签发）"));
        }
        if (license.getStatus() != null && license.getStatus() == 2) {
            return ApiResponse.ok(Map.of("valid", false, "reason", "激活码已被吊销"));
        }
        return ApiResponse.ok(Map.of("valid", true,
                "productId", license.getProductId(),
                "status", license.getStatus()));
    }
}
