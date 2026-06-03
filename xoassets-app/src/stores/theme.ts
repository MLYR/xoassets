/* 主题 Store：保存当前主题并在切换时同步 CSS 变量和 uni 原生外观。 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { DEFAULT_THEME_NAME, getTheme, themeOptions, type ThemeName } from '@/theme'
import { applyTheme } from '@/theme/applyTheme'

const THEME_KEY = 'xoassets_theme'

export const useThemeStore = defineStore('theme', () => {
  const currentThemeName = ref<ThemeName>((uni.getStorageSync(THEME_KEY) as ThemeName) || DEFAULT_THEME_NAME)
  const currentTheme = computed(() => getTheme(currentThemeName.value))

  function initTheme() {
    applyTheme(currentTheme.value)
  }

  function setTheme(name: ThemeName) {
    currentThemeName.value = name
    uni.setStorageSync(THEME_KEY, name)
    applyTheme(currentTheme.value)
  }

  return {
    currentThemeName,
    currentTheme,
    themeOptions,
    initTheme,
    setTheme
  }
})
