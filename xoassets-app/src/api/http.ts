import axios, { AxiosError, AxiosHeaders, type AxiosRequestConfig } from 'axios';
import { router } from 'expo-router';
import { Alert } from 'react-native';

import { getAccessToken, getRefreshToken, useAuthStore } from '@/stores/authStore';
import type { ApiResult } from '@/shared/types/api';
import type { LoginResponse } from '@/shared/types/auth';
import { getApiBaseUrl } from '@/utils/devHost';

const API_BASE_URL = getApiBaseUrl();
type RetryConfig = AxiosRequestConfig & { _retry?: boolean };

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
});

const bareHttp = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
});

let handlingAuthFailure = false;
let refreshPromise: Promise<LoginResponse | null> | null = null;

http.interceptors.request.use(async (config) => {
  if (!API_BASE_URL) {
    return Promise.reject(new Error('请先配置 EXPO_PUBLIC_API_BASE_URL'));
  }

  const token = await getAccessToken();
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
    const originalRequest = error.config as RetryConfig | undefined;
    if ((status === 401 || status === 403 || code === 40100) && originalRequest && !isAuthEndpoint(originalRequest.url)) {
      const refreshed = await refreshSession();
      if (refreshed) {
        originalRequest._retry = true;
        const headers = AxiosHeaders.from(originalRequest.headers as any).toJSON();
        originalRequest.headers = {
          ...headers,
          Authorization: `Bearer ${refreshed.accessToken}`
        };
        return http.request(originalRequest);
      }

      // 401/403 视为会话失效或无权限：清理本地登录态并强制回登录页。
      if (!handlingAuthFailure) {
        handlingAuthFailure = true;
        Alert.alert('登录已过期', '请重新登录后继续操作。');
      }
      await useAuthStore.getState().logout();
      if (router.canGoBack()) {
        router.dismissAll();
      }
      router.replace('/login');
      setTimeout(() => {
        handlingAuthFailure = false;
      }, 300);
    }
    return Promise.reject(new Error(error.response?.data?.message || error.message || '请求失败'));
  }
);

async function refreshSession() {
  const refreshToken = await getRefreshToken();
  if (!refreshToken) {
    return null;
  }

  if (!refreshPromise) {
    refreshPromise = bareHttp
      .post<ApiResult<LoginResponse>>('/api/auth/refresh', {
        refreshToken
      })
      .then(async (response) => {
        const result = response.data;
        if (typeof result?.code === 'number') {
          if (result.code !== 0 || !result.data) {
            return null;
          }
          await useAuthStore.getState().updateSession(result.data.accessToken, result.data.refreshToken, result.data.user);
          return result.data;
        }
        return null;
      })
      .catch(() => null)
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

function isAuthEndpoint(url?: string) {
  return url?.includes('/api/auth/login') || url?.includes('/api/auth/register') || url?.includes('/api/auth/refresh');
}

export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return http.request<unknown, T>(config);
}

export default http;
