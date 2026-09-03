-- 产品表新增收款码图片地址列（每个产品可独立配置收款码，由管理后台上传）
ALTER TABLE product
    ADD COLUMN pay_qr_url VARCHAR(500) NULL COMMENT '收款码图片地址(管理后台上传/配置, 供桌面端展示)' AFTER cover_url;
