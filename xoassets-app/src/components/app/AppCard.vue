<template>
  <view class="app-card" :style="cardStyle">
    <slot />
  </view>
</template>

<script setup lang="ts">
import { computed, type CSSProperties } from 'vue'
import { useTheme } from '@/theme/useTheme'

const props = withDefaults(defineProps<{
  padding?: string
  margin?: string
  radius?: string
  elevated?: boolean
  shadow?: boolean
  background?: string
}>(), {
  padding: '',
  margin: '',
  radius: '',
  elevated: false,
  shadow: true,
  background: ''
})

const { currentTheme } = useTheme()

const cardStyle = computed<CSSProperties>(() => {
  const theme = currentTheme.value
  return {
    background: props.background || (props.elevated ? theme.components.card.elevatedBg : theme.components.card.bg),
    borderRadius: props.radius || theme.components.card.radius,
    boxShadow: props.shadow ? theme.components.card.shadow : 'none',
    padding: props.padding || theme.spacing.md,
    margin: props.margin || '0',
    boxSizing: 'border-box'
  }
})
</script>

<style scoped lang="scss">
.app-card {
  width: 100%;
}
</style>
