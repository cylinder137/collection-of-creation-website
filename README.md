# 造物集主页

#### 介绍
**大连造物集有限公司**官方项目仓库 —— 造物集官网，提供旗下各软件（如 coBrain 白板笔记编辑器等）的展示与安装包下载服务。

- 项目类型：课程设计项目（团队 6 人协作）
- 仓库地址：https://github.com/cylinder137/collection-of-creation-website.git

#### 业务模式（2026-09 起）
官网**只做产品展示与 exe 自解压安装包下载，不在网页端发售激活码**。激活完全由桌面端驱动：
1. 用户从官网下载对应产品的安装包（exe 自解压格式）
2. 安装程序（管理员权限）在本机采集硬件指纹生成机器码，向后端发起购买下单
3. 管理员在隐藏后台（`/admin`，全站无任何入口链接）人工核验收款
4. 客户端提交机器码 → 后端 RSA 私钥签发激活码（绑定机器，换机失效）
5. 安装与每次产品启动均做激活校验（本地验签 + 可选在线核验）

> 📘 **思路转变背景**（为何从「网页端在线发售激活码」改为「桌面端闭环授权」）：见 `docs/激活码发售思路转变说明.md`
> 🔌 **客户端接入细节**（RSA 密钥体系 / 机器码规范 / 接口调用）：见 `docs/RSA与激活码接入文档.md`
> 📡 **全部接口文档（含 Base URL / 收款码 / 安装包直链 / 文件上传）**：见 `docs/API接口文档.md`

#### 技术栈
- **前端（front_end）**：Vue 3 + Vite + TypeScript + Element Plus + Pinia
- **后端（back_end）**：Java 21 + Spring Boot 3 + MyBatis + MySQL 8 + Flyway
- **部署**：本机服务器 + Cloudflare Tunnel 内网穿透（域名待购，无需备案）
- **激活码方案**：RSA 2048（SHA256withRSA）签名 + 机器码 SHA-256 绑定；管理端为无状态 RESTful（HMAC 令牌，每次请求回库核验）

#### 功能清单
1. 官网首页：公司介绍、产品展示、安装包下载、激活流程引导
2. 桌面端购买流程：客户端下单 → 扫码转账 → 管理员人工核验 → 客户端签发激活码
3. 激活码系统：机器码绑定签发、本地/在线验签、吊销管理
4. 管理后台（隐藏入口 `/admin`）：订单核验、激活码吊销、产品管理与上下架

#### 开发规范（重要！全员必读）
1. **前后端开发日志只增不删**：每次改动代码后，必须在对应目录的 `DEVLOG.md`（front_end/DEVLOG.md 或 back_end/DEVLOG.md）追加本次改动记录（时间、改动内容、提交人）
2. **每次改动必须跟进**：改代码 → 更新 DEVLOG → git 提交 → 推送远程
3. **采用 GitHub Flow 分支策略**：
   - 每次开发任务从 `main` 新建分支：`feature/xxx`（如 `feature/login-page`）
   - 开发完成 → 提交 Pull Request → 负责人 review 通过后合并进 `main`
   - **合并后当场删除该 feature 分支**（本地 + 远程），不留残留分支
   - 下次开发任务再新建新分支，禁止长期复用/堆积分支
4. 项目路径保持纯英文，避免中文目录导致的工具链问题

#### 安装教程

1.  前端：`cd front_end && npm install && npm run dev`
2.  后端：IDEA 打开 back_end，配置 MySQL 连接后启动
3.  xxxx

#### 使用说明

1.  xxxx
2.  xxxx
3.  xxxx

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request

#### 目录结构

```
collection-of-creation-website/
├── front_end/          # 前端（Vue3 + Vite），开发日志见 front_end/DEVLOG.md
├── back_end/           # 后端（Spring Boot 3），开发日志见 back_end/DEVLOG.md
├── docs/               # 设计/接入文档
│   ├── API接口文档.md              # 总接口文档：Base URL / 公开+管理端全部接口 / 上传
│   ├── 激活码发售思路转变说明.md   # 业务模式决策：网页发售 → 桌面端驱动激活
│   └── RSA与激活码接入文档.md      # 客户端接入：RSA 密钥体系 / 机器码 / 接口调用
└── README.md           # 项目说明与开发规范
```
