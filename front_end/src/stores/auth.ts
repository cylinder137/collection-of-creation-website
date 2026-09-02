import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api'
import type { LoginParams, UserInfo } from '@/types'

const TOKEN_KEY = 'coc_token'
const USER_KEY = 'coc_user'

/**
 * 认证状态：token + 用户信息，localStorage 持久化。
 * http.ts 请求拦截器读取 TOKEN_KEY 附加 Authorization 头（两处共用同一 key）。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  const userInfo = ref<UserInfo | null>(
    JSON.parse(localStorage.getItem(USER_KEY) ?? 'null') as UserInfo | null,
  )

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'admin')

  /** 登录：成功则持久化 token 与用户信息 */
  async function login(params: LoginParams) {
    const res = await authApi.login(params)
    token.value = res.token
    userInfo.value = res.userInfo
    localStorage.setItem(TOKEN_KEY, res.token)
    localStorage.setItem(USER_KEY, JSON.stringify(res.userInfo))
  }

  /** 退出：清理本地状态（后端 /auth/logout 失败不影响本地退出） */
  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // 忽略后端退出接口异常，本地状态必须清理
    }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, userInfo, isLoggedIn, isAdmin, login, logout }
})
