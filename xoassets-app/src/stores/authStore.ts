import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import { create } from 'zustand';

import type { AuthUser } from '@/shared/types/auth';

const TOKEN_KEY = 'xoassets_token';

async function saveToken(token: string) {
  if (Platform.OS === 'web') {
    // Web 仅用于本地预览，原生端仍使用 SecureStore 保存敏感 token。
    await AsyncStorage.setItem(TOKEN_KEY, token);
    return;
  }
  await SecureStore.setItemAsync(TOKEN_KEY, token);
}

async function removeToken() {
  if (Platform.OS === 'web') {
    await AsyncStorage.removeItem(TOKEN_KEY);
    return;
  }
  await SecureStore.deleteItemAsync(TOKEN_KEY);
}

async function loadToken() {
  if (Platform.OS === 'web') {
    return AsyncStorage.getItem(TOKEN_KEY);
  }
  return SecureStore.getItemAsync(TOKEN_KEY);
}

interface AuthState {
  token: string | null;
  userInfo: AuthUser | null;
  isLoggedIn: boolean;
  isHydrated: boolean;
  setToken: (token: string | null) => Promise<void>;
  setUserInfo: (userInfo: AuthUser | null) => void;
  login: (token: string, userInfo?: AuthUser | null) => Promise<void>;
  logout: () => Promise<void>;
  restoreToken: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  userInfo: null,
  isLoggedIn: false,
  isHydrated: false,
  setToken: async (token) => {
    if (token) {
      await saveToken(token);
    } else {
      await removeToken();
    }
    set({ token, isLoggedIn: Boolean(token) });
  },
  setUserInfo: (userInfo) => set({ userInfo }),
  login: async (token, userInfo = null) => {
    await saveToken(token);
    set({ token, userInfo, isLoggedIn: true, isHydrated: true });
  },
  logout: async () => {
    await removeToken();
    set({ token: null, userInfo: null, isLoggedIn: false, isHydrated: true });
  },
  restoreToken: async () => {
    // 原生端 token 只进 SecureStore；Web 预览无法使用 SecureStore，降级到 AsyncStorage。
    const token = await loadToken();
    set({ token, isLoggedIn: Boolean(token), isHydrated: true });
  }
}));

export async function getAuthToken() {
  const stateToken = useAuthStore.getState().token;
  return stateToken ?? loadToken();
}
