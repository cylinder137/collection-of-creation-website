#!/usr/bin/env node
/**
 * 前端页面快捷预览脚本（零依赖，Node 18+）
 *
 * 用法：
 *   node scripts/preview.mjs                 开发模式预览（Vite dev server，自动开浏览器）
 *   node scripts/preview.mjs --build         先生产构建，再预览构建产物（vite preview）
 *   node scripts/preview.mjs --port 8080     指定端口
 *   node scripts/preview.mjs --host          暴露到局域网（手机/同事访问）
 *   node scripts/preview.mjs --no-open       不自动打开浏览器
 *   node scripts/preview.mjs -h              查看帮助
 *
 * 也可通过 npm 脚本调用：npm run preview:quick -- --build
 */
import { spawn } from 'node:child_process'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '..') // front_end 目录
const viteBin = path.join(root, 'node_modules', 'vite', 'bin', 'vite.js')
const npmCmd = process.platform === 'win32' ? 'npm.cmd' : 'npm'

// ---------- 彩色输出 ----------
const color = {
  cyan: (s) => `\x1b[36m${s}\x1b[0m`,
  green: (s) => `\x1b[32m${s}\x1b[0m`,
  yellow: (s) => `\x1b[33m${s}\x1b[0m`,
  red: (s) => `\x1b[31m${s}\x1b[0m`,
  gray: (s) => `\x1b[90m${s}\x1b[0m`,
}
const log = (s) => console.log(`${color.cyan('[preview]')} ${s}`)
const ok = (s) => console.log(`${color.green('[preview]')} ${s}`)
const warn = (s) => console.log(`${color.yellow('[preview]')} ${s}`)
const fail = (s) => {
  console.error(`${color.red('[preview]')} ${s}`)
  process.exit(1)
}

// ---------- 参数解析 ----------
const opt = { build: false, open: true, host: false, port: undefined }
const argv = process.argv.slice(2)
for (let i = 0; i < argv.length; i++) {
  const a = argv[i]
  if (a === '--build') opt.build = true
  else if (a === '--no-open') opt.open = false
  else if (a === '--host') opt.host = true
  else if (a === '--port') {
    opt.port = argv[++i]
    if (!opt.port || !/^\d+$/.test(opt.port)) fail('--port 需要一个数字端口，例如 --port 8080')
  } else if (a === '-h' || a === '--help') {
    console.log(`
前端页面快捷预览脚本

  node scripts/preview.mjs [选项]

选项：
  --build       先执行生产构建（vue-tsc + vite build），再预览 dist 产物
  --port <n>    指定端口（默认 dev 5173 / preview 4173，被占用时自动顺延）
  --host        监听 0.0.0.0，允许局域网设备访问
  --no-open     不自动打开浏览器
  -h, --help    显示本帮助
`)
    process.exit(0)
  } else {
    fail(`未知参数：${a}（用 -h 查看帮助）`)
  }
}

// ---------- 子进程工具 ----------
function run(cmd, args, label) {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args, { cwd: root, stdio: 'inherit', shell: false })
    child.on('error', (e) => reject(new Error(`${label} 启动失败：${e.message}`)))
    child.on('close', (code) =>
      code === 0 ? resolve() : reject(new Error(`${label} 退出码 ${code}`))
    )
  })
}

// ---------- 主流程 ----------
const [major] = process.versions.node.split('.').map(Number)
if (major < 18) fail(`Node 版本过低（当前 ${process.version}），需要 Node 18+，建议 22.x`)

log(`项目目录：${color.gray(root)}`)

// 1. 依赖检查：node_modules 缺失则自动安装
if (!existsSync(path.join(root, 'node_modules'))) {
  warn('未检测到 node_modules，先执行 npm install …')
  await run(npmCmd, ['install'], 'npm install')
  ok('依赖安装完成')
}

// 2. vite 可用性检查
if (!existsSync(viteBin)) {
  fail('未找到 vite，请先在 front_end 目录执行 npm install')
}

// 3. 组装 vite 参数
const viteArgs = []
if (opt.port) viteArgs.push('--port', opt.port)
if (opt.host) viteArgs.push('--host')
if (opt.open) viteArgs.push('--open')

// 4. 启动
if (opt.build) {
  log(color.cyan('[1/2] 生产构建中（vue-tsc 类型检查 + vite build）…'))
  await run(npmCmd, ['run', 'build'], 'npm run build')
  ok('构建完成，产物在 dist/')
  log(color.cyan('[2/2] 启动产物预览服务器…'))
  log(`模式：${color.yellow('生产预览')}（Ctrl+C 停止）`)
  await run(process.execPath, [viteBin, 'preview', ...viteArgs], 'vite preview')
} else {
  log(`模式：${color.green('开发预览')}，改动即热更新（Ctrl+C 停止）`)
  log('页面：首页 / 产品 / 购买 / 激活码 / 管理后台（hash 路由，如 #/products）')
  await run(process.execPath, [viteBin, ...viteArgs], 'vite dev')
}
