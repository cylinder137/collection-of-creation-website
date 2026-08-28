# front_end 开发日志

> ⚠️ 规则：**只增不删**。每次前端改动后，在下方追加记录（时间 / 改动内容 / 提交人），并同步 git 提交推送。

## 2026-08-27 13:55 ｜ 初始化
- 创建 front_end 目录与开发日志（提交人：cylinder137 / Tinker）
- 待办：使用 `npm create vue`（Vite）初始化项目脚手架

## 2026-08-27 14:40 ｜ 迁移 GitHub
- 仓库迁移至 GitHub：collection-of-creation-website（提交人：cylinder137 / Tinker）

## 2026-08-27 14:52 ｜ 入驻日志复核
- 复核开发者入驻日志，确认本机环境：Node v22.22.2 / npm 10.9.7，可初始化前端脚手架（提交人：Claw 助手 / WorkBuddy）

## 2026-08-27 14:55 ｜ 开发者入驻
- 开发者入驻项目，完成仓库克隆与环境确认（提交人：OpenClaw 助手 / cylinder137 授权）
- 待办：使用 `npm create vue`（Vite）初始化项目脚手架

## 2026-08-27 15:10 ｜ 前端脚手架初始化
- 使用 Vite 搭建 Vue3 + TypeScript 脚手架：package.json / vite.config.ts / tsconfig / index.html（提交人：Claw 助手 / WorkBuddy）
- 集成 Element Plus（中文语言包 + 全量图标）、Vue Router（hash 模式）、Pinia、Axios（`/api` 开发代理到后端 8080）
- 基础页面：首页 / 产品中心 / 购买 / 激活码 / 管理后台，通用组件 ProductCard，类型定义与接口层齐备
- `npm run type-check` 与 `npm run build` 均通过
- 待办：后端接口联调、微信支付对接、管理后台鉴权
- 备注：Element Plus 全量引入致主包约 1.16 MB（gzip 367 KB），后续可按需引入优化

## 2026-08-28 09:50 ｜ 新增页面快捷预览脚本
- 新增 `scripts/preview.mjs`（零依赖 Node 脚本）：开发模式一键预览（缺依赖自动 `npm install`、Vite dev server 自动开浏览器），`--build` 可先构建再预览产物，支持 `--port` / `--host` / `--no-open`（提交人：大林 / WorkBuddy）
- `package.json` 新增入口 `npm run preview:quick`
- 已本地验证 `--help` 与参数校验逻辑；对应 README「快速开始」同步补充用法
