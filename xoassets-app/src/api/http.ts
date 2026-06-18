import axios, { AxiosError, AxiosHeaders, type AxiosRequestConfig } from 'axios';

import { getAuthToken, useAuthStore } from '@/stores/authStore';
import type { ApiResult } from '@/shared/types/api';
import { getApiBaseUrl } from '@/utils/devHost';

const API_BASE_URL = getApiBaseUrl();

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
});

http.interceptors.request.use(async (config) => {
  if (!API_BASE_URL) {
    return Promise.reject(new Error('请先配置 EXPO_PUBLIC_API_BASE_URL'));
  }

  const token = await getAuthToken();
  if (token) {
    config.headers = AxiosHeaders.from(config.headers);
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>;
    if (typeof result?.code === 'number') {
      if (result.code === 0) {
        return result.data;
      }
      return Promise.reject(new Error(result.message || '请求失败'));
    }
    return response.data;
  },
  async (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status;
    const code = error.response?.data?.code;
    if (status === 401 || code === 40100) {
      // 401 是会话失效边界，统一清理本地 token，由路由层回登录页。
      await useAuthStore.getState().logout();
    }
    return Promise.reject(new Error(error.response?.data?.message || error.message || '请求失败'));
  }
);

export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return http.request<unknown, T>(config);
}

export default http;
