import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { signRequest, decodePayload } from './sign'

/**
 * HTTP 客户端封装
 * - baseURL 默认 /api，由 Vite 代理转发到后端（见 vite.config.ts）
 * - 统一错误提示
 * - 反爬：请求自动带签名头（X-Timestamp/X-Nonce/X-Sign，见 api/sign.ts）
 */
const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
})

// 请求拦截器：每个请求附带时间戳 + 随机数 + 签名，裸爬虫直接调接口会被后端拒绝
http.interceptors.request.use((config) => {
  const signed = signRequest(config.method ?? 'get', config.url ?? '')
  Object.assign(config.headers, signed)
  return config
})

http.interceptors.response.use(
  (response) => {
    // 反爬：敏感响应经 XOR+Base64 编码传输时，先解码还原
    let res = response.data
    if (res && typeof res === 'object' && typeof res.payload === 'string') {
      res = decodePayload(res.payload)
    }
    // 约定后端统一返回 { code, data, message }
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
