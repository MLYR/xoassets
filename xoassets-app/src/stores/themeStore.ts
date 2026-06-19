import AsyncStorage from '@react-native-async-storage/async-storage';
import { create } from 'zustand';

export type ThemeMode = 'light' | 'dark' | 'system';

const THEME_MODE_KEY = 'xoassets_theme_mode';

interface ThemeState {
  themeMode: ThemeMode;
  isDark: boolean;
  setThemeMode: (themeMode: ThemeMode) => void;
  setIsDark: (isDark: boolean) => void;
}

export const useThemeStore = create<ThemeState>((set) => ({
  themeMode: 'system',
  isDark: false,
  setThemeMode: (themeMode) => {
    set({ themeMode });
    void AsyncStorage.setItem(THEME_MODE_KEY, themeMode);
  },
  setIsDark: (isDark) => set({ isDark })
}));

void AsyncStorage.getItem(THEME_MODE_KEY).then((themeMode) => {
  if (themeMode === 'light' || themeMode === 'dark' || themeMode === 'system') {
    useThemeStore.setState({ themeMode });
  }
});
