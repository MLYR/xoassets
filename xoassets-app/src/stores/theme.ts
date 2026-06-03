/* 主题 Store：保存当前主题并在切换时同步 CSS 变量和 uni 原生外观。 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { DEFAULT_THEME_NAME, getTheme, themeOptions, type ThemeName } from '@/theme'
import { applyTheme } from '@/theme/applyTheme'

const LEGACY_THEME_KEY = 'xoassets_theme'
const THEME_KEY = 'xoassets_theme_v2'
const THEME_STORAGE_VERSION = 2

type ThemeStorageValue = {
  key: ThemeName
  version: number
}

function resolveInitialThemeKey(): ThemeName {
  const stored = uni.getStorageSync(THEME_KEY) as ThemeStorageValue | ThemeName | ''
  if (typeof stored === 'object' && stored?.version === THEME_STORAGE_VERSION && stored.key) {
    return stored.key
  }
  if (typeof stored === 'string' && stored) {
    return stored as ThemeName
  }
  // 兼容旧版只保存主题名的 storage，读到后由 setTheme/initTheme 写回 v2 结构。
  return (uni.getStorageSync(LEGACY_THEME_KEY) as ThemeName) || DEFAULT_THEME_NAME
}

export const useThemeStore = defineStore('theme', () => {
  const currentThemeKey = ref<ThemeName>(resolveInitialThemeKey())
  const currentTheme = computed(() => getTheme(currentThemeKey.value))

  // 兼容已经接入 currentThemeName 的页面，后续页面统一使用 currentThemeKey。
  const currentThemeName = computed(() => currentThemeKey.value)

  function initTheme() {
    uni.setStorageSync(THEME_KEY, { key: currentThemeKey.value, version: THEME_STORAGE_VERSION })
    applyTheme(currentTheme.value)
  }

  function setTheme(name: ThemeName) {
    currentThemeKey.value = name
    uni.setStorageSync(THEME_KEY, { key: name, version: THEME_STORAGE_VERSION })
    applyTheme(currentTheme.value)
  }

  return {
    currentThemeKey,
    currentThemeName,
    currentTheme,
    themeOptions,
    initTheme,
    setTheme
  }
})
