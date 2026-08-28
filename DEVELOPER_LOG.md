# 开发者入驻日志

> 记录开发者入驻本项目的环境信息、项目理解与待办事项。
> 对应 DEVLOG 记录：`back_end/DEVLOG.md`、`front_end/DEVLOG.md`（2026-08-27 14:55 条目）

## 入驻信息

- **入驻时间**：2026-08-27 14:55 (GMT+8)
- **开发者**：Claw 助手 / WorkBuddy（经 cylinder137 授权提交）
- **分支策略**：`feature/dev-onboarding-log`（遵守项目规范：禁止直推 main，开发走 feature 分支 + PR review）

## 本地环境

> 环境信息于 2026-08-27 14:52 在本机复核确认。

| 项目 | 版本/说明 |
|---|---|
| 系统 | Windows 11 (10.0.26200) |
| Git | 2.55.0（origin: github.com/cylinder137/collection-of-creation-website.git） |
| Node.js | v22.22.2（npm 10.9.7，可初始化前端脚手架） |
| Python | 3.13.14 |
| Java | 未安装（需安装 JDK 21，目标版本） |
| Maven | 未安装（随后端骨架一起准备） |

## 项目理解

**造物集官网**（大连造物集有限公司）—— 课程设计项目，团队 6 人协作。

1. **定位**：公司官网，提供旗下软件（coBrain 白板笔记编辑器等）的展示、购买与激活码发售
2. **技术栈**：
   - 前端：Vue 3 + Vite + TypeScript（UI 库待定：Element Plus / Naive UI）
   - 后端：Java 21 + Spring Boot 3 + Maven + MySQL 8
   - 部署：本机服务器 + Cloudflare Tunnel 内网穿透
   - 激活码：RSA 签名 + 机器码绑定
3. **核心功能**：官网首页/产品展示 → 购买（微信支付）→ 机器码提交 → 激活码签发 → 客户端导入验签；后台订单与激活记录管理
4. **目录结构**：
   ```
   collection-of-creation-website/
   ├── front_end/   # Vue3 + Vite 前端（DEVLOG.md 日志）
   ├── back_end/    # Spring Boot 3 后端（DEVLOG.md 日志）
   └── README.md    # 项目说明与开发规范
   ```

## 协作规范（已确认）

1. 前后端 DEVLOG **只增不删**，每次改动必须追加记录（时间/内容/提交人）
2. 改动流程：改代码 → 更新 DEVLOG → git 提交 → 推送远程
3. **采用 GitHub Flow 分支策略**：每次开发从 main 新建 `feature/xxx` → 提 PR → review 合并 → **合并后当场删除该分支**（本地+远程）→ 下次开发再新建新分支
4. 项目路径保持纯英文

## 规范变更记录

- **2026-08-27 14:55 入驻**：初版规范（feature 分支 + PR 合并，未明确删分支）
- **2026-08-28 08:50 更新**：明确采用 GitHub Flow —— 每次开发新建 `feature/xxx`，合并 main 后当场删分支（本地+远程），下次开发重新新建

## 待办

- [ ] 前端：`npm create vue` 初始化脚手架
- [ ] 后端：Spring Initializr 初始化 Spring Boot 3 骨架
- [ ] 设计数据库结构（订单表、激活码表、产品表等）
- [ ] 确定 UI 组件库选型
- [ ] 激活码 RSA 签名方案落地

---

## 入驻信息（Dalin08）

- **入驻时间**：2026-08-28 09:35 (GMT+8)
- **开发者**：WorkBuddy（经 大林 / Dalin08 授权提交）
- **分支策略**：`feature/dev-onboarding-dalin08`（遵守项目规范：禁止直推 main，开发走 feature 分支 + PR review）
- **GitHub 接入**：已安装 gh CLI 并完成 `gh auth login` 浏览器授权，账号 Dalin08 对本仓库有 push 权限

## 本地环境（Dalin08）

> 环境信息于 2026-08-28 09:30 在本机复核确认。

| 项目 | 版本/说明 |
|---|---|
| 系统 | Windows 11 (10.0.26200.9168) |
| Git | 2.55.0（origin: github.com/cylinder137/collection-of-creation-website.git） |
| gh CLI | 2.98.0（winget 安装，已登录 Dalin08，HTTPS 协议） |
| Node.js | v22.22.2 |
| Python | 3.13.12 |
| Java | 未安装（需安装 JDK 21，目标版本） |
| Maven | 未安装（随后端骨架一起准备） |

## 协作规范（已确认，Dalin08）

1. 前后端 DEVLOG **只增不删**，每次改动必须追加记录（时间/内容/提交人）
2. 改动流程：改代码 → 更新 DEVLOG → git 提交 → 推送远程
3. **禁止直接推送 main**：从 main 拉取后建 `feature/xxx` 分支，完成后提 PR 由负责人 review 合并；按 GitHub Flow，**分支合并进 main 后当场删除**（本地 + 远端），每次开发新开分支
4. 项目路径保持纯英文

## 待办（Dalin08）

- [ ] 安装 JDK 21 + Maven，跑通后端骨架（`back_end`）
- [ ] 前端 `cd front_end && npm install && npm run dev` 跑通
- [ ] 熟悉已合并的脚手架代码与 PR 记录
- [ ] 确认 UI 组件库选型后参与前端页面开发
