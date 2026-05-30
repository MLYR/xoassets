<!-- 首页仪表盘：资产指标、趋势图、支出结构和最近流水。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">首页</h1>
        <p class="page-subtitle">欢迎回来，这是您的财务概览</p>
      </div>
    </div>

    <section class="grid-4">
      <MetricCard v-for="metric in data.dashboardMetrics" :key="metric.title" v-bind="metric" />
    </section>

    <section class="dashboard-grid">
      <div class="panel panel-padding chart-panel">
        <div class="panel-head">
          <div>
            <h3>资产趋势</h3>
            <p>近 30 天资产变化</p>
          </div>
          <el-segmented v-model="range" :options="['7天', '30天', '90天']" />
        </div>
        <BaseChart :option="assetOption" />
      </div>
      <div class="panel panel-padding">
        <div class="panel-head">
          <div>
            <h3>支出分析</h3>
            <p>本月分类占比</p>
          </div>
        </div>
        <BaseChart :option="expenseOption" height="210px" />
        <div class="legend-list">
          <div v-for="item in data.expenseBreakdown" :key="item.name" class="legend-row">
            <span><i />{{ item.name }}</span>
            <AmountText :value="item.value" muted />
          </div>
        </div>
      </div>
    </section>

    <section class="panel panel-padding">
      <div class="panel-head">
        <h3>最近交易</h3>
        <el-button link type="primary" @click="$router.push(ROUTES.transactions)">查看全部</el-button>
      </div>
      <el-table :data="data.transactions" stripe>
        <el-table-column prop="date" label="日期" min-width="150" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }"><StatusBadge :label="row.type" /></template>
        </el-table-column>
        <el-table-column prop="category" label="分类" />
        <el-table-column prop="account" label="账户" min-width="150" />
        <el-table-column label="金额" align="right">
          <template #default="{ row }"><AmountText :value="row.amount" with-sign /></template>
        </el-table-column>
        <el-table-column label="余额" align="right">
          <template #default="{ row }"><AmountText :value="row.balance ?? 0" /></template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
// 仪表盘图表配置使用 computed，后续数据变化时自动更新。
import { computed, ref } from 'vue';
import type { EChartsOption } from 'echarts';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { financeService } from '@/services/financeService';
import { ROUTES } from '@/constants/routes';

// 趋势周期目前仅用于 UI 展示。
const range = ref('30天');
// 首页聚合数据来自 financeService。
const data = financeService.getDashboard();

// 资产趋势折线图配置。
const assetOption = computed<EChartsOption>(() => ({
  grid: { left: 44, right: 16, top: 24, bottom: 32 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: data.assetTrend.map((item) => item.name), axisLine: { lineStyle: { color: '#e2e8f0' } } },
  yAxis: { type: 'value', axisLabel: { formatter: (value: number) => `${Math.round(value / 1000)}k` }, splitLine: { lineStyle: { color: '#e2e8f0' } } },
  series: [{ type: 'line', smooth: true, data: data.assetTrend.map((item) => item.value), symbolSize: 7, lineStyle: { color: '#3b82f6', width: 3 }, itemStyle: { color: '#3b82f6' }, areaStyle: { color: 'rgba(59, 130, 246, 0.1)' } }]
}));

// 支出分类饼图配置。
const expenseOption = computed<EChartsOption>(() => ({
  color: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['56%', '78%'], avoidLabelOverlap: true, label: { show: false }, data: data.expenseBreakdown }]
}));
</script>

<style scoped>
/* 首页核心布局为 2:1 图表区，复刻原型的信息密度。 */
.dashboard-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-head h3 {
  margin: 0;
  font-size: 18px;
}

.panel-head p {
  margin: 6px 0 0;
  color: var(--xo-muted);
  font-size: 13px;
}

.legend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.legend-row span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--xo-text);
  font-size: 14px;
}

.legend-row i {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  background: var(--xo-primary);
}

@media (max-width: 1080px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
