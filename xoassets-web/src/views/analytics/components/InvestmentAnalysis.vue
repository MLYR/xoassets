<!-- 投资分析：第一阶段使用 statistics 投资趋势展示市值、成本和浮盈。 -->
<template>
  <div class="analysis-grid">
    <section class="panel panel-padding wide">
      <h3>投资盈亏趋势</h3>
      <el-empty v-if="!loading && investmentTrend.length === 0" description="暂无投资盈亏数据" />
      <BaseChart v-else :option="profitOption" height="320px" />
    </section>

    <section class="panel panel-padding">
      <h3>投资市值趋势</h3>
      <el-empty v-if="!loading && investmentTrend.length === 0" description="暂无投资市值数据" />
      <BaseChart v-else :option="marketValueOption" />
    </section>

    <section class="panel panel-padding">
      <h3>投资成本趋势</h3>
      <el-empty v-if="!loading && investmentTrend.length === 0" description="暂无投资成本数据" />
      <BaseChart v-else :option="costOption" />
    </section>
  </div>
</template>

<script setup lang="ts">
// 第一阶段不接投资模块接口，避免把基金/股票/虚拟货币收益口径提前混合。
import { computed } from 'vue';
import type { EChartsOption } from 'echarts';
import BaseChart from '@/components/charts/BaseChart.vue';
import type { InvestmentProfitTrendPoint } from '@/services/statisticsApi';

const props = defineProps<{
  loading: boolean;
  investmentTrend: InvestmentProfitTrendPoint[];
}>();

const profitOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 44, right: 18, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: props.investmentTrend.map((item) => item.month) },
  yAxis: { type: 'value' },
  series: [{ name: '浮动盈亏', type: 'bar', data: props.investmentTrend.map((item) => item.floatingProfit), itemStyle: { color: chartColor('--xo-chart-green'), borderRadius: [10, 10, 0, 0] } }]
}));

const marketValueOption = computed<EChartsOption>(() => lineOption('投资市值', props.investmentTrend.map((item) => item.marketValue), '--xo-chart-blue'));
const costOption = computed<EChartsOption>(() => lineOption('投资成本', props.investmentTrend.map((item) => item.totalCost), '--xo-chart-yellow'));

// 投资市值和成本使用同一折线配置，减少两个小图之间的视觉差异。
function lineOption(name: string, data: number[], colorVar: string): EChartsOption {
  const color = chartColor(colorVar);
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 18, top: 24, bottom: 36 },
    xAxis: { type: 'category', data: props.investmentTrend.map((item) => item.month) },
    yAxis: { type: 'value' },
    series: [{ name, type: 'line', smooth: true, data, lineStyle: { color, width: 3 }, itemStyle: { color }, areaStyle: { color: chartColor('--xo-primary-soft') } }]
  };
}

function chartColor(name: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}
</script>
