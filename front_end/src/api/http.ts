import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

/**
 * HTTP 客户端封装
 *
 * - baseURL 取自 VITE_API_BASE（默认 /api，由 Vite 代理或 Nginx 反向代理转发到后端）
 * - 管理后台接口（/api/admin/**）自动携带 Authorization: Bearer <token>
 *   token 为后端签发的无状态令牌，登录后缓存于 localStorage，后端每次请求都会重新核验
 * - 统一解包 { code, data, message }；业务失败与网络异常统一提示
 */

const TOKEN_KEY = 'coc_admin_token'
const ADMIN_ROUTE_PREFIX = '/admin'

export function getAdminToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setAdminToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截：管理后台接口统一携带管理员令牌（无状态，后端逐次核验）
http.interceptors.request.use((config) => {
  const token = getAdminToken()
  if (token && config.url?.startsWith(ADMIN_ROUTE_PREFIX)) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 是否是管理后台页面（用于 401 时判断要不要跳登录） */
function isAdminPage(): boolean {
  return window.location.pathname.startsWith('/admin')
}

http.interceptors.response.use(
  (response) => {
    // 约定统一响应 { code, data, message }
    const res = response.data
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 0 || res.code === 200) {
        return res.data
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message

    // 401：管理员令牌失效/被禁用 → 清除缓存并回登录页
    if (status === 401) {
      setAdminToken(null)
      if (isAdminPage() && window.location.pathname !== '/admin') {
        ElMessage.warning(message || '登录已过期，请重新登录')
        window.location.replace('/admin')
      }
    } else if (status === 403) {
      ElMessage.error(message || '没有权限执行该操作')
    } else {
      ElMessage.error(message || error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

/** 通用请求封装（自动解包 code/data，直接返回 data） */
export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return http.request(config) as unknown as Promise<T>
}

export default http
