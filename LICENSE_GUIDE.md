# RSA 激活码系统使用文档

> 适用于：coBrain 等产品的授权激活机制（服务端签发 / 客户端激活）。
> 配套代码：`tools/license_tool/`（Python，依赖 cryptography）

---

## 一、RSA 激活码运行逻辑概述

### 1. 为什么用 RSA

激活码要解决三个问题：**防伪造**（不能自己造码）、**防篡改**（不能改机器码/有效期）、**可离线验签**（客户端断网也能校验激活码真伪）。

RSA 非对称加密天然满足：**私钥**只留在服务端签发端，**公钥**随客户端分发。私钥签出来的码，只有公钥能验证通过；而公钥无法反推私钥，所以客户端永远伪造不出合法激活码。

### 2. 激活码结构

```
license_key = base64url(payload)  "."  base64url(signature)
```

| 部分 | 内容 |
|---|---|
| payload | JSON：`machine`（机器码 SHA-256 哈希）、`product`（产品编码）、`type`（1永久/2订阅）、`issued`（签发时间）、`expires`（过期时间，永久为 null）、`order_no`（订单号） |
| signature | RSA-SHA256（PKCS#1 v1.5）对 payload 字节的签名 |

### 3. 签发流程（服务端）

```
管理员发起签发
   │
   ├─ 客户端上报机器码 → 服务端算 SHA-256 哈希（只存哈希，不存明文硬件指纹）
   ├─ 组装 payload JSON
   ├─ 私钥对 payload 签名
   └─ 生成 license_key → 写入 license 表（license_key / machine_code / sign / 状态）
```

### 4. 激活/校验流程（客户端 & 服务端，必须一致）

```
收到 license_key
   │
   ├─ ① 解析 payload 与 signature
   ├─ ② 公钥验签（失败 → "激活码被篡改/非本系统签发"）
   ├─ ③ 比对 payload.machine == sha256(本机机器码)（不匹配 → "绑定设备不符"）
   ├─ ④ 检查状态（服务端查 license 表：未激活、未吊销、未过期）
   └─ ⑤ 全部通过 → 激活成功，写入激活时间
```

> ⚠️ **服务端激活接口严禁只按"激活码存在"放行**，必须完整执行 ②③④ 三步。

### 5. 安全约定

- 私钥（`*_private.pem`）**只保存在服务端**，严禁提交仓库、严禁随客户端分发
- 数据库 `machine_code` 列只存 SHA-256 哈希（本项目 schema.sql / V1__init.sql 已按此设计，长度 64）
- 手机号、支付回调报文等敏感字段加密存储，见 `back_end/sql/schema.sql` 注释

---

## 二、工具用法（命令行）

安装依赖（仅需一次）：

```bash
pip install -r tools/license_tool/requirements.txt
```

### 1. 生成密钥对（服务端操作）

```bash
python tools/license_tool/cli.py genkey --out keys --name license
# 生成 keys/license_private.pem（私钥，服务端自留）与 keys/license_public.pem（公钥，随客户端分发）
```

### 2. 获取本机机器码哈希

```bash
python tools/license_tool/cli.py machine
# 输出 64 位 hex，如 a9d481819eca244f4b6b7cdbb6f7af93e036edb0259470dbb4f38041b78ecd63
```

### 3. 签发激活码（服务端操作）

```bash
python tools/license_tool/cli.py issue \
  --key keys/license_private.pem \
  --machine a9d481...（上一步的机器码哈希）\
  --product coBrain \
  --type 1 \
  --order ZW20260828001
# 输出 license_key 字符串，写入 license 表
```

订阅授权加 `--type 2 --expires 2027-08-28T00:00:00+08:00`。

### 4. 验签（客户端/服务端通用）

```bash
python tools/license_tool/cli.py verify \
  --pub keys/license_public.pem \
  --license <激活码> \
  --machine <机器码哈希>   # 传了则一并比对绑定设备
```

---

## 三、与后端接口的对接（规划）

后端就绪后，激活码相关接口约定如下（当前尚未实现，实现时按此规范）：

| 接口 | 方法 | 说明 |
|---|---|---|
| `POST /api/admin/license/issue` | 管理员签发 | 入参：machine_code 哈希、product、type、expires、order_no；出参：license_key |
| `POST /api/license/activate` | 客户端激活 | 入参：license_key、machine_code 哈希；执行验签+绑机+状态校验后置为已激活 |
| `GET  /api/license/verify` | 在线校验 | 入参：license_key、machine_code 哈希；返回激活状态/有效期（建议客户端每次启动调用） |

数据库表 `license` 已就绪（含 `license_key` / `machine_code` / `sign` / `license_type` / `status` / 各时间字段），字段与 payload 一一对应。

---

## ⚠️ 四、当前环境状态（重要）

**目前没有正在运行的后端服务器。**

- 支付 API、内网穿透（Cloudflare 域名）尚未就绪，后端服务暂未部署到公网
- 前端/客户端程序员如需联调接口，**需要自行在本地编译构建后端程序和数据库**：

```bash
# 1. 准备 MySQL 8（本机需已安装并运行）
# 2. 设置数据库密码环境变量（密码由各自本机 MySQL 决定，不要问别人要密码）
#     PowerShell:  $env:DB_PASSWORD='你的MySQL密码'
#     CMD:         set DB_PASSWORD=你的MySQL密码
# 3. 启动后端（首次启动 Flyway 会自动建库建表，无需手动执行 SQL）
cd back_end
mvn spring-boot:run
# 4. 验证：GET http://localhost:8080/api/health → {"status":"UP"}
```

- 前端本地开发：`cd front_end && npm install --include=dev && npm run dev`（详见 `front_end/README.md`）

---

## 五、待更新事项

- [ ] 公网 IP / 域名就绪后，更新本文档：部署地址、线上签发接口地址、客户端在线校验地址
- [ ] 后端实现 issue / activate / verify 三个接口后，补充真实请求/响应示例
- [ ] coBrain 客户端接入公钥验签逻辑（可复用 `tools/license_tool/rsa_license.py` 的 `verify_license`）

---

*文档维护：Tinker / cylinder137（2026-08-28 创建）*
