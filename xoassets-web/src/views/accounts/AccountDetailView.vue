<!-- 账户详情页：聚合展示普通流水、转账和投资交易形成的资金变化。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ account?.name || '账户详情' }}</h1>
        <p class="page-subtitle">{{ accountSubtitle }}</p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push('/accounts')">返回账户</el-button>
        <el-button type="primary" :icon="Download" :loading="exporting" @click="handleExportLedger">导出账户明细</el-button>
      </div>
    </div>

    <section class="account-info panel panel-padding">
      <div>
        <span class="info-label">账户类型</span>
        <strong>{{ account?.type || '-' }}</strong>
      </div>
      <div>
        <span class="info-label">币种</span>
        <strong>{{ account?.currency || '-' }}</strong>
      </div>
      <div>
        <span class="info-label">状态</span>
        <StatusBadge :label="account?.status === 1 ? '正常' : '停用'" />
      </div>
      <div>
        <span class="info-label">备注</span>
        <strong>{{ account?.remark || '暂无备注' }}</strong>
      </div>
    </section>

    <section class="grid-4">
      <MetricCard title="当前余额" :value="summary.currentBalance" :trend="0" description="账户实时余额" />
      <MetricCard title="累计流入" :value="summary.totalInflow" :trend="summary.totalInflow" description="正向资金合计" tone="success" />
      <MetricCard title="累计流出" :value="summary.totalOutflow" :trend="-summary.totalOutflow" description="负向资金合计" tone="danger" />
      <MetricCard title="净流入" :value="summary.netInflow" :trend="summary.netInflow" description="流入减流出" :tone="summary.netInflow >= 0 ? 'success' : 'danger'" />
    </section>

    <section class="grid-4">
      <MetricCard title="本期流入" :value="periodInflow" :trend="periodInflow" description="收入/转入/卖出" tone="success" />
      <MetricCard title="本期流出" :value="periodOutflow" :trend="-periodOutflow" description="支出/转出/买入" tone="danger" />
      <MetricCard title="本期净流入" :value="flowStats.netFlowAmount" :trend="flowStats.netFlowAmount" description="本期资金净变化" :tone="flowStats.netFlowAmount >= 0 ? 'success' : 'danger'" />
      <MetricCard title="投资净流入" :value="investmentNetFlow" :trend="investmentNetFlow" description="卖出减买入" :tone="investmentNetFlow >= 0 ? 'success' : 'danger'" />
    </section>

    <section class="chart-grid">
      <div class="panel chart-panel">
        <div class="section-head"><h2>资金流入流出趋势</h2></div>
        <el-empty v-if="flowStats.dailyFlowTrend.length === 0" description="暂无趋势数据" />
        <BaseChart v-else :option="trendOption" height="260px" />
      </div>
      <div class="panel chart-panel">
        <div class="section-head"><h2>支出分类占比</h2></div>
        <el-empty v-if="flowStats.categoryExpenseStats.length === 0" description="暂无支出分类数据" />
        <BaseChart v-else :option="categoryOption" height="260px" />
      </div>
      <div class="panel chart-panel">
        <div class="section-head"><h2>投资资金流向</h2></div>
        <el-empty v-if="flowStats.investmentFlowStats.length === 0" description="暂无投资流向数据" />
        <BaseChart v-else :option="investmentOption" height="260px" />
      </div>
    </section>

    <section class="panel filter-panel">
      <el-date-picker v-model="dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="reloadFromFirstPage" />
      <el-select v-model="typeFilter" clearable placeholder="全部类型" @change="reloadFromFirstPage">
        <el-option v-for="item in ledgerTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索标题、备注、分类或资产" clearable @change="reloadFromFirstPage" />
    </section>

    <section v-loading="loading" class="panel">
      <el-empty v-if="!loading && records.length === 0" description="暂无账户资金明细" />
      <template v-else>
        <el-table :data="records" stripe height="560">
          <el-table-column label="时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.transactionTime) }}</template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }"><StatusBadge :label="bizTypeLabel(row.bizType)" /></template>
          </el-table-column>
          <el-table-column label="标题" min-width="180" prop="title" />
          <el-table-column label="金额" min-width="140" align="right" header-align="right">
            <template #default="{ row }"><AmountText :class="['numeric-cell', row.amount >= 0 ? 'success-text' : 'danger-text']" :value="row.amount" with-sign :precision="4" /></template>
          </el-table-column>
          <el-table-column label="分类 / 资产" min-width="160">
            <template #default="{ row }">{{ row.categoryName || row.assetName || row.symbol || '-' }}</template>
          </el-table-column>
          <el-table-column label="对方账户" min-width="150">
            <template #default="{ row }">{{ row.relatedAccountName || '-' }}</template>
          </el-table-column>
          <el-table-column label="来源" width="110">
            <template #default="{ row }">{{ row.sourceType === 'INVESTMENT' ? '投资交易' : '普通流水' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><StatusBadge :label="row.status === 'REVOKED' ? '已撤销' : '正常'" /></template>
          </el-table-column>
          <el-table-column label="备注" min-width="180">
            <template #default="{ row }">{{ row.note || '-' }}</template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <span>共 {{ total }} 条明细</span>
          <el-pagination
            v-model:current-page="pageNo"
            v-model:page-size="pageSize"
            layout="total, sizes, prev, pager, next"
            :page-sizes="[10, 20, 50]"
            :total="total"
            @size-change="reloadFromFirstPage"
            @current-change="loadLedger"
          />
        </div>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
// 账户详情的图表和表格均来自后端聚合接口，避免前端重复拼普通流水和投资交易。
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { Download } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import type { EChartsOption } from 'echarts';
import AmountText from '@/components/finance/AmountText.vue';
import BaseChart from '@/components/charts/BaseChart.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { accountApi, type AccountFlowStatistics, type AccountItem, type AccountLedgerBizType, type AccountLedgerItem, type AccountLedgerSummary } from '@/services/accountApi';
import { exportApi } from '@/services/exportApi';

const route = useRoute();
const accountId = computed(() => String(route.params.id || ''));
const loading = ref(false);
const exporting = ref(false);
const account = ref<AccountItem | null>(null);
const summary = ref<AccountLedgerSummary>({ currentBalance: 0, initialBalance: 0, totalInflow: 0, totalOutflow: 0, netInflow: 0, transactionCount: 0 });
const flowStats = ref<AccountFlowStatistics>({ incomeAmount: 0, expenseAmount: 0, transferInAmount: 0, transferOutAmount: 0, investmentBuyAmount: 0, investmentSellAmount: 0, netFlowAmount: 0, categoryExpenseStats: [], investmentFlowStats: [], dailyFlowTrend: [] });
const records = ref<AccountLedgerItem[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(20);
const typeFilter = ref<AccountLedgerBizType | ''>('');
const keyword = ref('');
const dateRange = ref<[string, string] | null>(null);
const ledgerTypeOptions = [
  { label: '收入', value: 'INCOME' },
  { label: '支出', value: 'EXPENSE' },
  { label: '转账转出', value: 'TRANSFER_OUT' },
  { label: '转账转入', value: 'TRANSFER_IN' },
  { label: '退款', value: 'REFUND' },
  { label: '投资买入', value: 'INVEST_BUY' },
  { label: '投资卖出', value: 'INVEST_SELL' }
] as const;

onMounted(() => {
  loadAll();
});

const accountSubtitle = computed(() => `${account.value?.currency || '-'} · ${account.value?.remark || '查看账户资金变化'}`);
const periodInflow = computed(() => flowStats.value.incomeAmount + flowStats.value.transferInAmount + flowStats.value.investmentSellAmount);
const periodOutflow = computed(() => flowStats.value.expenseAmount + flowStats.value.transferOutAmount + flowStats.value.investmentBuyAmount);
const investmentNetFlow = computed(() => flowStats.value.investmentSellAmount - flowStats.value.investmentBuyAmount);
const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  grid: { top: 24, left: 40, right: 20, bottom: 44 },
  xAxis: { type: 'category', data: flowStats.value.dailyFlowTrend.map((item) => item.date) },
  yAxis: { type: 'value' },
  series: [
    { name: '流入', type: 'line', smooth: true, data: flowStats.value.dailyFlowTrend.map((item) => item.inflow) },
    { name: '流出', type: 'line', smooth: true, data: flowStats.value.dailyFlowTrend.map((item) => item.outflow) },
    { name: '净流入', type: 'bar', data: flowStats.value.dailyFlowTrend.map((item) => item.netFlow) }
  ]
}));
const categoryOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['42%', '70%'], data: flowStats.value.categoryExpenseStats.map((item) => ({ name: item.name, value: item.amount })) }]
}));
const investmentOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { top: 20, left: 80, right: 20, bottom: 30 },
  xAxis: { type: 'value' },
  yAxis: { type: 'category', data: flowStats.value.investmentFlowStats.map((item) => item.name) },
  series: [{ type: 'bar', data: flowStats.value.investmentFlowStats.map((item) => item.amount) }]
}));

async function loadAll() {
  await Promise.all([loadLedger(), loadFlowStatistics()]);
}

async function loadLedger() {
  loading.value = true;
  try {
    const result = await accountApi.ledger(accountId.value, queryParams());
    account.value = result.account;
    summary.value = result.summary;
    records.value = result.page.records;
    total.value = result.page.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '账户资金明细加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadFlowStatistics() {
  try {
    flowStats.value = await accountApi.flowStatistics(accountId.value, {
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资金流向统计加载失败');
  }
}

function reloadFromFirstPage() {
  pageNo.value = 1;
  loadAll();
}

async function handleExportLedger() {
  exporting.value = true;
  try {
    await exportApi.accountLedger({ accountId: accountId.value, ...queryParams() });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '账户明细导出失败');
  } finally {
    exporting.value = false;
  }
}

function queryParams() {
  return {
    pageNo: pageNo.value,
    pageSize: pageSize.value,
    type: typeFilter.value || undefined,
    keyword: keyword.value || undefined,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1]
  };
}

function bizTypeLabel(type: AccountLedgerBizType) {
  return ledgerTypeOptions.find((item) => item.value === type)?.label || type;
}

function formatDateTime(value: string) {
  return value.replace('T', ' ').slice(0, 16);
}
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.account-info {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.info-label {
  display: block;
  margin-bottom: 6px;
  color: var(--xo-muted);
  font-size: 13px;
}

.grid-4 {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.chart-panel {
  min-height: 330px;
}

.section-head {
  padding: 18px 18px 0;
}

.section-head h2 {
  margin: 0;
  font-size: 18px;
}

.filter-panel {
  display: grid;
  grid-template-columns: 320px 180px minmax(220px, 1fr);
  gap: 12px;
  padding: 16px;
}

.numeric-cell {
  display: inline-block;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.success-text {
  color: var(--xo-success);
}

.danger-text {
  color: var(--xo-danger);
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-top: 1px solid var(--xo-border);
  color: var(--xo-muted);
  font-size: 14px;
}

@media (max-width: 1080px) {
  .account-info,
  .grid-4,
  .chart-grid,
  .filter-panel {
    grid-template-columns: 1fr;
  }
}
</style>
