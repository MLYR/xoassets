import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import { useForm } from 'react-hook-form';

import { useAuthStore } from '@/stores/authStore';
import { authApi } from '../api/authApi';
import { loginSchema, type LoginFormValues } from '../schemas/authSchemas';

export function useLoginForm() {
  const login = useAuthStore((state) => state.login);
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      identifier: '',
      password: ''
    }
  });

  const onSubmit = form.handleSubmit(async (values) => {
    try {
      const result = await authApi.login({
        username: values.identifier,
        password: values.password
      });
      await login(result.token, result.user);
      router.replace('/home');
    } catch (error) {
      form.setError('root', {
        message: error instanceof Error ? error.message : '登录失败'
      });
    }
  });

  return {
    ...form,
    onSubmit
  };
}
