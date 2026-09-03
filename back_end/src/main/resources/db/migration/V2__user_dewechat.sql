-- ============================================================
-- 造物集官网数据库迁移脚本 V2：买家用户表去微信化
-- 背景：微信认证登录方式已废除（从未实际接入）。
--      旧结构以 openid（微信标识）为唯一键，下单时只能把联系方式
--      硬塞进 openid 列占位（contact_<手机/邮箱>）。
-- 目标：user 表改以 contact（手机/邮箱）为唯一标识，移除微信列。
-- 说明：V1 之后的存量行均为 contact_ 前缀占位数据，可无损还原。
-- ============================================================

-- 1. 新增 contact 列（先允许为空，回填后再收紧）
ALTER TABLE `user`
    ADD COLUMN `contact` VARCHAR(100) NULL COMMENT '联系方式（手机/邮箱），用户唯一标识' AFTER `id`;

-- 2. 历史数据还原：openid 的 contact_ 前缀占位 → contact 原文
UPDATE `user`
SET `contact` = CASE
    WHEN `openid` LIKE 'contact\_%' THEN SUBSTRING(`openid`, 9)
    ELSE `openid`
END;

-- 3. 兜底：contact 为空的行（理论不发生）用 user-<id> 填充，避免 NOT NULL 失败
UPDATE `user` SET `contact` = CONCAT('user-', `id`) WHERE `contact` IS NULL OR `contact` = '';

-- 4. 收紧约束：contact 非空 + 唯一，删除微信列及其索引
ALTER TABLE `user`
    MODIFY COLUMN `contact` VARCHAR(100) NOT NULL COMMENT '联系方式（手机/邮箱），用户唯一标识',
    ADD UNIQUE KEY `uk_user_contact` (`contact`),
    DROP INDEX `uk_user_openid`,
    DROP COLUMN `unionid`,
    DROP COLUMN `openid`;
