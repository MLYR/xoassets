<template>
  <view class="app-icon" :style="wrapperStyle">
    <image
      v-if="resolvedIcon?.type === 'image'"
      :src="resolvedIcon.src"
      class="app-icon-image"
      :style="imageStyle"
      mode="aspectFit"
    />
    <text
      v-else-if="resolvedIcon?.type === 'text' || !resolvedIcon"
      class="app-icon-text"
      :style="textStyle"
    >
      {{ iconText }}
    </text>
    <view
      v-else
      class="app-icon-class"
      :class="resolvedIcon.className"
      :style="classStyle"
    ></view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ThemeConfig, ThemeIcon } from '@/theme'
import { useTheme } from '@/theme/useTheme'

const props = withDefaults(defineProps<{
  name: string
  size?: string
  color?: string
  active?: boolean
}>(), {
  size: '36rpx',
  color: '',
  active: false
})

const { currentTheme } = useTheme()

function resolveIconByName(theme: ThemeConfig, name: string, active: boolean): ThemeIcon | undefined {
  const [group, key, state] = name.split('.')

  if (group === 'tabBar' && key) {
    const pair = theme.icons.tabBar[key as keyof ThemeConfig['icons']['tabBar']]
    if (!pair) return undefined
    if (state === 'active') return pair.active
    if (state === 'normal') return pair.normal
    return active ? pair.active : pair.normal
  }

  if (group === 'menu' && key) return theme.icons.menu[key]
  if (group === 'home' && key) return theme.icons.home[key as keyof ThemeConfig['icons']['home']]
  if (group === 'homeStats' && key) return theme.icons.homeStats[key as keyof ThemeConfig['icons']['homeStats']]
  if (group === 'recentActivities' && key) {
    return theme.icons.recentActivities[key as keyof ThemeConfig['icons']['recentActivities']]
  }
  if (group === 'category' && key) return theme.icons.category[key]
  if (group === 'categoryFallback' && key) {
    return theme.icons.categoryFallback[key as keyof ThemeConfig['icons']['categoryFallback']]
  }
  if (group === 'accounts' && key) return theme.icons.accounts[key as keyof ThemeConfig['icons']['accounts']]
  if (group === 'investmentActions' && key) {
    return theme.icons.investmentActions[key as keyof ThemeConfig['icons']['investmentActions']]
  }
  if (group === 'quickActions' && key) {
    return theme.icons.quickActions[key as keyof ThemeConfig['icons']['quickActions']]
  }
  if (group === 'common' && key) return theme.icons.common[key as keyof ThemeConfig['icons']['common']]
  if (group === 'charts' && key) return theme.icons.chartIcons[key as keyof ThemeConfig['icons']['chartIcons']]
  if (group === 'reports' && key) return theme.icons.reports[key as keyof ThemeConfig['icons']['reports']]

  return undefined
}

const resolvedIcon = computed(() => resolveIconByName(currentTheme.value, props.name, props.active))

const iconText = computed(() => {
  if (!resolvedIcon.value) return ''
  if (resolvedIcon.value.type === 'text') return resolvedIcon.value.value
  if (resolvedIcon.value.type === 'class') return resolvedIcon.value.className
  return ''
})

const resolvedColor = computed(() => props.color || currentTheme.value.colors.primary)

const wrapperStyle = computed(() => ({
  width: props.size,
  height: props.size,
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center'
}))

const textStyle = computed(() => ({
  fontSize: props.size,
  color: resolvedColor.value,
  lineHeight: props.size,
  fontWeight: '700'
}))

const classStyle = computed(() => ({
  width: props.size,
  height: props.size,
  color: resolvedColor.value
}))

const imageStyle = computed(() => ({
  width: props.size,
  height: props.size
}))
</script>

<style scoped lang="scss">
.app-icon-image {
  display: block;
}

.app-icon-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.app-icon-class {
  position: relative;
  display: inline-flex;
  box-sizing: border-box;
  color: currentColor;
}

.xo-icon-search {
  border: 3rpx solid currentColor;
  border-radius: 50%;
  transform: scale(0.72);
}

.xo-icon-search::after {
  content: '';
  position: absolute;
  width: 45%;
  height: 3rpx;
  right: -34%;
  bottom: -18%;
  border-radius: 999rpx;
  background: currentColor;
  transform: rotate(45deg);
}

.xo-icon-bell {
  border: 3rpx solid currentColor;
  border-bottom: 0;
  border-radius: 50% 50% 42% 42%;
  transform: scale(0.76);
}

.xo-icon-bell::before,
.xo-icon-bell::after {
  content: '';
  position: absolute;
  left: 50%;
  background: currentColor;
  transform: translateX(-50%);
}

.xo-icon-bell::before {
  bottom: -16%;
  width: 78%;
  height: 3rpx;
  border-radius: 999rpx;
}

.xo-icon-bell::after {
  bottom: -32%;
  width: 22%;
  height: 3rpx;
  border-radius: 999rpx;
}

.xo-icon-chart {
  background:
    linear-gradient(currentColor, currentColor) 14% 76% / 18% 34% no-repeat,
    linear-gradient(currentColor, currentColor) 48% 66% / 18% 54% no-repeat,
    linear-gradient(currentColor, currentColor) 82% 52% / 18% 82% no-repeat;
  border-radius: 4rpx;
}

.xo-icon-eye {
  border: 3rpx solid currentColor;
  border-radius: 70% 20% 70% 20%;
  transform: rotate(45deg) scale(0.72);
}

.xo-icon-eye::after,
.xo-icon-eye-off::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 30%;
  height: 30%;
  border-radius: 50%;
  background: currentColor;
  transform: translate(-50%, -50%);
}

.xo-icon-eye-off {
  border: 3rpx solid currentColor;
  border-radius: 70% 20% 70% 20%;
  transform: rotate(45deg) scale(0.72);
}

.xo-icon-eye-off::before {
  content: '';
  position: absolute;
  left: -18%;
  top: 50%;
  width: 136%;
  height: 3rpx;
  border-radius: 999rpx;
  background: currentColor;
  transform: rotate(-90deg);
}
</style>
