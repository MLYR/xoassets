// 认证 API：只封装登录、注册和当前用户接口。
import { request } from './http';

/** 登录用户信息。 */
export interface AuthUser {
  /** ID。 */
  id: string;
  /** 用户名。 */
  username: string;
  /** 昵称。 */
  nickname: string;
  /** 头像地址。 */
  avatarUrl: string | null;
}

/** 登录请求参数。 */
export interface LoginRequest {
  /** 用户名。 */
  username: string;
  /** 密码。 */
  password: string;
}

/** 注册请求参数。 */
export interface RegisterRequest extends LoginRequest {
  /** 昵称。 */
  nickname?: string;
}

/** 资料修改参数。 */
export interface UpdateProfileRequest {
  /** 昵称。 */
  nickname: string;
}

/** 密码修改参数。 */
export interface ChangePasswordRequest {
  /** 旧密码。 */
  oldPassword: string;
  /** 新密码。 */
  newPassword: string;
}

/** 登录返回数据。 */
export interface LoginResponse {
  /** 访问令牌。 */
  accessToken: string;
  /** 刷新令牌。 */
  refreshToken: string;
  /** 用户信息。 */
  user: AuthUser;
}

export const authApi = {
  // 注册。
  register(data: RegisterRequest) {
    return request<AuthUser>({
      url: '/auth/register',
      method: 'POST',
      data
    });
  },
  // 登录。
  login(data: LoginRequest) {
    return request<LoginResponse>({
      url: '/auth/login',
      method: 'POST',
      data
    });
  },
  // 刷新令牌。
  refresh(refreshToken: string) {
    return request<LoginResponse>({
      url: '/auth/refresh',
      method: 'POST',
      data: {
        refreshToken
      }
    });
  },
  // 查询当前用户。
  me() {
    return request<AuthUser>({
      url: '/auth/me',
      method: 'GET'
    });
  },
  // 更新资料。
  updateProfile(data: UpdateProfileRequest) {
    return request<AuthUser>({
      url: '/auth/profile',
      method: 'PUT',
      data
    });
  },
  // 修改密码。
  changePassword(data: ChangePasswordRequest) {
    return request<void>({
      url: '/auth/password',
      method: 'PUT',
      data
    });
  }
};
