# 造物集主页

#### 介绍
**大连造物集有限公司**官方项目仓库 —— 造物集官网，提供旗下各软件（如 coBrain 白板笔记编辑器等）的展示、购买与激活码发售服务。

- 项目类型：课程设计项目（团队 6 人协作）
- 仓库地址：https://github.com/cylinder137/collection-of-creation-website.git

#### 技术栈
- **前端（front_end）**：Vue 3 + Vite + TypeScript（UI 组件库待定，建议 Element Plus / Naive UI）
- **后端（back_end）**：Java 21 + Spring Boot 3 + Maven + MySQL 8
- **部署**：本机服务器 + Cloudflare Tunnel 内网穿透（域名待购，无需备案）
- **激活码方案**：RSA 签名 + 机器码绑定（客户端采集机器码 → 官网签发激活码 → 客户端导入验签）

#### 预期功能
1. 官网首页：公司介绍、产品展示（coBrain 等软件）
2. 购买流程：选择软件 → 微信支付（企业收款）→ 生成订单
3. 激活码系统：机器码提交 → 后台签发激活码 → 客户端导入激活
4. 订单与激活记录管理后台

#### 开发规范（重要！全员必读）
1. **前后端开发日志只增不删**：每次改动代码后，必须在对应目录的 `DEVLOG.md`（front_end/DEVLOG.md 或 back_end/DEVLOG.md）追加本次改动记录（时间、改动内容、提交人）
2. **每次改动必须跟进**：改代码 → 更新 DEVLOG → git 提交 → 推送远程
3. **禁止直接推送 master**：每人从 master 拉取后创建自己的 feature 分支（如 `feature/xxx`），开发完成后提交 Pull Request / 合并请求，由负责人 review 后合并
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
└── README.md           # 项目说明与开发规范
```
