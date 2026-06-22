// HTTP 请求封装：统一处理 baseURL、Authorization Header、业务响应和 401 失效跳转。
import axios, { AxiosError, AxiosHeaders, type AxiosRequestConfig } from 'axios';
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './token';

/** 后端统一响应结构。 */
interface ApiResult<T> {
  /** 编码。 */
  code: number;
  /** 提示信息。 */
  message: string;
  /** 响应数据。 */
  data: T;
  /** 链路ID。 */
  traceId?: string;
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
});

const bareHttp = axios.create({
  baseURL: '/api',
  timeout: 15000
});

type RetryConfig = AxiosRequestConfig & { _retry?: boolean };

let refreshPromise: Promise<void> | null = null;

http.interceptors.request.use((config) => {
  const token = getAccessToken();
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
          Authorization: `Bearer ${getAccessToken()}`
        };
        return http.request(originalRequest);
      }

      clearTokens();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(new Error(error.response?.data?.message || error.message || '请求失败'));
  }
);

async function refreshSession() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return false;
  }

  if (!refreshPromise) {
    refreshPromise = bareHttp
      .post<ApiResult<{ accessToken: string; refreshToken: string; user: unknown }>>('/auth/refresh', {
        refreshToken
      })
      .then((response) => {
        const result = response.data;
        if (typeof result?.code === 'number' && result.code === 0 && result.data) {
          setTokens(result.data.accessToken, result.data.refreshToken);
          return;
        }
        throw new Error(result?.message || '刷新失败');
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  try {
    await refreshPromise;
    return true;
  } catch {
    return false;
  }
}

function isAuthEndpoint(url?: string) {
  return url?.includes('/auth/login') || url?.includes('/auth/register') || url?.includes('/auth/refresh');
}

export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return http.request<unknown, T>(config);
}

export default http;
