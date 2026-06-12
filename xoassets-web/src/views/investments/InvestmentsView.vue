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
        <MetricCard title="投资总资产" :value="overviewTotalInvestmentAssetValue" :trend="overview?.holdingProfitRate ?? totalProfitRate" description="基金 + 股票 + 虚拟货币" tone="primary" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard title="持有收益" :value="overviewHoldingProfitValue" :trend="overview?.holdingProfitRate ?? totalProfitRate" description="全部资产当前市值 - 总成本" :tone="profitTone(overviewHoldingProfitValue)" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard title="持仓成本" :value="overviewTotalCostValue" :trend="overview?.holdingProfitRate ?? totalProfitRate" description="趋势为持有收益率" tone="primary" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard title="今日收益" :value="overviewTodayProfitValue" :trend="null" :description="overview?.todayProfitStatusLabel || overview?.todayProfitAssetScope || '今日有效价资产'" :tone="profitTone(overviewTodayProfitValue)" :precision="4" :currency-symbol="currencySymbol">
          <template #extra>
            <div class="metric-extra-row">
              <span>昨日收益</span>
              <AmountText :value="convertNullableAmount(overview?.yesterdayProfit, 'CNY')" with-sign :precision="4" :currency-symbol="currencySymbol" />
            </div>
          </template>
        </MetricCard>
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
          <div class="module-card-meta muted-text">
            <span>昨日收益</span>
            <AmountText :value="moduleCardYesterdayProfit(item)" with-sign :precision="4" :currency-symbol="currencySymbol" />
          </div>
          <div class="module-card-meta muted-text">
            <span>持有收益</span>
            <AmountText :value="convertAmount(item.holdingProfit, 'CNY')" with-sign :precision="4" :currency-symbol="currencySymbol" />
          </div>
        </div>
      </section>

      <section class="grid-2">
        <div class="panel panel-padding investment-trend-panel">
          <div class="panel-head">
            <div>
              <h3>资产趋势</h3>
              <p class="panel-subtitle">总览、股票、基金、虚拟货币同图展示，左轴单位 k</p>
            </div>
            <el-segmented v-model="trendPeriod" :options="trendPeriodOptions" @change="loadInvestmentTrends" />
          </div>
          <el-empty v-if="!loading && investmentTrendSeriesEmpty" description="暂无投资资产曲线数据" />
          <BaseChart v-else :option="investmentTrendOption" />
        </div>
      </section>

      <section v-loading="loading || dailyProfitCalendarLoading" class="panel panel-padding daily-profit-panel">
        <div class="panel-head daily-profit-head">
          <div>
            <h3>每日收益</h3>
            <p class="panel-subtitle">{{ dailyProfitSubtitle }}</p>
          </div>
          <div class="daily-profit-actions">
            <el-segmented v-model="dailyProfitPanelMode" :options="dailyProfitPanelOptions" />
            <template v-if="dailyProfitPanelMode === 'CALENDAR'">
              <el-button-group>
                <el-button :icon="ArrowLeft" aria-label="上一月" @click="changeProfitMonth(-1)" />
                <el-button :disabled="isCurrentProfitMonth" @click="resetProfitMonth">本月</el-button>
                <el-button :icon="ArrowRight" :disabled="!canGoNextProfitMonth" aria-label="下一月" @click="changeProfitMonth(1)" />
              </el-button-group>
              <el-date-picker
                v-model="profitCalendarMonth"
                class="profit-month-picker"
                type="month"
                format="YYYY年MM月"
                :clearable="false"
                :editable="false"
                :disabled-date="disabledFutureProfitMonth"
              />
            </template>
          </div>
        </div>
        <el-empty v-if="dailyProfitPanelMode === 'CALENDAR' && !loading && !dailyProfitCalendarLoading && dailyProfitCalendarEntries.length === 0" description="暂无每日收益数据" />
        <div v-else-if="dailyProfitPanelMode === 'CALENDAR'" class="daily-profit-calendar">
          <div class="daily-profit-summary">
            <div>
              <span>本月合计</span>
              <strong :class="dailyProfitValueClass(dailyProfitMonthlyTotal)">{{ formatSignedMoney(dailyProfitMonthlyTotal) }}</strong>
            </div>
            <div>
              <span>统计天数</span>
              <strong>{{ dailyProfitAvailableDays }} 天</strong>
            </div>
            <div>
              <span>盈利 / 回撤</span>
              <strong>{{ dailyProfitPositiveDays }} / {{ dailyProfitNegativeDays }}</strong>
            </div>
          </div>
          <div class="profit-calendar-weekdays">
            <span v-for="day in profitCalendarWeekdays" :key="day">{{ day }}</span>
          </div>
          <div class="profit-calendar-grid">
            <div v-for="cell in profitCalendarCells" :key="cell.key" class="profit-calendar-cell" :class="profitCalendarCellClass(cell)">
              <template v-if="!cell.empty">
                <div class="profit-calendar-date">
                  <span>{{ cell.dayNumber }}</span>
                  <em v-if="cell.isToday">今天</em>
                </div>
                <div class="profit-calendar-amount">{{ profitCalendarAmountText(cell) }}</div>
                <small>{{ profitCalendarStatusText(cell) }}</small>
              </template>
            </div>
          </div>
        </div>
        <div v-else class="investment-transactions-panel">
          <el-empty v-if="!loading && investmentTransactions.length === 0" description="暂无投资交易记录" />
          <template v-else>
            <el-table :data="pagedInvestmentTransactions" stripe class="clickable-table" @row-click="openTransactionHoldingDetail">
              <el-table-column label="时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.transactionTime) }}</template>
              </el-table-column>
              <el-table-column label="资产" min-width="180">
                <template #default="{ row }">
                  <div class="holding-name-cell">
                    <strong>{{ row.assetName || row.symbol || '-' }}</strong>
                    <span>{{ row.symbol || '-' }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="类型" width="90" align="center">
                <template #default="{ row }"><el-tag round effect="light" size="small" :type="transactionTypeTagType(row.type)">{{ transactionTypeLabel(row.type) }}</el-tag></template>
              </el-table-column>
              <el-table-column label="资金账户" min-width="130">
                <template #default="{ row }">{{ row.accountName || '-' }}</template>
              </el-table-column>
              <el-table-column label="数量" min-width="130" align="right" header-align="right">
                <template #default="{ row }">{{ formatTransactionQuantity(row) }}</template>
              </el-table-column>
              <el-table-column label="价格/净值" min-width="130" align="right" header-align="right">
                <template #default="{ row }">{{ formatTransactionPrice(row) }}</template>
              </el-table-column>
              <el-table-column label="金额" min-width="140" align="right" header-align="right">
                <template #default="{ row }"><AmountText :value="transactionAmount(row)" :precision="4" :currency-symbol="currencySymbol" /></template>
              </el-table-column>
              <el-table-column label="手续费" min-width="110" align="right" header-align="right">
                <template #default="{ row }"><AmountText :value="row.fee" :precision="4" :currency-symbol="currencySymbol" /></template>
              </el-table-column>
              <el-table-column label="状态" width="110" align="center">
                <template #default="{ row }"><el-tag round effect="light" size="small" :type="transactionStatusTagType(row.status)">{{ transactionStatusLabel(row.status) }}</el-tag></template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="danger" :disabled="!canRevokeTransaction(row)" @click.stop="handleRevokeTransaction(row)">撤销</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="table-footer">
              <el-pagination
                v-model:current-page="transactionPageNo"
                v-model:page-size="transactionPageSize"
                layout="total, sizes, prev, pager, next"
                :page-sizes="pageSizeOptions"
                :total="investmentTransactionTotal"
                @size-change="handleTransactionPageSizeChange"
              />
            </div>
          </template>
        </div>
      </section>
    </template>

    <template v-else>
      <section v-loading="loading" class="grid-3 module-summary-grid">
        <MetricCard :title="`${moduleLabel(activeModule)}总资产`" :value="currentModuleAssetAmountValue" :trend="currentModuleAsset?.assetRatio ?? 0" description="当前模块持仓市值" tone="primary" :precision="4" :currency-symbol="currencySymbol" />
        <MetricCard :title="currentModuleAsset?.primaryProfitLabel || modulePrimaryLabel(activeModule)" :value="currentModuleProfitValue" :trend="null" :description="currentModuleProfitDescription" :tone="profitTone(currentModuleProfitValue)" :precision="4" :currency-symbol="currencySymbol">
          <template #extra>
            <div class="metric-extra-row">
              <span>昨日收益</span>
              <AmountText :value="currentModuleAsset ? moduleCardYesterdayProfit(currentModuleAsset) : null" with-sign :precision="4" :currency-symbol="currencySymbol" />
            </div>
          </template>
        </MetricCard>
        <MetricCard title="持有收益" :value="currentModuleHoldingProfitValue" :trend="currentModuleAsset?.holdingProfitRate ?? 0" description="当前市值 - 持仓成本" :tone="profitTone(currentModuleHoldingProfitValue)" :precision="4" :currency-symbol="currencySymbol" />
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
        <el-table :data="pagedModuleHoldings" stripe :default-sort="{ prop: 'marketValue', order: 'descending' }" @sort-change="handleModuleHoldingSortChange" @row-click="openHoldingDetail">
          <el-table-column label="名称" min-width="220">
            <template #default="{ row }">
              <div class="holding-name-cell">
                <strong>{{ row.assetName || row.symbol || '-' }}</strong>
                <span>{{ row.symbol || '-' }} · {{ subTypeLabel(row.assetSubType) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="持有市值" prop="marketValue" sortable="custom" min-width="140" align="right" header-align="right">
            <template #default="{ row }"><AmountText :value="convertAmount(row.marketValue, row.currency)" :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="持有收益" prop="floatingProfit" sortable="custom" min-width="160" align="right" header-align="right">
            <template #default="{ row }"><AmountText :value="holdingDisplayProfit(row)" with-sign :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column label="今日收益/收益率" prop="todayProfit" sortable="custom" min-width="170" align="right" header-align="right">
            <template #default="{ row }">
              <div class="primary-profit-cell">
                <AmountText v-if="row.todayProfit !== null && row.todayProfit !== undefined" :value="convertAmount(row.todayProfit, row.currency)" with-sign :precision="4" :currency-symbol="currencySymbol" />
                <span v-else class="muted-text">--</span>
                <small>{{ todayProfitRateText(row) }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="昨日收益" prop="yesterdayProfit" sortable="custom" min-width="140" align="right" header-align="right">
            <!-- 昨日收益独立成列，和今日收益率分开看。 -->
            <template #default="{ row }"><AmountText :value="convertNullableAmount(row.yesterdayProfit, row.currency)" with-sign :precision="4" :currency-symbol="currencySymbol" /></template>
          </el-table-column>
          <el-table-column :label="modulePriceLabel(activeModule)" prop="latestPrice" sortable="custom" min-width="130" align="right" header-align="right">
            <template #default="{ row }">{{ formatPrice(row.latestPrice, row.priceScale) }}</template>
          </el-table-column>
          <el-table-column label="价格日期" min-width="120" align="right" header-align="right">
            <template #default="{ row }">{{ row.priceDate || '--' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="130" align="center">
            <template #default="{ row }"><el-tag round :type="priceStatusTagType(row)">{{ priceStatusLabel(row) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="190" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openHoldingDetail(row)">详情</el-button>
              <el-button link type="primary" :loading="refreshingAssetId === row.assetId" @click.stop="handleRefreshQuote(row)">刷新</el-button>
              <el-button link type="primary" @click.stop="openQuoteDialog(row)">价格</el-button>
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
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue';
import BaseChart from '@/components/charts/BaseChart.vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import { ROUTES } from '@/constants/routes';
import { exchangeRateApi } from '@/services/exchangeRateApi';
import { investmentApi, type AssetLookupItem, type AssetType, type HoldingItem, type HoldingRequest, type InvestmentCalendarDayProfit, type InvestmentModuleAsset, type InvestmentOverview, type InvestmentTransactionItem, type InvestmentTrendPoint, type QuoteSource } from '@/services/investmentApi';
import { useThemeStore } from '@/stores/theme';
import { readThemeVar } from '@/utils/theme';

type DisplayCurrency = 'CNY' | 'USD';
type InvestmentModule = 'ALL' | 'FUND' | 'STOCK' | 'CRYPTO';
type DailyProfitPanelMode = 'CALENDAR' | 'TRANSACTIONS';
type TrendModuleKey = InvestmentModule;
type TrendPeriod = 'WEEK' | 'MONTH' | 'YEAR';
type ModuleHoldingSortProp = 'marketValue' | 'floatingProfit' | 'todayProfit' | 'yesterdayProfit' | 'latestPrice';
type SortOrder = 'ascending' | 'descending' | null;
type DailyProfitCalendarCell = {
  key: string;
  empty: boolean;
  date: string;
  dayNumber?: number;
  profitAmount?: number | null;
  profitRate?: number | null;
  hasPrice?: boolean | null;
  marketClosed?: boolean | null;
  statusLabel?: string | null;
  isToday?: boolean;
  isFuture?: boolean;
};

const moduleTabs: Array<{ label: string; value: InvestmentModule }> = [
  { label: '总览', value: 'ALL' },
  { label: '基金', value: 'FUND' },
  { label: '股票', value: 'STOCK' },
  { label: '虚拟货币', value: 'CRYPTO' }
];
const trendLineModules: Array<{ label: string; value: TrendModuleKey; color: string }> = [
  { label: '总览', value: 'ALL', color: '#2563eb' },
  { label: '股票', value: 'STOCK', color: '#16a34a' },
  { label: '基金', value: 'FUND', color: '#8b5cf6' },
  { label: '虚拟货币', value: 'CRYPTO', color: '#f59e0b' }
];
const dailyProfitPanelOptions: Array<{ label: string; value: DailyProfitPanelMode }> = [
  { label: '收益日历', value: 'CALENDAR' },
  { label: '交易记录', value: 'TRANSACTIONS' }
];
const trendPeriodOptions: Array<{ label: string; value: TrendPeriod }> = [
  { label: '周', value: 'WEEK' },
  { label: '月', value: 'MONTH' },
  { label: '年', value: 'YEAR' }
];
const route = useRoute();
const router = useRouter();
const themeStore = useThemeStore();
const currencyOptions = [
  { label: '人民币', value: 'CNY' },
  { label: 'USD', value: 'USD' }
];
const overview = ref<InvestmentOverview | null>(null);
const holdings = ref<HoldingItem[]>([]);
const moduleHoldings = ref<HoldingItem[]>([]);
const investmentTrends = ref<Record<TrendModuleKey, InvestmentTrendPoint[]>>({ ALL: [], FUND: [], STOCK: [], CRYPTO: [] });
const dailyProfitCalendarEntries = ref<InvestmentCalendarDayProfit[]>([]);
const investmentTransactions = ref<InvestmentTransactionItem[]>([]);
const dailyProfitCalendarLoading = ref(false);
const dailyProfitCalendarFailed = ref(false);
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
const activeSubType = ref('ALL');
const profitCalendarMonth = ref(startOfMonth(new Date()));
const dailyProfitPanelMode = ref<DailyProfitPanelMode>('CALENDAR');
const trendPeriod = ref<TrendPeriod>('MONTH');
const usdCnyRate = ref(7.2);
// 投资模块持仓表格前端分页展示，统计类数据仍使用完整持仓列表计算。
const pageSizeOptions = [10, 50, 100, 300];
const modulePageNo = ref(1);
const modulePageSize = ref(10);
const moduleHoldingSort = ref<{ prop: ModuleHoldingSortProp; order: SortOrder }>({ prop: 'marketValue', order: 'descending' });
const transactionPageNo = ref(1);
const transactionPageSize = ref(10);
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

watch(profitCalendarMonth, () => {
  loadDailyProfitCalendar();
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
const overviewTotalInvestmentAssetValue = computed(() => overview.value ? convertAmount(overview.value.totalInvestmentAsset, 'CNY') : totalMarketValue.value);
const overviewHoldingProfitValue = computed(() => overview.value ? convertAmount(overview.value.holdingProfit, 'CNY') : totalProfit.value);
const overviewTotalCostValue = computed(() => overview.value ? convertAmount(overview.value.totalCost, 'CNY') : totalCost.value);
// 今日收益没有今日有效价格时显示 --，不能把未更新净值或休市收益兜底为 0。
const overviewTodayProfitValue = computed(() => {
  if (!overview.value) return null;
  return overview.value.todayProfitAvailable === false ? null : convertNullableAmount(overview.value.todayProfit, 'CNY');
});
const currentModuleAssetAmountValue = computed(() => currentModuleAsset.value ? convertAmount(currentModuleAsset.value.assetAmount, 'CNY') : 0);
const currentModuleProfitValue = computed(() => {
  if (!currentModuleAsset.value) return null;
  return currentModuleAsset.value.primaryProfitAvailable === false ? null : convertNullableAmount(currentModuleAsset.value.primaryProfitAmount, 'CNY');
});
const currentModuleHoldingProfitValue = computed(() => currentModuleAsset.value ? convertAmount(currentModuleAsset.value.holdingProfit, 'CNY') : 0);
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
  const assetAmount = round4(items.reduce((sum, item) => sum + amountToCny(item.marketValue, item.currency), 0));
  const cost = round4(items.reduce((sum, item) => sum + amountToCny(item.totalCost, item.currency), 0));
  const holdingProfit = round4(items.reduce((sum, item) => sum + amountToCny(item.floatingProfit, item.currency), 0));
  const totalMarketValueCny = round4(holdings.value.reduce((sum, item) => sum + amountToCny(item.marketValue, item.currency), 0));
  const primaryProfitAvailable = items.some((item) => item.todayPriceAvailable === true && item.todayProfit !== null && item.todayProfit !== undefined);
  return {
    module,
    name: moduleLabel(module),
    assetAmount,
    assetRatio: totalMarketValueCny > 0 ? round4((assetAmount / totalMarketValueCny) * 100) : 0,
    primaryProfitLabel: modulePrimaryLabel(module),
    primaryProfitAvailable,
    primaryProfitAmount: primaryProfitAvailable ? round4(items
      .filter((item) => item.todayPriceAvailable === true && item.todayProfit !== null && item.todayProfit !== undefined)
      .reduce((sum, item) => sum + amountToCny(Number(item.todayProfit), item.currency), 0)) : null,
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
const sortedModuleHoldings = computed(() => {
  const sort = moduleHoldingSort.value;
  const rows = [...filteredModuleHoldings.value];
  if (!sort.order) {
    return rows;
  }
  const order = sort.order;
  // 持仓列表先按全量数据排序再分页，避免只排序当前页造成数据顺序误导。
  return rows.sort((left, right) => compareHoldingSortValue(left, right, sort.prop, order));
});
const moduleHoldingTotal = computed(() => filteredModuleHoldings.value.length);
const pagedModuleHoldings = computed(() => {
  const start = (modulePageNo.value - 1) * modulePageSize.value;
  return sortedModuleHoldings.value.slice(start, start + modulePageSize.value);
});
const investmentTransactionTotal = computed(() => investmentTransactions.value.length);
const pagedInvestmentTransactions = computed(() => {
  const start = (transactionPageNo.value - 1) * transactionPageSize.value;
  // 交易记录复用全量接口，本地分页，避免为了展示分页扩大后端改动。
  return investmentTransactions.value.slice(start, start + transactionPageSize.value);
});
watch(moduleHoldingTotal, (total) => {
  const maxPage = Math.max(1, Math.ceil(total / modulePageSize.value));
  if (modulePageNo.value > maxPage) {
    modulePageNo.value = maxPage;
  }
});
watch(investmentTransactionTotal, (total) => {
  const maxPage = Math.max(1, Math.ceil(total / transactionPageSize.value));
  if (transactionPageNo.value > maxPage) {
    transactionPageNo.value = maxPage;
  }
});
const investmentTrendDates = computed(() => {
  const dates = new Set<string>();
  trendLineModules.forEach((module) => {
    investmentTrends.value[module.value].forEach((item) => dates.add(item.date));
  });
  return [...dates].sort();
});
const investmentTrendSeriesEmpty = computed(() => investmentTrendDates.value.length === 0);
const investmentTrendOption = computed<EChartsOption>(() => {
  const axisText = readThemeVar('--xo-muted', themeStore.resolvedTheme === 'dark' ? '#94a3b8' : '#475569');
  const axisLine = readThemeVar('--xo-border-strong', '#cbd5e1');
  const splitLine = readThemeVar('--xo-border', '#e2e8f0');
  return {
  color: trendLineModules.map((item) => item.color),
  grid: { left: 58, right: 28, top: 28, bottom: 42 },
  legend: { top: 0, right: 0, textStyle: { color: axisText } },
  tooltip: {
    trigger: 'axis',
    valueFormatter: (value) => formatMoney(Number(value) * 1000)
  },
  xAxis: {
    type: 'category',
    data: investmentTrendDates.value,
    axisLabel: { color: axisText, fontWeight: 600 },
    axisLine: { lineStyle: { color: axisLine } }
  },
  yAxis: {
    type: 'value',
    name: '金额(k)',
    axisLabel: { color: axisText, formatter: (value: number) => `${round4(value)}k` },
    splitLine: { lineStyle: { color: splitLine } }
  },
  series: trendLineModules.map((module) => {
    const pointMap = new Map(investmentTrends.value[module.value].map((item) => [item.date, item]));
    return {
      name: module.label,
      type: 'line',
      smooth: true,
      symbolSize: 6,
      connectNulls: true,
      data: investmentTrendDates.value.map((date) => trendPointToK(pointMap.get(date))),
      lineStyle: { width: 3 },
      areaStyle: module.value === 'ALL' ? { color: 'rgba(37, 99, 235, 0.08)' } : undefined
    };
  })
  };
});
const profitCalendarWeekdays = ['日', '一', '二', '三', '四', '五', '六'];
const profitCalendarMonthLabel = computed(() => `${calendarMonthKey(profitCalendarMonth.value)} 月收益`);
const isCurrentProfitMonth = computed(() => isSameCalendarMonth(profitCalendarMonth.value, new Date()));
const canGoNextProfitMonth = computed(() => !isAfterCalendarMonth(addMonths(profitCalendarMonth.value, 1), new Date()));
const dailyProfitMonthlyTotal = computed(() => round4(dailyProfitCalendarEntries.value.reduce((sum, item) => {
  return item.profitAmount === null || item.profitAmount === undefined ? sum : sum + convertAmount(item.profitAmount, 'CNY');
}, 0)));
const dailyProfitAvailableDays = computed(() => dailyProfitCalendarEntries.value.filter((item) => item.profitAmount !== null && item.profitAmount !== undefined).length);
const dailyProfitPositiveDays = computed(() => dailyProfitCalendarEntries.value.filter((item) => Number(item.profitAmount || 0) > 0).length);
const dailyProfitNegativeDays = computed(() => dailyProfitCalendarEntries.value.filter((item) => Number(item.profitAmount || 0) < 0).length);
const profitCalendarCells = computed<DailyProfitCalendarCell[]>(() => {
  const monthStart = startOfMonth(profitCalendarMonth.value);
  const year = monthStart.getFullYear();
  const month = monthStart.getMonth();
  const totalDays = new Date(year, month + 1, 0).getDate();
  const todayKey = formatDateKey(new Date());
  const entryMap = new Map(dailyProfitCalendarEntries.value.map((item) => [item.date, item]));
  const blanks: DailyProfitCalendarCell[] = Array.from({ length: monthStart.getDay() }, (_, index) => ({
    key: `blank-${index}`,
    empty: true,
    date: ''
  }));
  // 总览日历按自然月补齐日期格，接口没返回的日期仍保留位置，方便按月复盘。
  const days = Array.from({ length: totalDays }, (_, index) => {
    const dayNumber = index + 1;
    const date = `${year}-${pad(month + 1)}-${pad(dayNumber)}`;
    const entry = entryMap.get(date);
    return {
      key: date,
      empty: false,
      date,
      dayNumber,
      profitAmount: entry?.profitAmount ?? null,
      profitRate: entry?.profitRate ?? null,
      hasPrice: entry?.hasPrice ?? false,
      marketClosed: entry?.marketClosed ?? false,
      statusLabel: entry?.statusLabel ?? null,
      isToday: date === todayKey,
      isFuture: date > todayKey
    };
  });
  return [...blanks, ...days];
});
const dailyProfitSubtitle = computed(() => {
  if (dailyProfitCalendarEntries.value.length > 0) {
    return `${profitCalendarMonthLabel.value} · 基于所有持仓收益日历逐日汇总`;
  }
  if (dailyProfitCalendarFailed.value) {
    return '每日收益加载失败，暂不展示每日收益';
  }
  return `${profitCalendarMonthLabel.value} · 暂无精确每日收益数据`;
});

// 加载页面数据。
async function loadPageData() {
  loading.value = true;
  try {
    const [overviewResult, holdingList, transactionsResult] = await Promise.all([
      investmentApi.overviewInvestments(),
      investmentApi.listInvestmentHoldings({ module: 'ALL' }),
      investmentApi.listTransactions()
    ]);
    overview.value = overviewResult;
    holdings.value = holdingList;
    investmentTransactions.value = transactionsResult || [];
    moduleHoldings.value = activeModule.value === 'ALL' ? [] : await investmentApi.listInvestmentHoldings({ module: activeModule.value });
    await loadInvestmentTrends();
    // 每日收益月历固定使用全持仓聚合结果，不跟随资产趋势模块切换。
    await loadDailyProfitCalendar(false);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '投资数据加载失败');
  } finally {
    loading.value = false;
  }
}

// 加载四条资产趋势线，周期切换只刷新趋势数据，不重载整页。
async function loadInvestmentTrends() {
  try {
    const trendResults = await Promise.all(
      trendLineModules.map((module) => investmentApi.trendInvestments({ module: module.value, period: trendPeriod.value }))
    );
    investmentTrends.value = trendLineModules.reduce((result, module, index) => {
      result[module.value] = trendResults[index]?.points || [];
      return result;
    }, { ALL: [], FUND: [], STOCK: [], CRYPTO: [] } as Record<TrendModuleKey, InvestmentTrendPoint[]>);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '趋势加载失败');
  }
}

// 加载全持仓每日收益聚合结果，后端保证和持仓详情日历同一算法。
async function loadDailyProfitCalendar(showLoading = true) {
  if (showLoading) {
    dailyProfitCalendarLoading.value = true;
  }
  dailyProfitCalendarFailed.value = false;
  try {
    dailyProfitCalendarEntries.value = await investmentApi.dailyProfitCalendar(selectedProfitMonthParams());
  } catch {
    // 聚合接口失败时不能展示旧口径收益，避免把错误数据当作每日收益。
    dailyProfitCalendarEntries.value = [];
    dailyProfitCalendarFailed.value = true;
    ElMessage.warning('每日收益加载失败，暂不展示每日收益');
  } finally {
    if (showLoading) {
      dailyProfitCalendarLoading.value = false;
    }
  }
}

// 当前月历请求参数，和后端 year/month 查询保持一致。
function selectedProfitMonthParams() {
  return {
    year: profitCalendarMonth.value.getFullYear(),
    month: profitCalendarMonth.value.getMonth() + 1
  };
}

// 切换每日收益月份，只刷新月历数据。
function changeProfitMonth(delta: number) {
  const nextMonth = addMonths(profitCalendarMonth.value, delta);
  if (delta > 0 && isAfterCalendarMonth(nextMonth, new Date())) {
    return;
  }
  profitCalendarMonth.value = nextMonth;
}

// 回到当前自然月。
function resetProfitMonth() {
  const currentMonth = startOfMonth(new Date());
  if (isSameCalendarMonth(profitCalendarMonth.value, currentMonth)) {
    return;
  }
  profitCalendarMonth.value = currentMonth;
}

// 禁止选择未来月份，避免请求还未产生的收益日历。
function disabledFutureProfitMonth(date: Date) {
  return isAfterCalendarMonth(date, new Date());
}

// 获取月份开始日期。
function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

// 增减月份。
function addMonths(date: Date, delta: number) {
  return new Date(date.getFullYear(), date.getMonth() + delta, 1);
}

// 生成日历月份键。
function calendarMonthKey(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}`;
}

// 判断是否同月。
function isSameCalendarMonth(left: Date, right: Date) {
  return left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth();
}

// 判断是否晚于指定月份。
function isAfterCalendarMonth(left: Date, right: Date) {
  return left.getFullYear() > right.getFullYear() || (left.getFullYear() === right.getFullYear() && left.getMonth() > right.getMonth());
}

// 生成 YYYY-MM-DD，字符串比较可用于同格式日期先后判断。
function formatDateKey(date: Date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

// 两位补零。
function pad(value: number) {
  return String(value).padStart(2, '0');
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

// 切换持仓表头排序，排序字段受控，避免透传未知 prop 影响分页前排序。
function handleModuleHoldingSortChange({ prop, order }: { prop?: string; order: SortOrder }) {
  if (!isModuleHoldingSortProp(prop)) {
    moduleHoldingSort.value = { prop: 'marketValue', order: 'descending' };
    return;
  }
  moduleHoldingSort.value = { prop, order };
  modulePageNo.value = 1;
}

// 切换交易记录分页大小。
function handleTransactionPageSizeChange() {
  transactionPageNo.value = 1;
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

// 从全量交易记录跳转到对应持仓详情。
function openTransactionHoldingDetail(transaction: InvestmentTransactionItem) {
  if (!transaction.holdingId) {
    ElMessage.warning('该交易缺少持仓信息');
    return;
  }
  router.push({
    path: ROUTES.holdingDetail.replace(':id', transaction.holdingId),
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

// 按展示币种换算金额。
function convertAmount(value: number, sourceCurrency?: string | null) {
  const source = sourceCurrency || 'CNY';
  if (source === displayCurrency.value) return round4(Number(value));
  if (source === 'USD' && displayCurrency.value === 'CNY') return round4(Number(value) * usdCnyRate.value);
  if (source === 'CNY' && displayCurrency.value === 'USD') return round4(Number(value) / usdCnyRate.value);
  return round4(Number(value));
}

// 聚合 fallback 固定先折算成人民币，避免展示币种切换后被二次换算。
function amountToCny(value: number, sourceCurrency?: string | null) {
  const source = sourceCurrency || 'CNY';
  if (source === 'USD') return round4(Number(value) * usdCnyRate.value);
  return round4(Number(value));
}

// 清仓持仓的当前浮动盈亏为 0，列表仍展示已实现后的总收益，避免卖出后收益看起来消失。
function holdingDisplayProfit(item: HoldingItem) {
  const profit = Number(item.quantity || 0) <= 0 && item.totalProfit !== null && item.totalProfit !== undefined ? item.totalProfit : item.floatingProfit;
  return convertAmount(profit, item.currency);
}

// 排序必须和展示金额同源，清仓持仓不能继续按 0 浮动盈亏排序。
function holdingRawDisplayProfit(item: HoldingItem) {
  return Number(item.quantity || 0) <= 0 && item.totalProfit !== null && item.totalProfit !== undefined ? Number(item.totalProfit) : Number(item.floatingProfit || 0);
}

// 换算可为空金额。
function convertNullableAmount(value?: number | null, sourceCurrency?: string | null) {
  if (value === null || value === undefined) {
    return null;
  }
  return convertAmount(value, sourceCurrency);
}

// 判断模块持仓列表支持的排序字段。
function isModuleHoldingSortProp(prop?: string): prop is ModuleHoldingSortProp {
  return prop === 'marketValue' || prop === 'floatingProfit' || prop === 'todayProfit' || prop === 'yesterdayProfit' || prop === 'latestPrice';
}

// 按业务金额排序，金额类字段统一换算成人民币，空值始终排在最后。
function holdingSortValue(row: HoldingItem, prop: ModuleHoldingSortProp) {
  if (prop === 'latestPrice') {
    return row.latestPrice === null || row.latestPrice === undefined ? null : Number(row.latestPrice);
  }
  if (prop === 'floatingProfit') {
    return amountToCny(holdingRawDisplayProfit(row), row.currency);
  }
  const value = row[prop];
  if (value === null || value === undefined) {
    return null;
  }
  return amountToCny(Number(value), row.currency);
}

// 比较持仓排序值。
function compareHoldingSortValue(left: HoldingItem, right: HoldingItem, prop: ModuleHoldingSortProp, order: Exclude<SortOrder, null>) {
  const leftValue = holdingSortValue(left, prop);
  const rightValue = holdingSortValue(right, prop);
  if (leftValue === null && rightValue === null) {
    return 0;
  }
  if (leftValue === null) {
    return 1;
  }
  if (rightValue === null) {
    return -1;
  }
  return order === 'ascending' ? leftValue - rightValue : rightValue - leftValue;
}

// 模块昨日收益只认后端按收益日历聚合后的结果，避免前端用持仓行旧字段拼出不同日期的收益。
function moduleCardYesterdayProfit(item: InvestmentModuleAsset) {
  return convertNullableAmount(item.yesterdayProfit, 'CNY');
}

// 计算四位收益率。
function rate4(profit: number, cost: number) {
  return cost <= 0 ? 0 : round4((profit / cost) * 100);
}

// 保留四位小数。
function round4(value: number) {
  return Number(Number(value || 0).toFixed(4));
}

// 格式化金额。
function formatMoney(value: number) {
  return `${currencySymbol.value}${round4(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 })}`;
}

// 格式化带正负号的金额。
function formatSignedMoney(value: number) {
  const prefix = value > 0 ? '+' : value < 0 ? '-' : '';
  return `${prefix}${currencySymbol.value}${Math.abs(round4(value)).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 4 })}`;
}

// 趋势图按 k 作为左轴单位，原始金额仍按展示币种换算。
function trendPointToK(point?: InvestmentTrendPoint) {
  if (!point) {
    return null;
  }
  return round4(convertAmount(point.assetAmount ?? point.marketValue, 'CNY') / 1000);
}

// 转换交易类型文案。
function transactionTypeLabel(type?: string | null) {
  return type === 'SELL' ? '卖出' : '买入';
}

// 交易类型两处交易表统一颜色：买入偏收益色，卖出偏提醒色。
function transactionTypeTagType(type?: string | null) {
  return type === 'SELL' ? 'warning' : 'success';
}

// 转换交易状态文案。
function transactionStatusLabel(status?: string | null) {
  return ({ NORMAL: '正常', CONFIRMED: '已确认', PENDING_CONFIRM: '待确认', FAILED: '确认失败', CANCELLED: '已取消', REVOKED: '已撤销' } as Record<string, string>)[status || ''] || '正常';
}

// 交易状态颜色和持仓详情保持一致。
function transactionStatusTagType(status?: string | null) {
  return ({ NORMAL: 'success', CONFIRMED: 'success', PENDING_CONFIRM: 'warning', FAILED: 'danger', CANCELLED: 'info', REVOKED: 'info' } as Record<string, string>)[status || ''] || 'success';
}

// 格式化交易数量，股票 / 基金 / 虚拟货币混合列表统一保留最多 10 位。
function formatTransactionQuantity(row: InvestmentTransactionItem) {
  const quantity = row.confirmedQuantity ?? row.tradeQuantity ?? row.quantity;
  return Number(quantity || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 10 });
}

// 格式化交易价格，金额净值模式优先显示确认净值。
function formatTransactionPrice(row: InvestmentTransactionItem) {
  const price = row.confirmedNav ?? row.tradePrice ?? row.price;
  if (price === null || price === undefined) {
    return '--';
  }
  return Number(price).toLocaleString('zh-CN', { minimumFractionDigits: 4, maximumFractionDigits: 8 });
}

// 交易金额优先展示真实资金流金额，兼容金额净值模式。
function transactionAmount(row: InvestmentTransactionItem) {
  return Number(row.tradeAmount ?? row.amount ?? 0);
}

// 撤销或取消过的交易只保留查看，不允许重复撤销。
function canRevokeTransaction(row: InvestmentTransactionItem) {
  return row.status !== 'REVOKED' && row.status !== 'CANCELLED';
}

// 从全量交易列表撤销投资交易，撤销后刷新持仓、趋势、日历和交易列表。
async function handleRevokeTransaction(row: InvestmentTransactionItem) {
  try {
    await ElMessageBox.confirm('撤销后会反向恢复账户余额和持仓，确认继续吗？', '撤销投资交易', { type: 'warning', confirmButtonText: '撤销', cancelButtonText: '取消' });
    await investmentApi.revokeTransaction(row.id, '录入错误');
    ElMessage.success('投资交易已撤销');
    await loadPageData();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '投资交易撤销失败');
    }
  }
}

// 生成日历收益文案。
function profitCalendarAmountText(cell: DailyProfitCalendarCell) {
  if (cell.marketClosed) {
    return '休市';
  }
  if (cell.profitAmount === null || cell.profitAmount === undefined) {
    return '--';
  }
  return formatSignedMoney(convertAmount(cell.profitAmount, 'CNY'));
}

// 生成日历状态文案。
function profitCalendarStatusText(cell: DailyProfitCalendarCell) {
  if (cell.isFuture) {
    return '未到日期';
  }
  if (cell.marketClosed) {
    return cell.statusLabel || '休市';
  }
  if (cell.profitAmount !== null && cell.profitAmount !== undefined) {
    return cell.profitRate !== null && cell.profitRate !== undefined ? `收益率 ${formatRatio(cell.profitRate)}` : (cell.statusLabel || '已统计');
  }
  return cell.statusLabel || (cell.hasPrice ? '无收益' : '无价格');
}

// 生成日历单元格样式。
function profitCalendarCellClass(cell: DailyProfitCalendarCell) {
  if (cell.empty) {
    return 'is-empty';
  }
  if (cell.isFuture) {
    return ['is-muted', cell.isToday ? 'is-today' : ''];
  }
  if (cell.marketClosed) {
    return ['is-closed', cell.isToday ? 'is-today' : ''];
  }
  if (cell.profitAmount === null || cell.profitAmount === undefined) {
    return ['is-muted', cell.isToday ? 'is-today' : ''];
  }
  return [convertAmount(cell.profitAmount, 'CNY') >= 0 ? 'is-positive' : 'is-negative', cell.isToday ? 'is-today' : ''];
}

// 生成汇总金额样式。
function dailyProfitValueClass(value: number) {
  if (value > 0) return 'is-positive-text';
  if (value < 0) return 'is-negative-text';
  return 'is-muted-text';
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
  if (closedOutHolding(row)) {
    return '清仓';
  }
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
  if (closedOutHolding(row)) {
    return 'info';
  }
  if (row.priceStatus === 'MARKET_CLOSED') {
    return 'info';
  }
  return row.todayPriceAvailable === false ? 'warning' : 'success';
}

// 清仓持仓仍保留历史收益和交易记录，状态显示为清仓但不允许删除。
function closedOutHolding(row: HoldingItem) {
  return Number(row.quantity || 0) <= 0
    && (Math.abs(Number(row.realizedProfit || 0)) > 0 || Math.abs(Number(row.totalProfit || 0)) > 0);
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
  if (row.todayProfitRate !== null && row.todayProfitRate !== undefined) {
    return formatRatio(row.todayProfitRate);
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

.investment-trend-panel {
  grid-column: 1 / -1;
}

.metric-extra-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--xo-muted);
  font-size: 13px;
  font-weight: 500;
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

.daily-profit-panel {
  margin-top: 24px;
}

.daily-profit-head {
  align-items: flex-start;
}

.daily-profit-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.profit-month-picker {
  width: 150px;
}

.daily-profit-calendar {
  display: grid;
  gap: 14px;
}

.investment-transactions-panel {
  display: grid;
  gap: 14px;
}

/* 全量交易记录通过整行点击进入持仓详情，鼠标样式明确交互入口。 */
.clickable-table :deep(.el-table__body tr) {
  cursor: pointer;
}

.daily-profit-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.daily-profit-summary > div {
  padding: 14px 16px;
  border: 1px solid var(--xo-border);
  border-radius: 16px;
  background: linear-gradient(180deg, var(--xo-card-elevated), var(--xo-card));
}

.daily-profit-summary span {
  display: block;
  margin-bottom: 8px;
  color: var(--xo-muted);
  font-size: 13px;
}

.daily-profit-summary strong {
  display: block;
  color: var(--xo-text);
  font-size: 20px;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.profit-calendar-weekdays,
.profit-calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
}

.profit-calendar-weekdays {
  color: var(--xo-muted);
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.profit-calendar-cell {
  min-height: 104px;
  padding: 12px;
  border: 1px solid var(--xo-border);
  border-radius: 18px;
  background: var(--xo-card);
  box-sizing: border-box;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.profit-calendar-cell:not(.is-empty):hover {
  transform: translateY(-2px);
  border-color: var(--xo-border-strong);
  box-shadow: var(--xo-shadow-hover);
}

.profit-calendar-cell.is-empty {
  border-color: transparent;
  background: transparent;
}

.profit-calendar-cell.is-today {
  border-color: var(--xo-primary);
  box-shadow: inset 0 0 0 1px var(--xo-primary-soft);
}

.profit-calendar-cell.is-positive {
  background: rgba(16, 185, 129, 0.08);
}

.profit-calendar-cell.is-negative {
  background: rgba(239, 68, 68, 0.08);
}

.profit-calendar-cell.is-closed {
  border-style: dashed;
  background: rgba(148, 163, 184, 0.10);
}

.profit-calendar-cell.is-muted {
  background: var(--xo-input-muted);
}

.profit-calendar-date {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--xo-text);
  font-weight: 900;
}

.profit-calendar-date em {
  padding: 2px 7px;
  border-radius: 999px;
  background: var(--xo-primary-soft);
  color: var(--xo-primary);
  font-size: 11px;
  font-style: normal;
  font-weight: 800;
}

.profit-calendar-amount {
  margin-top: 12px;
  font-size: 17px;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.profit-calendar-cell small {
  display: block;
  margin-top: 8px;
  color: var(--xo-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.profit-calendar-cell.is-positive .profit-calendar-amount,
.is-positive-text {
  color: var(--xo-success);
}

.profit-calendar-cell.is-negative .profit-calendar-amount,
.is-negative-text {
  color: var(--xo-danger);
}

.profit-calendar-cell.is-muted .profit-calendar-amount,
.profit-calendar-cell.is-closed .profit-calendar-amount,
.is-muted-text {
  color: var(--xo-muted);
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
  background: var(--xo-card-elevated);
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
  .module-summary-grid,
  .daily-profit-summary {
    grid-template-columns: 1fr;
  }

  .profit-calendar-grid {
    gap: 8px;
  }

  .profit-calendar-cell {
    min-height: 92px;
    padding: 10px;
    border-radius: 14px;
  }

  .profit-calendar-amount {
    font-size: 14px;
  }

  .investment-trend-panel {
    grid-column: auto;
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

  .daily-profit-head {
    flex-direction: column;
  }

  .daily-profit-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .profit-month-picker {
    width: 100%;
  }

  .daily-profit-calendar {
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .profit-calendar-weekdays,
  .profit-calendar-grid {
    gap: 6px;
    min-width: 680px;
  }

  .profit-calendar-cell {
    min-height: 88px;
    padding: 10px;
    border-radius: 12px;
  }

  .profit-calendar-amount {
    font-size: 13px;
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
