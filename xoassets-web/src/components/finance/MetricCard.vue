<!-- 指标卡片组件：所有顶部 KPI 卡片统一从这里渲染。 -->
<template>
  <div class="metric-card panel panel-padding">
    <div class="metric-head">
      <span>{{ title }}</span>
      <el-icon :class="['metric-icon', `tone-${tone}`]">
        <component :is="iconComponent" />
      </el-icon>
    </div>
    <AmountText class="metric-value" :value="value" />
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
  }>(),
  {
    tone: 'primary'
  }
);

// 风险或支出类指标使用下降图标，其余使用趋势图标。
const iconComponent = computed(() => (props.tone === 'danger' ? BottomRight : TrendCharts));
</script>

<style scoped>
/* 卡片样式贴近原型：白底、细边框、轻阴影和 8px 圆角。 */
.metric-card {
  min-height: 154px;
}

.metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  color: var(--xo-muted);
  font-size: 14px;
}

.metric-icon {
  font-size: 20px;
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
  font-size: 30px;
  line-height: 1.15;
}
</style>
