/**
 * 接口请求签名 + 数据解码（前端反爬第一道防线）
 *
 * 设计目的：让数据只从"执行了本站 JS 的会话"流出——
 * 裸爬虫（requests/curl 直接抓接口）没有签名会被后端拒绝；
 * 能执行 JS 的爬虫也必须先逆向签名算法与编码规则，成本显著提高。
 *
 * 注意：前端没有绝对秘密，本方案是"抬门槛"而非绝对安全。
 * 生产环境应由后端配合：校验签名/时间戳防重放 + 接口鉴权 + 限流。
 */

// 密钥不明文出现，运行时拼装（改任一字符即全站签名失效，便于换版）
const PARTS = ['zao', 'wu', 'ji-2026', '-coc', '-web']
const SECRET = PARTS.join('')

/**
 * cyrb53 轻量哈希（同步、零依赖，演示级强度）
 * 生产建议：换 HMAC-SHA256（Web Crypto）并由后端验签
 */
export function cyrb53(str: string, seed = 0): string {
  let h1 = 0xdeadbeef ^ seed
  let h2 = 0x41c6ce57 ^ seed
  for (let i = 0; i < str.length; i++) {
    const ch = str.charCodeAt(i)
    h1 = Math.imul(h1 ^ ch, 2654435761)
    h2 = Math.imul(h2 ^ ch, 1597334677)
  }
  h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909)
  h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909)
  return (h2 >>> 0).toString(16).padStart(8, '0') + (h1 >>> 0).toString(16).padStart(8, '0')
}

export interface SignHeaders {
  'X-Timestamp': string
  'X-Nonce': string
  'X-Sign': string
}

/**
 * 为一次请求生成签名头：
 * X-Sign = hash( METHOD | url | timestamp | nonce | secret )
 * 后端按同样规则重算比对，并校验 timestamp 时效（如 ±5 分钟）防重放。
 */
export function signRequest(method: string, url: string): SignHeaders {
  const timestamp = Date.now().toString()
  const nonce = Math.random().toString(36).slice(2, 10)
  const raw = `${method.toUpperCase()}|${url}|${timestamp}|${nonce}|${SECRET}`
  return {
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-Sign': cyrb53(raw),
  }
}

/**
 * 解码后端编码传输的敏感数据：XOR(SECRET) + Base64
 * 后端返回 { payload: "<base64>" } 时，由拦截器调用还原为原 JSON
 */
export function decodePayload<T>(encoded: string): T {
  const bin = atob(encoded)
  const bytes = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) {
    bytes[i] = bin.charCodeAt(i) ^ SECRET.charCodeAt(i % SECRET.length)
  }
  return JSON.parse(new TextDecoder().decode(bytes)) as T
}
