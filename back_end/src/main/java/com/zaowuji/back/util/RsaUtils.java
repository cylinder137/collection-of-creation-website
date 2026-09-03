package com.zaowuji.back.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 工具：激活码 license_key 的 payload 签名（私钥）与验签（公钥）。
 * <p>
 * 密钥对持久化在 back_end/cert/ 下（已加入 .gitignore，严禁入库）：
 * - rsa_private.pem  PKCS#8 私钥（服务端签发用，绝不外发）
 * - rsa_public.pem   X.509 公钥（可随安装包分发 / 通过公开接口下发，供客户端验签）
 * <p>
 * 签名算法：SHA256withRSA；签名内容 = license_key 原文（base64 URL 安全编码后存入 license.sign）。
 */
public final class RsaUtils {

    private static final String ALGORITHM = "RSA";
    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    private static final int KEY_SIZE = 2048;

    /** 密钥文件目录（相对后端运行目录 back_end/ 或 back_end/target/，向上找 cert/） */
    private static final String CERT_DIR = "cert";
    private static final String PRIVATE_FILE = "rsa_private.pem";
    private static final String PUBLIC_FILE = "rsa_public.pem";

    private RsaUtils() {
    }

    /**
     * 确保密钥对存在：cert 目录下无密钥则生成一对（幂等，重启复用已生成密钥）。
     *
     * @return 密钥对
     */
    public static synchronized KeyPair ensureKeyPair() {
        try {
            Path dir = resolveCertDir();
            Path privPath = dir.resolve(PRIVATE_FILE);
            Path pubPath = dir.resolve(PUBLIC_FILE);
            if (Files.exists(privPath) && Files.exists(pubPath)) {
                return loadKeyPair(privPath, pubPath);
            }
            Files.createDirectories(dir);
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(KEY_SIZE);
            KeyPair pair = generator.generateKeyPair();
            // 私钥 PKCS#8 PEM
            Files.writeString(privPath,
                    "-----BEGIN PRIVATE KEY-----\n"
                            + wrapBase64(Base64.getEncoder().encodeToString(
                            pair.getPrivate().getEncoded()))
                            + "\n-----END PRIVATE KEY-----\n",
                    StandardCharsets.UTF_8);
            // 公钥 X.509 PEM
            Files.writeString(pubPath,
                    "-----BEGIN PUBLIC KEY-----\n"
                            + wrapBase64(Base64.getEncoder().encodeToString(
                            pair.getPublic().getEncoded()))
                            + "\n-----END PUBLIC KEY-----\n",
                    StandardCharsets.UTF_8);
            return pair;
        } catch (Exception e) {
            throw new IllegalStateException("RSA 密钥初始化失败", e);
        }
    }

    /** 私钥签名 → base64 URL 安全字符串 */
    public static String sign(PrivateKey privateKey, String content) {
        try {
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("RSA 签名失败", e);
        }
    }

    /** 公钥验签：content 的签名是否匹配 */
    public static boolean verify(PublicKey publicKey, String content, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getUrlDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }

    public static String publicKeyPem(PublicKey publicKey) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + wrapBase64(Base64.getEncoder().encodeToString(publicKey.getEncoded()))
                + "\n-----END PUBLIC KEY-----\n";
    }

    private static KeyPair loadKeyPair(Path privPath, Path pubPath) throws Exception {
        PrivateKey privateKey = KeyFactory.getInstance(ALGORITHM).generatePrivate(
                new PKCS8EncodedKeySpec(decodePem(Files.readString(privPath, StandardCharsets.UTF_8))));
        PublicKey publicKey = KeyFactory.getInstance(ALGORITHM).generatePublic(
                new X509EncodedKeySpec(decodePem(Files.readString(pubPath, StandardCharsets.UTF_8))));
        return new KeyPair(publicKey, privateKey);
    }

    private static byte[] decodePem(String pem) {
        String body = pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    private static String wrapBase64(String b64) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b64.length(); i += 64) {
            sb.append(b64, i, Math.min(i + 64, b64.length())).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /**
     * 解析 cert 目录：兼容从 back_end/ 或 back_end/target/ 启动（从当前目录向上查找已有 cert/）。
     */
    private static Path resolveCertDir() throws IOException {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path cursor = cwd;
        for (int i = 0; i < 6; i++) {
            Path candidate = cursor.resolve(CERT_DIR);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            cursor = cursor.getParent();
            if (cursor == null) {
                break;
            }
        }
        // 兜底：当前目录下创建 cert/
        Path fallback = cwd.resolve(CERT_DIR);
        Files.createDirectories(fallback);
        return fallback;
    }
}
