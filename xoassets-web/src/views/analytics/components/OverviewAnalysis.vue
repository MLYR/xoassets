<!-- 总览分析：集中展示净资产、收支和资产分布的核心图表。 -->
<template>
  <div class="analysis-grid">
    <section class="panel panel-padding wide">
      <h3>净资产趋势</h3>
      <el-empty v-if="!loading && netAssetsTrend.length === 0" description="暂无净资产趋势数据" />
      <BaseChart v-else :option="netAssetOption" height="320px" />
    </section>

    <section class="panel panel-padding">
      <h3>收支趋势</h3>
      <el-empty v-if="!loading && incomeExpenseTrend.length === 0" description="暂无收支趋势数据" />
      <BaseChart v-else :option="incomeExpenseOption" />
    </section>

    <section class="panel panel-padding">
      <h3>当前资产分布</h3>
      <el-empty v-if="!loading && assetDistribution.length === 0" description="暂无资产分布数据" />
      <BaseChart v-else :option="assetDistributionOption" @chart-click="handleAssetDistributionClick" />
    </section>
  </div>
</template>

<script setup lang="ts">
// 总览 Tab 只消费父级传入的数据，避免重复请求。
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import type { EChartsOption } from 'echarts';
import BaseChart from '@/components/charts/BaseChart.vue';
import { ROUTES } from '@/constants/routes';
import type { AssetSnapshotItem } from '@/services/snapshotApi';
import type { AssetDistributionItem, IncomeExpenseTrendPoint } from '@/services/statisticsApi';

const props = defineProps<{
  loading: boolean;
  netAssetsTrend: AssetSnapshotItem[];
  incomeExpenseTrend: IncomeExpenseTrendPoint[];
  assetDistribution: AssetDistributionItem[];
}>();
const router = useRouter();

const netAssetOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 44, right: 18, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: props.netAssetsTrend.map((item) => item.snapshotDate) },
  yAxis: { type: 'value' },
  series: [{ name: '净资产', type: 'line', smooth: true, data: props.netAssetsTrend.map((item) => item.netAsset), lineStyle: { color: chartColor('--xo-chart-blue'), width: 3 }, itemStyle: { color: chartColor('--xo-chart-blue') }, areaStyle: { color: chartColor('--xo-primary-soft') } }]
}));

const incomeExpenseOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 44, right: 18, top: 36, bottom: 36 },
  xAxis: { type: 'category', data: props.incomeExpenseTrend.map((item) => item.month) },
  yAxis: { type: 'value' },
  series: [
    { name: '收入', type: 'bar', data: props.incomeExpenseTrend.map((item) => item.income), itemStyle: { color: chartColor('--xo-chart-green'), borderRadius: [10, 10, 0, 0] } },
    { name: '支出', type: 'bar', data: props.incomeExpenseTrend.map((item) => item.expense), itemStyle: { color: chartColor('--xo-chart-red'), borderRadius: [10, 10, 0, 0] } },
    { name: '结余', type: 'line', smooth: true, data: props.incomeExpenseTrend.map((item) => item.balance), lineStyle: { color: chartColor('--xo-chart-blue'), width: 3 }, itemStyle: { color: chartColor('--xo-chart-blue') } }
  ]
}));

const assetDistributionOption = computed<EChartsOption>(() => ({
  color: [chartColor('--xo-chart-blue'), chartColor('--xo-chart-green'), chartColor('--xo-chart-purple'), chartColor('--xo-chart-yellow'), chartColor('--xo-chart-red')],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['44%', '72%'], data: props.assetDistribution.map((item) => ({ name: item.name, value: item.value, refId: item.refId || null, refType: item.refType || null })) }]
}));

// 图表颜色从主题变量读取，暗色模式下跟随全局 token。
function chartColor(name: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

function handleAssetDistributionClick(params: unknown) {
  const data = (params as { data?: { refId?: string | null; refType?: string | null } }).data;
  if (!data?.refId) {
    return;
  }
  if (data.refType === 'ACCOUNT') {
    router.push(ROUTES.accountDetail.replace(':id', data.refId));
    return;
  }
  if (data.refType === 'HOLDING') {
    router.push(ROUTES.holdingDetail.replace(':id', data.refId));
  }
}
</script>
