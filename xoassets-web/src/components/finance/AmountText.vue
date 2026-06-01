<!-- 金额文本组件：所有金额展示统一经过这里处理。 -->
<template>
  <span :class="['amount-text', toneClass]">{{ displayValue }}</span>
</template>

<script setup lang="ts">
// 金额显示支持正负色彩和是否展示正号。
import { computed } from 'vue';
import { formatAmount } from '@/utils/format';

const props = withDefaults(
  defineProps<{
    value: number;
    withSign?: boolean;
    muted?: boolean;
    precision?: number;
    currencySymbol?: string;
  }>(),
  {
    withSign: false,
    muted: false,
    precision: 2,
    currencySymbol: '¥'
  }
);

// 统一格式化金额，保证各页面小数位和货币符号一致。
const displayValue = computed(() => formatAmount(props.value, props.withSign, props.precision, props.currencySymbol));

// 根据金额正负和 muted 状态选择展示色。
const toneClass = computed(() => {
  if (props.muted) {
    return 'is-muted';
  }
  if (props.value > 0 && props.withSign) {
    return 'is-positive';
  }
  if (props.value < 0) {
    return 'is-negative';
  }
  return '';
});
</script>

<style scoped>
/* 金额颜色遵循收入绿色、支出红色，数字固定宽度便于表格对齐。 */
.amount-text {
  color: var(--xo-text);
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  white-space: nowrap;
}

.is-positive {
  color: var(--xo-success);
}

.is-negative {
  color: var(--xo-danger);
}

.is-muted {
  color: var(--xo-muted);
  font-weight: 500;
}
</style>
