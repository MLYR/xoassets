<!-- 趋势值组件：统一展示上涨、下跌及对比描述。 -->
<template>
  <span :class="['trend-value', trendClass]">
    <el-icon><component :is="iconComponent" /></el-icon>
    <span>{{ displayValue }}</span>
    <span v-if="description" class="trend-desc">{{ description }}</span>
  </span>
</template>

<script setup lang="ts">
// 根据数值正负自动选择箭头和颜色。
import { computed } from 'vue';
import { TopRight, BottomRight } from '@element-plus/icons-vue';
import { formatPercent } from '@/utils/format';

const props = defineProps<{
  value: number;
  description?: string;
  precision?: number;
}>();

// 百分比展示统一保留一位小数。
const displayValue = computed(() => formatPercent(props.value, props.precision ?? 1));

// 正数和零按上涨样式展示，负数按下跌样式展示。
const trendClass = computed(() => (props.value >= 0 ? 'is-up' : 'is-down'));

// 根据趋势方向切换图标。
const iconComponent = computed(() => (props.value >= 0 ? TopRight : BottomRight));
</script>

<style scoped>
/* 趋势值使用内联布局，适配指标卡和表格。 */
.trend-value {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
}

.is-up {
  color: var(--xo-success);
}

.is-down {
  color: var(--xo-danger);
}

.trend-desc {
  color: var(--xo-muted);
  margin-left: 4px;
}
</style>
