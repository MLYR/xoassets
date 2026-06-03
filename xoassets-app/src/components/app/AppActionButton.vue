<template>
  <view
    class="app-action-button"
    :class="{ 'is-disabled': disabled, 'is-block': block }"
    :style="buttonStyle"
    @click="handleClick"
  >
    <AppIcon
      v-if="icon"
      :name="icon"
      :size="iconSize"
      :color="iconColor"
    />
    <text class="app-action-button-text" :style="textStyle">{{ text }}</text>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'
import type { ButtonVariant } from '@/theme'
import { useTheme } from '@/theme/useTheme'

const props = withDefaults(defineProps<{
  text: string
  type?: ButtonVariant
  icon?: string
  iconSize?: string
  block?: boolean
  disabled?: boolean
  radius?: string
  height?: string
}>(), {
  type: 'primary',
  icon: '',
  iconSize: '28rpx',
  block: false,
  disabled: false,
  radius: '',
  height: ''
})

const emit = defineEmits<{
  click: []
}>()

const { currentTheme, investmentTokens } = useTheme()

const variantConfig = computed(() => currentTheme.value.components.button.variants[props.type])

const buttonStyle = computed(() => ({
  // 投资页三枚交易按钮按原型使用胶囊高度，其它按钮继续走组件默认高度。
  height: props.height || (props.type === 'sell' ? investmentTokens.value.actionCapsuleHeight : currentTheme.value.components.button.height),
  borderRadius: props.radius || currentTheme.value.components.button.radius,
  background: props.disabled ? currentTheme.value.components.button.disabledBg : variantConfig.value.background,
  color: variantConfig.value.text,
  border: variantConfig.value.border ? `2rpx solid ${variantConfig.value.border}` : 'none',
  boxShadow: props.disabled ? 'none' : (variantConfig.value.shadow || 'none'),
  width: props.block ? '100%' : 'auto',
  opacity: props.disabled ? '0.7' : '1'
}))

const textStyle = computed(() => ({
  color: variantConfig.value.text,
  fontSize: currentTheme.value.typography.fontSizeLg,
  fontWeight: '600'
}))

const iconColor = computed(() => variantConfig.value.text)

function handleClick() {
  if (props.disabled) return
  emit('click')
}
</script>

<style scoped lang="scss">
.app-action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  column-gap: 12rpx;
  padding: 0 28rpx;
  box-sizing: border-box;
}

.app-action-button.is-block {
  display: flex;
}

.app-action-button-text {
  white-space: nowrap;
}
</style>
