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
      v-else
      class="app-icon-text"
      :style="textStyle"
    >
      {{ iconText }}
    </text>
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
  if (group === 'categoryFallback' && key) {
    return theme.icons.categoryFallback[key as keyof ThemeConfig['icons']['categoryFallback']]
  }
  if (group === 'investmentActions' && key) {
    return theme.icons.investmentActions[key as keyof ThemeConfig['icons']['investmentActions']]
  }
  if (group === 'quickActions' && key) {
    return theme.icons.quickActions[key as keyof ThemeConfig['icons']['quickActions']]
  }

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
</style>
