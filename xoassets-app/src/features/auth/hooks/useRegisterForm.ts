import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import { useForm } from 'react-hook-form';

import { useAuthStore } from '@/stores/authStore';
import { authApi } from '../api/authApi';
import { registerSchema, type RegisterFormValues } from '../schemas/authSchemas';

export function useRegisterForm() {
  const login = useAuthStore((state) => state.login);
  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      username: '',
      password: '',
      confirmPassword: ''
    }
  });

  const onSubmit = form.handleSubmit(async (values) => {
    try {
      await authApi.register({
        username: values.username,
        password: values.password
      });
      // 后端注册接口只返回用户信息，注册成功后复用登录接口建立真实会话。
      const result = await authApi.login({
        username: values.username,
        password: values.password
      });
      await login(result.token, result.user);
      router.replace('/home');
    } catch (error) {
      form.setError('root', {
        message: error instanceof Error ? error.message : '注册失败'
      });
    }
  });

  return {
    ...form,
    onSubmit
  };
}
