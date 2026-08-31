package com.zaowuji.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 激活码展示 VO
 */
@Data
public class ActivationCodeVO {
    private Long id;
    /** 激活码 */
    private String code;
    private Long productId;
    private String productName;
    /** 绑定的机器码（明文回显，前端展示用） */
    private String machineCode;
    /** 0未激活 1已激活 2已吊销 3已过期 */
    private Integer status;
    private LocalDateTime createdAt;
}
