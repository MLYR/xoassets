<template>
  <text class="app-amount" :style="amountStyle">{{ displayValue }}</text>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useTheme } from '@/theme/useTheme'

type AmountSize = 'sm' | 'md' | 'lg'
type AmountTone = 'auto' | 'positive' | 'negative' | 'neutral'
type AmountSemantic = 'amount' | 'profit'

const props = withDefaults(defineProps<{
  value: number | string | null | undefined
  prefix?: string
  decimals?: number
  signed?: boolean
  size?: AmountSize
  tone?: AmountTone
  semantic?: AmountSemantic
  color?: string
}>(), {
  prefix: '',
  decimals: 2,
  signed: false,
  size: 'md',
  tone: 'auto',
  semantic: 'amount',
  color: ''
})

const { currentTheme } = useTheme()

const isEmptyValue = computed(() => props.value === null || props.value === undefined || props.value === '')
const numericValue = computed(() => isEmptyValue.value ? null : Number(props.value))

const displayValue = computed(() => {
  if (isEmptyValue.value) {
    return '--'
  }
  const value = numericValue.value
  const numberText = value !== null && Number.isFinite(value)
    ? value.toLocaleString('zh-CN', {
        minimumFractionDigits: props.decimals,
        maximumFractionDigits: props.decimals
      })
    : String(props.value)

  if (props.signed && value !== null && Number.isFinite(value) && value > 0) {
    return `${props.prefix}+${numberText}`
  }
  return `${props.prefix}${numberText}`
})

const resolvedColor = computed(() => {
  const theme = currentTheme.value
  if (props.color) return props.color
  if (props.tone === 'positive') return theme.colors.positive
  if (props.tone === 'negative') return theme.colors.negative
  if (props.tone === 'neutral') return theme.colors.textPrimary
  if (props.semantic === 'profit') {
    if (numericValue.value !== null && numericValue.value > 0) return theme.colors.profitPositive
    if (numericValue.value !== null && numericValue.value < 0) return theme.colors.profitNegative
    return theme.colors.textPrimary
  }
  if (numericValue.value !== null && numericValue.value > 0) return theme.colors.positive
  if (numericValue.value !== null && numericValue.value < 0) return theme.colors.negative
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
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
