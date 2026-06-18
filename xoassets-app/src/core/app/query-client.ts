import { QueryClient } from '@tanstack/react-query';

// 统一复用单例 QueryClient，避免根布局每次渲染重复创建实例。
export const queryClient = new QueryClient();
