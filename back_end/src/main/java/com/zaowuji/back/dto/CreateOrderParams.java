package com.zaowuji.back.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建订单入参
 */
@Data
public class CreateOrderParams {

    @NotNull(message = "产品ID不能为空")
    private Long productId;

    /** 下单人联系方式（手机/邮箱），可选；空则由服务端兜底为 anonymous */
    private String contact;

    private String remark;
}
