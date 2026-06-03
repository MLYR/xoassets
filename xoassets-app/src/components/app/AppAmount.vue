<template>
  <text class="app-amount" :style="amountStyle">{{ displayValue }}</text>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTheme } from '@/theme/useTheme'

type AmountSize = 'sm' | 'md' | 'lg'
type AmountTone = 'auto' | 'positive' | 'negative' | 'neutral'

const props = withDefaults(defineProps<{
  value: number | string
  prefix?: string
  decimals?: number
  signed?: boolean
  size?: AmountSize
  tone?: AmountTone
}>(), {
  prefix: '',
  decimals: 2,
  signed: false,
  size: 'md',
  tone: 'auto'
})

const { currentTheme } = useTheme()

const numericValue = computed(() => Number(props.value || 0))

const displayValue = computed(() => {
  const value = numericValue.value
  const numberText = Number.isFinite(value)
    ? value.toLocaleString('zh-CN', {
        minimumFractionDigits: props.decimals,
        maximumFractionDigits: props.decimals
      })
    : String(props.value)

  if (props.signed && Number.isFinite(value) && value > 0) {
    return `${props.prefix}+${numberText}`
  }
  return `${props.prefix}${numberText}`
})

const resolvedColor = computed(() => {
  const theme = currentTheme.value
  if (props.tone === 'positive') return theme.colors.positive
  if (props.tone === 'negative') return theme.colors.negative
  if (props.tone === 'neutral') return theme.colors.textPrimary
  if (numericValue.value > 0) return theme.colors.positive
  if (numericValue.value < 0) return theme.colors.negative
  return theme.colors.textPrimary
})

const resolvedFontSize = computed(() => {
  const typography = currentTheme.value.typography
  if (props.size === 'sm') return typography.amountSm
  if (props.size === 'lg') return typography.amountHuge
  return typography.amountMd
})

const amountStyle = computed(() => ({
  color: resolvedColor.value,
  fontSize: resolvedFontSize.value,
  fontWeight: '700',
  fontVariantNumeric: 'tabular-nums'
}))
</script>

<style scoped lang="scss">
.app-amount {
  display: inline-flex;
  align-items: baseline;
}
</style>
