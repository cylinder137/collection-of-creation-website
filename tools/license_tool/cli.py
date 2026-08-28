#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
造物集 RSA 激活码命令行工具
============================
用法：
  python cli.py genkey  [--out keys] [--name license]        生成密钥对
  python cli.py machine                                       打印本机机器码哈希
  python cli.py issue   --key keys/license_private.pem --machine <hash> \
                        --product coBrain [--type 1] [--expires 2027-08-28] \
                        [--order ZW20260828001]              签发激活码
  python cli.py verify  --pub keys/license_public.pem --license <code> \
                        [--machine <hash>]                   验签（可带机器码比对）

示例：
  python cli.py genkey --out keys
  python cli.py machine
  python cli.py issue --key keys/license_private.pem --machine <上一步输出> --product coBrain --order ZW20260828001
  python cli.py verify --pub keys/license_public.pem --license <上一步输出> --machine <机器码哈希>
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from rsa_license import (  # noqa: E402
    generate_keypair, get_machine_code, issue_license, load_private_key,
    load_public_key, save_keypair, verify_license,
)


def cmd_genkey(args: argparse.Namespace) -> None:
    priv, pub = generate_keypair()
    priv_path, pub_path = save_keypair(priv, pub, args.out, args.name)
    print(f"[OK] 私钥: {priv_path}")
    print(f"[OK] 公钥: {pub_path}")
    print("[!] 私钥仅保存在服务端，严禁提交到仓库/分发给客户端")


def cmd_machine(_: argparse.Namespace) -> None:
    print(get_machine_code())


def cmd_issue(args: argparse.Namespace) -> None:
    key = load_private_key(args.key)
    code = issue_license(
        key,
        machine_hash=args.machine,
        product=args.product,
        license_type=args.type,
        expires=args.expires,
        order_no=args.order,
    )
    print(code)


def cmd_verify(args: argparse.Namespace) -> None:
    pub = load_public_key(args.pub)
    try:
        payload = verify_license(pub, args.license, machine_hash=args.machine)
    except ValueError as e:
        print(f"[FAIL] {e}")
        sys.exit(1)
    print(f"[OK] 验签通过: {payload}")


def main() -> None:
    parser = argparse.ArgumentParser(description="造物集 RSA 激活码工具", prog="license_tool")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_gen = sub.add_parser("genkey", help="生成 RSA 密钥对")
    p_gen.add_argument("--out", default="keys", help="输出目录（默认 keys/）")
    p_gen.add_argument("--name", default="license", help="密钥文件名前缀（默认 license）")
    p_gen.set_defaults(func=cmd_genkey)

    sub.add_parser("machine", help="打印本机机器码哈希").set_defaults(func=cmd_machine)

    p_issue = sub.add_parser("issue", help="签发激活码")
    p_issue.add_argument("--key", required=True, help="私钥 PEM 路径")
    p_issue.add_argument("--machine", required=True, help="机器码 SHA-256 哈希")
    p_issue.add_argument("--product", required=True, help="产品编码（如 coBrain）")
    p_issue.add_argument("--type", type=int, default=1, choices=(1, 2), help="授权类型：1永久 2订阅（默认 1）")
    p_issue.add_argument("--expires", default=None, help="过期时间 ISO8601（如 2027-08-28T00:00:00+08:00，订阅必填）")
    p_issue.add_argument("--order", default=None, help="关联订单号")
    p_issue.set_defaults(func=cmd_issue)

    p_ver = sub.add_parser("verify", help="验签激活码")
    p_ver.add_argument("--pub", required=True, help="公钥 PEM 路径")
    p_ver.add_argument("--license", required=True, help="激活码内容")
    p_ver.add_argument("--machine", default=None, help="机器码 SHA-256 哈希（传了则一并比对绑定）")
    p_ver.set_defaults(func=cmd_verify)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
