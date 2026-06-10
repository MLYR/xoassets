// Web 主题状态：支持手动日间/夜间，也支持跟随系统偏好自动切换。
import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

export type ThemeMode = 'system' | 'light' | 'dark';
type ThemeScheme = 'light' | 'dark';

const STORAGE_KEY = 'xoassets_web_theme_mode';
const DARK_QUERY = '(prefers-color-scheme: dark)';
const themeModes: ThemeMode[] = ['system', 'light', 'dark'];

let mediaQuery: MediaQueryList | null = null;
let listenerReady = false;

// localStorage 可能被手动写入异常值，读取时只接受明确支持的模式。
function normalizeThemeMode(value: string | null): ThemeMode {
  return themeModes.includes(value as ThemeMode) ? (value as ThemeMode) : 'system';
}

function getSystemScheme(): ThemeScheme {
  if (typeof window === 'undefined') {
    return 'light';
  }
  return window.matchMedia(DARK_QUERY).matches ? 'dark' : 'light';
}

// Element Plus 暗色变量依赖 html.dark，业务样式依赖 data-theme。
function applyTheme(scheme: ThemeScheme, mode: ThemeMode) {
  if (typeof document === 'undefined') {
    return;
  }
  const root = document.documentElement;
  root.dataset.theme = scheme;
  root.dataset.themeMode = mode;
  root.classList.toggle('dark', scheme === 'dark');
  root.style.colorScheme = scheme;
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>('system');
  const systemScheme = ref<ThemeScheme>('light');
  const resolvedTheme = computed<ThemeScheme>(() => (mode.value === 'system' ? systemScheme.value : mode.value));

  function syncTheme() {
    applyTheme(resolvedTheme.value, mode.value);
  }

  function handleSystemChange(event: MediaQueryListEvent) {
    systemScheme.value = event.matches ? 'dark' : 'light';
    syncTheme();
  }

  function initTheme() {
    if (typeof window === 'undefined') {
      return;
    }
    mode.value = normalizeThemeMode(window.localStorage.getItem(STORAGE_KEY));
    systemScheme.value = getSystemScheme();
    syncTheme();

    if (!mediaQuery) {
      mediaQuery = window.matchMedia(DARK_QUERY);
    }
    if (!listenerReady) {
      mediaQuery.addEventListener('change', handleSystemChange);
      listenerReady = true;
    }
  }

  function setThemeMode(nextMode: ThemeMode) {
    mode.value = nextMode;
    window.localStorage.setItem(STORAGE_KEY, nextMode);
    syncTheme();
  }

  return {
    mode,
    resolvedTheme,
    initTheme,
    setThemeMode
  };
});
