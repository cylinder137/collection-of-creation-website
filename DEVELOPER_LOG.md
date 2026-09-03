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

---

## 2026-09-03 打卡（提交人：Claw 助手 / cylinder137 授权）

- **09:12 开工打卡**：拉取并同步 main 最新代码至 `f211c4d`（含前端官网重构 + 隐藏式管理后台、后端管理端接口与鉴权、部署与接入文档等），复核仓库状态与远程分支，开始今日开发
- 按团队规范（GitHub Flow）：本次打卡改动走 `feature/dev-checkin-20260903` 分支提交，合并 main 后当场删除分支

---

## 2026-09-03 打卡（提交人：Claw 助手 / WorkBuddy，经 cylinder137 授权）

- **09:30 开工打卡**：同步 main 至 f211c4d（后端 v0.1 全量合入 + 前端重构 + 管理后台/订单/激活码流程 + 部署与接口文档），复核本地与远程分支状态，开始今日开发
- **状态确认**：远程待合并前端分支（大林：frontend-login / frontend-anti-crawler / frontend-deploy）与本地遗留 feature/frontend-onboarding（入驻日志核验 103d7b9）待处理
- 按团队规范（GitHub Flow）：本次打卡改动在 feature/dev-checkin-20260903-workbuddy 分支提交，合并 main 后当场删除分支

---

## 重要变更记录

### 2026-09-02/03 ｜ 业务模式思路转变：网页发售激活码 → 桌面端驱动激活

**提交人**：靠谱 / WorkBuddy（经 cylinder137 授权）；**分支**：`main`（本次经负责人确认直推）

**背景**：旧方案在浏览器内完成「浏览 → 支付 → 提交机器码 → 拿激活码 → 回软件激活」，存在浏览器权限受限、机器码依赖浏览器指纹易变、网页端校验可被绕过、链路割裂等根本问题。

**新思路**：官网只做「产品展示 + exe 自解压安装包下载」，零售；授权完全由运行在用户主机的桌面端闭环 —— 安装程序以管理员权限采集稳定硬件指纹生成机器码，下单 → 线下转账 → 管理员在隐藏后台 `/admin` 人工核验 → 后端 RSA 私钥签发激活码（绑定机器）→ 安装/每次启动本地验签。

**落地**：
- 前端：官网重写（Hero/下载/激活引导/FAQ），删除购买页、激活页、浏览器端机器码采集工具；新增 `/admin` 隐藏入口 + 独立管理后台布局；官网全站无任何指向 `/admin` 的链接或按钮
- 后端：管理端无状态 RESTful 鉴权（HMAC 令牌，每次请求回库核验管理员状态）；新增产品 CRUD / 上下架、激活码吊销、订单人工核验接口
- 文档：新增 `docs/激活码发售思路转变说明.md`（决策背景/架构时序/新旧对照）、`docs/RSA与激活码接入文档.md`（客户端接入规范）

**详细决策说明**：见 `docs/激活码发售思路转变说明.md`

### 2026-09-03 ｜ docs: README 补充思路转变文档索引

- README「业务模式」节补充两份 docs 文档的索引（思路转变说明 + RSA 接入文档），「目录结构」节展开 docs/ 下两份文档说明
- 目的：避免新增决策文档无人可发现，团队成员与 coBrain 接入方能直接定位
- 提交人：靠谱 / WorkBuddy
