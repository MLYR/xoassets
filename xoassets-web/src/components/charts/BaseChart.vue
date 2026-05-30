<!-- ECharts 基础封装：页面只传 option，生命周期和自适应在这里处理。 -->
<template>
  <div ref="chartRef" class="base-chart" :style="{ height }" />
</template>

<script setup lang="ts">
// 使用 ResizeObserver 和 window resize 保证图表在卡片布局中稳定适配。
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import * as echarts from 'echarts';
import type { EChartsOption } from 'echarts';

const props = withDefaults(
  defineProps<{
    option: EChartsOption;
    height?: string;
  }>(),
  {
    height: '280px'
  }
);

const chartRef = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;
let observer: ResizeObserver | null = null;

// 渲染或更新图表；option 变化时复用已有实例，避免重复初始化。
function renderChart() {
  if (!chartRef.value) {
    return;
  }
  chart ??= echarts.init(chartRef.value);
  chart.setOption(props.option, true);
}

// 外层布局尺寸变化时重新计算画布尺寸。
function resizeChart() {
  chart?.resize();
}

// 深度监听图表配置，页面只要更新 option 就能触发重绘。
watch(() => props.option, renderChart, { deep: true });

onMounted(() => {
  renderChart();
  window.addEventListener('resize', resizeChart);
  if (chartRef.value) {
    observer = new ResizeObserver(resizeChart);
    observer.observe(chartRef.value);
  }
});

// 释放监听器和 ECharts 实例，避免页面切换后的内存占用。
onBeforeUnmount(() => {
  observer?.disconnect();
  window.removeEventListener('resize', resizeChart);
  chart?.dispose();
});
</script>

<style scoped>
/* 图表容器固定宽度 100%，高度由页面传入。 */
.base-chart {
  width: 100%;
  min-width: 0;
}
</style>
