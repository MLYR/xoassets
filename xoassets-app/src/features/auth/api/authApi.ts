import type { AuthUser, LoginRequest, LoginResponse, RegisterRequest } from '@/shared/types/auth';

import { request } from '@/api/http';

export const authApi = {
  register(data: RegisterRequest) {
    return request<AuthUser>({
      url: '/api/auth/register',
      method: 'POST',
      data
    });
  },
  login(data: LoginRequest) {
    return request<LoginResponse>({
      url: '/api/auth/login',
      method: 'POST',
      data
    });
  },
  me() {
    return request<AuthUser>({
      url: '/api/auth/me',
      method: 'GET'
    });
  }
};
