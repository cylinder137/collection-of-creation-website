package com.zaowuji.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建订单入参
 */
@Data
public class CreateOrderParams {

    @NotNull(message = "产品ID不能为空")
    private Long productId;

    /** 下单人联系方式（手机/邮箱） */
    @NotBlank(message = "联系方式不能为空")
    private String contact;

    private String remark;
}
