/**
 * 设备指纹机器码（前端自动获取，无需用户手动填写）
 *
 * 存储策略（优先级从高到低）：
 * 1. COC 共享文件夹（File System Access API，Chrome/Edge）
 *    - 用户授权一次后，在所选目录下创建 COC/device.json
 *    - 多个浏览器（Chrome/Edge）各授权一次，共用同一文件 → 机器码跨浏览器一致
 *    - 机器码 = sha256(设备ID)，与浏览器特征解耦，保证跨浏览器相同
 * 2. localStorage（兜底）：用户不授权共享文件夹时使用
 *
 * 说明：浏览器沙箱无法自动创建本地文件夹，必须用户点一次授权；
 * 授权后浏览器会记住（IndexedDB 存目录句柄），后续访问自动读写。
 * Firefox/Safari 不支持 File System Access API，只能走 localStorage。
 */

const LS_DEVICE_KEY = 'zwj_device_id'
const LS_MACHINE_KEY = 'zwj_machine_code'

const IDB_DB = 'coc-device'
const IDB_STORE = 'handles'
const IDB_HANDLE_KEY = 'coc-dir'

const COC_DIR = 'COC'
const DEVICE_FILE = 'device.json'

// ---------------------------------------------------------------------------
// IndexedDB：持久化目录句柄（FileSystemDirectoryHandle 只能存 IndexedDB）
// ---------------------------------------------------------------------------

function idbOpen(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(IDB_DB, 1)
    req.onupgradeneeded = () => {
      req.result.createObjectStore(IDB_STORE)
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

async function idbSaveHandle(handle: FileSystemDirectoryHandle): Promise<void> {
  const db = await idbOpen()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IDB_STORE, 'readwrite')
    tx.objectStore(IDB_STORE).put(handle, IDB_HANDLE_KEY)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

async function idbGetHandle(): Promise<FileSystemDirectoryHandle | null> {
  try {
    const db = await idbOpen()
    return new Promise((resolve) => {
      const tx = db.transaction(IDB_STORE, 'readonly')
      const req = tx.objectStore(IDB_STORE).get(IDB_HANDLE_KEY)
      req.onsuccess = () => resolve((req.result as FileSystemDirectoryHandle) ?? null)
      req.onerror = () => resolve(null)
    })
  } catch {
    return null
  }
}

// ---------------------------------------------------------------------------
// COC 共享文件夹读写
// ---------------------------------------------------------------------------

interface SharedDevice {
  deviceId: string
  machineCode: string
}

async function readSharedDevice(cocHandle: FileSystemDirectoryHandle): Promise<SharedDevice | null> {
  try {
    const fh = await cocHandle.getFileHandle(DEVICE_FILE)
    const file = await fh.getFile()
    const text = await file.text()
    const data = JSON.parse(text)
    if (data && typeof data.deviceId === 'string' && typeof data.machineCode === 'string') {
      return data as SharedDevice
    }
  } catch {
    /* 文件不存在或损坏 */
  }
  return null
}

async function writeSharedDevice(cocHandle: FileSystemDirectoryHandle, data: SharedDevice): Promise<void> {
  const fh = await cocHandle.getFileHandle(DEVICE_FILE, { create: true })
  const w = await fh.createWritable()
  await w.write(JSON.stringify(data))
  await w.close()
}

/** 从 IndexedDB 取句柄并确认读写权限 */
async function getUsableHandle(): Promise<FileSystemDirectoryHandle | null> {
  const handle = await idbGetHandle()
  if (!handle) return null
  try {
    const perm = await handle.queryPermission({ mode: 'readwrite' })
    if (perm === 'granted') return handle
    if (perm === 'prompt') {
      const granted = await handle.requestPermission({ mode: 'readwrite' })
      if (granted === 'granted') return handle
    }
  } catch {
    /* 权限查询失败按不可用处理 */
  }
  return null
}

// ---------------------------------------------------------------------------
// 工具函数
// ---------------------------------------------------------------------------

function genId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

async function sha256Hex(text: string): Promise<string> {
  const data = new TextEncoder().encode(text)
  const buf = await crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(buf))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

/** 机器码 = sha256(设备ID)，与浏览器特征解耦 → 跨浏览器一致 */
async function machineCodeOf(deviceId: string): Promise<string> {
  return sha256Hex(`coc:${deviceId}`)
}

function lsGet(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function lsSet(key: string, value: string): void {
  try {
    localStorage.setItem(key, value)
  } catch {
    /* 忽略 */
  }
}

function getOrCreateDeviceId(): string {
  const existing = lsGet(LS_DEVICE_KEY)
  if (existing) return existing
  const id = genId()
  lsSet(LS_DEVICE_KEY, id)
  return id
}

// ---------------------------------------------------------------------------
// 对外接口
// ---------------------------------------------------------------------------

/**
 * 授权 COC 共享文件夹：用户选一个父目录，自动创建 COC/device.json
 * 返回当前生效的设备信息（所有浏览器共用同一份）
 */
export async function setupSharedFolder(): Promise<{
  ok: boolean
  deviceId?: string
  machineCode?: string
  message: string
}> {
  if (typeof window === 'undefined' || !('showDirectoryPicker' in window)) {
    return { ok: false, message: '当前浏览器不支持共享文件夹（需 Chrome/Edge）' }
  }
  try {
    const dirHandle = await (window as any).showDirectoryPicker({ mode: 'readwrite' })
    const cocHandle = await dirHandle.getDirectoryHandle(COC_DIR, { create: true })

    // 已有共享设备则复用，否则用当前设备ID生成并写入
    let shared = await readSharedDevice(cocHandle)
    if (!shared) {
      const deviceId = getOrCreateDeviceId()
      shared = { deviceId, machineCode: await machineCodeOf(deviceId) }
      await writeSharedDevice(cocHandle, shared)
    }

    // 同步到 localStorage 缓存（供校验工具读取），并持久化句柄
    lsSet(LS_DEVICE_KEY, shared.deviceId)
    lsSet(LS_MACHINE_KEY, shared.machineCode)
    await idbSaveHandle(cocHandle)

    return { ok: true, deviceId: shared.deviceId, machineCode: shared.machineCode, message: '共享文件夹已生效' }
  } catch (e: any) {
    // 用户取消选择或写入失败
    return { ok: false, message: e?.name === 'AbortError' ? '已取消授权' : `授权失败：${e?.message ?? e}` }
  }
}

/** 当前存储模式：shared=COC共享文件夹 local=浏览器本地 none=未生成 */
export async function getStorageMode(): Promise<'shared' | 'local' | 'none'> {
  const handle = await getUsableHandle()
  if (handle) {
    const coc = await handle.getDirectoryHandle(COC_DIR, { create: false }).catch(() => null)
    if (coc) return 'shared'
  }
  return lsGet(LS_MACHINE_KEY) ? 'local' : 'none'
}

/**
 * 获取本机机器码：COC 共享文件夹优先，localStorage 兜底；
 * 结果缓存到 localStorage（zwj_machine_code）供本地校验工具读取
 */
export async function getMachineCode(): Promise<string> {
  // 1. 共享文件夹优先
  const handle = await getUsableHandle()
  if (handle) {
    try {
      const coc = await handle.getDirectoryHandle(COC_DIR, { create: false })
      const shared = await readSharedDevice(coc)
      if (shared) {
        lsSet(LS_DEVICE_KEY, shared.deviceId)
        lsSet(LS_MACHINE_KEY, shared.machineCode)
        return shared.machineCode
      }
    } catch {
      /* 共享文件夹异常则降级 */
    }
  }

  // 2. localStorage 缓存
  const cached = lsGet(LS_MACHINE_KEY)
  if (cached) return cached

  // 3. 全新生成（sha256(设备ID)，与浏览器特征解耦）
  const deviceId = getOrCreateDeviceId()
  const code = await machineCodeOf(deviceId)
  lsSet(LS_MACHINE_KEY, code)
  return code
}
