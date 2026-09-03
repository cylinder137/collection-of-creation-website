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
    /** 安装包下载地址（exe 自解压安装包） */
    /** 收款码图片地址 */
    private String payQrUrl;
    private String downloadUrl;
    /** 价格（元） */
    private BigDecimal price;
    private Integer status;
    private Integer sort;
}
