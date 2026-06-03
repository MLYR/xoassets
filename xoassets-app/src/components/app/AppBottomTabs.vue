<template>
  <view class="app-bottom-tabs" :style="tabsStyle">
    <view
      v-for="tab in tabs"
      :key="tab.key"
      class="app-bottom-tabs-item"
      @click="emit('change', tab.key)"
    >
      <AppIcon
        :name="tab.icon || `tabBar.${resolveTabIconKey(tab.key)}`"
        :active="tab.key === currentKey"
        :color="tab.key === currentKey ? currentTheme.colors.primary : currentTheme.components.tabBar.color"
        size="36rpx"
      />
      <text class="app-bottom-tabs-label" :style="labelStyle(tab.key === currentKey)">
        {{ tab.label }}
      </text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import { useTheme } from '@/theme/useTheme'

export interface BottomTabItem {
  key: 'home' | 'add' | 'accounts' | 'investments' | 'mine' | string
  label: string
  icon?: string
}

const props = defineProps<{
  tabs: BottomTabItem[]
  currentKey: string
}>()

const emit = defineEmits<{
  change: [key: string]
}>()

const { currentTheme } = useTheme()

const tabsStyle = computed(() => ({
  background: currentTheme.value.components.tabBar.backgroundColor,
  borderTop: `1rpx solid ${currentTheme.value.colors.border}`,
  paddingBottom: 'calc(12rpx + env(safe-area-inset-bottom, 0px))'
}))

function labelStyle(active: boolean) {
  return {
    color: active ? currentTheme.value.components.tabBar.selectedColor : currentTheme.value.components.tabBar.color,
    fontSize: currentTheme.value.typography.fontSizeXs
  }
}

function resolveTabIconKey(key: string) {
  if (key === 'add') return 'record'
  return key
}
</script>

<style scoped lang="scss">
.app-bottom-tabs {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding-top: 12rpx;
}

.app-bottom-tabs-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  row-gap: 8rpx;
}
</style>
