#!/usr/bin/env node
/**
 * 前端产物静态服务器（零依赖，Node 18+）——用于生产部署
 *
 * 用法：
 *   node scripts/serve-dist.mjs                # 默认 8081 端口
 *   node scripts/serve-dist.mjs --port 8081
 *   node scripts/serve-dist.mjs --host         # 监听 0.0.0.0（Tunnel/局域网访问需要）
 *
 * 说明：
 * - 服务 dist/ 目录（先执行 npm run build）
 * - hash 路由（/#/...）天然适配静态服务；另带 index.html fallback 兜底
 * - assets/ 带内容哈希，长缓存；index.html 不缓存
 */
import { createServer } from 'node:http'
import { readFile, stat } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '..')
const distDir = path.join(root, 'dist')

// ---------- 参数 ----------
const opt = { port: 8081, host: false }
const argv = process.argv.slice(2)
for (let i = 0; i < argv.length; i++) {
  const a = argv[i]
  if (a === '--port') opt.port = Number(argv[++i]) || 8081
  else if (a === '--host') opt.host = true
  else if (a === '-h' || a === '--help') {
    console.log('node scripts/serve-dist.mjs [--port 8081] [--host]')
    process.exit(0)
  }
}

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.webp': 'image/webp',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.txt': 'text/plain; charset=utf-8',
  '.map': 'application/json',
}

const color = {
  cyan: (s) => `\x1b[36m${s}\x1b[0m`,
  green: (s) => `\x1b[32m${s}\x1b[0m`,
  red: (s) => `\x1b[31m${s}\x1b[0m`,
  gray: (s) => `\x1b[90m${s}\x1b[0m`,
}
const log = (s) => console.log(`${color.cyan('[serve-dist]')} ${s}`)

async function fileExists(p) {
  try {
    const s = await stat(p)
    return s.isFile()
  } catch {
    return false
  }
}

const server = createServer(async (req, res) => {
  try {
    // 仅支持 GET/HEAD
    const urlPath = decodeURIComponent(new URL(req.url, 'http://x').pathname)
    // 防目录穿越
    const safePath = path
      .normalize(urlPath)
      .replace(/^([/\\])+/, '')
      .replace(/^(\.\.[/\\])+/, '')
    let filePath = path.join(distDir, safePath)
    if (!filePath.startsWith(distDir)) {
      res.writeHead(403).end('Forbidden')
      return
    }

    if (!(await fileExists(filePath))) {
      // SPA fallback：非资源请求回退到 index.html（hash 路由下主要起兜底作用）
      filePath = path.join(distDir, 'index.html')
      if (!(await fileExists(filePath))) {
        res.writeHead(404).end('Not Found - dist 不存在，请先 npm run build')
        return
      }
    }

    const ext = path.extname(filePath).toLowerCase()
    const isIndex = filePath.endsWith('index.html')
    const isHashedAsset = filePath.includes(`${path.sep}assets${path.sep}`)
    res.writeHead(200, {
      'Content-Type': MIME[ext] || 'application/octet-stream',
      // 哈希产物长缓存；index.html 不缓存，保证发版即生效
      'Cache-Control': isIndex
        ? 'no-cache'
        : isHashedAsset
          ? 'public, max-age=31536000, immutable'
          : 'public, max-age=3600',
    })
    const body = await readFile(filePath)
    res.end(req.method === 'HEAD' ? undefined : body)
  } catch (e) {
    res.writeHead(500).end('Internal Server Error')
    console.error(color.red(`[serve-dist] ${req.url} -> ${e.message}`))
  }
})

server.listen(opt.port, opt.host ? '0.0.0.0' : '127.0.0.1', () => {
  log(`目录：${color.gray(distDir)}`)
  log(`监听：${color.green(`http://${opt.host ? '0.0.0.0' : '127.0.0.1'}:${opt.port}`)}（Ctrl+C 停止）`)
})
