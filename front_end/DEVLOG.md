# front_end 寮€鍙戞棩蹇?

> 鈿狅笍 瑙勫垯锛?**鍙涓嶅垹**銆傛瘡娆″墠绔敼鍔ㄥ悗锛屽湪涓嬫柟杩藉姞璁板綍锛堟椂闂? / 鏀瑰姩鍐呭 / 鎻愪氦浜猴級锛屽苟鍚屾 git 鎻愪氦鎺ㄩ€併€?

## 2026-08-27 13:55 锝? 鍒濆鍖?
- 鍒涘缓 front_end 鐩綍涓庡紑鍙戞棩蹇楋紙鎻愪氦浜猴細cylinder137 / Tinker锛?
- 寰呭姙锛氫娇鐢? `npm create vue`锛圴ite锛夊垵濮嬪寲椤圭洰鑴氭墜鏋?

## 2026-08-27 14:40 锝? 杩佺Щ GitHub
- 浠撳簱杩佺Щ鑷? GitHub锛歝ollection-of-creation-website锛堟彁浜や汉锛歝ylinder137 / Tinker锛?

## 2026-08-27 14:52 锝? 鍏ラ┗鏃ュ織澶嶆牳
- 澶嶆牳寮€鍙戣€呭叆椹绘棩蹇楋紝纭鏈満鐜锛歂ode v22.22.2 / npm 10.9.7锛屽彲鍒濆鍖栧墠绔剼鎵嬫灦锛堟彁浜や汉锛欳law 鍔╂墜 / WorkBuddy锛?

## 2026-08-27 14:55 锝? 寮€鍙戣€呭叆椹?
- 寮€鍙戣€呭叆椹婚」鐩紝瀹屾垚浠撳簱鍏嬮殕涓庣幆澧冪‘璁わ紙鎻愪氦浜猴細OpenClaw 鍔╂墜 / cylinder137 鎺堟潈锛?
- 寰呭姙锛氫娇鐢? `npm create vue`锛圴ite锛夊垵濮嬪寲椤圭洰鑴氭墜鏋?

## 2026-08-27 15:10 锝? 鍓嶇鑴氭墜鏋跺垵濮嬪寲
- 浣跨敤 Vite 鎼缓 Vue3 + TypeScript 鑴氭墜鏋讹細package.json / vite.config.ts / tsconfig / index.html锛堟彁浜や汉锛欳law 鍔╂墜 / WorkBuddy锛?
- 闆嗘垚 Element Plus锛堜腑鏂囪瑷€鍖? + 鍏ㄩ噺鍥炬爣锛夈€乂ue Router锛坔ash 妯″紡锛夈€丳inia銆丄xios锛坄/api` 寮€鍙戜唬鐞嗗埌鍚庣 8080锛?
- 鍩虹椤甸潰锛氶椤? / 浜у搧涓績 / 璐拱 / 婵€娲荤爜 / 绠＄悊鍚庡彴锛岄€氱敤缁勪欢 ProductCard锛岀被鍨嬪畾涔変笌鎺ュ彛灞傞綈澶?
- `npm run type-check` 涓? `npm run build` 鍧囬€氳繃
- 寰呭姙锛氬悗绔帴鍙ｈ仈璋冦€佸井淇℃敮浠樺鎺ャ€佺鐞嗗悗鍙伴壌鏉?
- 备注：Element Plus 全量引入致主包约 1.16 MB（gzip 367 KB），后续可按需引入优化

## 2026-08-28 08:52 ｜ 分支策略更新（GitHub Flow）
- 项目规范更新：每次开发从 main 新建 feature/xxx，合并后当场删分支（提交人：Claw 助手 / cylinder137 授权）


## 2026-08-28 09:50 ｜ 新增页面快捷预览脚本
- 新增 `scripts/preview.mjs`（零依赖 Node 脚本）：开发模式一键预览（缺依赖自动 `npm install`、Vite dev server 自动开浏览器），`--build` 可先构建再预览产物，支持 `--port` / `--host` / `--no-open`（提交人：大林 / WorkBuddy）
- `package.json` 新增入口 `npm run preview:quick`
- 已本地验证 `--help` 与参数校验逻辑；对应 README「快速开始」同步补充用法

## 2026-09-02 ｜ 前端整体重构：官网纯下载模式 + 隐藏式管理后台
- **业务模式切换**：官网不再发售激活码，仅提供产品展示与 exe 自解压安装包下载；激活流程全部移到桌面客户端（安装程序提权采集机器码 → 调后端签发），前端删除浏览器端机器码采集（utils/device.ts）
- **页面重构**：重写 HomeView（Hero/产品下载/激活流程引导/FAQ，全新文案）与 OfficialLayout；删除 ProductsView / PurchaseView / ActivationView / 旧 AdminView / DefaultLayout / ProductCard
- **管理后台（隐藏入口）**：全站任何页面无指向 /admin 的链接或按钮；新增 AdminLoginView（/admin 直达登录）+ AdminLayout + AdminDashboardView（订单人工核验 / 激活码吊销 / 产品 CRUD 与上下架）
- **无状态鉴权**：登录换取 HMAC 令牌缓存 localStorage；axios 拦截器对 /admin/** 请求自动附 Authorization: Bearer；401 自动清令牌跳登录；后端每次请求验签 + 回库核验（提交人：靠谱 / WorkBuddy）
- `npm run build`（含 vue-tsc 类型检查）通过


## 2026-09-03 ｜ AI 客服对话组件（官网悬浮窗）
- 新增 `components/AiChatWidget.vue`：官网右下角悬浮 AI 客服「小造」——聊天气泡 / 输入中动画 / 快捷提问 / Enter 发送 / 移动端适配，随 OfficialLayout 挂在全部官网页面（管理后台不展示）
- 新增 `api/ai.ts`：POST /api/ai/chat 发送多轮消息（`{role, content}`，DeepSeek 官方兼容结构）；API Key 不落前端，由后端代理统一调用 DeepSeek
- 顺带将本文件历史混入的 GBK 编码行统一转 UTF-8（内容不变）
- `npm run type-check` 通过（提交人：Claw 助手 / cylinder137 授权）


