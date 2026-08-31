package com.zaowuji.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交机器码申请激活码入参
 */
@Data
public class ActivateParams {

    @NotNull(message = "产品ID不能为空")
    private Long productId;

    /** 客户端硬件机器码（明文，服务端 SHA-256 哈希后存储/比对） */
    @NotBlank(message = "机器码不能为空")
    private String machineCode;

    /** 关联订单号（可选）：下单后激活时携带，服务端校验订单并绑定到激活码 */
    private String orderNo;
}
