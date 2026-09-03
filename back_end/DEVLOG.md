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

## 2026-08-28 11:30 ｜ 数据库结构安全加固（v2）+ Flyway 版本化（提交人：cylinder137 / Tinker）
- 安全审查后优化 schema.sql：① payment.order_no 加 UNIQUE（防微信重复回调）② 金额/状态加 CHECK 约束 ③ machine_code 改为只存 SHA-256 哈希（64 字符，不存明文硬件指纹）④ user.phone 与 payment.notify_raw 改为加密存储（个保法合规）⑤ 删除 device.status 冗余字段（激活状态以 license 表为准）⑥ 排序规则升级 utf8mb4_0900_ai_ci ⑦ license 新增 license_type（永久/订阅）⑧ 新增 idx_orders_status 索引
- 新增 Flyway 版本化迁移：back_end/src/main/resources/db/migration/V1__init.sql（Spring Boot 启动自动建表，后续变更新增 V2__xxx.sql，禁止修改已执行脚本）
- 待办：pom.xml 需引入 flyway-core + flyway-mysql 依赖；生成 MyBatis 实体/Mapper；编写产品/订单/激活码业务接口（提交人：小麦能磨面 / MaiMai11185）

## 2026-09-02 ｜ 管理端无状态鉴权强化 + 产品 CRUD + 激活码吊销
- AdminAuthInterceptor 升级：验签 HMAC 令牌后回库核验管理员存在且启用（账号被删/禁用后旧令牌立即失效），满足「每次管理员操作逐次核验身份」要求
- AdminController 新增：GET/POST /admin/products、PUT /admin/products/{id}、PATCH /admin/products/{id}/status（价格元↔分换算）、POST /admin/licenses/{id}/revoke（吊销并记录 revoked_at）
- ProductService 增加管理端 create/update/updateStatus（编码唯一校验 + @CacheEvict 全量失效，官网 5 分钟缓存写后立即生效）；ProductMapper/LicenseMapper 补 selectAll/insert/updateById/updateStatus 及对应 XML
- 配套文档：docs/RSA与激活码接入文档.md（面向 coBrain 客户端：RSA 密钥体系 / 机器码规范 / 下单-核验-激活-验签全流程 / Python·C# 验签示例 / 错误码表）
- mvn compile 通过（提交人：靠谱 / WorkBuddy）

## 2026-09-03 ｜ AI 客服代理接口（DeepSeek）
- 新增 `POST /api/ai/chat`（AiChatController + AiChatService + dto ChatMessage/ChatRequest）：接收 `{messages:[{role,content}]}`，服务端拼接基础客服提示词「小造」后按 DeepSeek 官方接口格式（POST /chat/completions，model=deepseek-chat，OpenAI 兼容）转发，返回 `{reply}`
- 密钥经环境变量 `DEEPSEEK_API_KEY` 注入（application.yml `zaowuji.ai.*`），不落仓库、不进前端；含角色/长度/轮数校验、连接与读超时、异常兜底
- `后端接口文档.md` 新增「10. AI 客服对话（DeepSeek 转发）」章节（请求/响应示例、校验规则、上游官方接口调用格式、配置项）
- `mvn compile` 通过（提交人：Claw 助手 / cylinder137 授权）

## 2026-09-03 ｜ 买家用户数据接口落地（user 表去微信化）
- 微信认证登录方式已废除：新增 Flyway V2 迁移 `V2__user_dewechat.sql`——user 表删除 openid/unionid 列及其唯一索引，新增 `contact`（手机/邮箱）为 NOT NULL 唯一标识；存量 `contact_` 前缀占位数据无损还原；同步更新 `sql/schema.sql` 全量脚本
- User 实体/Mapper 去微信化：selectByOpenid → selectByContact；新增 `selectAllWithStats`（用户列表含订单数/激活码数统计）
- OrderService 下单建档改造：联系方式 trim 后按 user.contact 查找/建档（DuplicateKey 并发兜底幂等），替换原 openid 占位写入
- 管理端订单列表带下单人联系方式：OrdersMapper 新增 `selectAllWithUser`（LEFT JOIN user，金额分→元），OrderVO 增 userId/contact 字段（C 端查询接口不回填，避免泄露他人联系方式）
- 激活码签发绑定用户：ActivationService 携带订单号签发时，license/device 记录 userId（订单所属买家），打通「用户 → 激活码」追溯链
- 新增管理端买家用户接口：UserController + UserService —— `GET /api/admin/users`（列表）、`GET /api/admin/users/{id}`（详情：基本信息 + 名下订单 + 名下激活码），走既有 /api/admin Bearer 鉴权
- 接口文档更新：用户接口章节、订单 contact 语义、微信登录待办标记已废除
- `mvn compile` 通过（提交人：Claw 助手 / cylinder137 授权）
