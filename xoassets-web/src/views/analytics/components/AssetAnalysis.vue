<!-- 资产分析：拆分净资产、总资产、资产结构和当前分布。 -->
<template>
  <div class="analysis-grid">
    <section class="panel panel-padding">
      <h3>净资产趋势</h3>
      <el-empty v-if="!loading && netAssetsTrend.length === 0" description="暂无净资产趋势数据" />
      <BaseChart v-else :option="netAssetOption" />
    </section>

    <section class="panel panel-padding">
      <h3>总资产趋势</h3>
      <el-empty v-if="!loading && netAssetsTrend.length === 0" description="暂无总资产趋势数据" />
      <BaseChart v-else :option="totalAssetOption" />
    </section>

    <section class="panel panel-padding wide">
      <h3>现金 / 投资资产变化</h3>
      <el-empty v-if="!loading && netAssetsTrend.length === 0" description="暂无资产结构变化数据" />
      <BaseChart v-else :option="assetStructureOption" height="320px" />
    </section>

    <section class="panel panel-padding wide">
      <h3>当前资产分布</h3>
      <el-empty v-if="!loading && assetDistribution.length === 0" description="暂无资产分布数据" />
      <BaseChart v-else :option="assetDistributionOption" height="320px" />
    </section>
  </div>
</template>

<script setup lang="ts">
// 资产分析使用资产快照作为趋势权威来源。
import { computed } from 'vue';
import type { EChartsOption } from 'echarts';
import BaseChart from '@/components/charts/BaseChart.vue';
import type { AssetSnapshotItem } from '@/services/snapshotApi';
import type { AssetDistributionItem } from '@/services/statisticsApi';

const props = defineProps<{
  loading: boolean;
  netAssetsTrend: AssetSnapshotItem[];
  assetDistribution: AssetDistributionItem[];
}>();

const netAssetOption = computed<EChartsOption>(() => lineOption('净资产', props.netAssetsTrend.map((item) => item.snapshotDate), props.netAssetsTrend.map((item) => item.netAsset), '--xo-chart-blue'));
const totalAssetOption = computed<EChartsOption>(() => lineOption('总资产', props.netAssetsTrend.map((item) => item.snapshotDate), props.netAssetsTrend.map((item) => item.totalAsset), '--xo-chart-green'));

const assetStructureOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 44, right: 18, top: 36, bottom: 36 },
  xAxis: { type: 'category', data: props.netAssetsTrend.map((item) => item.snapshotDate) },
  yAxis: { type: 'value' },
  series: [
    { name: '现金资产', type: 'line', smooth: true, data: props.netAssetsTrend.map((item) => item.cashAsset), lineStyle: { color: chartColor('--xo-chart-blue'), width: 3 }, itemStyle: { color: chartColor('--xo-chart-blue') } },
    { name: '投资资产', type: 'line', smooth: true, data: props.netAssetsTrend.map((item) => item.investmentAsset), lineStyle: { color: chartColor('--xo-chart-green'), width: 3 }, itemStyle: { color: chartColor('--xo-chart-green') } }
  ]
}));

const assetDistributionOption = computed<EChartsOption>(() => ({
  color: [chartColor('--xo-chart-blue'), chartColor('--xo-chart-green'), chartColor('--xo-chart-purple'), chartColor('--xo-chart-yellow'), chartColor('--xo-chart-red')],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['42%', '70%'], data: props.assetDistribution.map((item) => ({ name: item.name, value: item.value })) }]
}));

// 单线趋势图共用配置，保持资产类图表视觉一致。
function lineOption(name: string, xData: string[], data: number[], colorVar: string): EChartsOption {
  const color = chartColor(colorVar);
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 18, top: 24, bottom: 36 },
    xAxis: { type: 'category', data: xData },
    yAxis: { type: 'value' },
    series: [{ name, type: 'line', smooth: true, data, lineStyle: { color, width: 3 }, itemStyle: { color }, areaStyle: { color: chartColor('--xo-primary-soft') } }]
  };
}

function chartColor(name: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}
</script>
