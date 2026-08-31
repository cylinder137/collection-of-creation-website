# -*- coding: utf-8 -*-
"""
造物集 · 激活码本地校验工具 (license-checker)

作用：读取「本机浏览器 localStorage 中的设备指纹/机器码」与「激活码文件 license.txt
中的激活码」进行比对，验证该激活码是否由当前浏览器机器码签发，并显示校验结果。

用法：
    python license_checker.py

交互菜单：
    1) 查看当前激活码      —— 显示 license.txt 内容
    2) 修改激活码          —— 输入新激活码，写回 license.txt
    3) 校验激活码          —— 扫描浏览器 localStorage，比对机器码哈希
    4) 退出

说明：
    - 浏览器 localStorage 存放在各浏览器 profile 的 LevelDB 文件中
      （明文存储，本工具直接扫描 key 提取，无需第三方依赖）
    - 默认扫描 Edge / Chrome 的默认用户目录，可用 --profile 指定其他 profile
    - 激活码格式：<机器码SHA-256(64位hex)>-<产品ID>
"""

import hashlib
import os
import re
import sys

# ---------------------------------------------------------------------------
# 配置
# ---------------------------------------------------------------------------

LICENSE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "license.txt")

# 默认扫描的浏览器 Local Storage 目录（Edge / Chrome）
DEFAULT_PROFILES = [
    # Edge
    os.path.expandvars(r"%LOCALAPPDATA%\Microsoft\Edge\User Data\Default\Local Storage\leveldb"),
    # Chrome
    os.path.expandvars(r"%LOCALAPPDATA%\Google\Chrome\User Data\Default\Local Storage\leveldb"),
]

# 要提取的 localStorage key
KEY_DEVICE_ID = b"zwj_device_id"
KEY_MACHINE_CODE = b"zwj_machine_code"

# ---------------------------------------------------------------------------
# 浏览器 localStorage 读取（LevelDB 明文扫描）
# ---------------------------------------------------------------------------


def _scan_files(dirs):
    """遍历目录下所有 .ldb / .log 文件，产出二进制内容"""
    for d in dirs:
        if not os.path.isdir(d):
            continue
        for name in os.listdir(d):
            if name.endswith((".ldb", ".log")):
                path = os.path.join(d, name)
                try:
                    with open(path, "rb") as f:
                        yield f.read()
                except (OSError, PermissionError):
                    continue


def _extract_after(data, key_bytes, pattern):
    """在 key 出现位置之后不远处查找 value（LevelDB 中 key 与 value 连续存储）"""
    results = []
    start = 0
    while True:
        idx = data.find(key_bytes, start)
        if idx == -1:
            break
        # key 后跟 8 字节 internal key（seq+type），再跟 value
        chunk = data[idx + len(key_bytes): idx + len(key_bytes) + 200]
        m = re.search(pattern, chunk)
        if m:
            results.append(m.group(1).decode("ascii", errors="ignore"))
        start = idx + len(key_bytes)
    return results


def read_browser_localstorage(profiles=None):
    """
    扫描浏览器 localStorage，返回 { 'device_id': str|None, 'machine_code': str|None }
    """
    dirs = profiles or DEFAULT_PROFILES
    device_ids, machine_codes = [], []
    for blob in _scan_files(dirs):
        device_ids += _extract_after(blob, KEY_DEVICE_ID, rb"([0-9a-fA-F-]{36})")
        machine_codes += _extract_after(blob, KEY_MACHINE_CODE, rb"([0-9a-fA-F]{64})")

    def _first(items):
        # 去重保序
        seen = set()
        for it in items:
            if it not in seen:
                seen.add(it)
                return it
        return None

    return {
        "device_id": _first(device_ids),
        "machine_code": _first(machine_codes),
    }


# ---------------------------------------------------------------------------
# 激活码读写
# ---------------------------------------------------------------------------


def read_license():
    """读取 license.txt 中的激活码（去空白，兼容 UTF-8 BOM）"""
    if not os.path.exists(LICENSE_FILE):
        return ""
    try:
        with open(LICENSE_FILE, "r", encoding="utf-8-sig") as f:
            return f.read().strip()
    except (OSError, UnicodeDecodeError):
        return ""


def write_license(code):
    """把激活码写入 license.txt"""
    with open(LICENSE_FILE, "w", encoding="utf-8") as f:
        f.write(code.strip() + "\n")


# ---------------------------------------------------------------------------
# 校验逻辑
# ---------------------------------------------------------------------------

LICENSE_PATTERN = re.compile(r"^([0-9a-fA-F]{64})-(\d+)$")


def verify(license_code, machine_code):
    """
    校验激活码与机器码是否匹配
    激活码 = sha256(机器码明文) + "-" + 产品ID
    返回 (ok: bool, msg: str)
    """
    if not license_code:
        return False, "激活码为空（请先录入激活码）"

    m = LICENSE_PATTERN.match(license_code)
    if not m:
        return False, "激活码格式不正确：应为 <64位SHA-256>-<产品ID>"

    hash_part, product_id = m.group(1), m.group(2)

    if not machine_code:
        return False, "未在浏览器 localStorage 中找到机器码（请先打开 http://localhost:5173 访问一次激活页）"

    expect = hashlib.sha256(machine_code.encode("utf-8")).hexdigest()
    if hash_part.lower() == expect:
        return True, f"校验通过：激活码由当前浏览器机器码签发（产品ID={product_id}）"
    return False, (
        f"校验失败：激活码绑定的机器码与当前浏览器不一致\n"
        f"  激活码哈希: {hash_part}\n"
        f"  本机哈希:   {expect}\n"
        f"  （激活码是哪个浏览器签发的，就要用哪个浏览器校验）"
    )


# ---------------------------------------------------------------------------
# 交互
# ---------------------------------------------------------------------------


def main():
    # Windows 控制台默认 GBK，强制 UTF-8 输出避免中文报错
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stdin.reconfigure(encoding="utf-8")
    except (AttributeError, ValueError):
        pass

    # 可选 --profile <目录>：指定浏览器 profile 的 Local Storage\leveldb 目录
    profiles = DEFAULT_PROFILES
    args = sys.argv[1:]
    if "--profile" in args:
        i = args.index("--profile")
        if i + 1 < len(args):
            profiles = [args[i + 1]]

    print("=" * 56)
    print("  造物集 · 激活码本地校验工具")
    print("=" * 56)

    # 启动时自动读取激活码并扫描浏览器
    license_code = read_license()
    ls = read_browser_localstorage(profiles)
    print(f"激活码文件 : {LICENSE_FILE}")
    print(f"当前激活码 : {license_code or '（空）'}")
    print(f"浏览器设备 : device_id={ls['device_id'] or '未找到'}  machine_code={'已找到' if ls['machine_code'] else '未找到'}")
    print()

    while True:
        print("-" * 56)
        print("1) 查看当前激活码")
        print("2) 修改激活码")
        print("3) 校验激活码")
        print("4) 退出")
        choice = input("请选择: ").strip()

        if choice == "1":
            print(f"当前激活码: {read_license() or '（空）'}")
        elif choice == "2":
            new_code = input("请输入新激活码: ").strip()
            if new_code:
                write_license(new_code)
                license_code = new_code
                print("已保存到 license.txt")
            else:
                print("输入为空，未修改")
        elif choice == "3":
            license_code = read_license()
            ls = read_browser_localstorage(profiles)
            ok, msg = verify(license_code, ls["machine_code"])
            print(("✅ " if ok else "❌ ") + msg)
        elif choice == "4":
            print("再见")
            break
        else:
            print("无效选项")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n已退出")
        sys.exit(0)
