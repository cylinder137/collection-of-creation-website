package com.zaowuji.back.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品表
 */
@Data
public class Product {
    private Long id;
    private String name;
    private String code;
    private String description;
    /** 价格（分） */
    private Integer price;
    private String version;
    private String coverUrl;
    /** 安装包下载地址（exe 自解压安装包） */
    /** 收款码图片地址 */
    private String payQrUrl;
    private String downloadUrl;
    /** 1上架 0下架 */
    private Integer status;
    /** 排序权重，越小越靠前 */
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
