import type { AuthUser, ChangePasswordRequest, LoginRequest, LoginResponse, RegisterRequest, UpdateProfileRequest } from '@/shared/types/auth';

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
  refresh(refreshToken: string) {
    return request<LoginResponse>({
      url: '/api/auth/refresh',
      method: 'POST',
      data: {
        refreshToken
      }
    });
  },
  me() {
    return request<AuthUser>({
      url: '/api/auth/me',
      method: 'GET'
    });
  },
  updateProfile(data: UpdateProfileRequest) {
    return request<AuthUser>({
      url: '/api/auth/profile',
      method: 'PUT',
      data
    });
  },
  changePassword(data: ChangePasswordRequest) {
    return request<void>({
      url: '/api/auth/password',
      method: 'PUT',
      data
    });
  }
};
