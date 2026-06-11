<!-- 数据分析筛选栏：周期控制趋势，月份控制分类支出和预算。 -->
<template>
  <section class="analysis-toolbar panel panel-padding">
    <div class="toolbar-copy">
      <span class="toolbar-kicker">数据分析</span>
      <h2>资产、收支、投资、预算统一复盘</h2>
      <p>周期用于趋势图，月份用于支出分类和预算进度，避免不同口径混在一起。</p>
    </div>

    <div class="toolbar-actions">
      <el-segmented :model-value="period" :options="ANALYTICS_PERIOD_OPTIONS" @update:model-value="updatePeriod" />
      <el-date-picker :model-value="selectedMonth" type="month" value-format="YYYY-MM" :clearable="false" @update:model-value="updateSelectedMonth" />
      <el-button :loading="loading" :icon="Refresh" @click="$emit('refresh')">刷新</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
// 工具栏只负责筛选状态输入，不直接请求数据。
import { Refresh } from '@element-plus/icons-vue';
import { ANALYTICS_PERIOD_OPTIONS, type AnalyticsPeriod } from '../composables/useAnalyticsData';

defineProps<{
  period: AnalyticsPeriod;
  selectedMonth: string;
  loading: boolean;
}>();

const emit = defineEmits<{
  'update:period': [value: AnalyticsPeriod];
  'update:selectedMonth': [value: string];
  refresh: [];
}>();

function updatePeriod(value: string | number | boolean) {
  emit('update:period', value as AnalyticsPeriod);
}

function updateSelectedMonth(value: string) {
  emit('update:selectedMonth', value);
}
</script>

<style scoped>
/* 顶部工具栏在宽屏横向展示，窄屏自动堆叠保证筛选控件可点击。 */
.analysis-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.toolbar-copy {
  min-width: 0;
}

.toolbar-kicker {
  display: inline-flex;
  margin-bottom: 8px;
  color: var(--xo-primary);
  font-size: 13px;
  font-weight: 800;
}

.toolbar-copy h2 {
  margin: 0;
  color: var(--xo-text);
  font-size: 24px;
  line-height: 1.2;
}

.toolbar-copy p {
  max-width: 560px;
  margin: 8px 0 0;
  color: var(--xo-muted);
  font-size: 14px;
  line-height: 1.55;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 860px) {
  .analysis-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}
</style>
