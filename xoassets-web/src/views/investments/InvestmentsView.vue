<!-- 投资持仓页：展示真实持仓、市值、浮动盈亏，并支持手动维护。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">投资持仓</h1>
        <p class="page-subtitle">查看投资账户配置、收益和持仓结构</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Plus" @click="openAssetDialog">新增资产</el-button>
        <el-button type="primary" :icon="Plus" @click="openHoldingDialog">新增持仓</el-button>
      </div>
    </div>

    <section class="grid-4">
      <MetricCard title="持仓市值" :value="marketValue" :trend="0" description="实时估算" tone="success" />
      <MetricCard title="持仓成本" :value="totalCost" :trend="0" description="移动平均" tone="primary" />
      <MetricCard title="浮动盈亏" :value="totalProfit" :trend="0" description="未实现" :tone="totalProfit >= 0 ? 'success' : 'danger'" />
      <MetricCard title="持仓数量" :value="holdings.length" :trend="0" description="资产数" tone="warning" />
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

    <section v-loading="loading" class="panel">
      <el-empty v-if="!loading && holdings.length === 0" description="还没有投资持仓，新增资产和持仓后即可开始记录投资" />
      <el-table v-else :data="holdings" stripe>
        <el-table-column label="名称" min-width="150">
          <template #default="{ row }">
            <strong>{{ row.assetName || '-' }}</strong>
            <small class="muted-line">{{ row.assetType || '-' }}</small>
          </template>
        </el-table-column>
        <el-table-column prop="symbol" label="代码" />
        <el-table-column label="数量" align="right">
          <template #default="{ row }">{{ formatQuantity(row.quantity) }}</template>
        </el-table-column>
        <el-table-column label="当前价" align="right">
          <template #default="{ row }"><AmountText :value="row.latestPrice || 0" /></template>
        </el-table-column>
        <el-table-column label="市值" align="right">
          <template #default="{ row }"><AmountText :value="row.marketValue" /></template>
        </el-table-column>
        <el-table-column label="成本" align="right">
          <template #default="{ row }"><AmountText :value="row.totalCost" /></template>
        </el-table-column>
        <el-table-column label="收益" align="right">
          <template #default="{ row }"><AmountText :value="row.floatingProfit" with-sign /></template>
        </el-table-column>
        <el-table-column label="收益率">
          <template #default="{ row }"><TrendValue :value="row.floatingProfitRate" /></template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTradeDialog(row, 'BUY')">买入</el-button>
            <el-button link type="primary" @click="openTradeDialog(row, 'SELL')">卖出</el-button>
            <el-button link @click="handleRefreshQuote(row)">刷新</el-button>
            <el-button link @click="openQuoteDialog(row)">价格</el-button>
            <el-button link type="danger" @click="handleDeleteHolding(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="assetDialogVisible" title="新增资产" width="440px">
      <el-form label-position="top" @submit.prevent="handleCreateAsset">
        <el-form-item label="资产名称"><el-input v-model.trim="assetForm.name" placeholder="例如：比特币" /></el-form-item>
        <el-form-item label="资产代码"><el-input v-model.trim="assetForm.symbol" placeholder="例如：BTC" /></el-form-item>
        <el-form-item label="资产类型">
          <el-select v-model="assetForm.type" class="full-width">
            <el-option label="股票" value="STOCK" />
            <el-option label="基金" value="FUND" />
            <el-option label="虚拟货币" value="CRYPTO" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="币种"><el-input v-model.trim="assetForm.currency" placeholder="CNY" /></el-form-item>
        <el-form-item label="行情来源">
          <el-select v-model="assetForm.quoteSource" class="full-width">
            <el-option label="手动" value="MANUAL" />
            <el-option label="CoinGecko" value="COINGECKO" />
            <el-option label="Alpha Vantage" value="ALPHA_VANTAGE" />
            <el-option label="TuShare" value="TUSHARE" />
            <el-option label="AKShare" value="AKSHARE" />
          </el-select>
        </el-form-item>
        <el-form-item label="行情键"><el-input v-model.trim="assetForm.quoteKey" placeholder="后续接行情源使用" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateAsset">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="holdingDialogVisible" title="新增持仓" width="440px">
      <el-form label-position="top" @submit.prevent="handleCreateHolding">
        <el-form-item label="资产">
          <el-select v-model="holdingForm.assetId" class="full-width" filterable remote reserve-keyword :remote-method="searchAssets" :loading="assetLoading" placeholder="搜索资产名称或代码">
            <el-option v-for="asset in assets" :key="asset.id" :label="`${asset.name} (${asset.symbol})`" :value="asset.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input-number v-model="holdingForm.quantity" class="full-width" :min="0" :precision="10" /></el-form-item>
        <el-form-item label="平均成本"><el-input-number v-model="holdingForm.avgCost" class="full-width" :min="0" :precision="4" /></el-form-item>
        <el-form-item label="备注"><el-input v-model.trim="holdingForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="holdingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateHolding">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tradeDialogVisible" :title="tradeForm.type === 'BUY' ? '买入' : '卖出'" width="440px">
      <el-form label-position="top" @submit.prevent="handleCreateTrade">
        <el-form-item label="资产"><el-input :model-value="activeHolding?.assetName || '-'" disabled /></el-form-item>
        <el-form-item label="数量"><el-input-number v-model="tradeForm.quantity" class="full-width" :min="0" :precision="10" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="tradeForm.price" class="full-width" :min="0" :precision="4" /></el-form-item>
        <el-form-item label="手续费"><el-input-number v-model="tradeForm.fee" class="full-width" :min="0" :precision="4" /></el-form-item>
        <el-form-item label="交易时间"><el-date-picker v-model="tradeForm.transactionTime" type="datetime" class="full-width" /></el-form-item>
        <el-form-item label="备注"><el-input v-model.trim="tradeForm.note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tradeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateTrade">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quoteDialogVisible" title="手动更新价格" width="420px">
      <el-form label-position="top" @submit.prevent="handleManualQuote">
        <el-form-item label="资产"><el-input :model-value="activeHolding?.assetName || '-'" disabled /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="quoteForm.price" class="full-width" :min="0" :precision="4" /></el-form-item>
        <el-form-item label="币种"><el-input v-model.trim="quoteForm.currency" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quoteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleManualQuote">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// 投资页接入真实接口；阶段一只支持手动资产和手动价格。
import { computed, onMounted, reactive, ref } from 'vue';
import type { EChartsOption } from 'echarts';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import TrendValue from '@/components/finance/TrendValue.vue';
import { investmentApi, type AssetItem, type AssetRequest, type HoldingItem, type InvestmentTransactionType } from '@/services/investmentApi';

const holdings = ref<HoldingItem[]>([]);
const assets = ref<AssetItem[]>([]);
const loading = ref(false);
const assetLoading = ref(false);
const submitting = ref(false);
const assetDialogVisible = ref(false);
const holdingDialogVisible = ref(false);
const tradeDialogVisible = ref(false);
const quoteDialogVisible = ref(false);
const activeHolding = ref<HoldingItem | null>(null);
const assetForm = reactive<AssetRequest>({ name: '', symbol: '', type: 'CRYPTO', currency: 'CNY', quoteSource: 'MANUAL', quoteKey: '' });
const holdingForm = reactive({ assetId: '', quantity: 0, avgCost: 0, remark: '' });
const tradeForm = reactive({ type: 'BUY' as InvestmentTransactionType, quantity: 0, price: 0, fee: 0, transactionTime: new Date(), note: '' });
const quoteForm = reactive({ price: 0, currency: 'CNY' });

onMounted(() => {
  loadHoldings();
  searchAssets('');
});

const marketValue = computed(() => holdings.value.reduce((sum, item) => sum + Number(item.marketValue), 0));
const totalCost = computed(() => holdings.value.reduce((sum, item) => sum + Number(item.totalCost), 0));
const totalProfit = computed(() => holdings.value.reduce((sum, item) => sum + Number(item.floatingProfit), 0));
const allocationOption = computed<EChartsOption>(() => ({
  color: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['45%', '72%'], data: holdings.value.map((item) => ({ name: item.assetName || item.symbol || '-', value: item.marketValue })) }]
}));
const profitOption = computed<EChartsOption>(() => ({
  grid: { left: 50, right: 18, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: holdings.value.map((item) => item.symbol || '-') },
  yAxis: { type: 'value' },
  tooltip: { trigger: 'axis' },
  series: [{ type: 'bar', data: holdings.value.map((item) => item.floatingProfit), itemStyle: { color: '#3b82f6', borderRadius: [6, 6, 0, 0] } }]
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

async function searchAssets(keyword: string) {
  assetLoading.value = true;
  try {
    assets.value = await investmentApi.searchAssets({ keyword });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资产搜索失败');
  } finally {
    assetLoading.value = false;
  }
}

function openAssetDialog() {
  assetDialogVisible.value = true;
}

function openHoldingDialog() {
  holdingForm.assetId = '';
  holdingForm.quantity = 0;
  holdingForm.avgCost = 0;
  holdingForm.remark = '';
  holdingDialogVisible.value = true;
}

function openTradeDialog(holding: HoldingItem, type: InvestmentTransactionType) {
  activeHolding.value = holding;
  tradeForm.type = type;
  tradeForm.quantity = 0;
  tradeForm.price = Number(holding.latestPrice || holding.avgCost || 0);
  tradeForm.fee = 0;
  tradeForm.transactionTime = new Date();
  tradeForm.note = '';
  tradeDialogVisible.value = true;
}

function openQuoteDialog(holding: HoldingItem) {
  activeHolding.value = holding;
  quoteForm.price = Number(holding.latestPrice || holding.avgCost || 0);
  quoteForm.currency = holding.currency || 'CNY';
  quoteDialogVisible.value = true;
}

async function handleCreateAsset() {
  if (!assetForm.name || !assetForm.symbol) {
    ElMessage.warning('请输入资产名称和代码');
    return;
  }
  submitting.value = true;
  try {
    const asset = await investmentApi.createAsset(assetForm);
    assets.value = [asset, ...assets.value];
    assetDialogVisible.value = false;
    ElMessage.success('资产已新增');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资产保存失败');
  } finally {
    submitting.value = false;
  }
}

async function handleCreateHolding() {
  if (!holdingForm.assetId) {
    ElMessage.warning('请选择资产');
    return;
  }
  submitting.value = true;
  try {
    await investmentApi.createHolding(holdingForm);
    holdingDialogVisible.value = false;
    ElMessage.success('持仓已新增');
    await loadHoldings();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '持仓保存失败');
  } finally {
    submitting.value = false;
  }
}

async function handleCreateTrade() {
  if (!activeHolding.value || tradeForm.quantity <= 0 || tradeForm.price <= 0) {
    ElMessage.warning('请输入有效的数量和价格');
    return;
  }
  submitting.value = true;
  try {
    await investmentApi.createTransaction({
      holdingId: activeHolding.value.id,
      assetId: activeHolding.value.assetId,
      type: tradeForm.type,
      quantity: tradeForm.quantity,
      price: tradeForm.price,
      fee: tradeForm.fee,
      transactionTime: formatDateTime(tradeForm.transactionTime),
      note: tradeForm.note
    });
    tradeDialogVisible.value = false;
    ElMessage.success(tradeForm.type === 'BUY' ? '买入已记录' : '卖出已记录');
    await loadHoldings();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '交易保存失败');
  } finally {
    submitting.value = false;
  }
}

async function handleManualQuote() {
  if (!activeHolding.value || quoteForm.price <= 0) {
    ElMessage.warning('请输入有效价格');
    return;
  }
  submitting.value = true;
  try {
    await investmentApi.manualQuote({ assetId: activeHolding.value.assetId, price: quoteForm.price, currency: quoteForm.currency });
    quoteDialogVisible.value = false;
    ElMessage.success('价格已更新');
    await loadHoldings();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '价格更新失败');
  } finally {
    submitting.value = false;
  }
}

async function handleRefreshQuote(holding: HoldingItem) {
  submitting.value = true;
  try {
    await investmentApi.refreshQuote({ assetId: holding.assetId });
    ElMessage.success('行情已刷新');
    await loadHoldings();
  } catch (error) {
    // 刷新失败只提示错误，列表继续使用最近一次价格快照展示。
    ElMessage.error(error instanceof Error ? error.message : '行情刷新失败');
  } finally {
    submitting.value = false;
  }
}

async function handleDeleteHolding(holding: HoldingItem) {
  try {
    await ElMessageBox.confirm(`确认删除持仓「${holding.assetName || holding.symbol}」吗？`, '删除持仓', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' });
    await investmentApi.removeHolding(holding.id);
    ElMessage.success('持仓已删除');
    await loadHoldings();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '持仓删除失败');
    }
  }
}

function formatDateTime(date: Date) {
  const pad = (value: number) => `${value}`.padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function formatQuantity(value: number) {
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 10 });
}
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

.header-actions {
  display: flex;
  gap: 10px;
}

.muted-line {
  display: block;
  margin-top: 4px;
  color: var(--xo-muted);
}

.full-width {
  width: 100%;
}
</style>
