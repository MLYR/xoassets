<!-- 投资明细页：按资产类型分页查看并操作具体持仓。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">投资明细</h1>
        <p class="page-subtitle">按基金、股票、虚拟货币查看每一项持仓并记录操作</p>
      </div>
      <div class="header-actions">
        <el-segmented v-model="displayCurrency" :options="currencyOptions" />
        <el-input-number v-model="usdCnyRate" class="rate-input" :min="0.0001" :precision="4" />
        <el-button @click="$router.push(ROUTES.investments)">返回主页</el-button>
        <el-button type="primary" :icon="Plus" @click="openHoldingDialog()">新增持仓</el-button>
      </div>
    </div>

    <section class="panel filter-panel">
      <el-segmented v-model="activeType" :options="typeOptions" @change="resetPage" />
      <el-input v-model="keyword" placeholder="搜索名称或代码" clearable @change="resetPage" />
    </section>

    <section v-loading="loading" class="panel">
      <el-empty v-if="!loading && pagedHoldings.length === 0" description="暂无符合条件的投资明细" />
      <template v-else>
        <el-table :data="pagedHoldings" stripe>
          <el-table-column label="持仓" min-width="180">
            <template #default="{ row }">
              <strong>{{ row.assetName || '-' }}</strong>
              <small class="muted-line">{{ row.symbol || '-' }} · {{ row.currency || '-' }}</small>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }"><StatusBadge :label="typeLabel(row.assetType)" /></template>
          </el-table-column>
          <el-table-column label="数量" align="right">
            <template #default="{ row }">{{ formatQuantity(row.quantity) }}</template>
          </el-table-column>
          <el-table-column label="当前价" align="right">
            <template #default="{ row }"><AmountText :value="displayValue(row.latestPrice, row.currency)" :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="市值" align="right">
            <template #default="{ row }"><AmountText :value="displayValue(row.marketValue, row.currency)" :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="总成本" align="right">
            <template #default="{ row }"><AmountText :value="displayValue(row.totalCost, row.currency)" :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="浮动盈亏" align="right">
            <template #default="{ row }"><AmountText :value="displayValue(row.floatingProfit, row.currency)" with-sign :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="收益率">
            <template #default="{ row }"><TrendValue :value="round4(row.floatingProfitRate)" :precision="4" /></template>
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
        <div class="table-footer">
          <span>共 {{ filteredHoldings.length }} 条明细</span>
          <el-pagination
            v-model:current-page="pageNo"
            v-model:page-size="pageSize"
            layout="total, sizes, prev, pager, next"
            :page-sizes="[10, 20, 50]"
            :total="filteredHoldings.length"
          />
        </div>
      </template>
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
          <el-form-item label="数量"><el-input-number v-model="holdingForm.quantity" class="full-width" :min="0.0001" :precision="4" /></el-form-item>
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
        <el-form-item label="数量"><el-input-number v-model="tradeForm.quantity" class="full-width" :min="0.0001" :precision="4" /></el-form-item>
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
// 明细页复用持仓列表接口，在前端完成类型筛选、分页和币种展示换算。
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import AmountText from '@/components/finance/AmountText.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import TrendValue from '@/components/finance/TrendValue.vue';
import { ROUTES } from '@/constants/routes';
import { investmentApi, type AssetType, type HoldingItem, type HoldingRequest, type InvestmentTransactionType } from '@/services/investmentApi';

type DisplayCurrency = 'CNY' | 'USD';

const holdings = ref<HoldingItem[]>([]);
const loading = ref(false);
const submitting = ref(false);
const activeType = ref<AssetType | 'ALL'>('ALL');
const keyword = ref('');
const pageNo = ref(1);
const pageSize = ref(10);
const displayCurrency = ref<DisplayCurrency>('CNY');
const usdCnyRate = ref(7.2);
const holdingDialogVisible = ref(false);
const tradeDialogVisible = ref(false);
const quoteDialogVisible = ref(false);
const editingHolding = ref<HoldingItem | null>(null);
const activeHolding = ref<HoldingItem | null>(null);
const currencyOptions = [
  { label: '人民币', value: 'CNY' },
  { label: 'USD', value: 'USD' }
];
const typeOptions = [
  { label: '全部', value: 'ALL' },
  { label: '基金', value: 'FUND' },
  { label: '股票', value: 'STOCK' },
  { label: '虚拟货币', value: 'CRYPTO' }
];
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

watch(
  () => holdingForm.assetType,
  (assetType) => {
    // 新增虚拟货币时默认使用 USD，已有持仓编辑时保留原币种避免误改历史口径。
    if (!editingHolding.value && assetType === 'CRYPTO') {
      holdingForm.currency = 'USD';
    }
  }
);

const currencySymbol = computed(() => (displayCurrency.value === 'CNY' ? '¥' : '$'));
const filteredHoldings = computed(() => holdings.value.filter((item) => {
  const matchedType = activeType.value === 'ALL' || item.assetType === activeType.value;
  const keywordText = keyword.value.trim().toLowerCase();
  const matchedKeyword = !keywordText || `${item.assetName || ''} ${item.symbol || ''}`.toLowerCase().includes(keywordText);
  return matchedType && matchedKeyword;
}));
const pagedHoldings = computed(() => {
  const start = (pageNo.value - 1) * pageSize.value;
  return filteredHoldings.value.slice(start, start + pageSize.value);
});

async function loadHoldings() {
  loading.value = true;
  try {
    holdings.value = await investmentApi.listHoldings();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '投资明细加载失败');
  } finally {
    loading.value = false;
  }
}

function openHoldingDialog(holding?: HoldingItem) {
  editingHolding.value = holding || null;
  holdingForm.assetName = holding?.assetName || '';
  holdingForm.symbol = holding?.symbol || '';
  holdingForm.assetType = holding?.assetType || 'FUND';
  holdingForm.currency = holding?.currency || (holdingForm.assetType === 'CRYPTO' ? 'USD' : 'CNY');
  holdingForm.quoteSource = holding?.quoteSource || 'MANUAL';
  holdingForm.quoteKey = holding?.symbol || '';
  holdingForm.quantity = round4(Number(holding?.quantity || 0));
  holdingForm.avgCost = round4(Number(holding?.avgCost || 0));
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
    const payload = { ...holdingForm, quantity: round4(holdingForm.quantity), avgCost: round4(holdingForm.avgCost) };
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
  tradeForm.price = round4(Number(holding.latestPrice || holding.avgCost || 0));
  tradeForm.fee = 0;
  tradeForm.transactionTime = new Date();
  tradeForm.note = '';
  tradeDialogVisible.value = true;
}

function openQuoteDialog(holding: HoldingItem) {
  activeHolding.value = holding;
  quoteForm.price = round4(Number(holding.latestPrice || holding.avgCost || 0));
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
      quantity: round4(tradeForm.quantity),
      price: round4(tradeForm.price),
      fee: round4(tradeForm.fee),
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
    await investmentApi.manualQuote({ assetId: activeHolding.value.assetId, price: round4(quoteForm.price), currency: quoteForm.currency });
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

function displayValue(value: number, sourceCurrency?: string | null) {
  return convertAmount(value, sourceCurrency);
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

function resetPage() {
  pageNo.value = 1;
}

function typeLabel(type?: string | null) {
  return ({ FUND: '基金', STOCK: '股票', CRYPTO: '虚拟货币', OTHER: '其他' } as Record<string, string>)[type || ''] || '-';
}

function formatDateTime(date: Date) {
  const pad = (value: number) => `${value}`.padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function formatQuantity(value: number) {
  return round4(Number(value)).toLocaleString('zh-CN', { minimumFractionDigits: 4, maximumFractionDigits: 4 });
}

function round4(value: number) {
  return Number(Number(value || 0).toFixed(4));
}
</script>

<style scoped>
/* 明细页承载投资操作，主页只保留概览。 */
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.filter-panel {
  display: grid;
  grid-template-columns: auto minmax(220px, 320px);
  gap: 12px;
  padding: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.rate-input {
  width: 130px;
}

.muted-line {
  display: block;
  margin-top: 4px;
  color: var(--xo-muted);
}

.full-width {
  width: 100%;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
}

@media (max-width: 760px) {
  .filter-panel,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .header-actions {
    flex-wrap: wrap;
  }
}
</style>
