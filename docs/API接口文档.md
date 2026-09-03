# 造物集官网 API 接口文档（总）

> 版本：v1.0 ｜ 更新：2026-09-03
> 后端：Spring Boot 3.5 + MyBatis + MySQL（Flyway 版本化）
> 适用：官网前端（front_end）、桌面激活工具（zaowuji-activator / coBrain 等客户端）、管理后台

---

## 一、Base URL

| 环境 | Base URL | 说明 |
|---|---|---|
| **生产** | `https://collectionofcreations.uk/api` | Cloudflare Tunnel → 本机 8080；前后端同源 |
| 本地后端 | `http://localhost:8080/api` | 直接联调后端 |
| 本地前端 dev | `http://localhost:5173/api` | Vite 代理转发到 8080（前端代码统一走相对路径 `/api`，由 `VITE_API_BASE` 控制，禁止硬编码 IP/域名） |

> 公网域名经 Cloudflare 提供 HTTPS；`/api/*` 之外的静态资源（网页、`/uploads/*` 上传文件）由前端静态服务（8081）提供。

## 二、通用约定

### 响应格式（所有接口统一）

```json
{ "code": 0, "data": { ... }, "message": "ok" }
```

- `code`：0 = 成功；非 0 = 业务错误（HTTP 仍为 200）。HTTP 4xx/5xx 时 body 为 `{ "code": …, "message": … }`
- 常见业务 code：`400` 参数/业务错误、`401` 未登录/令牌失效、`403` 无权限、`404` 资源不存在、`500` 服务器内部错误

### 金额单位

- 接口出入参均为**元**（小数）；数据库存储分为单位（内部换算）

### 机器码约定

- 客户端采集稳定硬件指纹（主板/磁盘序列号等）后取 SHA-256 摘要，64 位 hex 字符串
- 激活码与机器码绑定，无法转移；吊销后在线核验立即失败

## 三、公开接口（无需鉴权）

### 1. 健康检查
`GET /health` → `{ status: "UP" }`

### 2. 产品列表（官网在售）
`GET /products`

```json
{ "id": 1, "name": "coBrain", "code": "coBrain", "description": "…", "version": "1.0.0",
  "coverUrl": "/uploads/cover/x.png", "payQrUrl": "/uploads/qr/x.png",
  "downloadUrl": "/uploads/package/x.exe", "price": 199.0, "status": 1, "sort": 0 }
```

> **安装包直链** = `downloadUrl`（相对路径拼接 Base URL 的源，或绝对 URL）。桌面端下载：`{源}/uploads/package/…`。
> 产品若有更新版本，重新上传安装包并更新产品即可，直链保持稳定（文件名含随机串，建议存库后不换）。

### 3. 产品详情
`GET /products/{id}` → 同产品对象（含 `downloadUrl` / `payQrUrl`）

### 4. 产品收款码图片（桌面端展示用）
`GET /products/{id}/pay-qr`

- 返回图片二进制流（image/png 等）；未配置/不存在 → 404
- `payQrUrl` 为 `/uploads/...` 时直接回读文件流；为 http(s) 绝对地址时 302 跳转

### 5. 创建订单（购买请求）
`POST /orders`

```json
{ "productId": 1, "contact": "QQ/邮箱等联系方式", "remark": "可选备注" }
```

→ `{ "orderNo": "20260903120000000123", "status": 0, "amount": 199.0, … }`（status: 0 待支付）

> 订单创建后用户按产品收款码转账，**管理员在后台人工核验**后订单置为已支付（1），随后客户端才能激活。

### 6. 查询订单
`GET /orders/{orderNo}` → 订单详情（status：0 待支付 / 1 已支付可激活 / 4 已签发）

### 7. RSA 公钥（客户端本地验签用）
`GET /license-key/public-key` → `{ "algorithm": "SHA256withRSA", "pem": "-----BEGIN PUBLIC KEY-----…" }`

### 8. 提交激活（RSA 签发激活码，核心）
`POST /activations`

```json
{ "productId": 1, "machineCode": "<64位hex>", "orderNo": "20260903120000000123" }
```

- 前提：订单存在且已支付（人工核验通过）
- → `{ "licenseKey": "机器码哈希-产品ID", "sign": "<RSA签名 base64>", "licenseType": 1, "status": 1, "issuedAt": "…" }`
- 激活码结构/本地验签算法见 `LICENSE_GUIDE.md` 与 `docs/RSA与激活码接入文档.md`

### 9. 本机激活记录
`GET /activations?machineCode=<64位hex>` → 该机器码全部激活码（含吊销状态）

### 10. 在线核验激活码（防吊销，客户端启动时建议调用）
`GET /license-key/verify?code=<licenseKey>` → `{ "valid": true, "productId": 1, … }`（吊销/伪造返回 valid=false）

## 四、管理端接口（需鉴权）

鉴权：`Authorization: Bearer <token>`；token 由登录接口签发（HMAC 无状态，每次请求回库核验管理员状态）。前端缓存于 localStorage。

### 认证
| 接口 | 说明 |
|---|---|
| `POST /admin/login` | 登录 `{ username, password }` → `{ token, nickname, role }`（唯一公开的管理端接口） |
| `GET /admin/me` | 当前管理员信息（顺带校验令牌） |

### 订单
| 接口 | 说明 |
|---|---|
| `GET /admin/orders` | 订单列表（新→旧） |
| `POST /admin/orders/{orderNo}/review-pass` | 人工核验通过：待支付(0)→已支付(1)，驱动可签发 |

### 激活码
| 接口 | 说明 |
|---|---|
| `GET /admin/licenses` | 全部激活码签发记录（新→旧） |
| `POST /admin/licenses/{id}/revoke` | 吊销激活码（客户端在线核验立即失败） |

### 产品 CRUD
| 接口 | 说明 |
|---|---|
| `GET /admin/products` | 产品列表（含下架） |
| `POST /admin/products` | 新建（code 唯一；price 单位元） |
| `PUT /admin/products/{id}` | 全量更新 |
| `PATCH /admin/products/{id}/status` | 上下架 `{ "status": 0|1 }` |
| `DELETE /admin/products/{id}` | **删除**：存在订单/激活码关联时拒绝（建议改下架） |

产品字段（POST/PUT body）：`name`*、`code`*（字母数字_-，1-64）、`description`、`version`、`coverUrl`（封面）、`payQrUrl`（**收款码**）、`downloadUrl`（**安装包直链**）、`price`*（元）、`status`*（1上架 0下架）、`sort`

### 文件上传
`POST /admin/upload`（multipart/form-data：`file` + `kind`）

| kind | 类型白名单 | 存储目录 | 用途 |
|---|---|---|---|
| `cover` | png/jpg/jpeg/webp/gif/svg | `/uploads/cover/` | 产品封面图 |
| `qr` | png/jpg/jpeg/webp/gif/svg | `/uploads/qr/` | **产品收款码** |
| `package` | exe/msi/zip/7z | `/uploads/package/` | **安装包** |

→ `{ "url": "/uploads/cover/xxx.png" }`（相对路径，直接存产品字段；上限 2GB）
上传产物经 `/uploads/**` 静态访问（WebConfig 映射磁盘目录；前端静态服务 serve-dist 同样映射，前后端路径一致）。

### 其他
`POST /admin/…/` AI 客服代理：`POST /api/ai/chat`（DeepSeek 代理，官网悬浮窗用；密钥走环境变量 `DEEPSEEK_API_KEY`，未配置时返回不可用提示）

## 五、文档索引

- 激活码体系与 RSA 客户端接入（含 Python 验签实现）：`docs/RSA与激活码接入文档.md`
- RSA 原理 + 命令行签发/验签工具：`LICENSE_GUIDE.md`
- 业务模式（官网只下载 + 桌面端闭环授权）：`docs/激活码发售思路转变说明.md`
- 前端部署（Cloudflare Tunnel / serve-dist / /uploads 映射）：`front_end/DEPLOY.md`
