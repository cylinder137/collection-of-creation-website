package com.zaowuji.back.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 管理员无状态令牌（HMAC-SHA256 签名，类似 JWT 的简化版）。
 * <p>
 * 结构：base64url(payload).base64url(signature)
 * payload = {"uid": 管理员ID, "exp": 过期时间戳(秒)}
 * 服务端持有 secret（application.yml: zaowuji.admin-secret），验签即可确认身份，无需会话存储。
 */
public final class AdminTokenUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TTL_SECONDS = 12 * 3600; // 12 小时

    private AdminTokenUtil() {
    }

    /** 签发令牌 */
    public static String issue(Long adminId, String secret) {
        String payload = "{\"uid\":" + adminId
                + ",\"exp\":" + (System.currentTimeMillis() / 1000 + TTL_SECONDS) + "}";
        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String sig = hmac(payloadB64, secret);
        return payloadB64 + "." + sig;
    }

    /**
     * 校验令牌：签名有效且未过期。
     *
     * @return 管理员 ID；无效返回 null
     */
    public static Long verify(String token, String secret) {
        if (token == null || secret == null) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return null;
        }
        String expect = hmac(parts[0], secret);
        if (!constantTimeEquals(expect, parts[1])) {
            return null;
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(parts[0]),
                    StandardCharsets.UTF_8);
            long exp = Long.parseLong(json.replaceAll(".*\"exp\":(\\d+).*", "$1"));
            if (System.currentTimeMillis() / 1000 >= exp) {
                return null;
            }
            return Long.parseLong(json.replaceAll(".*\"uid\":(\\d+).*", "$1"));
        } catch (Exception e) {
            return null;
        }
    }

    private static String hmac(String content, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
