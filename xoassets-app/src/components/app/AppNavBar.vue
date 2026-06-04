<template>
  <view class="app-nav-spacer"></view>
  <view class="app-nav-bar">
    <view class="app-nav-inner">
      <view class="app-nav-side app-nav-left">
        <slot name="left">
          <view v-if="props.showBack && !props.detail" class="app-nav-icon" @click="goBack">
            <AppIcon name="common.back" size="38rpx" :color="theme.components.navBar.iconColor" />
          </view>
        </slot>
      </view>
      <text class="app-nav-title">{{ props.title }}</text>
      <view class="app-nav-side app-nav-right">
        <slot name="right"></slot>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from '@/components/app/AppIcon.vue'
import { useTheme } from '@/theme/useTheme'

const props = withDefaults(defineProps<{
  title: string
  showBack?: boolean
  detail?: boolean
}>(), {
  showBack: false,
  detail: false
})

const { currentTheme } = useTheme()
const theme = computed(() => currentTheme.value)

function goBack() {
  uni.navigateBack()
}
</script>

<style scoped lang="scss">
.app-nav-spacer {
  height: calc(var(--xo-nav-height) + env(safe-area-inset-top, 0px));
  flex-shrink: 0;
}

.app-nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: var(--xo-nav-z-index);
  padding-top: env(safe-area-inset-top, 0px);
  background: var(--xo-nav-bg);
  box-shadow: var(--xo-nav-shadow);
  backdrop-filter: blur(var(--xo-nav-blur));
  -webkit-backdrop-filter: blur(var(--xo-nav-blur));
}

.app-nav-inner {
  height: var(--xo-nav-height);
  display: grid;
  grid-template-columns: 132rpx minmax(0, 1fr) 132rpx;
  align-items: center;
  padding: 0 24rpx;
  box-sizing: border-box;
}

.app-nav-side {
  min-width: 0;
  display: flex;
  align-items: center;
}

.app-nav-left {
  justify-content: flex-start;
}

.app-nav-right {
  justify-content: flex-end;
  column-gap: 18rpx;
}

.app-nav-title {
  overflow: hidden;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--xo-font-xl);
  font-weight: 800;
  color: var(--xo-nav-text);
}

.app-nav-icon {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
