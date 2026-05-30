<!-- 数据分析页：集中展示资产、收支和消费结构图表。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">数据分析</h1>
        <p class="page-subtitle">从趋势、分类和结余三个维度观察财务表现</p>
      </div>
      <el-segmented v-model="period" :options="['本月', '近三月', '全年']" />
    </div>

    <section class="grid-3">
      <MetricCard title="平均日支出" :value="312.4" :trend="-4.8" description="较上月" tone="success" />
      <MetricCard title="收入稳定度" :value="92.6" :trend="3.4" description="评分提升" tone="primary" />
      <MetricCard title="储蓄率基准" :value="28.1" :trend="5.2" description="百分点" tone="success" />
    </section>

    <section class="grid-2">
      <div class="panel panel-padding">
        <h3>资产趋势</h3>
        <BaseChart :option="assetOption" />
      </div>
      <div class="panel panel-padding">
        <h3>支出分类</h3>
        <BaseChart :option="expenseOption" />
      </div>
      <div class="panel panel-padding wide">
        <h3>月度结余</h3>
        <BaseChart :option="balanceOption" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
// 分析页复用 ECharts 基础封装，所有图表风格保持一致。
import { computed, ref } from 'vue';
import type { EChartsOption } from 'echarts';
import BaseChart from '@/components/charts/BaseChart.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import { financeService } from '@/services/financeService';

// 统计周期暂为本地状态，后续接入 API 时作为查询参数。
const period = ref('本月');
// 统计页图表数据来自统一 mock 服务。
const data = financeService.getAnalytics();

// 资产趋势折线图配置。
const assetOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 44, right: 18, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: data.assetTrend.map((item) => item.name) },
  yAxis: { type: 'value' },
  series: [{ type: 'line', smooth: true, data: data.assetTrend.map((item) => item.value), lineStyle: { color: '#3b82f6', width: 3 }, itemStyle: { color: '#3b82f6' } }]
}));

// 支出分类饼图配置。
const expenseOption = computed<EChartsOption>(() => ({
  color: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: '72%', data: data.expenseBreakdown }]
}));

// 月度结余柱状图配置。
const balanceOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 44, right: 18, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: data.monthlyBalance.map((item) => item.name) },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: data.monthlyBalance.map((item) => item.value), itemStyle: { color: '#10b981', borderRadius: [6, 6, 0, 0] } }]
}));
</script>

<style scoped>
/* 分析页第三张图跨两列，形成和原型一致的主次层级。 */
h3 {
  margin: 0 0 16px;
  font-size: 18px;
}

.wide {
  grid-column: 1 / -1;
}
</style>
