# 激活码体系与 RSA 接入文档

> 面向对象：coBrain（及其他造物集产品）桌面客户端 / 安装程序开发者
> 后端基地址：`https://collentionofcreation.uk/api`（下文简称 `BASE`）
> 文档版本：2026-09-02 · 离谱人 & 造物集后端

---

## 0. 业务模式总览

2026-09 起激活码发售逻辑全面切换为**客户端驱动**模式：

```
┌─────────┐  1. 下载 exe 安装包   ┌──────────┐
│  官网    │ ───────────────────→ │ 用户主机  │
│ (纯展示) │                      │ 安装程序  │
└─────────┘                      └────┬─────┘
                                      │ 2. 提权读取硬件指纹 → 生成机器码
                                      │ 3. 创建订单（购买请求）
                                      ▼
┌─────────────────────────────────────────────────┐
│ 后端                                             │
│  下单(待人工核验) → 管理员确认收款 → 已支付        │
│  客户端提交机器码 → 服务端 RSA 私钥签发激活码       │
└────┬────────────────────────────────────────────┘
     │ 4. 返回 license_key + sign
     ▼
用户主机：安装时 / 每次启动时用公钥本地验签（可选在线核验）
```

要点：

- **官网零发售**：官网只提供产品展示与 exe 自解压安装包下载，不售卖、不展示激活码。
- **激活发生在用户主机**：安装程序以管理员权限运行，可直接读取主板/CPU/磁盘序列号等硬件指纹，不受浏览器沙箱限制。
- **激活码绑定机器**：license_key 由机器码哈希派生，换机即失效，无法分享给他人。
- **产品启动二次校验**：安装时激活一次；产品每次启动时本地验签（离线可用），并可定期调用在线核验接口检查吊销状态。

---

## 1. RSA 密钥体系

| 项 | 值 |
|---|---|
| 算法 | RSA 2048 |
| 签名算法 | **SHA256withRSA** |
| 签名内容 | `license_key` 原文（UTF-8 字节） |
| 签名编码 | Base64 **URL-Safe**（`-`/`_`，无 `=` 填充） |
| 私钥 | `back_end/cert/rsa_private.pem`（PKCS#8，仅服务端持有，**严禁外发/入库**，已在 .gitignore） |
| 公钥 | `back_end/cert/rsa_public.pem`（X.509/SPKI），可随安装包分发或经接口拉取 |

- 密钥对由后端首次启动时自动生成并持久化于 `cert/` 目录，重启复用，**不会轮换**（轮换将导致历史激活码全部失效）。
- 私钥仅用于服务端签发；公钥可公开。客户端验签完全离线可做。

### 获取公钥

```
GET {BASE}/license-key/public-key
```

响应：

```json
{
  "code": 0,
  "data": {
    "pem": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqh...\n-----END PUBLIC KEY-----\n",
    "algorithm": "SHA256withRSA"
  },
  "message": "ok"
}
```

> 建议：把公钥直接编译进安装包/客户端（离线验签），同时保留此接口用于密钥万一更换时热更新。

---

## 2. 机器码规范

客户端在本机（建议以管理员权限）采集硬件指纹，拼接后做 SHA-256：

```
machine_code = SHA256( "{CPU序列号}|{主板序列号}|{系统盘卷序列号}" )   // 64 位小写 hex
```

推荐拼接字段（按稳定性排序，任选 2-3 项，**必须与 coBrain 客户端实现保持一致**）：

| 字段 | Windows 获取方式 |
|---|---|
| CPU 序列号 | `wmic cpu get ProcessorId` |
| 主板序列号 | `wmic baseboard get SerialNumber` |
| 系统盘卷序列号 | `wmic logicaldisk where "DeviceID='C:'" get VolumeSerialNumber` |

> ⚠️ 后端**只存机器码的 SHA-256 哈希**（machine_hash），明文不落库。因此客户端发给后端的 `machineCode` 就是上述明文 machine_code；后端自行做哈希。**同一台机器每次采集结果必须稳定**——安装程序与产品本体务必使用同一套采集逻辑（建议封装成同一 DLL/模块复用）。

---

## 3. 购买与激活全流程接口

### 3.1 查询产品（可选，客户端可硬编码 productId）

```
GET {BASE}/products
```

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "name": "coBrain",
      "code": "coBrain",
      "description": "AI 视觉笔记白板编辑器",
      "version": "0.2.0",
      "coverUrl": null,
      "downloadUrl": "https://…/coBrain-setup.exe",
      "price": 199.00,
      "status": 1,
      "sort": 0
    }
  ],
  "message": "ok"
}
```

### 3.2 创建购买请求（下单）

```
POST {BASE}/orders
Content-Type: application/json

{ "productId": 1, "contact": "13800000000" }
```

- `contact`：手机/邮箱，作为用户标识（当前无账号体系），**必填**，管理员靠它人工核对转账。
- 返回 `orderNo` 后引导用户扫码转账（收款码由客户端内置或展示）。

```json
{
  "code": 0,
  "data": {
    "id": 3,
    "orderNo": "20260902143000123",
    "productId": 1,
    "productName": "coBrain",
    "amount": 199.00,
    "status": 0,
    "paidAt": null,
    "createdAt": "2026-09-02T14:30:00"
  },
  "message": "ok"
}
```

订单状态机：`0 待人工核验 → 1 已支付 → 4 已签发`（`2 已取消`、`3 已退款` 为终态）。

### 3.3 轮询订单状态（可选，改善体验）

```
GET {BASE}/orders/{orderNo}
```

管理员确认收款后 `status` 变为 `1`，此时即可调用 3.4 激活。

### 3.4 提交机器码，签发激活码（核心接口）

```
POST {BASE}/activations
Content-Type: application/json

{ "productId": 1, "machineCode": "<64位hex机器码>", "orderNo": "20260902143000123" }
```

成功响应：

```json
{
  "code": 0,
  "data": {
    "id": 5,
    "code": "a3f5…(机器码哈希64位)-1",
    "sign": "TlMS0kRrZ0d…(base64url 签名)",
    "productId": 1,
    "productName": "coBrain",
    "machineCode": "<原样返回>",
    "status": 0,
    "createdAt": "2026-09-02T14:35:00"
  },
  "message": "ok"
}
```

行为约定：

- **幂等**：同一产品 + 同一机器码重复调用直接返回已有激活码。
- 订单校验：`orderNo` 必须存在、产品匹配、且 `status ≥ 1`（已支付）；待核验(0)会返回错误提示等待管理员确认。
- 激活码签发成功后订单自动置为 `4 已签发`。
- **客户端必须持久化保存 `code` 与 `sign`**（如写注册表 `HKLM\SOFTWARE\ZaowuJi\coBrain` 或安装目录受保护文件），安装程序与产品本体共用。

### 3.5 查询本机激活记录

```
GET {BASE}/activations?machineCode=<64位hex机器码>
```

返回该机器所有产品的激活码列表（重装系统后找回激活码用）。

### 3.6 在线核验（防吊销/防伪，可选但建议产品启动时调用）

```
GET {BASE}/license-key/verify?code=<激活码>&sign=<签名>&machineCode=<机器码>
```

```json
{ "code": 0, "data": { "valid": true, "productId": 1, "status": 1 }, "message": "ok" }
```

`valid=false` 时 `reason` 说明原因（机器码不匹配 / 签名无效 / 不存在 / 已吊销）。网络不可达时降级为仅本地验签。

---

## 4. 本地验签（客户端实现）

验签逻辑：`SHA256withRSA_verify(public_key, license_key_bytes, base64url_decode(sign))`

### Python（coBrain / customtkinter 客户端可直接用）

```python
import base64, hashlib
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding

PUBLIC_KEY_PEM = b"""-----BEGIN PUBLIC KEY-----
...（从 /license-key/public-key 拉取或内置）
-----END PUBLIC KEY-----"""

def get_machine_code(cpu_id: str, board_sn: str, disk_sn: str) -> str:
    """与安装程序保持完全一致的拼接顺序！"""
    raw = f"{cpu_id}|{board_sn}|{disk_sn}"
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()

def verify_license(license_key: str, sign_b64url: str) -> bool:
    pub = serialization.load_pem_public_key(PUBLIC_KEY_PEM)
    # base64url 无填充 → 补齐 padding
    pad = "=" * (-len(sign_b64url) % 4)
    sig = base64.urlsafe_b64decode(sign_b64url + pad)
    try:
        pub.verify(
            sig,
            license_key.encode("utf-8"),
            padding.PKCS1v15(),
            hashes.SHA256(),
        )
        return True
    except Exception:
        return False

# 启动校验三连：本地格式 → 本地验签 → (可选)在线核验
def check_at_startup(license_key, sign, machine_code) -> bool:
    if not license_key.startswith(f"{hashlib.sha256(machine_code.encode()).hexdigest()}-"):
        return False                      # 激活码与本机不符
    if not verify_license(license_key, sign):
        return False                      # 签名被篡改/伪造
    return True
```

### C#（安装程序 / Inno Setup 可通过自定义 DLL 调用）

```csharp
using System.Security.Cryptography;

static bool VerifyLicense(string publicKeyPem, string licenseKey, string signB64Url)
{
    var pub = RSA.Create();
    pub.ImportFromPem(publicKeyPem);
    byte[] sig = Convert.FromBase64String(signB64Url
        .Replace('-', '+').Replace('_', '/')
        .PadRight((signB64Url.Length + 3) / 4 * 4, '='));
    return pub.VerifyData(
        System.Text.Encoding.UTF8.GetBytes(licenseKey),
        sig, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1);
}
```

> Base64 URL-Safe 与标准 Base64 的差异只有 `+→-`、`/→_`、去掉 `=` 填充，转换后再解码即可。

---

## 5. 统一响应与错误码

所有接口返回 `{ "code": number, "data": any, "message": string }`；`code = 0` 成功。

| code / HTTP | 含义 | 客户端处理建议 |
|---|---|---|
| 0 | 成功 | — |
| 400 | 业务错误（参数/状态不满足） | 读 `message` 展示给用户 |
| 401 | 未登录/令牌失效（仅管理端） | — |
| 404 | 资源不存在 | 检查 orderNo / productId |
| 500 | 服务器内部错误 | 提示稍后重试 |

常见业务错误 `message`：

- `产品已下架` → productId 失效
- `订单不存在：xxx`
- `订单待人工审核，管理员确认收款后即可激活` → 引导用户等待，轮询 3.3
- `订单与所选产品不匹配`
- `产品编码已存在`（仅管理端）

---

## 6. 管理后台接口（供运维/联调参考）

管理端为**无状态 RESTful**，登录换令牌后每次请求带 `Authorization: Bearer <token>`，后端逐次验签并回库核验账号状态（12 小时有效期）。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin/login` | 登录（公开），body `{username, password}` |
| GET | `/admin/me` | 当前管理员信息 |
| GET | `/admin/orders` | 订单列表 |
| POST | `/admin/orders/{orderNo}/review-pass` | 人工核验收款 |
| GET | `/admin/licenses` | 激活码记录 |
| POST | `/admin/licenses/{id}/revoke` | 吊销激活码 |
| GET | `/admin/products` | 产品列表（含下架） |
| POST | `/admin/products` | 新建产品（price 单位元） |
| PUT | `/admin/products/{id}` | 更新产品 |
| PATCH | `/admin/products/{id}/status` | 上下架 `{status: 0\|1}` |

---

## 7. coBrain 接入 Checklist

- [ ] 确认机器码采集函数（安装程序与主程序共用同一实现）
- [ ] 安装向导中嵌入：下单 → 展示收款码 → 轮询订单 → 激活
- [ ] 持久化 `license_key` + `sign`（建议 HKLM 注册表 + 安装目录双写）
- [ ] 主程序启动三连校验（格式 / 验签 / 在线核验）
- [ ] 内置 RSA 公钥，保留接口热更新通道
- [ ] 处理 400/404/网络超时的用户提示文案
