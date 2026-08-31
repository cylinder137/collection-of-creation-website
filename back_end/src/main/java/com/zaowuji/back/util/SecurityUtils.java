package com.zaowuji.back.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 安全工具类
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * SHA-256 哈希，返回 hex 字符串（小写）
     * 用于机器码存储：只存哈希，不存明文硬件指纹
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
