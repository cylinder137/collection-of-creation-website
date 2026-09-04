-- ============================================================
-- 造物集官网（大连造物集有限公司）数据库初始化脚本（v2 安全加固版）
-- 数据库：zaowuji   字符集：utf8mb4   引擎：InnoDB   适用：MySQL 8.0.16+
-- 说明：本脚本为「全量手动建库」参考；项目实际迁移请走 Flyway：
--       back_end/src/main/resources/db/migration/V1__init.sql（Spring Boot 启动自动执行）
-- 约定：
--   1. 金额一律用 INT，单位「分」（微信支付单位就是分，避免浮点误差）
--   2. 状态字段用 TINYINT + CHECK 约束 + 注释枚举
--   3. 主键 BIGINT 自增，统一 created_at / updated_at
--   4. 表名 order 为 MySQL 保留字，故订单表命名为 orders
--   5. 敏感字段（手机号、支付回调原文）加密存储；机器码只存 SHA-256 哈希，不存明文硬件指纹
--   6. 支付回调必须幂等：payment.order_no 唯一约束 + 应用层校验 payment.amount == orders.amount
-- 执行方式：mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `zaowuji`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `zaowuji`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 产品表 product
-- ============================================================
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(100) NOT NULL                COMMENT '产品名称',
  `code`        VARCHAR(50)  NOT NULL                COMMENT '产品编码',
  `description` TEXT                                 COMMENT '产品简介',
  `price`       INT          NOT NULL DEFAULT 0      COMMENT '价格（分），必须 >= 0',
  `version`     VARCHAR(50)  DEFAULT NULL            COMMENT '当前版本号',
  `cover_url`   VARCHAR(255) DEFAULT NULL            COMMENT '封面图 URL',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1上架 0下架',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序权重，越小越靠前',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`code`),
  CONSTRAINT `chk_product_price`  CHECK (`price` >= 0),
  CONSTRAINT `chk_product_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品表';

-- ============================================================
-- 2. 买家用户表 user（user 为 MySQL 保留字，访问时需用反引号）
--    说明：微信认证登录已废除（2026-09），用户以联系方式（手机/邮箱）为唯一标识；
--    与迁移脚本 V2__user_dewechat.sql 保持一致。
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `contact`    VARCHAR(100) NOT NULL                COMMENT '联系方式（手机/邮箱），用户唯一标识',
  `nickname`   VARCHAR(100) DEFAULT NULL            COMMENT '昵称',
  `phone`      VARCHAR(64)  DEFAULT NULL            COMMENT '手机号（AES 加密后存储，禁止明文；个保法敏感个人信息）',
  `email`      VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_contact` (`contact`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='买家用户表';

-- ============================================================
-- 3. 后台管理员表 admin_user
-- ============================================================
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`      VARCHAR(50)  NOT NULL                COMMENT '登录名',
  `password`      VARCHAR(100) NOT NULL                COMMENT 'BCrypt 加密密码（禁止明文；cost >= 10）',
  `nickname`      VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
  `role`          TINYINT      NOT NULL DEFAULT 2      COMMENT '角色：1超管 2普通',
  `status`        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0禁用',
  `last_login_at` DATETIME     DEFAULT NULL            COMMENT '最后登录时间',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_username` (`username`),
  CONSTRAINT `chk_admin_role`   CHECK (`role` IN (1, 2)),
  CONSTRAINT `chk_admin_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台管理员表';

-- ============================================================
-- 4. 订单表 orders
-- ============================================================
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`       VARCHAR(32)  NOT NULL                COMMENT '订单号（业务唯一）',
  `user_id`        BIGINT       NOT NULL                COMMENT '下单用户',
  `product_id`     BIGINT       NOT NULL                COMMENT '购买产品',
  `product_name`   VARCHAR(100) NOT NULL                COMMENT '产品名快照',
  `amount`         INT          NOT NULL                COMMENT '订单金额（分），必须 >= 0',
  `status`         TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0待支付 1已支付 2已取消 3已退款 4已签发',
  `pay_type`       TINYINT      DEFAULT NULL            COMMENT '支付方式：1微信',
  `transaction_id` VARCHAR(64)  DEFAULT NULL            COMMENT '微信支付流水号',
  `paid_at`        DATETIME     DEFAULT NULL            COMMENT '支付时间',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_orders_user` (`user_id`),
  KEY `idx_orders_product` (`product_id`),
  KEY `idx_orders_status` (`status`),
  CONSTRAINT `fk_orders_user`    FOREIGN KEY (`user_id`)    REFERENCES `user` (`id`),
  CONSTRAINT `fk_orders_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `chk_orders_amount` CHECK (`amount` >= 0),
  CONSTRAINT `chk_orders_status` CHECK (`status` IN (0, 1, 2, 3, 4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

-- ============================================================
-- 5. 机器码登记表 device
--    注意：machine_code 只存 SHA-256 十六进制哈希（64 字符），
--    不存明文硬件指纹；比对时对客户端提交的机器码做同样哈希后匹配。
--    激活状态一律以 license 表为准，本表不冗余状态字段。
-- ============================================================
DROP TABLE IF EXISTS `device`;
CREATE TABLE `device` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `machine_code` VARCHAR(64)  NOT NULL                COMMENT '机器码 SHA-256 哈希（hex，64 字符）',
  `product_id`   BIGINT       NOT NULL                COMMENT '产品',
  `order_id`     BIGINT       DEFAULT NULL            COMMENT '关联订单',
  `user_id`      BIGINT       DEFAULT NULL            COMMENT '关联用户',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_machine_code` (`machine_code`),
  KEY `idx_device_order` (`order_id`),
  KEY `idx_device_product` (`product_id`),
  CONSTRAINT `fk_device_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `fk_device_order`   FOREIGN KEY (`order_id`)   REFERENCES `orders` (`id`),
  CONSTRAINT `fk_device_user`    FOREIGN KEY (`user_id`)    REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='机器码登记表';

-- ============================================================
-- 6. 激活码表 license
--    安全约定：
--    - license_key 为激活码内容（payload），sign 为其 RSA 签名（私钥签发，公钥验签）
--    - 激活时必须：①验签通过 ②提交的 machine_code 与绑定值一致 ③状态为未激活/未过期
--      严禁只按"激活码存在"放行
--    - machine_code 只存 SHA-256 哈希
-- ============================================================
DROP TABLE IF EXISTS `license`;
CREATE TABLE `license` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `license_key`  VARCHAR(512) NOT NULL                COMMENT '激活码内容（payload），RSA-2048 签名场景建议 <= 512 字符',
  `machine_code` VARCHAR(64)  NOT NULL                COMMENT '绑定机器码 SHA-256 哈希（hex，64 字符）',
  `product_id`   BIGINT       NOT NULL                COMMENT '产品',
  `order_id`     BIGINT       DEFAULT NULL            COMMENT '关联订单',
  `user_id`      BIGINT       DEFAULT NULL            COMMENT '关联用户',
  `device_id`    BIGINT       DEFAULT NULL            COMMENT '关联机器码记录',
  `sign`         VARCHAR(512) NOT NULL                COMMENT 'RSA 签名（服务端私钥签发，客户端/服务端公钥验签）',
  `license_type` TINYINT      NOT NULL DEFAULT 1      COMMENT '授权类型：1永久 2订阅（订阅时 expires_at 必填）',
  `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0未激活 1已激活 2已吊销 3已过期',
  `issued_at`    DATETIME     DEFAULT NULL            COMMENT '签发时间',
  `activated_at` DATETIME     DEFAULT NULL            COMMENT '激活时间',
  `expires_at`   DATETIME     DEFAULT NULL            COMMENT '过期时间（永久授权为 NULL）',
  `revoked_at`   DATETIME     DEFAULT NULL            COMMENT '吊销时间',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_license_key` (`license_key`),
  KEY `idx_license_machine` (`machine_code`),
  KEY `idx_license_order` (`order_id`),
  KEY `idx_license_device` (`device_id`),
  CONSTRAINT `fk_license_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `fk_license_order`   FOREIGN KEY (`order_id`)   REFERENCES `orders` (`id`),
  CONSTRAINT `fk_license_user`    FOREIGN KEY (`user_id`)    REFERENCES `user` (`id`),
  CONSTRAINT `fk_license_device`  FOREIGN KEY (`device_id`)  REFERENCES `device` (`id`),
  CONSTRAINT `chk_license_type`   CHECK (`license_type` IN (1, 2)),
  CONSTRAINT `chk_license_status` CHECK (`status` IN (0, 1, 2, 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='激活码表';

-- ============================================================
-- 7. 支付流水表 payment
--    安全约定：
--    - order_no 唯一约束：微信回调会重试，重复回调直接幂等拒绝，防止重复流水/重复入账
--    - 应用层必须校验 payment.amount == orders.amount（防金额篡改）
--    - notify_raw 含 openid 等个人信息，必须 AES 加密后存储
-- ============================================================
DROP TABLE IF EXISTS `payment`;
CREATE TABLE `payment` (
  `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`       BIGINT      NOT NULL                COMMENT '订单',
  `order_no`       VARCHAR(32) NOT NULL                COMMENT '订单号（唯一，防微信重复回调）',
  `transaction_id` VARCHAR(64) DEFAULT NULL            COMMENT '微信支付流水号',
  `amount`         INT         NOT NULL                COMMENT '支付金额（分），必须 >= 0',
  `status`         TINYINT     NOT NULL DEFAULT 0      COMMENT '状态：0待支付 1成功 2失败',
  `notify_raw`     TEXT                                COMMENT '回调原始报文（AES 加密存储，对账排查用）',
  `paid_at`        DATETIME    DEFAULT NULL            COMMENT '支付时间',
  `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_order_no` (`order_no`),
  KEY `idx_payment_order` (`order_id`),
  CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `chk_payment_amount` CHECK (`amount` >= 0),
  CONSTRAINT `chk_payment_status` CHECK (`status` IN (0, 1, 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付流水表';

-- ============================================================
-- 8. 操作日志表 operation_log
-- ============================================================
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `admin_id`   BIGINT       DEFAULT NULL            COMMENT '操作管理员',
  `action`     VARCHAR(100) NOT NULL                COMMENT '操作动作',
  `detail`     VARCHAR(500) DEFAULT NULL            COMMENT '操作明细',
  `ip`         VARCHAR(45)  DEFAULT NULL            COMMENT '来源 IP（兼容 IPv6）',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_optlog_admin` (`admin_id`),
  CONSTRAINT `fk_optlog_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 初始数据（种子数据）
-- ============================================================

-- 产品示例
INSERT INTO `product` (`name`, `code`, `description`, `price`, `version`, `status`, `sort`) VALUES
('coBrain 白板笔记编辑器', 'coBrain', '面向团队协作的白板笔记编辑器，支持多端同步与手写识别。', 19900, '1.0.0', 1, 1);

-- 后台管理员（password 为 BCrypt 加密串；示例明文 admin123 仅作格式参考）
-- ⚠️ 上线前必须用 BCryptPasswordEncoder 生成真实哈希替换，严禁使用示例密码：
-- INSERT INTO `admin_user` (`username`, `password`, `nickname`, `role`, `status`) VALUES
-- ('admin', '$2a$10$REPLACE_WITH_BCRYPT_HASH', '超级管理员', 1, 1);
