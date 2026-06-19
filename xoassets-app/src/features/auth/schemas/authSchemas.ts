import { z } from 'zod';

export const loginSchema = z.object({
  username: z.string().trim().min(1, '请输入账户名'),
  password: z.string().min(6, '密码至少 6 位')
});

export const registerSchema = z
  .object({
    username: z.string().trim().min(1, '请输入账户名'),
    password: z.string().min(6, '密码至少 6 位'),
    confirmPassword: z.string().min(6, '请再次输入密码')
  })
  .refine((value) => value.password === value.confirmPassword, {
    path: ['confirmPassword'],
    message: '两次输入的密码不一致'
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
export type RegisterFormValues = z.infer<typeof registerSchema>;
