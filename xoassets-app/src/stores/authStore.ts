import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';
import { create } from 'zustand';

import type { AuthUser } from '@/shared/types/auth';

const ACCESS_TOKEN_KEY = 'xoassets_access_token';
const REFRESH_TOKEN_KEY = 'xoassets_refresh_token';

async function saveSession(accessToken: string, refreshToken: string) {
  if (Platform.OS === 'web') {
    // Web 仅用于本地预览，原生端仍使用 SecureStore 保存敏感 token。
    await AsyncStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    await AsyncStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    return;
  }
  await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, accessToken);
  await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, refreshToken);
}

async function removeSession() {
  if (Platform.OS === 'web') {
    await AsyncStorage.removeItem(ACCESS_TOKEN_KEY);
    await AsyncStorage.removeItem(REFRESH_TOKEN_KEY);
    return;
  }
  await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY);
  await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
}

async function loadAccessToken() {
  if (Platform.OS === 'web') {
    return AsyncStorage.getItem(ACCESS_TOKEN_KEY);
  }
  return SecureStore.getItemAsync(ACCESS_TOKEN_KEY);
}

async function loadRefreshToken() {
  if (Platform.OS === 'web') {
    return AsyncStorage.getItem(REFRESH_TOKEN_KEY);
  }
  return SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  userInfo: AuthUser | null;
  isLoggedIn: boolean;
  isHydrated: boolean;
  setUserInfo: (userInfo: AuthUser | null) => void;
  login: (accessToken: string, refreshToken: string, userInfo?: AuthUser | null) => Promise<void>;
  updateSession: (accessToken: string, refreshToken: string, userInfo?: AuthUser | null) => Promise<void>;
  logout: () => Promise<void>;
  restoreToken: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  userInfo: null,
  isLoggedIn: false,
  isHydrated: false,
  setUserInfo: (userInfo) => set({ userInfo }),
  login: async (accessToken, refreshToken, userInfo = null) => {
    await saveSession(accessToken, refreshToken);
    set({ accessToken, refreshToken, userInfo, isLoggedIn: true, isHydrated: true });
  },
  updateSession: async (accessToken, refreshToken, userInfo = null) => {
    await saveSession(accessToken, refreshToken);
    set({ accessToken, refreshToken, userInfo, isLoggedIn: true, isHydrated: true });
  },
  logout: async () => {
    await removeSession();
    set({ accessToken: null, refreshToken: null, userInfo: null, isLoggedIn: false, isHydrated: true });
  },
  restoreToken: async () => {
    // 原生端 token 只进 SecureStore；Web 预览无法使用 SecureStore，降级到 AsyncStorage。
    const accessToken = await loadAccessToken();
    const refreshToken = await loadRefreshToken();
    set({
      accessToken,
      refreshToken,
      isLoggedIn: Boolean(accessToken || refreshToken),
      isHydrated: true
    });
  }
}));

export async function getAccessToken() {
  const stateToken = useAuthStore.getState().accessToken;
  return stateToken ?? loadAccessToken();
}

export async function getRefreshToken() {
  const stateToken = useAuthStore.getState().refreshToken;
  return stateToken ?? loadRefreshToken();
}
