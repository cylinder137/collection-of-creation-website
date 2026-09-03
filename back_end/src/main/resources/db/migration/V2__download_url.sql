-- V2: 产品增加安装包下载地址字段（人工核验/桌面端激活模式下，官网仅提供下载，不发售激活码）
ALTER TABLE product
    ADD COLUMN download_url VARCHAR(500) NULL COMMENT '安装包下载地址（exe 自解压安装包）' AFTER cover_url;
