<!-- 投资主页：总览拆分基金、股票、虚拟货币收益口径，避免把净值型资产硬塞到今日收益。 -->
<template>
  <div class="page investments-overview-page">
    <div class="investment-nav-row">
      <el-tabs v-model="activeModule" class="investment-module-tabs" @tab-change="handleModuleChange">
        <el-tab-pane v-for="item in moduleTabs" :key="item.value" :label="item.label" :name="item.value" />
      </el-tabs>
      <div class="investment-nav-actions">
        <el-select v-model="displayCurrency" class="currency-select" placeholder="展示币种">
          <el-option v-for="item in currencyOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button v-if="activeModule !== 'ALL'" type="primary" @click="openCreateHoldingDialog">新增持仓</el-button>
      </div>
    </div>

    <template v-if="activeModule === 'ALL'">
      <section v-loading="loading" class="grid-4">
        <MetricCard title="投资总资产" :value="overview?.totalInvestmentAsset ?? totalMarketValue" :trend="overview?.holdingProfitRate ?? totalProfitRate" description="基金 + 股票 + 虚拟货币" tone="primary" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard title="持有收益" :value="overview?.holdingProfit ?? totalProfit" :trend="overview?.holdingProfitRate ?? totalProfitRate" description="全部资产当前市值 - 总成本" :tone="profitTone(overview?.holdingProfit ?? totalProfit)" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard title="持仓成本" :value="overview?.totalCost ?? totalCost" :trend="overview?.holdingProfitRate ?? totalProfitRate" description="趋势为持有收益率" tone="primary" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard title="今日收益" :value="overviewTodayProfitValue" :trend="null" :description="overview?.todayProfitStatusLabel || overview?.todayProfitAssetScope || '今日有效价资产'" :tone="profitTone(overviewTodayProfitValue)" :precision="4" :currency-symbol="currencySymbol" />
      </section>

      <section v-loading="loading" class="module-card-grid">
        <div v-for="item in moduleAssets" :key="item.module" class="module-card panel panel-padding" @click="switchModule(item.module)">
          <div class="module-card-top">
            <span>{{ item.name }}</span>
            <el-tag round>{{ formatRatio(item.assetRatio) }}</el-tag>
          </div>
          <strong>{{ formatMoney(convertAmount(item.assetAmount, 'CNY')) }}</strong>
          <div class="module-card-meta">
            <span>{{ item.primaryProfitLabel }}</span>
            <AmountText :value="item.primaryProfitAvailable === false ? null : convertNullableAmount(item.primaryProfitAmount, 'CNY')" with-sign :precision="4" :currency-symbol="currencySymbol" />
          </div>
          <p v-if="item.primaryProfitAvailable === false" class="module-status-tip">{{ item.primaryProfitStatusLabel || '今日价格未更新' }}</p>
          <div class="module-card-meta muted-text">
            <span>持有收益</span>
            <AmountText :value="convertAmount(item.holdingProfit, 'CNY')" with-sign :precision="4" :currency-symbol="currencySymbol" />
          </div>
        </div>
      </section>

      <section class="grid-2">
        <div class="panel panel-padding">
          <div class="panel-head">
            <h3>资产趋势</h3>
            <el-segmented v-model="trendModule" :options="moduleTabs" @change="loadTrend" />
          </div>
          <el-empty v-if="!loading && investmentTrend.length === 0" description="暂无投资资产曲线数据" />
          <BaseChart v-else :option="investmentTrendOption" />
        </div>
        <div class="panel panel-padding">
          <div class="panel-head">
            <h3>投资分布</h3>
          </div>
          <el-empty v-if="!loading && holdings.length === 0" description="暂无投资分布数据" />
          <BaseChart v-else :option="allocationOption" />
        </div>
      </section>

      <section class="panel panel-padding contribution-panel">
        <div class="panel-head">
          <div>
            <h3>收益贡献</h3>
            <p class="panel-subtitle">今日收益只统计今日价格或净值已更新的持仓</p>
          </div>
          <el-segmented v-model="contributionMode" :options="contributionOptions" />
        </div>
        <el-empty v-if="!loading && contributionData.length === 0" description="暂无收益贡献数据" />
        <BaseChart v-else :option="profitOption" height="320px" />
      </section>
    </template>

    <template v-else>
      <section v-loading="loading" class="grid-3 module-summary-grid">
        <MetricCard :title="`${moduleLabel(activeModule)}总资产`" :value="currentModuleAsset?.assetAmount ?? 0" :trend="currentModuleAsset?.assetRatio ?? 0" description="当前模块持仓市值" tone="primary" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard :title="currentModuleAsset?.primaryProfitLabel || modulePrimaryLabel(activeModule)" :value="currentModuleProfitValue" :trend="null" :description="currentModuleProfitDescription" :tone="profitTone(currentModuleProfitValue)" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard title="持有收益" :value="currentModuleAsset?.holdingProfit ?? 0" :trend="currentModuleAsset?.holdingProfitRate ?? 0" description="当前市值 - 持仓成本" :tone="profitTone(currentModuleAsset?.holdingProfit ?? 0)" :precision="4" :currency-symbol="currencySymbol" />
      </section>

      <section class="panel panel-padding module-holdings-panel">
        <div class="panel-head module-panel-head">
          <div>
            <h3>{{ moduleLabel(activeModule) }}持仓</h3>
            <p class="panel-subtitle">今日收益严格按今日有效价计算，未更新时显示 --</p>
          </div>
          <div class="module-actions">
            <el-segmented v-model="activeSubType" :options="subTypeOptions" />
          </div>
        </div>
        <el-table :data="pagedModuleHoldings" stripe @row-click="openHoldingDetail">
          <el-table-column label="名称" min-width="220">
            <template #default="{ row }">
              <div class="holding-name-cell">
                <strong>{{ row.assetName || row.symbol || '-' }}</strong>
                <span>{{ row.symbol || '-' }} · {{ subTypeLabel(row.assetSubType) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="持有市值" min-width="140" align="right" header-align="right">
            <template #default="{ row }"><AmountText :value="convertAmount(row.marketValue, row.currency)" :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="持有收益" min-width="160" align="right" header-align="right">
            <template #default="{ row }"><AmountText :value="convertAmount(row.floatingProfit, row.currency)" with-sign :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="今日收益/收益率" min-width="170" align="right" header-align="right">
            <template #default="{ row }">
              <div class="primary-profit-cell">
                <AmountText v-if="row.todayProfitByCurrentQuantity !== null && row.todayProfitByCurrentQuantity !== undefined" :value="convertAmount(row.todayProfitByCurrentQuantity, row.currency)" with-sign :precision="4" :currency-symbol="currencySymbol" />
                <span v-else class="muted-text">--</span>
                <small>{{ todayProfitRateText(row) }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="modulePriceLabel(activeModule)" min-width="130" align="right" header-align="right">
            <template #default="{ row }">{{ formatPrice(row.latestPrice, row.priceScale) }}</template>
          </el-table-column>
          <el-table-column label="价格日期" min-width="120" align="right" header-align="right">
            <template #default="{ row }">{{ row.priceDate || '--' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="130" align="center">
            <template #default="{ row }"><el-tag round :type="priceStatusTagType(row)">{{ priceStatusLabel(row) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="240" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openHoldingDetail(row)">详情</el-button>
              <el-button link type="primary" :loading="refreshingAssetId === row.assetId" @click.stop="handleRefreshQuote(row)">刷新</el-button>
              <el-button link type="primary" @click.stop="openQuoteDialog(row)">价格</el-button>
              <el-button v-if="canDeleteHolding(row)" link type="danger" @click.stop="handleDeleteHolding(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <el-pagination
            v-model:current-page="modulePageNo"
            v-model:page-size="modulePageSize"
            layout="total, sizes, prev, pager, next"
            :page-sizes="pageSizeOptions"
            :total="moduleHoldingTotal"
            @size-change="handleModulePageSizeChange"
          />
        </div>
      </section>
    </template>

    <el-dialog v-model="holdingDialogVisible" class="xo-form-dialog investment-holding-dialog" width="760px" top="12px">
      <template #header>
        <div class="xo-dialog-header-content">
          <span class="xo-dialog-kicker">{{ moduleLabel(activeModule) }}模块</span>
          <h2>新增{{ moduleLabel(activeModule) }}持仓</h2>
          <p>资产类型已锁定；可搜索识别，也可手动录入行情信息。</p>
        </div>
      </template>
      <el-form class="xo-dialog-form" label-position="top" @submit.prevent="handleSaveHolding">
        <section class="xo-dialog-section">
          <div class="xo-dialog-section-title">
            <strong>资产信息</strong>
            <span>搜索识别或手动录入</span>
          </div>
          <div class="lookup-panel">
            <div class="lookup-row">
              <el-select v-if="holdingForm.assetType === 'STOCK'" v-model="lookupMarket" class="market-select" placeholder="市场">
                <el-option label="自动" value="" />
                <el-option label="上交所 SH" value="SH" />
                <el-option label="深交所 SZ" value="SZ" />
                <el-option label="北交所 BJ" value="BJ" />
                <el-option label="美股 US" value="US" />
              </el-select>
              <el-input v-model.trim="lookupKeyword" :placeholder="quoteKeyPlaceholder" clearable />
              <el-button :loading="lookupLoading" @click="handleLookupAsset">搜索</el-button>
            </div>
            <div v-if="lookupResults.length > 0" class="lookup-results">
              <button v-for="item in lookupResults" :key="`${item.assetType}-${item.market}-${item.symbol}-${item.quoteSource}`" type="button" class="lookup-item" @click="applyLookupResult(item)">
                <strong>{{ item.name }}</strong>
                <span>{{ item.symbol }} · {{ item.market || '-' }} · {{ item.currency }} · {{ item.quoteSource }}</span>
                <span v-if="item.latestPrice">当前价 {{ formatLookupPrice(item) }} · {{ formatDateTime(item.quoteTime) || '暂无时间' }}</span>
              </button>
            </div>
          </div>
          <div class="form-grid compact-form-grid">
            <el-form-item label="持仓名称"><el-input v-model.trim="holdingForm.assetName" placeholder="例如：沪深300ETF / 比特币" /></el-form-item>
            <el-form-item label="资产代码"><el-input v-model.trim="holdingForm.symbol" placeholder="例如：510300 / bitcoin" /></el-form-item>
            <el-form-item label="资产类型"><el-input :model-value="moduleLabel(holdingForm.assetType)" disabled /></el-form-item>
            <el-form-item label="市场"><el-input v-model.trim="holdingForm.market" placeholder="自动识别，例如 SH / US / CN_FUND / CRYPTO" /></el-form-item>
            <el-form-item label="币种">
              <el-select v-model="holdingForm.currency" class="full-width">
                <el-option label="人民币 CNY" value="CNY" />
                <el-option label="美元 USD" value="USD" />
                <el-option label="港币 HKD" value="HKD" />
              </el-select>
            </el-form-item>
            <el-form-item label="行情来源">
              <el-select v-model="holdingForm.quoteSource" class="full-width">
                <el-option label="手动" value="MANUAL" />
                <el-option label="CoinGecko" value="COINGECKO" />
                <el-option label="天天基金" value="EASTMONEY" />
                <el-option label="新浪 A 股" value="SINA" />
                <el-option label="Yahoo 美股" value="YAHOO" />
              </el-select>
            </el-form-item>
            <el-form-item label="行情键">
              <el-input v-model.trim="holdingForm.quoteKey" :placeholder="quoteKeyPlaceholder" />
            </el-form-item>
          </div>
        </section>
        <section class="xo-dialog-section">
          <div class="xo-dialog-section-title">
            <strong>持仓与价格</strong>
            <span>{{ holdingForm.assetType === 'FUND' ? '0 份额建仓，买入后确认份额' : '用于初始化当前市值和成本' }}</span>
          </div>
          <div class="holding-price-grid">
            <template v-if="holdingForm.assetType !== 'FUND'">
              <el-form-item label="数量"><el-input-number v-model="holdingForm.quantity" class="full-width" :min="quantityMin" :precision="quantityPrecision" /></el-form-item>
              <el-form-item label="平均成本"><el-input-number v-model="holdingForm.avgCost" class="full-width" :min="0" :precision="4" /></el-form-item>
            </template>
            <el-form-item label="当前价格"><el-input-number v-model="holdingForm.latestPrice" class="full-width" :min="0" :precision="formPricePrecision" /></el-form-item>
            <el-form-item class="holding-remark-item" label="备注"><el-input v-model.trim="holdingForm.remark" type="textarea" :rows="2" /></el-form-item>
          </div>
        </section>
      </el-form>
      <template #footer>
        <div class="xo-dialog-footer">
          <el-button @click="holdingDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSaveHolding">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="quoteDialogVisible" title="手动更新价格" width="420px">
      <el-form label-position="top" @submit.prevent="handleManualQuote">
        <el-form-item label="持仓"><el-input :model-value="activeHolding?.assetName || '-'" disabled /></el-form-item>
        <el-form-item :label="activeHolding?.assetType === 'FUND' ? '净值' : '价格'"><el-input-number v-model="quoteForm.price" class="full-width" :min="0.000001" :precision="activePricePrecision" /></el-form-item>
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
// Web 投资页负责模块化展示和模块内新增持仓，资产类型由当前模块锁定。
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { EChartsOption } from 'echarts';
import { ElMessage, ElMessageBox } from 'element-plus';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import { ROUTES } from '@/constants/routes';
import { exchangeRateApi } from '@/services/exchangeRateApi';
import { investmentApi, type AssetLookupItem, type AssetType, type HoldingItem, type HoldingRequest, type InvestmentModuleAsset, type InvestmentOverview, type InvestmentTransactionItem, type InvestmentTrendPoint, type QuoteSource } from '@/services/investmentApi';

type DisplayCurrency = 'CNY' | 'USD';
type ContributionMode = 'TOTAL' | 'PRIMARY' | 'MONTH' | 'YEAR';
type InvestmentModule = 'ALL' | 'FUND' | 'STOCK' | 'CRYPTO';

const moduleTabs: Array<{ label: string; value: InvestmentModule }> = [
  { label: '总览', value: 'ALL' },
  { label: '基金', value: 'FUND' },
  { label: '股票', value: 'STOCK' },
  { label: '虚拟货币', value: 'CRYPTO' }
];
const route = useRoute();
const router = useRouter();
const currencyOptions = [
  { label: '人民币', value: 'CNY' },
  { label: 'USD', value: 'USD' }
];
const contributionOptions = [
  { label: '持有', value: 'TOTAL' },
  { label: '主收益', value: 'PRIMARY' },
  { label: '当月', value: 'MONTH' },
  { label: '当年', value: 'YEAR' }
];

const overview = ref<InvestmentOverview | null>(null);
const holdings = ref<HoldingItem[]>([]);
const moduleHoldings = ref<HoldingItem[]>([]);
const investmentTrend = ref<InvestmentTrendPoint[]>([]);
const transactions = ref<InvestmentTransactionItem[]>([]);
const loading = ref(false);
const submitting = ref(false);
const holdingDialogVisible = ref(false);
const quoteDialogVisible = ref(false);
const lookupKeyword = ref('');
const lookupMarket = ref('');
const lookupLoading = ref(false);
const lookupResults = ref<AssetLookupItem[]>([]);
const activeHolding = ref<HoldingItem | null>(null);
const refreshingAssetId = ref('');
const displayCurrency = ref<DisplayCurrency>('CNY');
const activeModule = ref<InvestmentModule>(routeModule(route.query.module));
const trendModule = ref<InvestmentModule>('ALL');
const activeSubType = ref('ALL');
const contributionMode = ref<ContributionMode>('TOTAL');
const usdCnyRate = ref(7.2);
// 投资模块持仓表格前端分页展示，统计类数据仍使用完整持仓列表计算。
const pageSizeOptions = [10, 50, 100, 300];
const modulePageNo = ref(1);
const modulePageSize = ref(10);
const holdingForm = reactive<Required<Omit<HoldingRequest, 'assetId'>>>({
  assetName: '',
  symbol: '',
  assetType: 'FUND',
  market: 'CN_FUND',
  currency: 'CNY',
  quoteSource: 'EASTMONEY',
  quoteKey: '',
  latestPrice: 0,
  previousClose: null,
  changePercent: null,
  quoteTime: null,
  marketStatus: '',
  quantity: 0,
  avgCost: 0,
  remark: ''
});
const quoteForm = reactive({ price: 0, currency: 'CNY' });

onMounted(() => {
  loadPageData();
  loadExchangeRate();
});

watch(activeModule, () => {
  activeSubType.value = 'ALL';
  modulePageNo.value = 1;
  syncModuleQuery();
});

watch(activeSubType, () => {
  modulePageNo.value = 1;
});

watch(
  () => route.query.module,
  async (value) => {
    const nextModule = routeModule(value);
    if (nextModule === activeModule.value) {
      return;
    }
    // 浏览器前进 / 后退只会改 URL query，必须反向同步 tab 和模块持仓列表。
    activeModule.value = nextModule;
    await handleModuleChange();
  }
);

const currencySymbol = computed(() => (displayCurrency.value === 'CNY' ? '¥' : '$'));
const totalMarketValue = computed(() => round4(holdings.value.reduce((sum, item) => sum + convertAmount(item.marketValue, item.currency), 0)));
const totalCost = computed(() => round4(holdings.value.reduce((sum, item) => sum + convertAmount(item.totalCost, item.currency), 0)));
const totalProfit = computed(() => round4(holdings.value.reduce((sum, item) => sum + convertAmount(item.floatingProfit, item.currency), 0)));
const totalProfitRate = computed(() => rate4(totalProfit.value, totalCost.value));
const formPricePrecision = computed(() => holdingForm.assetType === 'CRYPTO' ? 8 : 4);
const quantityPrecision = computed(() => holdingForm.assetType === 'CRYPTO' ? 10 : 4);
const quantityMin = computed(() => holdingForm.assetType === 'CRYPTO' ? 0.0000000001 : 0.0001);
const activePricePrecision = computed(() => pricePrecision(activeHolding.value));
const quoteKeyPlaceholder = computed(() => {
  if (holdingForm.assetType === 'CRYPTO') return 'bitcoin / ethereum / dogecoin';
  if (holdingForm.assetType === 'FUND') return '000001';
  if (holdingForm.assetType === 'STOCK' && holdingForm.quoteSource === 'SINA') return '600519.SH / 000001.SZ';
  if (holdingForm.assetType === 'STOCK' && holdingForm.quoteSource === 'YAHOO') return 'AAPL / MSFT';
  return '资产行情键';
});
const quoteKeyTip = computed(() => {
  if (holdingForm.assetType === 'CRYPTO') return 'CRYPTO 使用 CoinGecko id，例如 bitcoin、ethereum、dogecoin。';
  if (holdingForm.assetType === 'FUND') return '基金填写基金代码，例如 000001，行情来源选择天天基金。';
  if (holdingForm.assetType === 'STOCK' && holdingForm.quoteSource === 'SINA') return 'A 股填写代码和市场，例如 600519.SH、000001.SZ、430047.BJ。';
  if (holdingForm.assetType === 'STOCK' && holdingForm.quoteSource === 'YAHOO') return '美股填写股票代码，例如 AAPL。';
  return '手动行情可填写任意唯一键。';
});
const moduleAssets = computed(() => overview.value?.moduleAssets || fallbackModuleAssets.value);
const currentModuleAsset = computed(() => moduleAssets.value.find((item) => item.module === activeModule.value) || null);
// 今日收益没有今日有效价格时显示 --，不能把未更新净值或休市收益兜底为 0。
const overviewTodayProfitValue = computed(() => {
  if (!overview.value) return 0;
  return overview.value.todayProfitAvailable === false ? null : overview.value.todayProfit;
});
const currentModuleProfitValue = computed(() => {
  if (!currentModuleAsset.value) return 0;
  return currentModuleAsset.value.primaryProfitAvailable === false ? null : currentModuleAsset.value.primaryProfitAmount;
});
// 模块顶部 KPI 缺少今日收益时要把休市原因暴露出来，避免用户只看到 -- 不知道是休市还是行情故障。
const currentModuleProfitDescription = computed(() => {
  if (!currentModuleAsset.value) return '按模块收益口径展示';
  if (currentModuleAsset.value.primaryProfitAvailable === false) {
    return currentModuleAsset.value.primaryProfitStatusLabel || moduleUnavailableLabel(activeModule.value);
  }
  return currentModuleAsset.value.primaryProfitStatusLabel || '按模块收益口径展示';
});
const fallbackModuleAssets = computed<InvestmentModuleAsset[]>(() => (['FUND', 'STOCK', 'CRYPTO'] as const).map((module) => {
  const items = holdings.value.filter((item) => item.assetType === module);
  const assetAmount = round4(items.reduce((sum, item) => sum + convertAmount(item.marketValue, item.currency), 0));
  const cost = round4(items.reduce((sum, item) => sum + convertAmount(item.totalCost, item.currency), 0));
  const holdingProfit = round4(items.reduce((sum, item) => sum + convertAmount(item.floatingProfit, item.currency), 0));
  const primaryProfitAvailable = items.some((item) => item.todayPriceAvailable === true && item.todayProfitByCurrentQuantity !== null && item.todayProfitByCurrentQuantity !== undefined);
  return {
    module,
    name: moduleLabel(module),
    assetAmount,
    assetRatio: totalMarketValue.value > 0 ? round4((assetAmount / totalMarketValue.value) * 100) : 0,
    primaryProfitLabel: modulePrimaryLabel(module),
    primaryProfitAvailable,
    primaryProfitAmount: primaryProfitAvailable ? round4(items
      .filter((item) => item.todayPriceAvailable === true && item.todayProfitByCurrentQuantity !== null && item.todayProfitByCurrentQuantity !== undefined)
      .reduce((sum, item) => sum + convertAmount(item.todayProfitByCurrentQuantity || 0, item.currency), 0)) : null,
    primaryProfitStatusLabel: primaryProfitAvailable ? '今日有效价资产' : moduleUnavailableLabel(module, items),
    holdingProfit,
    holdingProfitRate: rate4(holdingProfit, cost),
    holdingCount: items.length
  };
}));
const subTypeOptions = computed(() => {
  if (activeModule.value === 'FUND') {
    return [
      { label: '全部', value: 'ALL' },
      { label: '场外基金', value: 'OTC_FUND' },
      { label: '货币基金', value: 'MONEY_FUND' },
      { label: '债券基金', value: 'BOND_FUND' },
      { label: 'QDII', value: 'QDII_FUND' },
      { label: 'ETF', value: 'ETF' }
    ];
  }
  if (activeModule.value === 'STOCK') {
    return [
      { label: '全部', value: 'ALL' },
      { label: 'A股', value: 'CN_STOCK' },
      { label: '港股', value: 'HK_STOCK' },
      { label: '美股', value: 'US_STOCK' },
      { label: 'ETF', value: 'ETF' }
    ];
  }
  return [
    { label: '全部', value: 'ALL' },
    { label: '现货', value: 'CRYPTO_SPOT' }
  ];
});
const filteredModuleHoldings = computed(() => {
  const rows = moduleHoldings.value.length ? moduleHoldings.value : holdings.value.filter((item) => item.assetType === activeModule.value);
  return activeSubType.value === 'ALL' ? rows : rows.filter((item) => item.assetSubType === activeSubType.value);
});
const moduleHoldingTotal = computed(() => filteredModuleHoldings.value.length);
const pagedModuleHoldings = computed(() => {
  const start = (modulePageNo.value - 1) * modulePageSize.value;
  return filteredModuleHoldings.value.slice(start, start + modulePageSize.value);
});
watch(moduleHoldingTotal, (total) => {
  const maxPage = Math.max(1, Math.ceil(total / modulePageSize.value));
  if (modulePageNo.value > maxPage) {
    modulePageNo.value = maxPage;
  }
});
const allocationOption = computed<EChartsOption>(() => ({
  color: ['#3b82f6', '#2dd4bf', '#8b5cf6', '#f6c453', '#fb7185', '#60a5fa', '#a78bfa'],
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['45%', '72%'],
    data: moduleAssets.value.map((item) => ({ name: item.name, value: round4(convertAmount(item.assetAmount, 'CNY')) })).filter((item) => item.value > 0)
  }]
}));
const investmentTrendOption = computed<EChartsOption>(() => ({
  grid: { left: 54, right: 18, top: 24, bottom: 36 },
  tooltip: { trigger: 'axis', valueFormatter: (value) => formatMoney(Number(value)) },
  xAxis: { type: 'category', data: investmentTrend.value.map((item) => item.date), axisLine: { lineStyle: { color: '#e2e8f0' } } },
  yAxis: { type: 'value', axisLabel: { formatter: (value: number) => compactMoney(value) }, splitLine: { lineStyle: { color: '#e2e8f0' } } },
  series: [{
    name: moduleLabel(trendModule.value),
    type: 'line',
    smooth: true,
    symbolSize: 6,
    data: investmentTrend.value.map((item) => round4(convertAmount(item.assetAmount ?? item.marketValue, 'CNY'))),
    lineStyle: { color: '#2563eb', width: 3 },
    itemStyle: { color: '#2563eb' },
    areaStyle: { color: 'rgba(37, 99, 235, 0.12)' }
  }]
}));
const profitOption = computed<EChartsOption>(() => ({
  grid: { left: 50, right: 18, top: 24, bottom: 36 },
  xAxis: { type: 'category', data: contributionData.value.map((item) => item.name), axisLabel: { interval: 0, rotate: contributionData.value.length > 6 ? 25 : 0 } },
  yAxis: { type: 'value', axisLabel: { formatter: (value: number) => compactMoney(value) } },
  tooltip: { trigger: 'axis', valueFormatter: (value) => formatMoney(Number(value)) },
  series: [{ type: 'bar', data: contributionData.value.map((item) => item.value), itemStyle: { color: '#3b82f6', borderRadius: [10, 10, 0, 0] } }]
}));
const contributionData = computed(() => contributionRows()
  .map((item) => ({ name: item.name, value: round4(item.value) }))
  .filter((item) => item.value !== 0)
  .sort((a, b) => Math.abs(b.value) - Math.abs(a.value)));

// 加载页面数据。
async function loadPageData() {
  loading.value = true;
  try {
    const [overviewResult, holdingList, trendResult, transactionList] = await Promise.all([
      investmentApi.overviewInvestments(),
      investmentApi.listInvestmentHoldings({ module: 'ALL' }),
      investmentApi.trendInvestments({ module: trendModule.value, period: 'MONTH' }),
      investmentApi.listTransactions()
    ]);
    overview.value = overviewResult;
    holdings.value = holdingList;
    moduleHoldings.value = activeModule.value === 'ALL' ? [] : await investmentApi.listInvestmentHoldings({ module: activeModule.value });
    investmentTrend.value = trendResult.points || [];
    transactions.value = transactionList;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '投资数据加载失败');
  } finally {
    loading.value = false;
  }
}

// 加载趋势数据。
async function loadTrend() {
  try {
    const result = await investmentApi.trendInvestments({ module: trendModule.value, period: 'MONTH' });
    investmentTrend.value = result.points || [];
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '趋势加载失败');
  }
}

// 切换投资模块。
async function handleModuleChange() {
  if (activeModule.value === 'ALL') {
    moduleHoldings.value = [];
    return;
  }
  loading.value = true;
  try {
    moduleHoldings.value = await investmentApi.listInvestmentHoldings({ module: activeModule.value });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '模块持仓加载失败');
  } finally {
    loading.value = false;
  }
}

// 切换持仓分页大小。
function handleModulePageSizeChange() {
  modulePageNo.value = 1;
}

// 切换模块。
function switchModule(module: string) {
  activeModule.value = module as InvestmentModule;
  handleModuleChange();
}

// 加载汇率。
async function loadExchangeRate() {
  try {
    const result = await exchangeRateApi.usdCny();
    usdCnyRate.value = Number(result.rate || usdCnyRate.value);
  } catch {
    // 汇率接口失败时保留默认值，避免影响投资页主体展示。
  }
}

// 生成收益贡献行。
function contributionRows() {
  if (contributionMode.value === 'PRIMARY') {
    return holdings.value
      .filter((item) => item.todayPriceAvailable === true && item.todayProfitByCurrentQuantity !== null && item.todayProfitByCurrentQuantity !== undefined)
      .map((item) => ({ name: item.assetName || item.symbol || '-', value: convertAmount(Number(item.todayProfitByCurrentQuantity), item.currency) }));
  }
  if (contributionMode.value === 'MONTH' || contributionMode.value === 'YEAR') {
    return realizedContributionRows(contributionMode.value);
  }
  return holdings.value.map((item) => ({ name: item.assetName || item.symbol || '-', value: convertAmount(Number(item.floatingProfit || 0), item.currency) }));
}

// 打开新增持仓弹窗。
function openCreateHoldingDialog() {
  if (activeModule.value === 'ALL') {
    ElMessage.warning('请先进入基金、股票或虚拟货币模块');
    return;
  }
  resetHoldingForm(activeModule.value as AssetType);
  holdingDialogVisible.value = true;
}

// 重置持仓表单。
function resetHoldingForm(assetType: AssetType) {
  holdingForm.assetName = '';
  holdingForm.symbol = '';
  holdingForm.assetType = assetType;
  holdingForm.market = defaultMarket(assetType);
  holdingForm.currency = assetType === 'CRYPTO' ? 'USD' : 'CNY';
  holdingForm.quoteSource = defaultQuoteSource(assetType);
  holdingForm.quoteKey = '';
  holdingForm.latestPrice = 0;
  holdingForm.previousClose = null;
  holdingForm.changePercent = null;
  holdingForm.quoteTime = null;
  holdingForm.marketStatus = '';
  holdingForm.quantity = 0;
  holdingForm.avgCost = 0;
  holdingForm.remark = '';
  lookupKeyword.value = '';
  lookupMarket.value = '';
  lookupResults.value = [];
}

// 打开持仓详情。
function openHoldingDetail(holding: HoldingItem) {
  router.push({
    path: ROUTES.holdingDetail.replace(':id', holding.id),
    // 详情页直达或浏览器历史缺失时仍能回到刚才所在投资模块。
    query: { fromModule: activeModule.value }
  });
}

// 解析路由模块。
function routeModule(value: unknown): InvestmentModule {
  const module = Array.isArray(value) ? value[0] : value;
  return moduleTabs.some((item) => item.value === module) ? module as InvestmentModule : 'ALL';
}

// 同步模块路由参数。
function syncModuleQuery() {
  // 模块状态写入 URL，详情页返回时不会因为组件重建而丢回“总览”。
  const nextQuery = { ...route.query };
  if (activeModule.value === 'ALL') {
    delete nextQuery.module;
  } else {
    nextQuery.module = activeModule.value;
  }
  if (nextQuery.module === route.query.module || (!nextQuery.module && !route.query.module)) {
    return;
  }
  router.replace({ path: ROUTES.investments, query: nextQuery });
}

// 获取默认行情来源。
function defaultQuoteSource(assetType: AssetType): QuoteSource {
  if (assetType === 'CRYPTO') return 'COINGECKO';
  if (assetType === 'FUND') return 'EASTMONEY';
  if (assetType === 'STOCK') return 'SINA';
  return 'MANUAL';
}

// 获取默认市场。
function defaultMarket(assetType: AssetType, symbol = '') {
  if (assetType === 'CRYPTO') return 'CRYPTO';
  if (assetType === 'FUND') return 'CN_FUND';
  if (assetType === 'STOCK') {
    const normalized = symbol.toUpperCase();
    if (normalized.endsWith('.SH')) return 'SH';
    if (normalized.endsWith('.SZ')) return 'SZ';
    if (normalized.endsWith('.BJ')) return 'BJ';
    return normalized && !/^\d{6}$/.test(normalized) ? 'US' : '';
  }
  return 'UNKNOWN';
}

// 识别资产。
async function handleLookupAsset() {
  if (!lookupKeyword.value) {
    ElMessage.warning('请输入代码或名称');
    return;
  }
  lookupLoading.value = true;
  try {
    lookupResults.value = await investmentApi.lookupAssets({
      type: holdingForm.assetType,
      keyword: lookupKeyword.value,
      market: lookupMarket.value || undefined
    });
    if (lookupResults.value.length === 0) {
      ElMessage.warning('没有查询到资产信息，可手动录入');
    }
  } catch (error) {
    lookupResults.value = [];
    ElMessage.error(error instanceof Error ? error.message : '资产信息查询失败，可手动录入');
  } finally {
    lookupLoading.value = false;
  }
}

// 应用资产识别结果。
function applyLookupResult(item: AssetLookupItem) {
  if (item.assetType !== holdingForm.assetType) {
    ElMessage.warning('查询结果类型与当前模块不一致');
    return;
  }
  holdingForm.assetName = item.name;
  holdingForm.symbol = item.symbol;
  holdingForm.market = item.market || defaultMarket(item.assetType, item.symbol);
  holdingForm.currency = item.currency;
  holdingForm.quoteSource = item.quoteSource;
  holdingForm.quoteKey = item.quoteKey;
  holdingForm.latestPrice = item.latestPrice ? roundTo(item.latestPrice, item.assetType === 'CRYPTO' ? 8 : 4) : 0;
  holdingForm.previousClose = item.previousClose ?? null;
  holdingForm.changePercent = item.changePercent ?? null;
  holdingForm.quoteTime = item.quoteTime || null;
  holdingForm.marketStatus = 'LOOKUP';
  if (!holdingForm.avgCost && item.latestPrice) {
    holdingForm.avgCost = roundTo(item.latestPrice, 4);
  }
  lookupKeyword.value = item.symbol;
  ElMessage.success('资产信息已填充');
}

// 保存持仓。
async function handleSaveHolding() {
  if (!holdingForm.assetName || !holdingForm.symbol) {
    ElMessage.warning('请输入持仓名称和资产代码');
    return;
  }
  if (holdingForm.assetType !== 'FUND' && holdingForm.quantity <= 0) {
    ElMessage.warning('请输入有效数量');
    return;
  }
  submitting.value = true;
  try {
    const payload: HoldingRequest = {
      ...holdingForm,
      assetType: activeModule.value as AssetType,
      quantity: holdingForm.assetType === 'FUND' ? 0 : roundQuantity(holdingForm.quantity, holdingForm.assetType),
      avgCost: holdingForm.assetType === 'FUND' ? 0 : round4(holdingForm.avgCost)
    };
    await investmentApi.createHolding(payload);
    holdingDialogVisible.value = false;
    ElMessage.success('持仓已保存');
    await loadPageData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '持仓保存失败');
  } finally {
    submitting.value = false;
  }
}

// 打开价格弹窗。
function openQuoteDialog(holding: HoldingItem) {
  activeHolding.value = holding;
  quoteForm.price = roundTo(Number(holding.latestPrice || holding.avgCost || 0), pricePrecision(holding));
  quoteForm.currency = holding.currency || 'CNY';
  quoteDialogVisible.value = true;
}

// 保存手动价格。
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

// 刷新行情。
async function handleRefreshQuote(holding: HoldingItem) {
  refreshingAssetId.value = holding.assetId;
  try {
    await investmentApi.refreshQuote({ assetId: holding.assetId });
    ElMessage.success('行情已刷新');
    await loadPageData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '行情刷新失败');
  } finally {
    refreshingAssetId.value = '';
  }
}

// 判断能否删除持仓。
function canDeleteHolding(holding: HoldingItem) {
  return Number(holding.quantity || 0) <= 0;
}

// 删除持仓。
async function handleDeleteHolding(holding: HoldingItem) {
  try {
    await ElMessageBox.confirm(`确认删除 ${holding.assetName || holding.symbol || '该持仓'}？`, '删除持仓', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await investmentApi.removeHolding(holding.id);
    ElMessage.success('持仓已删除');
    await loadPageData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '持仓删除失败');
  }
}

// 生成已实现收益贡献行。
function realizedContributionRows(mode: Extract<ContributionMode, 'MONTH' | 'YEAR'>) {
  const begin = periodStart(mode);
  const holdingMap = new Map(holdings.value.map((item) => [item.assetId, item]));
  const result = new Map<string, number>();
  transactions.value
    .filter((item) => item.status !== 'REVOKED' && item.type === 'SELL' && item.realizedProfit !== null && item.realizedProfit !== undefined)
    .filter((item) => new Date(item.transactionTime).getTime() >= begin.getTime())
    .forEach((item) => {
      const holding = holdingMap.get(item.assetId);
      const name = item.assetName || holding?.assetName || item.symbol || '-';
      // 月/年贡献当前只统计已实现卖出盈亏，不用缺失的历史市值曲线冒充周期贡献。
      const value = convertAmount(Number(item.realizedProfit || 0), holding?.currency || 'CNY');
      result.set(name, round4((result.get(name) || 0) + value));
    });
  return [...result.entries()].map(([name, value]) => ({ name, value }));
}

// 按展示币种换算金额。
function convertAmount(value: number, sourceCurrency?: string | null) {
  const source = sourceCurrency || 'CNY';
  if (source === displayCurrency.value) return round4(Number(value));
  if (source === 'USD' && displayCurrency.value === 'CNY') return round4(Number(value) * usdCnyRate.value);
  if (source === 'CNY' && displayCurrency.value === 'USD') return round4(Number(value) / usdCnyRate.value);
  return round4(Number(value));
}

// 换算可为空金额。
function convertNullableAmount(value?: number | null, sourceCurrency?: string | null) {
  if (value === null || value === undefined) {
    return null;
  }
  return convertAmount(value, sourceCurrency);
}

// 计算四位收益率。
function rate4(profit: number, cost: number) {
  return cost <= 0 ? 0 : round4((profit / cost) * 100);
}

// 保留四位小数。
function round4(value: number) {
  return Number(Number(value || 0).toFixed(4));
}

// 格式化紧凑金额。
function compactMoney(value: number) {
  const abs = Math.abs(value);
  if (abs >= 10000) return `${currencySymbol.value}${round4(value / 10000)}万`;
  return `${currencySymbol.value}${round4(value)}`;
}

// 格式化金额。
function formatMoney(value: number) {
  return `${currencySymbol.value}${round4(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 })}`;
}

// 格式化比例。
function formatRatio(value?: number | null) {
  return `${round4(Number(value || 0)).toFixed(2)}%`;
}

// 格式化价格。
function formatPrice(value?: number | null, scale?: number | null) {
  if (value === null || value === undefined) return '--';
  const precision = scale || 4;
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: Math.min(precision, 4), maximumFractionDigits: precision });
}

// 格式化识别价格。
function formatLookupPrice(item: AssetLookupItem) {
  return formatPrice(item.latestPrice, item.assetType === 'CRYPTO' ? 8 : 4);
}

// 格式化日期时间。
function formatDateTime(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '';
}

// 获取价格精度。
function pricePrecision(holding?: HoldingItem | null) {
  return holding?.assetType === 'CRYPTO' ? 8 : 4;
}

// 按指定精度取值。
function roundTo(value: number, precision: number) {
  return Number(Number(value || 0).toFixed(precision));
}

// 按资产类型处理数量精度。
function roundQuantity(value: number, assetType: AssetType) {
  return roundTo(value, assetType === 'CRYPTO' ? 10 : 4);
}

// 计算周期开始日期。
function periodStart(mode: Extract<ContributionMode, 'MONTH' | 'YEAR'>) {
  const now = new Date();
  return mode === 'MONTH' ? new Date(now.getFullYear(), now.getMonth(), 1) : new Date(now.getFullYear(), 0, 1);
}

// 转换模块名称。
function moduleLabel(module?: string | null) {
  return ({ ALL: '总览', FUND: '基金', STOCK: '股票', CRYPTO: '虚拟货币' } as Record<string, string>)[module || ''] || '-';
}

// 转换模块主指标名称。
function modulePrimaryLabel(module?: string | null) {
  return '今日收益';
}

// 转换模块价格名称。
function modulePriceLabel(module?: string | null) {
  return module === 'FUND' ? '最新净值' : '当前价';
}

// 转换资产子类型名称。
function subTypeLabel(value?: string | null) {
  return ({ OTC_FUND: '场外基金', MONEY_FUND: '货币基金', BOND_FUND: '债券基金', QDII_FUND: 'QDII', ETF: 'ETF', CN_STOCK: 'A股', HK_STOCK: '港股', US_STOCK: '美股', CRYPTO_SPOT: '现货' } as Record<string, string>)[value || ''] || '-';
}

// 转换价格状态文案。
function priceStatusLabel(row: HoldingItem) {
  if (row.priceStatus === 'MARKET_CLOSED') {
    return '休市';
  }
  if (row.todayPriceAvailable === false) {
    return row.assetType === 'FUND' ? '今日净值未更新' : '今日价未更新';
  }
  return row.priceDate ? `${row.priceLabel || modulePriceLabel(row.assetType)} ${row.priceDate}` : '正常';
}

// 转换价格状态标签类型。
function priceStatusTagType(row: HoldingItem) {
  if (row.priceStatus === 'MARKET_CLOSED') {
    return 'info';
  }
  return row.todayPriceAvailable === false ? 'warning' : 'success';
}

// 生成模块不可用提示。
function moduleUnavailableLabel(module: string, items: HoldingItem[] = []) {
  if (items.length === 0) {
    return '暂无持仓';
  }
  if (items.some((item) => item.priceStatus === 'MARKET_CLOSED')) {
    return '今日休市';
  }
  return module === 'FUND' ? '今日净值未更新' : '今日价格未更新';
}

// 生成今日收益率文案。
function todayProfitRateText(row: HoldingItem) {
  if (row.todayProfitRateByCurrentQuantity !== null && row.todayProfitRateByCurrentQuantity !== undefined) {
    return formatRatio(row.todayProfitRateByCurrentQuantity);
  }
  return priceStatusLabel(row);
}

// 计算收益颜色语义。
function profitTone(value?: number | null): 'success' | 'danger' | 'warning' | 'primary' {
  if (value === null || value === undefined) return 'warning';
  if (value > 0) return 'success';
  if (value < 0) return 'danger';
  return 'primary';
}
</script>

<style scoped>
/* 投资总览采用项目 xo-design 卡片风格，模块卡保留轻量可点击入口。 */
.investment-nav-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--xo-border);
}

.investment-module-tabs {
  min-width: 0;
  flex: 1;
}

.investment-module-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.investment-module-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.investment-nav-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  padding-bottom: 8px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-head h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
}

.panel-subtitle {
  margin: 4px 0 0;
  color: var(--xo-muted);
  font-size: 13px;
}

.currency-select {
  width: 120px;
}

.module-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin: 20px 0;
}

.module-card {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.module-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--xo-shadow-lg);
}

.module-card-top,
.module-card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.module-card-top span {
  font-size: 15px;
  color: var(--xo-muted);
}

.module-card strong {
  display: block;
  margin: 12px 0 16px;
  font-size: 28px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.module-card-meta {
  margin-top: 8px;
  font-size: 13px;
}

.module-status-tip {
  margin: 8px 0 0;
  color: var(--xo-warning);
  font-size: 12px;
}

.module-summary-grid {
  margin-bottom: 20px;
}

.module-holdings-panel {
  overflow: hidden;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-top: 16px;
  color: var(--xo-muted);
  font-size: 14px;
}

.module-panel-head {
  align-items: flex-start;
}

.module-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.holding-name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.holding-name-cell strong {
  color: var(--xo-text);
}

.holding-name-cell span,
.primary-profit-cell small,
.muted-text {
  color: var(--xo-muted);
}

.primary-profit-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.contribution-panel {
  margin-top: 24px;
}

.lookup-panel {
  margin-bottom: 8px;
}

.lookup-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  width: 100%;
}

.lookup-row:has(.market-select) {
  grid-template-columns: 118px minmax(0, 1fr) auto;
}

.lookup-results {
  display: grid;
  gap: 8px;
  margin-bottom: 14px;
}

.lookup-item {
  display: grid;
  gap: 3px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--xo-border);
  border-radius: 10px;
  background: #fff;
  color: var(--xo-text);
  text-align: left;
  cursor: pointer;
}

.lookup-item:hover {
  border-color: rgba(37, 99, 235, 0.35);
}

.lookup-item span,
.form-tip {
  color: var(--xo-muted);
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 14px;
}

.holding-price-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 14px;
}

.holding-remark-item {
  grid-column: span 2;
}

.full-width {
  width: 100%;
}

@media (max-width: 1080px) {
  .module-card-grid,
  .module-summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .investment-nav-row {
    flex-direction: column;
    align-items: stretch;
  }

  .investment-nav-actions {
    justify-content: flex-start;
  }

  .form-grid,
  .holding-price-grid {
    grid-template-columns: 1fr;
  }

  .holding-remark-item {
    grid-column: auto;
  }
}
</style>
