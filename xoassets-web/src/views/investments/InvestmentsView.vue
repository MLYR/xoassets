<!-- 投资主页：按持仓直接管理投资，不再暴露“资产 + 持仓”两步概念。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">投资持仓</h1>
        <p class="page-subtitle">按基金、股票和虚拟货币查看投资规模与收益</p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push(ROUTES.investmentDetails)">投资明细</el-button>
        <el-button type="primary" :icon="Plus" @click="openHoldingDialog()">新增持仓</el-button>
      </div>
    </div>

    <section class="grid-4">
      <MetricCard title="总投资" :value="totalMarketValue" :trend="totalProfitRate" description="全部持仓市值" :tone="totalProfit >= 0 ? 'success' : 'danger'" />
      <MetricCard title="基金" :value="typeStats.FUND.marketValue" :trend="typeStats.FUND.profitRate" description="基金持仓" tone="primary" />
      <MetricCard title="股票" :value="typeStats.STOCK.marketValue" :trend="typeStats.STOCK.profitRate" description="股票持仓" tone="warning" />
      <MetricCard title="虚拟货币" :value="typeStats.CRYPTO.marketValue" :trend="typeStats.CRYPTO.profitRate" description="币种持仓" tone="success" />
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

    <section v-loading="loading" class="panel">
      <el-empty v-if="!loading && holdings.length === 0" description="还没有投资持仓，新增第一条持仓后即可跟踪投资" />
      <el-table v-else :data="holdings" stripe>
        <el-table-column label="持仓" min-width="170">
          <template #default="{ row }">
            <strong>{{ row.assetName || '-' }}</strong>
            <small class="muted-line">{{ row.symbol || '-' }} · {{ typeLabel(row.assetType) }}</small>
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="320" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTradeDialog(row, 'BUY')">买入</el-button>
            <el-button link type="primary" @click="openTradeDialog(row, 'SELL')">卖出</el-button>
            <el-button link type="primary" @click="openHoldingDialog(row)">编辑</el-button>
            <el-button link @click="handleRefreshQuote(row)">刷新</el-button>
            <el-button link @click="openQuoteDialog(row)">价格</el-button>
            <el-button link type="danger" @click="handleDeleteHolding(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="holdingDialogVisible" :title="editingHolding ? '编辑持仓' : '新增持仓'" width="520px">
      <el-form label-position="top" @submit.prevent="handleSaveHolding">
        <div class="form-grid">
          <el-form-item label="持仓名称"><el-input v-model.trim="holdingForm.assetName" placeholder="例如：比特币 / 沪深300ETF" /></el-form-item>
          <el-form-item label="资产代码"><el-input v-model.trim="holdingForm.symbol" placeholder="例如：BTC / 510300" /></el-form-item>
          <el-form-item label="资产类型">
            <el-select v-model="holdingForm.assetType" class="full-width">
              <el-option label="基金" value="FUND" />
              <el-option label="股票" value="STOCK" />
              <el-option label="虚拟货币" value="CRYPTO" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="币种">
            <el-select v-model="holdingForm.currency" class="full-width">
              <el-option label="人民币 CNY" value="CNY" />
              <el-option label="美元 USD" value="USD" />
              <el-option label="港币 HKD" value="HKD" />
              <el-option label="欧元 EUR" value="EUR" />
            </el-select>
          </el-form-item>
          <el-form-item label="行情来源">
            <el-select v-model="holdingForm.quoteSource" class="full-width">
              <el-option label="手动" value="MANUAL" />
              <el-option label="CoinGecko" value="COINGECKO" />
              <el-option label="Alpha Vantage" value="ALPHA_VANTAGE" />
              <el-option label="TuShare" value="TUSHARE" />
              <el-option label="AKShare" value="AKSHARE" />
            </el-select>
          </el-form-item>
          <el-form-item label="行情键"><el-input v-model.trim="holdingForm.quoteKey" placeholder="BTC/ETH 或 CoinGecko id" /></el-form-item>
          <el-form-item label="数量"><el-input-number v-model="holdingForm.quantity" class="full-width" :min="0.0000000001" :precision="10" /></el-form-item>
          <el-form-item label="平均成本"><el-input-number v-model="holdingForm.avgCost" class="full-width" :min="0" :precision="4" /></el-form-item>
        </div>
        <el-form-item label="备注"><el-input v-model.trim="holdingForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="holdingDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveHolding">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tradeDialogVisible" :title="tradeForm.type === 'BUY' ? '买入' : '卖出'" width="440px">
      <el-form label-position="top" @submit.prevent="handleCreateTrade">
        <el-form-item label="持仓"><el-input :model-value="activeHolding?.assetName || '-'" disabled /></el-form-item>
        <el-form-item label="数量"><el-input-number v-model="tradeForm.quantity" class="full-width" :min="0.0000000001" :precision="10" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="tradeForm.price" class="full-width" :min="0.0001" :precision="4" /></el-form-item>
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
        <el-form-item label="持仓"><el-input :model-value="activeHolding?.assetName || '-'" disabled /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="quoteForm.price" class="full-width" :min="0.0001" :precision="4" /></el-form-item>
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
// 投资页只让用户管理持仓；公共资产表作为后端内部行情基础数据。
import { computed, onMounted, reactive, ref } from 'vue';
import type { EChartsOption } from 'echarts';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import TrendValue from '@/components/finance/TrendValue.vue';
import { ROUTES } from '@/constants/routes';
import { investmentApi, type AssetType, type HoldingItem, type HoldingRequest, type InvestmentTransactionType } from '@/services/investmentApi';

const holdings = ref<HoldingItem[]>([]);
const loading = ref(false);
const submitting = ref(false);
const holdingDialogVisible = ref(false);
const tradeDialogVisible = ref(false);
const quoteDialogVisible = ref(false);
const editingHolding = ref<HoldingItem | null>(null);
const activeHolding = ref<HoldingItem | null>(null);

const holdingForm = reactive<Required<Omit<HoldingRequest, 'assetId'>>>({
  assetName: '',
  symbol: '',
  assetType: 'FUND',
  currency: 'CNY',
  quoteSource: 'MANUAL',
  quoteKey: '',
  quantity: 0,
  avgCost: 0,
  remark: ''
});
const tradeForm = reactive({ type: 'BUY' as InvestmentTransactionType, quantity: 0, price: 0, fee: 0, transactionTime: new Date(), note: '' });
const quoteForm = reactive({ price: 0, currency: 'CNY' });

onMounted(() => {
  loadHoldings();
});

const totalMarketValue = computed(() => holdings.value.reduce((sum, item) => sum + Number(item.marketValue), 0));
const totalCost = computed(() => holdings.value.reduce((sum, item) => sum + Number(item.totalCost), 0));
const totalProfit = computed(() => holdings.value.reduce((sum, item) => sum + Number(item.floatingProfit), 0));
const totalProfitRate = computed(() => (totalCost.value <= 0 ? 0 : (totalProfit.value / totalCost.value) * 100));
const typeStats = computed(() => ({
  FUND: calcTypeStat('FUND'),
  STOCK: calcTypeStat('STOCK'),
  CRYPTO: calcTypeStat('CRYPTO')
}));
const allocationOption = computed<EChartsOption>(() => ({
  color: ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6'],
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['45%', '72%'], data: ['FUND', 'STOCK', 'CRYPTO', 'OTHER'].map((type) => ({ name: typeLabel(type), value: sumByType(type as AssetType, 'marketValue') })).filter((item) => item.value > 0) }]
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

function openHoldingDialog(holding?: HoldingItem) {
  editingHolding.value = holding || null;
  holdingForm.assetName = holding?.assetName || '';
  holdingForm.symbol = holding?.symbol || '';
  holdingForm.assetType = holding?.assetType || 'FUND';
  holdingForm.currency = holding?.currency || 'CNY';
  holdingForm.quoteSource = holding?.quoteSource || 'MANUAL';
  holdingForm.quoteKey = holding?.symbol || '';
  holdingForm.quantity = Number(holding?.quantity || 0);
  holdingForm.avgCost = Number(holding?.avgCost || 0);
  holdingForm.remark = holding?.remark || '';
  holdingDialogVisible.value = true;
}

async function handleSaveHolding() {
  if (!holdingForm.assetName || !holdingForm.symbol || holdingForm.quantity <= 0) {
    ElMessage.warning('请输入持仓名称、资产代码和有效数量');
    return;
  }
  submitting.value = true;
  try {
    const payload = { ...holdingForm };
    if (editingHolding.value) {
      await investmentApi.updateHolding(editingHolding.value.id, payload);
    } else {
      await investmentApi.createHolding(payload);
    }
    holdingDialogVisible.value = false;
    ElMessage.success('持仓已保存');
    await loadHoldings();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '持仓保存失败');
  } finally {
    submitting.value = false;
  }
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

function calcTypeStat(type: AssetType) {
  const items = holdings.value.filter((item) => item.assetType === type);
  const marketValue = items.reduce((sum, item) => sum + Number(item.marketValue), 0);
  const cost = items.reduce((sum, item) => sum + Number(item.totalCost), 0);
  const profit = items.reduce((sum, item) => sum + Number(item.floatingProfit), 0);
  return { marketValue, profitRate: cost <= 0 ? 0 : (profit / cost) * 100 };
}

function sumByType(type: AssetType, field: 'marketValue' | 'floatingProfit') {
  return holdings.value.filter((item) => item.assetType === type).reduce((sum, item) => sum + Number(item[field]), 0);
}

function typeLabel(type?: string | null) {
  return ({ FUND: '基金', STOCK: '股票', CRYPTO: '虚拟货币', OTHER: '其他' } as Record<string, string>)[type || ''] || '-';
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
/* 投资主页突出汇总，具体逐项查看放到投资明细页。 */
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.muted-line {
  display: block;
  margin-top: 4px;
  color: var(--xo-muted);
}

.full-width {
  width: 100%;
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
