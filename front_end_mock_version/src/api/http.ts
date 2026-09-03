import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

/**
 * HTTP 客户端封装
 * - baseURL 默认 /api，由 Vite 代理转发到后端（见 vite.config.ts）
 * - 统一错误提示
 */
const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
})

http.interceptors.response.use(
  (response) => {
    // 约定后端统一返回 { code, data, message }
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
    ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  },
)

/** 泛型请求封装：适配上面拦截器返回 data 的逻辑 */
export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return http.request(config) as unknown as Promise<T>
}

export default http
