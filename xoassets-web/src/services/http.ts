// HTTP 请求封装：统一处理 baseURL、Authorization Header、业务响应和 401 失效跳转。
import axios, { AxiosError, type AxiosRequestConfig } from 'axios';
import { clearToken, getToken } from './token';

interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
  traceId?: string;
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
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
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status;
    const code = error.response?.data?.code;
    if (status === 401 || code === 40100) {
      clearToken();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(new Error(error.response?.data?.message || error.message || '请求失败'));
  }
);

export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return http.request<unknown, T>(config);
}

export default http;
