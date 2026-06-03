<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

onLaunch(() => {
  // 启动时应用主题，确保 H5 CSS 变量和原生导航/TabBar 配色一致。
  const themeStore = useThemeStore()
  themeStore.initTheme()

  // 启动时检查登录态，如果已登录则获取用户信息
  const authStore = useAuthStore()
  if (authStore.hasToken()) {
    authStore.fetchUser().catch(() => {
      // token 失效，清除登录态
      authStore.logout()
    })
  }
})
</script>

<style lang="scss">
/* 全局 app 样式 */
page {
  background-color: var(--xo-page-bg);
  font-family: var(--xo-font-family);
  font-size: var(--xo-font-md);
  color: var(--xo-text-primary);
}
</style>
