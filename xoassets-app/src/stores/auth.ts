/* 认证 Store — 管理登录态和用户信息 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type AuthUser } from '@/services/authApi'
import { setToken, getToken, clearToken, hasToken } from '@/services/token'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const token = ref<string | null>(getToken())

  const isLoggedIn = computed(() => !!token.value && !!user.value)

  /** 登录，保存 token 和用户信息 */
  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    token.value = res.token
    user.value = res.user
    setToken(res.token)
    return res
  }

  /** 注册 */
  async function register(username: string, password: string, nickname?: string) {
    await authApi.register({ username, password, nickname })
  }

  /** 获取当前用户信息 */
  async function fetchUser() {
    const u = await authApi.me()
    user.value = u
    return u
  }

  /** 退出登录 */
  function logout() {
    token.value = null
    user.value = null
    clearToken()
    uni.reLaunch({ url: '/pages/login/login' })
  }

  return {
    user,
    token,
    isLoggedIn,
    hasToken,
    login,
    register,
    fetchUser,
    logout
  }
})
