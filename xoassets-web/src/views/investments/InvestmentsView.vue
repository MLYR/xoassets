<!-- 投资持仓页：展示组合指标、配置图和持仓表。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">投资持仓</h1>
        <p class="page-subtitle">查看投资账户配置、收益和持仓结构</p>
      </div>
      <el-button type="primary" :icon="Plus">新增持仓</el-button>
    </div>

    <section class="grid-4">
      <MetricCard title="持仓市值" :value="marketValue" :trend="4.6" description="较上月" tone="success" />
      <MetricCard title="累计收益" :value="totalProfit" :trend="7.3" description="较上月" tone="success" />
      <MetricCard title="现金仓位" :value="18500" :trend="-1.1" description="较上周" tone="warning" />
      <MetricCard title="风险敞口" :value="60810" :trend="2.8" description="较上月" tone="primary" />
    </section>

    <section class="grid-2">
      <div class="panel panel-padding">
        <div class="panel-head">
          <h3>资产配置</h3>
        </div>
        <BaseChart :option="allocationOption" />
      </div>
      <div class="panel panel-padding">
        <div class="panel-head">
          <h3>收益贡献</h3>
        </div>
        <BaseChart :option="profitOption" />
      </div>
    </section>

    <section class="panel">
      <el-table :data="holdings" stripe>
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="code" label="代码" />
        <el-table-column label="市值" align="right">
          <template #default="{ row }"><AmountText :value="row.marketValue" /></template>
        </el-table-column>
        <el-table-column label="收益" align="right">
          <template #default="{ row }"><AmountText :value="row.profit" with-sign /></template>
        </el-table-column>
        <el-table-column label="收益率">
          <template #default="{ row }"><TrendValue :value="row.profitRate" /></template>
        </el-table-column>
        <el-table-column prop="allocation" label="占比">
          <template #default="{ row }">{{ row.allocation }}%</template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
// 持仓页图表直接使用持仓数组生成，保持 mock 数据单一来源。
import { computed } from 'vue';
import type { EChartsOption } from 'echarts';
import { Plus } from '@element-plus/icons-vue';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import TrendValue from '@/components/finance/TrendValue.vue';
import { financeService } from '@/services/financeService';

// 持仓列表来自统一 mock 服务。
const holdings = financeService.getInvestments();
// 持仓市值合计。
const marketValue = computed(() => holdings.reduce((sum, item) => sum + item.marketValue, 0));
// 累计浮动收益合计。
const totalProfit = computed(() => holdings.reduce((sum, item) => sum + item.profit, 0));

// 持仓资产配置饼图。
const allocationOption = computed<EChartsOption>(() => ({
  color: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444'],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['45%', '72%'], data: holdings.map((item) => ({ name: item.name, value: item.allocation })) }]
}));

// 持仓收益贡献柱状图。
const profitOption = computed<EChartsOption>(() => ({
  grid: { left: 50, right: 18, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: holdings.map((item) => item.name) },
  yAxis: { type: 'value' },
  tooltip: { trigger: 'axis' },
  series: [{ type: 'bar', data: holdings.map((item) => item.profit), itemStyle: { color: '#3b82f6', borderRadius: [6, 6, 0, 0] } }]
}));
</script>

<style scoped>
/* 图表标题使用与仪表盘一致的卡片标题节奏。 */
.panel-head {
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 0;
  font-size: 18px;
}
</style>
