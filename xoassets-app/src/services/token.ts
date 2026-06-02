/* Token 存储工具 —— uni-app 版本，使用 uni.storage API */

const TOKEN_KEY = 'xoassets_token'

export function getToken(): string | null {
  return uni.getStorageSync(TOKEN_KEY) || null
}

export function setToken(token: string): void {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function clearToken(): void {
  uni.removeStorageSync(TOKEN_KEY)
}

export function hasToken(): boolean {
  return Boolean(getToken())
}
