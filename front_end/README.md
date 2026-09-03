# front_end

造物集官网前端（Vue 3 + Vite + TypeScript + Element Plus）

## 技术栈

- Vue 3（`<script setup>` 组合式 API）
- Vite + TypeScript
- Element Plus（UI 组件库，含中文语言包 + 全量图标）
- Vue Router（hash 模式，方便静态/内网穿透部署）
- Pinia（状态管理）
- Axios（HTTP 客户端，`/api` 前缀经 Vite 代理到后端）

## 快速开始

```bash
npm install        # 安装依赖
npm run dev        # 启动开发服务器 http://localhost:5173
npm run build      # 构建产物到 dist/
npm run preview    # 本地预览构建产物
npm run type-check # 类型检查
```

一键快捷预览（缺依赖自动安装、自动开浏览器，支持 `--build` / `--port` / `--host`）：

```bash
npm run preview:quick            # 开发模式预览
npm run preview:quick -- --build # 构建后预览产物
```

## 目录结构

```
front_end/
├── public/              # 静态资源（favicon）
├── src/
│   ├── api/             # 接口层（http.ts 封装 + index.ts 业务接口）
│   ├── components/      # 通用组件（ProductCard）
│   ├── layouts/         # 布局（DefaultLayout：顶栏 + 路由出口 + 页脚）
│   ├── router/          # 路由配置（hash 模式）
│   ├── stores/          # Pinia 状态
│   ├── styles/          # 全局样式
│   ├── types/           # TypeScript 类型定义
│   ├── views/           # 页面：首页/产品/购买/激活码/管理后台
│   ├── App.vue
│   └── main.ts
├── index.html
├── vite.config.ts       # 含 /api -> localhost:8080 开发代理
└── tsconfig*.json
```

## 接口约定

后端统一返回 `{ code, data, message }`（`code=0/200` 为成功），见 `src/api/http.ts` 拦截器。
开发环境接口前缀 `/api` 由 Vite 代理转发到 `http://localhost:8080`（Spring Boot 默认端口）。

## 反爬设计（前端侧）

页面为 Vue SPA，数据完全由 JS 渲染（HTML 中不含业务数据），并在此之上加了三道"抬门槛"措施：

1. **请求签名**（`src/api/sign.ts`）：每个接口请求带 `X-Timestamp` / `X-Nonce` / `X-Sign`，
   `X-Sign = cyrb53(METHOD|url|timestamp|nonce|secret)`。裸爬虫直接抓接口无法通过校验。
2. **编码传输**：敏感接口后端可返回 `{ "payload": "<XOR+Base64>" }`，前端拦截器自动解码（`decodePayload`）。
3. **环境检测**（`src/utils/antiCrawler.ts`）：识别 `navigator.webdriver`、无头 UA、异常指纹组合，
   生产环境命中即拒绝挂载应用（开发环境自动放行）；构建时剔除 console/debugger。

> 需要后端配合：校验签名与时间戳（±5 分钟防重放）、敏感数据按需编码返回、网关层限流。
> 签名哈希为演示级（cyrb53），正式上线建议换 HMAC-SHA256 并由后端验签。

## 待办

- [ ] 后端接口联调（产品 / 订单 / 激活码）
- [ ] 微信支付（企业收款）对接
- [ ] 管理后台鉴权
