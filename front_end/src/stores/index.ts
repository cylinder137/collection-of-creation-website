import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getAdminToken, setAdminToken } from '@/api/http'
import { adminApi } from '@/api'
import type { AdminLoginResult, AdminProfile } from '@/types'

/**
 * 管理员身份 store
 *
 * 采用「无状态令牌」方案：
 * - 登录成功后令牌缓存到 localStorage（刷新页面不丢）
 * - 每次调用 /api/admin/** 接口都会携带 Authorization: Bearer <token>
 * - 后端对每个请求重新验签并回库校验管理员状态（不存在/被禁用 → 401）
 * 因此前端不需要持有会话，服务端也不需要保存 session。
 */
export const useAdminStore = defineStore('admin', () => {
  const token = ref<string | null>(getAdminToken())
  const profile = ref<AdminProfile | null>(null)

  const isLoggedIn = computed(() => !!token.value && !!profile.value)
  const isSuperAdmin = computed(() => profile.value?.role === 1)

  /** 登录：换令牌 → 落缓存 → 拉管理员信息 */
  async function login(username: string, password: string): Promise<AdminLoginResult> {
    const result = await adminApi.login(username, password)
    token.value = result.token
    setAdminToken(result.token)
    await fetchProfile()
    return result
  }

  /** 拉取当前管理员信息（同时用于校验令牌是否仍然有效） */
  async function fetchProfile(): Promise<AdminProfile> {
    const data = await adminApi.me()
    profile.value = data
    return data
  }

  /** 退出登录：清缓存 + 重置状态 */
  function logout() {
    token.value = null
    profile.value = null
    setAdminToken(null)
  }

  return {
    token,
    profile,
    isLoggedIn,
    isSuperAdmin,
    login,
    fetchProfile,
    logout,
  }
})
