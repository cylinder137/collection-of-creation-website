# 部署与内网穿透计划（DEPLOYMENT.md）

> 2026-08-31 建立 | 提交人：Tinker / cylinder137

## 背景与决策

- **Cloudflare Tunnel 已打通但国内访问被墙**（2026-08-29 实测）：
  - 本机 → Cloudflare 边缘出站连接正常（region1 QUIC 7844 通）
  - 但公网访问端被 GFW 重置：`172.67.x.x` / `104.21.x.x` 段 TCP 重置（ERR_CONNECTION_RESET -101）
  - `104.16.x.x` 段实测可通（curl 200）→ 属 IP 段封锁，非隧道故障
- **决策**：公网稳定访问走**花生壳内网穿透**，Cloudflare Tunnel 保留为备用线路（自用/演示可用 hosts 优选 IP）
- 背景约束：`.uk` 域名无法在国内 ICP 备案 → 不能接国内 CDN / 国内云 80/443；花生壳走 CNAME 绑定自定义域名（DNS only）可规避

## 目标架构

```
公网用户
   ↓ https://collectionofcreations.uk
花生壳国内节点（CNAME 绑定，DNS only，不走 CF 代理）
   ↓ 花生壳隧道
本机花生壳客户端 → 127.0.0.1:8080（Spring Boot 后端）
```

前端（Vite build 静态页）后续加第二条映射或由后端托管静态资源。

## 实施步骤（待办）

1. [ ] 注册花生壳账号（用户操作，手机号/邮箱）
2. [ ] 下载安装花生壳 Windows 客户端（本机）
3. [ ] 添加内网映射：`127.0.0.1:8080` → 花生壳分配域名（先免费版验证链路）
4. [ ] 开通自定义域名绑定（免费版不支持绑定自有域名，需付费，约 ¥6-10/月）
5. [ ] Cloudflare DNS：`collectionofcreations.uk` 与 `www` 加 CNAME → 花生壳域名，**代理状态关闭（灰云 DNS only）**
6. [ ] 验证国内公网访问 `https://collectionofcreations.uk/api/health` → 200
7. [ ] 前端接入（build 静态页 → 同端口托管或第二条映射）
8. [ ] 上线前安全项：后端 8080 加鉴权（当前裸奔公网）

## 备用与参考

- Cloudflare Tunnel 配置：`C:\Users\Administrator\.cloudflared\config.yml`（隧道 zaowuji，id `41839652-7eb3-4013-87b2-709fb8d735fd`）
- cloudflared 安装：`E:\program\cloudflared\cloudflared.exe`（2026.8.2）
- 开机自启：启动文件夹 `start_tunnel.vbs`（登录后自动拉起 CF 隧道，零管理员权限）
- 国内优选 IP 参考：`104.16.0.0` 段可通（hosts 指定可用，仅自用/演示）
- 服务商备选：cpolar（~¥99/年）、香港 VPS + frp（~¥30-50/月，长期最稳）
