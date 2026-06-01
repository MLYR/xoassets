<!-- 投资主页：只展示投资统计和分布，持仓明细与操作集中到投资明细页。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">投资持仓</h1>
        <p class="page-subtitle">按基金、股票和虚拟货币查看投资规模与收益</p>
      </div>
      <div class="header-actions">
        <el-select v-model="displayCurrency" class="currency-select" placeholder="展示币种">
          <el-option v-for="item in currencyOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="$router.push(ROUTES.investmentDetails)">投资明细</el-button>
      </div>
    </div>

    <section class="grid-4">
      <MetricCard title="总投资" :value="totalMarketValue" :trend="totalProfitRate" description="全部持仓市值" :tone="totalProfit >= 0 ? 'success' : 'danger'" :precision="4" :currency-symbol="currencySymbol" />
      <MetricCard title="基金" :value="typeStats.FUND.marketValue" :trend="typeStats.FUND.profitRate" description="基金持仓" tone="primary" :precision="4" :currency-symbol="currencySymbol" />
      <MetricCard title="股票" :value="typeStats.STOCK.marketValue" :trend="typeStats.STOCK.profitRate" description="股票持仓" tone="warning" :precision="4" :currency-symbol="currencySymbol" />
      <MetricCard title="虚拟货币" :value="typeStats.CRYPTO.marketValue" :trend="typeStats.CRYPTO.profitRate" description="币种持仓" tone="success" :precision="4" :currency-symbol="currencySymbol" />
    </section>

    <section class="grid-2">
      <div class="panel panel-padding">
        <div class="panel-head">
          <h3>投资分布</h3>
        </div>
        <el-empty v-if="!loading && holdings.length === 0" description="暂无投资分布数据" />
        <BaseChart v-else :option="allocationOption" />
      </div>
      <div class="panel panel-padding">
        <div class="panel-head">
          <h3>收益贡献</h3>
        </div>
        <el-empty v-if="!loading && holdings.length === 0" description="暂无收益贡献数据" />
        <BaseChart v-else :option="profitOption" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
// 主页只做聚合展示；所有持仓增删改和交易操作放在明细页，减少首页干扰。
import { computed, onMounted, ref } from 'vue';
import type { EChartsOption } from 'echarts';
import { ElMessage } from 'element-plus';
import BaseChart from '@/components/charts/BaseChart.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import { ROUTES } from '@/constants/routes';
import { exchangeRateApi } from '@/services/exchangeRateApi';
import { investmentApi, type AssetType, type HoldingItem } from '@/services/investmentApi';

type DisplayCurrency = 'CNY' | 'USD';

const holdings = ref<HoldingItem[]>([]);
const loading = ref(false);
const displayCurrency = ref<DisplayCurrency>('CNY');
const usdCnyRate = ref(7.2);
const currencyOptions = [
  { label: '人民币', value: 'CNY' },
  { label: 'USD', value: 'USD' }
];

onMounted(() => {
  loadHoldings();
  loadExchangeRate();
});

const currencySymbol = computed(() => (displayCurrency.value === 'CNY' ? '¥' : '$'));
const totalMarketValue = computed(() => round4(holdings.value.reduce((sum, item) => sum + convertAmount(item.marketValue, item.currency), 0)));
const totalCost = computed(() => round4(holdings.value.reduce((sum, item) => sum + convertAmount(item.totalCost, item.currency), 0)));
const totalProfit = computed(() => round4(holdings.value.reduce((sum, item) => sum + convertAmount(item.floatingProfit, item.currency), 0)));
const totalProfitRate = computed(() => rate4(totalProfit.value, totalCost.value));
const typeStats = computed(() => ({
  FUND: calcTypeStat('FUND'),
  STOCK: calcTypeStat('STOCK'),
  CRYPTO: calcTypeStat('CRYPTO')
}));
const allocationOption = computed<EChartsOption>(() => ({
  color: ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#14b8a6', '#6366f1'],
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['45%', '72%'],
    // 投资分布按具体产品展示，避免只看到基金/股票/币的大类桶。
    data: holdings.value
      .map((item) => ({ name: item.assetName || item.symbol || typeLabel(item.assetType), value: round4(convertAmount(item.marketValue, item.currency)) }))
      .filter((item) => item.value > 0)
  }]
}));
const profitOption = computed<EChartsOption>(() => ({
  grid: { left: 50, right: 18, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: holdings.value.map((item) => item.assetName || item.symbol || '-') },
  yAxis: { type: 'value' },
  tooltip: { trigger: 'axis' },
  series: [{ type: 'bar', data: holdings.value.map((item) => round4(convertAmount(item.floatingProfit, item.currency))), itemStyle: { color: '#3b82f6', borderRadius: [6, 6, 0, 0] } }]
}));

async function loadHoldings() {
  loading.value = true;
  try {
    holdings.value = await investmentApi.listHoldings();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '持仓加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadExchangeRate() {
  try {
    const result = await exchangeRateApi.usdCny();
    usdCnyRate.value = Number(result.rate || usdCnyRate.value);
  } catch {
    // 汇率接口失败时保留默认值，币种切换仍可用。
  }
}

function calcTypeStat(type: AssetType) {
  const items = holdings.value.filter((item) => item.assetType === type);
  const marketValue = round4(items.reduce((sum, item) => sum + convertAmount(item.marketValue, item.currency), 0));
  const cost = round4(items.reduce((sum, item) => sum + convertAmount(item.totalCost, item.currency), 0));
  const profit = round4(items.reduce((sum, item) => sum + convertAmount(item.floatingProfit, item.currency), 0));
  return { marketValue, profitRate: rate4(profit, cost) };
}

function convertAmount(value: number, sourceCurrency?: string | null) {
  const source = sourceCurrency || 'CNY';
  if (source === displayCurrency.value) {
    return round4(Number(value));
  }
  if (source === 'USD' && displayCurrency.value === 'CNY') {
    return round4(Number(value) * usdCnyRate.value);
  }
  if (source === 'CNY' && displayCurrency.value === 'USD') {
    return round4(Number(value) / usdCnyRate.value);
  }
  return round4(Number(value));
}

function rate4(profit: number, cost: number) {
  return cost <= 0 ? 0 : round4((profit / cost) * 100);
}

function round4(value: number) {
  return Number(Number(value || 0).toFixed(4));
}

function typeLabel(type?: string | null) {
  return ({ FUND: '基金', STOCK: '股票', CRYPTO: '虚拟货币', OTHER: '其他' } as Record<string, string>)[type || ''] || '-';
}
</script>

<style scoped>
/* 投资主页突出统计，操作入口集中到投资明细页。 */
.panel-head {
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 0;
  font-size: 18px;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.currency-select {
  width: 120px;
}
</style>
