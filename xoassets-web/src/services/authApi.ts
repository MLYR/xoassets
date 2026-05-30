// 认证 API：只封装登录、注册和当前用户接口。
import { request } from './http';

export interface AuthUser {
  id: string;
  username: string;
  nickname: string;
  avatarUrl: string | null;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  nickname?: string;
}

export interface UpdateProfileRequest {
  nickname: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

export interface LoginResponse {
  token: string;
  user: AuthUser;
}

export const authApi = {
  register(data: RegisterRequest) {
    return request<AuthUser>({
      url: '/auth/register',
      method: 'POST',
      data
    });
  },
  login(data: LoginRequest) {
    return request<LoginResponse>({
      url: '/auth/login',
      method: 'POST',
      data
    });
  },
  me() {
    return request<AuthUser>({
      url: '/auth/me',
      method: 'GET'
    });
  },
  updateProfile(data: UpdateProfileRequest) {
    return request<AuthUser>({
      url: '/auth/profile',
      method: 'PUT',
      data
    });
  },
  changePassword(data: ChangePasswordRequest) {
    return request<void>({
      url: '/auth/password',
      method: 'PUT',
      data
    });
  }
};
