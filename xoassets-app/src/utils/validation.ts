// 过渡导出：认证表单校验逐步下沉到 features/auth，旧入口先保留给现有引用。
export {
  loginSchema,
  registerSchema,
  type LoginFormValues,
  type RegisterFormValues
} from '@/features/auth';
