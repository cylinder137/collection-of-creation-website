# 前端部署指南（Cloudflare Tunnel + 本机服务器）

域名：**collectionofcreations.uk**（NS 已托管 Cloudflare）
架构：域名 → Cloudflare → Tunnel（cloudflared）→ 本机服务器
- `/api/*` → Spring Boot（`http://localhost:8080`）
- 其余 → 前端静态产物（`http://localhost:8081`，由 `scripts/serve-dist.mjs` 提供）

前后端**同源部署**，前端代码里 `/api` 是相对路径，天然无跨域问题，无需设置 `VITE_API_BASE`。

## 一、服务器端部署步骤（在跑 Tunnel 的机器上执行）

```bash
# 1. 拉最新代码
git pull origin main

# 2. 构建前端产物
cd front_end
npm install        # 或 npm ci
npm run build      # 产物在 dist/

# 3. 启动静态服务（监听 8081）
node scripts/serve-dist.mjs --port 8081 --host
```

保持常驻（三选一）：
- Windows：`nssm install coc-frontend "node" "front_end/scripts/serve-dist.mjs --port 8081 --host"`
- 或 `pm2 start scripts/serve-dist.mjs --name coc-frontend -- --port 8081 --host`
- Linux：systemd unit（WorkingDirectory=front_end，ExecStart=`node scripts/serve-dist.mjs --port 8081 --host`）

## 二、cloudflared 配置（关键）

编辑服务器上的 cloudflared 配置（通常 `~/.cloudflared/config.yml` 或 `C:\Users\<用户>\.cloudflared\config.yml`）：

```yaml
ingress:
  # /api 走 Spring Boot —— 注意：path 规则必须放在最前面
  - hostname: collectionofcreations.uk
    path: ^/api(/.*)?$
    service: http://localhost:8080
  # 其余全部走前端静态服务
  - hostname: collectionofcreations.uk
    service: http://localhost:8081
  - service: http_status:404
```

改完重启 cloudflared：

```bash
# 如果是服务方式运行
cloudflared service restart
# 或直接重启运行中的 cloudflared 进程
```

## 三、验证上线

```bash
curl -I https://collectionofcreations.uk/           # 200 + text/html
curl -I https://collectionofcreations.uk/assets/    # 静态资源可访问
curl https://collectionofcreations.uk/api/health    # 后端接口（路径以后端实际为准）
```

浏览器打开 `https://collectionofcreations.uk/` 应看到首页；`/#/products` 等 hash 路由可直接访问/刷新。

## 四、更新发布流程（以后每次发版）

```bash
git pull origin main && cd front_end
npm install && npm run build
# serve-dist 无需重启（文件即改即读，index.html no-cache 保证生效）
```

## 五、备选方案（Cloudflare Pages）

若想摆脱本机服务器托管前端：把 `dist/` 部署到 Cloudflare Pages（`npx wrangler pages deploy dist`），
域名根记录指向 Pages，`/api` 仍需经 Tunnel 到后端（用 Worker 路由或保留现有 ingress path 规则）。
优势：CDN 加速、服务器只跑后端；劣势：多一层配置。当前同源方案已够用，暂不推荐切换。
