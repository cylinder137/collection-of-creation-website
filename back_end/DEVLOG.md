# back_end 开发日志

> ⚠️ 规则：**只增不删**。每次后端改动后，在下方追加记录（时间 / 改动内容 / 提交人），并同步 git 提交推送。

## 2026-08-27 13:55 ｜ 初始化
- 创建 back_end 目录与开发日志（提交人：cylinder137 / Tinker）
- 待办：使用 Spring Initializr 初始化 Spring Boot 3 项目骨架，设计数据库结构

## 2026-08-27 14:40 ｜ 迁移 GitHub
- 仓库迁移至 GitHub：collection-of-creation-website（提交人：cylinder137 / Tinker）

## 2026-08-27 15:55 ｜ 后端脚手架初始化 & 开发者加入
- 开发者加入：`小麦能磨面`（GitHub: MaiMai11185）正式加入后端开发，负责后端模块（提交人：小麦能磨面）
- 后端环境就绪：JDK 21（Temurin 21.0.12.1 LTS）+ Maven 3.9.11（阿里云镜像 + UTF-8）+ MySQL 8.0.45，本地库 `zaowuji`（utf8mb4，root）
- 创建 Spring Boot 3.5.16 + MyBatis 3.0.5 + MySQL 驱动的 Maven 项目骨架：`pom.xml`、主类 `BackEndApplication`、`HealthController` 健康检查接口、`application.yml` 数据源配置
- 已验证 `mvn compile` 构建成功（BUILD SUCCESS，Java 21）
- 新增根目录 `.gitignore`（排除 `target/`、`node_modules/`、IDE 配置等）
- 分支：`feature/backend-scaffold`，按团队规范走 PR 合并，不直推 main
- 待办：设计业务表结构（产品 / 订单 / 激活码表），编写 Mapper / Service / Controller 业务代码

## 2026-08-28 10:24 ｜ 数据库表结构设计
- 设计数据库 8 张表：`product`（产品）、`user`（买家）、`orders`（订单）、`device`（机器码登记）、`license`（激活码）、`admin_user`（管理员）+ 辅助表 `payment`（支付流水）、`operation_log`（操作日志）
- 生成建表脚本 `back_end/sql/schema.sql`：utf8mb4 + InnoDB，金额统一用分（INT），含索引与外键约束，附 coBrain 产品种子数据
- 关键约定：订单表命名 `orders` 避开 MySQL 保留字；激活码表存 RSA 签名 `sign` + 绑定机器码 `machine_code`
- 待办：生成 MyBatis 实体类/Mapper，编写产品 / 订单 / 激活码业务接口（提交人：小麦能磨面 / MaiMai11185）


## 2026-08-28 08:52 �� ��֧���Ը��£�GitHub Flow��
- ��Ŀ�淶���£�ÿ�ο����� main �½� feature/xxx���ϲ��󵱳�ɾ��֧���ύ�ˣ�Claw ���� / cylinder137 ��Ȩ��
