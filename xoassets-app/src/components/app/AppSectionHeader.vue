<template>
  <view class="app-section-header" :style="wrapperStyle">
    <view class="app-section-header-main">
      <view class="app-section-header-title-row">
        <slot name="prefix"></slot>
        <text class="app-section-header-title" :style="titleStyle">{{ title }}</text>
      </view>
      <text v-if="subtitle" class="app-section-header-subtitle" :style="subtitleStyle">{{ subtitle }}</text>
    </view>

    <view
      v-if="actionText || $slots.action"
      class="app-section-header-action"
      @click="emit('action')"
    >
      <slot name="action">
        <text class="app-section-header-action-text" :style="actionStyle">
          {{ actionText }}
          <slot name="actionIcon">
            <text v-if="showArrow"> ›</text>
          </slot>
        </text>
      </slot>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTheme } from '@/theme/useTheme'

const props = withDefaults(defineProps<{
  title: string
  subtitle?: string
  actionText?: string
  showArrow?: boolean
  marginBottom?: string
}>(), {
  subtitle: '',
  actionText: '',
  showArrow: true,
  marginBottom: ''
})

const emit = defineEmits<{
  action: []
}>()

const { currentTheme } = useTheme()

const wrapperStyle = computed(() => ({
  marginBottom: props.marginBottom || currentTheme.value.spacing.sm
}))

const titleStyle = computed(() => ({
  color: currentTheme.value.colors.textPrimary,
  fontSize: currentTheme.value.typography.fontSizeLg,
  fontWeight: '700'
}))

const subtitleStyle = computed(() => ({
  color: currentTheme.value.colors.textSecondary,
  fontSize: currentTheme.value.typography.fontSizeSm
}))

const actionStyle = computed(() => ({
  color: currentTheme.value.colors.primary,
  fontSize: currentTheme.value.typography.fontSizeSm
}))
</script>

<style scoped lang="scss">
.app-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  column-gap: 16rpx;
}

.app-section-header-main {
  display: flex;
  flex-direction: column;
  row-gap: 4rpx;
}

.app-section-header-title-row {
  display: flex;
  align-items: center;
  column-gap: 8rpx;
}

.app-section-header-action {
  flex-shrink: 0;
}
</style>
