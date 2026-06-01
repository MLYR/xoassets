<!-- 指标卡片组件：所有顶部 KPI 卡片统一从这里渲染。 -->
<template>
  <div class="metric-card panel panel-padding">
    <div class="metric-head">
      <span>{{ title }}</span>
      <el-icon :class="['metric-icon', `tone-${tone}`]">
        <component :is="iconComponent" />
      </el-icon>
    </div>
    <AmountText class="metric-value" :value="value" :precision="precision" :currency-symbol="currencySymbol" />
    <TrendValue :value="trend" :description="description" />
  </div>
</template>

<script setup lang="ts">
// 指标卡只接收纯数据，金额和趋势交给统一组件格式化。
import { computed } from 'vue';
import { TrendCharts, BottomRight } from '@element-plus/icons-vue';
import AmountText from './AmountText.vue';
import TrendValue from './TrendValue.vue';

const props = withDefaults(
  defineProps<{
    title: string;
    value: number;
    trend: number;
    description: string;
    tone?: 'success' | 'danger' | 'warning' | 'primary';
    precision?: number;
    currencySymbol?: string;
  }>(),
  {
    tone: 'primary',
    precision: 2,
    currencySymbol: '¥'
  }
);

// 风险或支出类指标使用下降图标，其余使用趋势图标。
const iconComponent = computed(() => (props.tone === 'danger' ? BottomRight : TrendCharts));
</script>

<style scoped>
/* 指标卡强调金额和轻盈层次，hover 仅提供轻微反馈。 */
.metric-card {
  min-height: 154px;
  position: relative;
  overflow: hidden;
  transition: box-shadow 0.2s ease, transform 0.2s ease, border-color 0.2s ease;
}

.metric-card::after {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.08), transparent 42%);
  content: "";
  opacity: 0;
  transition: opacity 0.2s ease;
}

.metric-card:hover {
  border-color: rgba(37, 99, 235, 0.18);
  box-shadow: var(--xo-shadow-hover);
  transform: translateY(-2px);
}

.metric-card:hover::after {
  opacity: 1;
}

.metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  color: #475569;
  font-size: 14px;
  font-weight: 700;
}

.metric-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 12px;
  background: #eff6ff;
  font-size: 18px;
}

.tone-success {
  color: var(--xo-success);
}

.tone-danger {
  color: var(--xo-danger);
}

.tone-warning {
  color: var(--xo-warning);
}

.tone-primary {
  color: var(--xo-primary);
}

.metric-value {
  display: block;
  margin-bottom: 10px;
  font-size: 31px;
  line-height: 1.15;
  letter-spacing: 0;
}
</style>
