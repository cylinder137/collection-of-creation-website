package com.zaowuji.back.dto;

import lombok.Data;

/**
 * 管理后台产品新增/编辑入参
 * price 单位为元（前端展示口径），服务端落库前换算为分
 */
@Data
public class ProductSaveParams {

    private String name;

    /** 产品编码（客户端对接用，如 coBrain） */
    private String code;

    private String description;

    private String version;

    private String coverUrl;

    /** 安装包下载地址（exe 自解压安装包） */
    private String downloadUrl;

    /** 价格（元） */
    private Double price;

    /** 1上架 0下架 */
    private Integer status;

    /** 排序权重，越小越靠前 */
    private Integer sort;
}
