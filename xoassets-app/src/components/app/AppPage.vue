<template>
  <view
    class="app-page"
    :style="pageStyle"
  >
    <slot />
  </view>
</template>

<script setup lang="ts">
import { computed, type CSSProperties } from 'vue'
import { useTheme } from '@/theme/useTheme'

const props = withDefaults(defineProps<{
  padding?: boolean
  safeTop?: boolean
  safeBottom?: boolean
  gap?: string
  background?: string
}>(), {
  padding: true,
  safeTop: false,
  safeBottom: true,
  gap: '',
  background: ''
})

const { currentTheme } = useTheme()

const pageStyle = computed<CSSProperties>(() => {
  const theme = currentTheme.value
  return {
    background: props.background || theme.components.page.background || theme.backgrounds.page,
    paddingLeft: props.padding ? theme.components.page.paddingX : '0',
    paddingRight: props.padding ? theme.components.page.paddingX : '0',
    paddingTop: props.padding
      ? (props.safeTop ? `calc(${theme.components.page.paddingY} + env(safe-area-inset-top, 0px))` : theme.components.page.paddingY)
      : '0',
    paddingBottom: props.padding
      ? (props.safeBottom ? `calc(${theme.components.page.paddingY} + env(safe-area-inset-bottom, 0px))` : theme.components.page.paddingY)
      : '0',
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    rowGap: props.gap || theme.components.page.sectionGap,
    boxSizing: 'border-box'
  }
})
</script>

<style scoped lang="scss">
.app-page {
  width: 100%;
}
</style>
