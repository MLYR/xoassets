import { request } from './http';
import type { AuthUser } from '@/shared/types/auth';

export const userApi = {
  me() {
    return request<AuthUser>({
      url: '/api/auth/me',
      method: 'GET'
    });
  }
};
