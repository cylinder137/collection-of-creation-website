/**
 * 设备指纹机器码（前端自动获取，无需用户手动填写）
 *
 * 说明：浏览器沙箱拿不到主板/CPU 序列号等真实硬件信息，
 * 这里用「本地持久化设备 ID + 浏览器特征」生成稳定的设备指纹：
 * - 同一浏览器内保持一致（localStorage 持久化）
 * - 换浏览器 / 清站点数据后才会变化，属 Web 场景的标准做法
 *
 * 注：crypto.subtle 需要安全上下文（localhost / https），生产环境走 https 即可。
 */

const STORAGE_KEY = 'zwj_device_id'

function genId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  // 降级：手动生成 UUID v4
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

function getDeviceId(): string {
  try {
    let id = localStorage.getItem(STORAGE_KEY)
    if (!id) {
      id = genId()
      localStorage.setItem(STORAGE_KEY, id)
    }
    return id
  } catch {
    return genId() // localStorage 不可用（如隐私模式）时每次生成
  }
}

async function sha256Hex(text: string): Promise<string> {
  const data = new TextEncoder().encode(text)
  const buf = await crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(buf))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

/** 获取本机机器码（设备指纹），异步计算 */
export async function getMachineCode(): Promise<string> {
  const parts = [
    getDeviceId(),
    navigator.userAgent,
    navigator.language,
    `${screen.width}x${screen.height}`,
    String(screen.colorDepth),
    Intl.DateTimeFormat().resolvedOptions().timeZone,
  ]
  return sha256Hex(parts.join('|'))
}
