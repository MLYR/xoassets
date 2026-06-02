/* HTTP 请求封装 — uni-app 版本，复用现有后端 API 格式 */

import { clearToken, getToken } from './token'

/** 后端统一响应结构 */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

const BASE_URL = '/api'

/** uni-app 封装的请求方法，自动处理 token、业务码、401 */
export function request<T>(options: UniApp.RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header: Record<string, string> = {
      'Content-Type': 'application/json',
      ...((options.header as Record<string, string>) || {})
    }
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header,
      timeout: 15000,
      success(res) {
        const result = res.data as ApiResult<T>
        if (res.statusCode === 401 || result?.code === 40100) {
          clearToken()
          uni.reLaunch({ url: '/pages/login/login' })
          reject(new Error('登录已过期，请重新登录'))
          return
        }
        if (typeof result?.code === 'number') {
          if (result.code === 0) {
            resolve(result.data)
          } else {
            reject(new Error(result.message || '请求失败'))
          }
        } else {
          resolve(res.data as T)
        }
      },
      fail(err) {
        reject(new Error(err.errMsg || '网络请求失败'))
      }
    })
  })
}

export default request
