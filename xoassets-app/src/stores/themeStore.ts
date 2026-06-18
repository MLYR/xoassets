import AsyncStorage from '@react-native-async-storage/async-storage';
import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

export type ThemeMode = 'light' | 'dark' | 'system';

interface ThemeState {
  themeMode: ThemeMode;
  isDark: boolean;
  setThemeMode: (themeMode: ThemeMode) => void;
  setIsDark: (isDark: boolean) => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      themeMode: 'system',
      isDark: false,
      setThemeMode: (themeMode) => set({ themeMode }),
      setIsDark: (isDark) => set({ isDark })
    }),
    {
      name: 'xoassets_theme_mode',
      storage: createJSONStorage(() => AsyncStorage)
    }
  )
);
