<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'

onLaunch(() => {
  // 启动时检查登录态，如果已登录则获取用户信息
  const authStore = useAuthStore()
  if (authStore.hasToken) {
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
  background-color: #F0F4F8;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: #333;
}
</style>
