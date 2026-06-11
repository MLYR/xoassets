<!-- 数据分析核心 KPI：复用 MetricCard 展示分析页最高频指标。 -->
<template>
  <section class="grid-4 analysis-kpi-grid">
    <MetricCard title="净资产" :value="latestNetAsset" :trend="0" description="最新快照" tone="primary" />
    <MetricCard title="本期结余" :value="periodBalance" :trend="0" description="收入 - 支出" :tone="periodBalance >= 0 ? 'success' : 'danger'" with-sign />
    <MetricCard title="投资浮盈" :value="latestInvestmentProfit" :trend="0" description="最新投资快照" :tone="investmentProfitTone" with-sign />
    <MetricCard title="预算剩余" :value="budgetRemaining" :trend="0" description="所选月份预算" :tone="budgetRemaining >= 0 ? 'success' : 'danger'" with-sign />
  </section>
</template>

<script setup lang="ts">
// KPI 入参已在页面容器中归一，组件只负责展示正负状态。
import { computed } from 'vue';
import MetricCard from '@/components/finance/MetricCard.vue';

const props = defineProps<{
  latestNetAsset: number | null;
  periodBalance: number;
  latestInvestmentProfit: number | null;
  budgetRemaining: number;
}>();

// 投资趋势可能为空，空值不强行归入正收益或负收益。
const investmentProfitTone = computed(() => (props.latestInvestmentProfit === null || props.latestInvestmentProfit >= 0 ? 'success' : 'danger'));
</script>

<style scoped>
/* KPI 区和通用 grid-4 共享断点，仅补充金额卡之间的稳定高度。 */
.analysis-kpi-grid {
  align-items: stretch;
}
</style>
