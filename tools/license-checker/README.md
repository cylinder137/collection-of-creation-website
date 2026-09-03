# license-checker · 激活码本地校验工具

模拟客户端软件对激活码做本地校验：读取本机浏览器 localStorage 中的机器码（设备指纹），
与激活码文件中的激活码比对，判断该激活码是否由当前浏览器签发。

## 使用

```bash
python license_checker.py
```

启动后自动读取同目录 `license.txt`（激活码）并扫描浏览器 localStorage，交互菜单：
1. 查看当前激活码
2. 修改激活码（写回 license.txt）
3. 校验激活码（比对机器码哈希）
4. 退出

## 前置条件

- 先用浏览器打开过 `http://localhost:5173` 的激活页（前端会把机器码写入 localStorage 的
  `zwj_machine_code`）
- 默认扫描 Edge / Chrome 默认用户目录；其他浏览器/自定义 profile 用 `--profile <目录>` 指定

## 校验原理

激活码 = `sha256(机器码明文)` + `-` + 产品ID（服务端签发）。
工具从 localStorage 读出机器码明文 → 本地算 sha256 → 与激活码前缀比对。

## 注意

- localStorage 按「浏览器 × 域名」隔离：不同浏览器/不同 profile 的机器码不同，
  激活码绑定的是签发时那个浏览器，换浏览器校验会失败（这是预期行为）
- 本地哈希比对可断网进行；首次签发激活码需要联网（走后端接口）
