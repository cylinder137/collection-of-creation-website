#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
造物集 RSA 激活码核心库
========================
激活码签发 / 验签 / 编解码 / 机器码采集，纯 Python 标准库 + cryptography。

激活码格式（与数据库 license 表对应）：
    license_key = base64url(payload)  "."  base64url(signature)

    payload   = JSON 字符串（UTF-8），字段：
        machine  机器码 SHA-256 哈希（hex，64 字符）——服务端/客户端只存哈希，不存明文硬件指纹
        product  产品编码（如 coBrain）
        type     授权类型：1 永久 / 2 订阅
        issued   签发时间 ISO8601
        expires  过期时间 ISO8601（永久授权为 null）
        order_no 关联订单号（可空）

    signature = RSA-SHA256（PKCS#1 v1.5）对 payload 字节的签名，私钥签发

验签逻辑（服务端激活接口 & 客户端启动校验必须一致）：
    1. 解析 license_key，取 payload 与 signature
    2. 用公钥验签：signature 必须是私钥对 payload 的合法签名
    3. 比对 payload.machine == sha256(客户端提交的机器码)
    4. 检查状态/有效期（服务端查 license 表 status + expires_at）
    5. 全部通过才允许激活/运行；任何一步失败即拒绝

⚠️ 安全约定：
    - 私钥只保存在服务端（签发端），严禁进入客户端/前端仓库
    - 公钥随客户端分发，仅用于验签，无法伪造激活码
    - 数据库 machine_code 列只存哈希（本项目 schema 已按此设计）
"""
from __future__ import annotations

import base64
import hashlib
import json
import platform
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives.asymmetric.rsa import RSAPrivateKey, RSAPublicKey

# ---------------------------------------------------------------------------
# 密钥管理
# ---------------------------------------------------------------------------

def generate_keypair(key_size: int = 2048) -> tuple[RSAPrivateKey, RSAPublicKey]:
    """生成 RSA 密钥对（默认 2048 位）。"""
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=key_size)
    return private_key, private_key.public_key()


def save_keypair(private_key: RSAPrivateKey, public_key: RSAPublicKey,
                 out_dir: str | Path, name: str = "license") -> tuple[Path, Path]:
    """保存 PEM 密钥对，返回 (私钥路径, 公钥路径)。"""
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    priv_path = out_dir / f"{name}_private.pem"
    pub_path = out_dir / f"{name}_public.pem"
    priv_path.write_bytes(private_key.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption(),
    ))
    pub_path.write_bytes(public_key.public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo,
    ))
    return priv_path, pub_path


def load_private_key(path: str | Path) -> RSAPrivateKey:
    return serialization.load_pem_private_key(Path(path).read_bytes(), password=None)


def load_public_key(path: str | Path) -> RSAPublicKey:
    return serialization.load_pem_public_key(Path(path).read_bytes())


# ---------------------------------------------------------------------------
# 机器码采集（设备指纹 → SHA-256 哈希）
# ---------------------------------------------------------------------------

def _ps(cmd: list[str]) -> str:
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
        return r.stdout.strip()
    except Exception:
        return ""


def get_machine_code() -> str:
    """采集本机硬件指纹并返回 SHA-256 哈希（hex）。

    Windows：CPU ProcessorId + 主板 SerialNumber + 磁盘 SerialNumber
    其他平台：MAC 地址 + 主机名（尽力而为，可自行扩展）
    """
    raw_parts: list[str] = []
    if sys.platform == "win32":
        ps_cmd = (
            "Get-CimInstance Win32_Processor | Select-Object -ExpandProperty ProcessorId;"
            "Get-CimInstance Win32_BaseBoard | Select-Object -ExpandProperty SerialNumber;"
            "Get-CimInstance Win32_DiskDrive | Select-Object -First 1 -ExpandProperty SerialNumber"
        )
        out = _ps(["powershell", "-NoProfile", "-Command", ps_cmd])
        raw_parts = [line.strip() for line in out.splitlines() if line.strip()]
    else:
        raw_parts = [str(platform.node()), str(platform.machine())]
        mid = Path("/etc/machine-id")
        if mid.exists():
            raw_parts.append(mid.read_text().strip())

    raw = "|".join(raw_parts) if raw_parts else "unknown"
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


# ---------------------------------------------------------------------------
# 激活码签发 / 验签
# ---------------------------------------------------------------------------

def _b64e(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _b64d(s: str) -> bytes:
    return base64.urlsafe_b64decode(s + "=" * (-len(s) % 4))


def build_payload(machine_hash: str, product: str, license_type: int = 1,
                  expires: Optional[str] = None, order_no: Optional[str] = None,
                  issued: Optional[str] = None) -> bytes:
    """构造激活码 payload（JSON 字节）。expires 传 ISO8601 字符串或 None（永久）。"""
    payload = {
        "machine": machine_hash,
        "product": product,
        "type": license_type,
        "issued": issued or datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "expires": expires,
        "order_no": order_no,
    }
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def issue_license(private_key: RSAPrivateKey, machine_hash: str, product: str,
                  license_type: int = 1, expires: Optional[str] = None,
                  order_no: Optional[str] = None) -> str:
    """签发激活码：payload + RSA-SHA256 签名，返回 license_key 字符串。

    参数 machine_hash 必须是 get_machine_code() 输出的 SHA-256 哈希。
    """
    payload = build_payload(machine_hash, product, license_type, expires, order_no)
    signature = private_key.sign(payload, padding.PKCS1v15(), hashes.SHA256())
    return f"{_b64e(payload)}.{_b64e(signature)}"


def parse_license(license_key: str) -> tuple[dict, bytes]:
    """解析激活码 → (payload_dict, signature_bytes)。格式非法抛 ValueError。"""
    try:
        payload_b64, sign_b64 = license_key.split(".", 1)
        payload = json.loads(_b64d(payload_b64))
        signature = _b64d(sign_b64)
    except Exception as e:  # noqa: BLE001
        raise ValueError(f"激活码格式非法: {e}") from e
    return payload, signature


def verify_license(public_key: RSAPublicKey, license_key: str,
                   machine_hash: Optional[str] = None) -> dict:
    """验签激活码。

    返回 payload dict；验签失败 / 机器码不匹配抛 ValueError。
    machine_hash 传 None 时只验签不比对机器码（不推荐用于激活流程）。
    """
    payload, signature = parse_license(license_key)
    payload_bytes = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    try:
        public_key.verify(signature, payload_bytes, padding.PKCS1v15(), hashes.SHA256())
    except Exception as e:  # noqa: BLE001
        raise ValueError(f"验签失败（激活码被篡改或非本系统签发）: {e}") from e
    if machine_hash is not None and payload.get("machine") != machine_hash:
        raise ValueError("激活码绑定的机器码与当前设备不匹配")
    return payload
