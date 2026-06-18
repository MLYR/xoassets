export interface AuthUser {
  id: string;
  username: string;
  nickname?: string | null;
  avatarUrl?: string | null;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  nickname?: string;
}

export interface LoginResponse {
  token: string;
  user: AuthUser;
}
