package com.zaowuji.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 激活码展示 VO
 */
@Data
public class ActivationCodeVO {
    private Long id;
    /** 激活码（payload：机器码哈希-产品ID） */
    private String code;
    /** RSA 私钥签名（base64url），客户端可用公钥离线验签 */
    private String sign;
    private Long productId;
    private String productName;
    /** 绑定的机器码（明文回显，前端展示用） */
    private String machineCode;
    /** 0未激活 1已激活 2已吊销 3已过期 */
    private Integer status;
    private LocalDateTime createdAt;
}
