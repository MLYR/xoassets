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
        <el-button :icon="Download" :loading="exporting" @click="handleExportInvestmentTransactions">导出投资交易</el-button>
        <el-button type="primary" :icon="Plus" @click="openHoldingDialog()">新增持仓</el-button>
      </div>
    </div>

    <section class="panel filter-panel">
      <el-segmented v-model="activeType" :options="typeOptions" @change="resetPage" />
      <el-input v-model="keyword" placeholder="搜索名称或代码" clearable @change="resetPage" />
    </section>

    <section class="summary-grid">
      <MetricCard title="投资总市值" :value="displayedSummary.totalMarketValue" :trend="0" description="持仓最新市值合计" :precision="4" :currency-symbol="currencySymbol" />
      <MetricCard title="今日收益" :value="displayedSummary.todayProfit" :trend="displayedSummary.todayProfit" description="按最新价与昨价计算" :precision="4" :currency-symbol="currencySymbol" :tone="profitTone(displayedSummary.todayProfit)" />
      <MetricCard title="昨日收益" :value="displayedSummary.yesterdayProfit" :trend="displayedSummary.yesterdayProfit" description="按昨价与前日价计算" :precision="4" :currency-symbol="currencySymbol" :tone="profitTone(displayedSummary.yesterdayProfit)" />
      <MetricCard title="总收益" :value="displayedSummary.floatingProfit" :trend="summary.floatingProfitRate" description="浮动盈亏 / 总成本" :precision="4" :currency-symbol="currencySymbol" :tone="profitTone(displayedSummary.floatingProfit)" />
      <MetricCard title="持仓数量" :value="summary.holdingCount" :trend="summary.floatingProfitRate" description="当前持仓项目数" :precision="0" currency-symbol="" />
    </section>

    <section v-loading="loading" class="panel">
      <el-empty v-if="!loading && pagedHoldings.length === 0" description="暂无符合条件的投资明细" />
      <template v-else>
        <el-table :data="pagedHoldings" stripe height="560">
          <el-table-column label="持仓" min-width="190" fixed="left">
            <template #default="{ row }">
              <strong>{{ row.assetName || '-' }}</strong>
              <small class="muted-line">{{ row.symbol || '-' }} · {{ row.currency || '-' }}</small>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }"><StatusBadge :label="typeLabel(row.assetType)" /></template>
          </el-table-column>
          <el-table-column label="数量" min-width="130" align="right" header-align="right">
            <template #default="{ row }"><span class="numeric-cell">{{ formatQuantity(row.quantity) }}</span></template>
          </el-table-column>
          <el-table-column label="成本价" min-width="130" align="right" header-align="right">
            <template #default="{ row }"><AmountText class="numeric-cell" :value="displayValue(row.avgCost, row.currency, 4)" :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="当前价" min-width="150" align="right" header-align="right">
            <template #default="{ row }"><AmountText class="numeric-cell" :value="displayValue(row.latestPrice, row.currency, pricePrecision(row))" :precision="pricePrecision(row)" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="昨价" min-width="140" align="right" header-align="right">
            <template #default="{ row }"><span class="numeric-cell">{{ formatOptionalPrice(row.previousPrice, row) }}</span></template>
          </el-table-column>
          <el-table-column label="今日涨跌" min-width="140" align="right" header-align="right">
            <template #default="{ row }"><TrendValue v-if="row.todayChangeRate !== null && row.todayChangeRate !== undefined" class="numeric-cell" :value="round4(row.todayChangeRate)" :precision="4" /><span v-else class="numeric-cell muted-text">暂无</span></template>
          </el-table-column>
          <el-table-column label="市值" min-width="150" align="right" header-align="right">
            <template #default="{ row }"><AmountText class="numeric-cell" :value="displayValue(row.marketValue, row.currency, 4)" :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="总成本" min-width="150" align="right" header-align="right">
            <template #default="{ row }"><AmountText class="numeric-cell" :value="displayValue(row.totalCost, row.currency, 4)" :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="今日收益" min-width="150" align="right" header-align="right">
            <template #default="{ row }"><AmountText v-if="row.todayProfit !== null && row.todayProfit !== undefined" class="numeric-cell" :value="displayValue(row.todayProfit, row.currency, 4)" with-sign :precision="4" :currency-symbol="currencySymbol" /><span v-else class="numeric-cell muted-text">暂无</span></template>
          </el-table-column>
          <el-table-column label="昨日收益" min-width="150" align="right" header-align="right">
            <template #default="{ row }"><AmountText v-if="row.yesterdayProfit !== null && row.yesterdayProfit !== undefined" class="numeric-cell" :value="displayValue(row.yesterdayProfit, row.currency, 4)" with-sign :precision="4" :currency-symbol="currencySymbol" /><span v-else class="numeric-cell muted-text">暂无</span></template>
          </el-table-column>
          <el-table-column label="总收益" min-width="150" align="right" header-align="right">
            <template #default="{ row }"><AmountText class="numeric-cell" :value="displayValue(row.floatingProfit, row.currency, 4)" with-sign :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="收益率" min-width="120" align="right" header-align="right">
            <template #default="{ row }"><TrendValue class="numeric-cell" :value="round4(row.floatingProfitRate)" :precision="4" /></template>
          </el-table-column>
          <el-table-column label="回本涨幅" min-width="150" align="right" header-align="right">
            <template #default="{ row }"><span :class="['numeric-cell', breakEvenClass(row.breakEvenRate)]">{{ formatBreakEven(row.breakEvenRate) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="260" align="center" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click="openTradeDialog(row, 'BUY')">买入</el-button>
                <el-button link type="primary" @click="openTradeDialog(row, 'SELL')">卖出</el-button>
                <el-button link type="primary" @click="openHoldingDialog(row)">编辑</el-button>
                <el-button link @click="handleRefreshQuote(row)">刷新</el-button>
                <el-button link @click="openQuoteDialog(row)">价格</el-button>
                <el-button link type="danger" @click="handleDeleteHolding(row)">删除</el-button>
              </div>
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
        <el-form-item :label="tradeForm.type === 'BUY' ? '扣款账户' : '到账账户'">
          <el-select v-model="tradeForm.accountId" class="full-width" placeholder="请选择资金账户">
            <el-option v-for="account in accounts" :key="account.id" :label="`${account.name} · ${account.balance.toFixed(2)} ${account.currency}`" :value="account.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input-number v-model="tradeForm.quantity" class="full-width" :min="0.0001" :precision="4" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="tradeForm.price" class="full-width" :min="0.000001" :precision="activePricePrecision" /></el-form-item>
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
        <el-form-item label="价格"><el-input-number v-model="quoteForm.price" class="full-width" :min="0.000001" :precision="activePricePrecision" /></el-form-item>
        <el-form-item label="币种"><el-input v-model.trim="quoteForm.currency" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quoteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleManualQuote">保存</el-button>
      </template>
    </el-dialog>

    <section v-loading="loading" class="panel transactions-panel">
      <div class="section-head">
        <h2>交易记录</h2>
        <span class="muted-text">买入/卖出只影响资金账户和持仓，不进入普通收支</span>
      </div>
      <el-table :data="transactions" stripe height="320">
        <el-table-column label="时间" prop="transactionTime" min-width="170" />
        <el-table-column label="资产" min-width="150">
          <template #default="{ row }">{{ row.assetName || row.symbol || '-' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }"><StatusBadge :label="row.type === 'BUY' ? '买入' : '卖出'" /></template>
        </el-table-column>
        <el-table-column label="资金账户" prop="accountName" min-width="140" />
        <el-table-column label="数量" min-width="120" align="right" header-align="right">
          <template #default="{ row }"><span class="numeric-cell">{{ formatQuantity(row.quantity) }}</span></template>
        </el-table-column>
        <el-table-column label="成交金额" min-width="130" align="right" header-align="right">
          <template #default="{ row }"><AmountText class="numeric-cell" :value="row.amount" :precision="4" /></template>
        </el-table-column>
        <el-table-column label="已实现盈亏" min-width="140" align="right" header-align="right">
          <template #default="{ row }"><AmountText v-if="row.realizedProfit !== null && row.realizedProfit !== undefined" class="numeric-cell" :value="row.realizedProfit" with-sign :precision="4" /><span v-else class="numeric-cell muted-text">暂无</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusBadge :label="row.status === 'REVOKED' ? '已撤销' : '正常'" /></template>
        </el-table-column>
        <el-table-column label="撤销信息" min-width="180">
          <template #default="{ row }">{{ row.status === 'REVOKED' ? (row.revokeReason || formatTableTime(row.revokeTime) || '已撤销') : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" :disabled="row.status === 'REVOKED'" @click="handleRevokeTransaction(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
// 明细页复用持仓列表接口，在前端完成类型筛选、分页和币种展示换算。
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Download, Plus } from '@element-plus/icons-vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import TrendValue from '@/components/finance/TrendValue.vue';
import { ROUTES } from '@/constants/routes';
import { accountApi, type AccountItem } from '@/services/accountApi';
import { exportApi } from '@/services/exportApi';
import { investmentApi, type AssetType, type HoldingItem, type HoldingRequest, type HoldingSummary, type InvestmentTransactionItem, type InvestmentTransactionType } from '@/services/investmentApi';

type DisplayCurrency = 'CNY' | 'USD';

const holdings = ref<HoldingItem[]>([]);
const summary = ref<HoldingSummary>({ totalMarketValue: 0, totalCost: 0, todayProfit: 0, yesterdayProfit: 0, floatingProfit: 0, floatingProfitRate: 0, holdingCount: 0 });
const accounts = ref<AccountItem[]>([]);
const transactions = ref<InvestmentTransactionItem[]>([]);
const loading = ref(false);
const submitting = ref(false);
const exporting = ref(false);
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
const tradeForm = reactive({ type: 'BUY' as InvestmentTransactionType, accountId: '', quantity: 0, price: 0, fee: 0, transactionTime: new Date(), note: '' });
const quoteForm = reactive({ price: 0, currency: 'CNY' });

onMounted(() => {
  loadPageData();
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
const activePricePrecision = computed(() => activeHolding.value ? pricePrecision(activeHolding.value) : 4);
const displayedSummary = computed(() => {
  // 后端负责权威汇总口径；前端仅在用户切换币种时按当前展示汇率换算金额字段。
  return {
    totalMarketValue: sumDisplayed('marketValue'),
    todayProfit: sumDisplayed('todayProfit'),
    yesterdayProfit: sumDisplayed('yesterdayProfit'),
    floatingProfit: sumDisplayed('floatingProfit')
  };
});

async function loadHoldings() {
  loading.value = true;
  try {
    holdings.value = await investmentApi.listHoldings();
    summary.value = await investmentApi.summaryHoldings();
    transactions.value = await investmentApi.listTransactions();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '投资明细加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadPageData() {
  loading.value = true;
  try {
    // 交易弹窗需要展示当前用户资金账户，后端仍会再次校验 accountId 归属。
    const [holdingList, holdingSummary, accountList, transactionList] = await Promise.all([
      investmentApi.listHoldings(),
      investmentApi.summaryHoldings(),
      accountApi.list(),
      investmentApi.listTransactions()
    ]);
    holdings.value = holdingList;
    summary.value = holdingSummary;
    accounts.value = accountList;
    transactions.value = transactionList;
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
  tradeForm.accountId = accounts.value[0]?.id || '';
  tradeForm.quantity = 0;
  tradeForm.price = roundTo(Number(holding.latestPrice || holding.avgCost || 0), pricePrecision(holding));
  tradeForm.fee = 0;
  tradeForm.transactionTime = new Date();
  tradeForm.note = '';
  tradeDialogVisible.value = true;
}

function openQuoteDialog(holding: HoldingItem) {
  activeHolding.value = holding;
  quoteForm.price = roundTo(Number(holding.latestPrice || holding.avgCost || 0), pricePrecision(holding));
  quoteForm.currency = holding.currency || 'CNY';
  quoteDialogVisible.value = true;
}

async function handleCreateTrade() {
  if (!activeHolding.value || !tradeForm.accountId || tradeForm.quantity <= 0 || tradeForm.price <= 0) {
    ElMessage.warning('请选择资金账户并输入有效的数量和价格');
    return;
  }
  submitting.value = true;
  try {
    await investmentApi.createTransaction({
      holdingId: activeHolding.value.id,
      assetId: activeHolding.value.assetId,
      accountId: tradeForm.accountId,
      type: tradeForm.type,
      quantity: round4(tradeForm.quantity),
      price: roundTo(tradeForm.price, activePricePrecision.value),
      fee: round4(tradeForm.fee),
      transactionTime: formatDateTime(tradeForm.transactionTime),
      note: tradeForm.note
    });
    tradeDialogVisible.value = false;
    ElMessage.success(tradeForm.type === 'BUY' ? '买入已记录' : '卖出已记录');
    await loadPageData();
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
    await investmentApi.manualQuote({ assetId: activeHolding.value.assetId, price: roundTo(quoteForm.price, activePricePrecision.value), currency: quoteForm.currency });
    quoteDialogVisible.value = false;
    ElMessage.success('价格已更新');
    await loadPageData();
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
    await loadPageData();
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
    await loadPageData();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '持仓删除失败');
    }
  }
}

async function handleRevokeTransaction(transaction: InvestmentTransactionItem) {
  try {
    await ElMessageBox.confirm('撤销后会反向恢复账户余额和持仓，确认继续吗？', '撤销投资交易', { type: 'warning', confirmButtonText: '撤销', cancelButtonText: '取消' });
    await investmentApi.revokeTransaction(transaction.id, '录入错误');
    ElMessage.success('投资交易已撤销');
    await loadPageData();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '投资交易撤销失败');
    }
  }
}

async function handleExportInvestmentTransactions() {
  exporting.value = true;
  try {
    await exportApi.investmentTransactions();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '投资交易导出失败');
  } finally {
    exporting.value = false;
  }
}

function displayValue(value: number, sourceCurrency?: string | null, precision = 4) {
  return convertAmount(value, sourceCurrency, precision);
}

function convertAmount(value: number, sourceCurrency?: string | null, precision = 4) {
  const source = sourceCurrency || 'CNY';
  if (source === displayCurrency.value) {
    return roundTo(Number(value), precision);
  }
  if (source === 'USD' && displayCurrency.value === 'CNY') {
    return roundTo(Number(value) * usdCnyRate.value, precision);
  }
  if (source === 'CNY' && displayCurrency.value === 'USD') {
    return roundTo(Number(value) / usdCnyRate.value, precision);
  }
  return roundTo(Number(value), precision);
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

function formatTableTime(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '';
}

function formatQuantity(value: number) {
  return round4(Number(value)).toLocaleString('zh-CN', { minimumFractionDigits: 4, maximumFractionDigits: 4 });
}

function formatOptionalPrice(value: number | null | undefined, row: HoldingItem) {
  if (value === null || value === undefined) {
    return '暂无';
  }
  return `${currencySymbol.value}${displayValue(value, row.currency, pricePrecision(row)).toLocaleString('zh-CN', { minimumFractionDigits: pricePrecision(row), maximumFractionDigits: pricePrecision(row) })}`;
}

function formatBreakEven(value: number | null | undefined) {
  // 回本涨幅只在亏损时提示还需上涨比例，盈利或打平直接展示已盈利。
  if (value === null || value === undefined) {
    return '暂无';
  }
  return Number(value) > 0 ? `还需 +${round4(value).toFixed(4)}%` : '已盈利';
}

function breakEvenClass(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return 'muted-text';
  }
  return Number(value) > 0 ? 'warning-text' : 'success-text';
}

function profitTone(value: number): 'success' | 'danger' | 'primary' {
  if (value > 0) {
    return 'success';
  }
  if (value < 0) {
    return 'danger';
  }
  return 'primary';
}

function sumDisplayed(field: 'marketValue' | 'todayProfit' | 'yesterdayProfit' | 'floatingProfit') {
  return round4(holdings.value.reduce((total, item) => total + displayValue(Number(item[field] || 0), item.currency, 4), 0));
}

function pricePrecision(row: HoldingItem) {
  // 后端返回 priceScale 作为权威精度；老数据没有该字段时按资产类型兜底。
  return row.priceScale || (row.assetType === 'CRYPTO' ? 6 : 4);
}

function round4(value: number) {
  return roundTo(value, 4);
}

function roundTo(value: number, precision: number) {
  return Number(Number(value || 0).toFixed(precision));
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.transactions-panel {
  margin-top: 18px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 18px 8px;
}

.section-head h2 {
  margin: 0;
  font-size: 18px;
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

.numeric-cell {
  display: inline-block;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.muted-text {
  color: var(--xo-muted);
}

.success-text {
  color: var(--xo-success);
}

.warning-text {
  color: var(--xo-warning);
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
  justify-content: center;
  white-space: nowrap;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
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
  .form-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .header-actions {
    flex-wrap: wrap;
  }
}
</style>
