#!/usr/bin/env node
/**
 * 前端产物静态服务器（零依赖，Node 18+）——用于生产部署
 *
 * 用法：
 *   node scripts/serve-dist.mjs                # 默认 8081 端口
 *   node scripts/serve-dist.mjs --port 8081
 *   node scripts/serve-dist.mjs --host         # 监听 0.0.0.0（Tunnel/局域网访问需要）
 *   node scripts/serve-dist.mjs --uploads <目录>  # 额外静态目录，映射 /uploads/**（默认 ../back_end/uploads）
 *
 * 说明：
 * - 服务 dist/ 目录（先执行 npm run build）
 * - /uploads/** 从上传目录读取（管理后台上传的封面图/安装包），不存在则 404（不做 SPA fallback）
 * - hash 路由（/#/...）天然适配静态服务；另带 index.html fallback 兜底
 * - assets/ 带内容哈希，长缓存；index.html 不缓存
 */
import { createServer } from 'node:http'
import { request as httpRequest } from 'node:http'
import { createReadStream } from 'node:fs'
import { stat } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '..')
const distDir = path.join(root, 'dist')
const defaultUploads = path.resolve(root, '..', 'back_end', 'uploads')

// ---------- 参数 ----------
const opt = { port: 8081, host: false, uploads: defaultUploads, apiProxy: null }
const argv = process.argv.slice(2)
for (let i = 0; i < argv.length; i++) {
  const a = argv[i]
  if (a === '--port') opt.port = Number(argv[++i]) || 8081
  else if (a === '--host') opt.host = true
  else if (a === '--uploads') opt.uploads = path.resolve(argv[++i] || defaultUploads)
  else if (a === '--api-proxy') opt.apiProxy = argv[++i] || 'http://127.0.0.1:8080'
  else if (a === '-h' || a === '--help') {
    console.log('node scripts/serve-dist.mjs [--port 8081] [--host] [--uploads <目录>] [--api-proxy <后端地址>]')
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
  // 安装包（浏览器按附件下载）
  '.exe': 'application/octet-stream',
  '.msi': 'application/octet-stream',
  '.zip': 'application/zip',
  '.7z': 'application/x-7z-compressed',
}

const color = {
  cyan: (s) => `\x1b[36m${s}\x1b[0m`,
  green: (s) => `\x1b[32m${s}\x1b[0m`,
  red: (s) => `\x1b[31m${s}\x1b[0m`,
  gray: (s) => `\x1b[90m${s}\x1b[0m`,
}
const log = (s) => console.log(`${color.cyan('[serve-dist]')} ${s}`)

/** 安全地把 baseDir 下的 urlPath 文件发给客户端；allowFallback 时文件缺失回退 baseDir/index.html
 *  流式发送 + 支持 Range（断点续传/安装包大文件友好） */
async function serveFile(req, res, baseDir, urlPath, allowFallback) {
  const safePath = path
    .normalize(urlPath)
    .replace(/^([/\\])+/, '')
    .replace(/^(\.\.[/\\])+/, '')
  let filePath = path.join(baseDir, safePath)
  if (!filePath.startsWith(baseDir)) {
    res.writeHead(403).end('Forbidden')
    return
  }

  let st
  try {
    st = await stat(filePath)
    if (!st.isFile()) throw new Error('not a file') // 目录/特殊文件按不存在处理
  } catch {
    if (allowFallback) {
      // SPA fallback：非资源请求回退到 index.html（hash 路由下主要起兜底作用）
      filePath = path.join(baseDir, 'index.html')
      try {
        st = await stat(filePath)
        if (!st.isFile()) throw new Error('not a file')
      } catch {
        res.writeHead(404).end('Not Found - dist 不存在，请先 npm run build')
        return
      }
    } else {
      res.writeHead(404).end('Not Found')
      return
    }
  }

  const ext = path.extname(filePath).toLowerCase()
  const isIndex = filePath.endsWith('index.html')
  const isHashedAsset = filePath.includes(`${path.sep}assets${path.sep}`)
  const isUpload = filePath.startsWith(opt.uploads)
  const headers = {
    'Content-Type': MIME[ext] || 'application/octet-stream',
    // 哈希产物长缓存；index.html 不缓存，保证发版即生效；上传文件短缓存便于更新后立即可见
    'Cache-Control': isIndex
      ? 'no-cache'
      : isHashedAsset
        ? 'public, max-age=31536000, immutable'
        : isUpload
          ? 'public, max-age=300'
          : 'public, max-age=3600',
    'Accept-Ranges': 'bytes',
  }

  // Range 支持（单段）
  const total = st.size
  let start = 0
  let end = total - 1
  let statusCode = 200
  const range = req.headers.range
  if (range) {
    const m = /bytes=(\d*)-(\d*)/.exec(range)
    if (m && (m[1] || m[2])) {
      if (m[1]) start = parseInt(m[1], 10)
      if (m[2]) end = Math.min(parseInt(m[2], 10), total - 1)
      if (start > end || start >= total) {
        res.writeHead(416, { 'Content-Range': `bytes */${total}` }).end()
        return
      }
      statusCode = 206
      headers['Content-Range'] = `bytes ${start}-${end}/${total}`
      headers['Content-Length'] = end - start + 1
    }
  } else {
    headers['Content-Length'] = total
  }

  res.writeHead(statusCode, headers)
  if (req.method === 'HEAD') {
    res.end()
    return
  }
  const stream = createReadStream(filePath, { start, end })
  // 客户端中途断开/读文件出错时静默收尾，绝不让错误冒泡崩溃进程
  stream.on('error', () => {
    if (!res.headersSent) res.writeHead(502).end('Read Error')
    else res.destroy()
  })
  res.on('close', () => stream.destroy())
  stream.pipe(res)
}

/**
 * /api/** 反向代理到后端（流式转发，支持 POST 大文件上传；供本地管理实例使用）
 * 例：node scripts/serve-dist.mjs --port 8082 --api-proxy http://127.0.0.1:8080
 */
function proxyApi(req, res) {
  let target
  try {
    target = new URL(opt.apiProxy)
  } catch {
    res.writeHead(500).end('Bad api-proxy config')
    return
  }
  const headers = { ...req.headers, host: target.host }
  // 去掉 hop-by-hop 头，避免转发歧义
  for (const h of ['connection', 'keep-alive', 'transfer-encoding', 'upgrade']) {
    delete headers[h]
  }
  const preq = httpRequest(
    {
      hostname: target.hostname,
      port: target.port || (target.protocol === 'https:' ? 443 : 80),
      path: req.url, // 保留原始 path + query
      method: req.method,
      headers,
    },
    (pres) => {
      res.writeHead(pres.statusCode, pres.headers)
      pres.pipe(res)
    },
  )
  preq.on('error', (e) => {
    res.writeHead(502).end('Bad Gateway: ' + e.message)
  })
  req.pipe(preq)
}

const server = createServer(async (req, res) => {
  try {
    // /api/** 代理到后端（存在 --api-proxy 时）
    const rawUrl = new URL(req.url, 'http://x')
    const urlPath = decodeURIComponent(rawUrl.pathname)
    if (opt.apiProxy && (urlPath === '/api' || urlPath.startsWith('/api/'))) {
      proxyApi(req, res)
      return
    }

    // 静态资源仅支持 GET/HEAD
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      res.writeHead(405, { Allow: 'GET, HEAD' }).end('Method Not Allowed')
      return
    }

    // /uploads/** -> 上传目录（封面图/安装包），不做 SPA fallback
    if (urlPath === '/uploads' || urlPath.startsWith('/uploads/')) {
      await serveFile(req, res, opt.uploads, urlPath.slice('/uploads'.length), false)
      return
    }

    await serveFile(req, res, distDir, urlPath, true)
  } catch (e) {
    // 响应头可能已发送（流式中途出错等），避免二次 writeHead 崩溃
    if (!res.headersSent) {
      res.writeHead(500).end('Internal Server Error')
    } else {
      res.destroy()
    }
    console.error(color.red(`[serve-dist] ${req.url} -> ${e.message}`))
  }
})

server.listen(opt.port, opt.host ? '0.0.0.0' : '127.0.0.1', () => {
  log(`目录：${color.gray(distDir)}`)
  log(`上传：${color.gray(opt.uploads)} (/uploads/**)`)
  log(`监听：${color.green(`http://${opt.host ? '0.0.0.0' : '127.0.0.1'}:${opt.port}`)}（Ctrl+C 停止）`)
  if (opt.apiProxy) {
    log(`API 代理：${color.gray(opt.apiProxy)} (/api/**)`)
  }
})
