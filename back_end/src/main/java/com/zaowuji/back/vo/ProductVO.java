package com.zaowuji.back.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 产品展示 VO（价格转元）
 */
@Data
public class ProductVO {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String version;
    private String coverUrl;
    /** 价格（元） */
    private BigDecimal price;
    private Integer status;
    private Integer sort;
}
