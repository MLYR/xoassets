/* 主题读取 composable：给页面和组件提供当前主题和切换方法。 */
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useThemeStore } from '@/stores/theme'

export function useTheme() {
  const themeStore = useThemeStore()
  const { currentThemeKey, currentTheme } = storeToRefs(themeStore)
  const homeTokens = computed(() => currentTheme.value.pageTokens.home)
  const investmentTokens = computed(() => currentTheme.value.pageTokens.investments)

  function getThemeAsset(key: string) {
    // 页面只通过主题资产层取素材，避免后续多主题时硬编码散落在业务组件。
    return currentTheme.value.assets.icons[key] || currentTheme.value.assets.backgrounds[key] || ''
  }

  return {
    currentThemeKey,
    currentTheme,
    homeTokens,
    investmentTokens,
    getThemeAsset,
    themeOptions: computed(() => themeStore.themeOptions),
    setTheme: themeStore.setTheme,
    initTheme: themeStore.initTheme
  }
}
