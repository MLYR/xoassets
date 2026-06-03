/* 主题读取 composable：给页面和组件提供当前主题和切换方法。 */
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useThemeStore } from '@/stores/theme'

export function useTheme() {
  const themeStore = useThemeStore()
  const { currentThemeKey, currentTheme } = storeToRefs(themeStore)

  return {
    currentThemeKey,
    currentTheme,
    themeOptions: computed(() => themeStore.themeOptions),
    setTheme: themeStore.setTheme,
    initTheme: themeStore.initTheme
  }
}
