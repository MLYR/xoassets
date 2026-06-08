<!-- 账户详情页：聚合展示普通流水、转账和投资交易形成的资金变化。 -->
<template>
  <div class="page">
    <div class="page-actions">
      <el-button @click="$router.push('/accounts')">返回账户</el-button>
      <el-button :icon="Edit" @click="openBalanceAdjustment">余额修正</el-button>
      <el-button type="primary" :icon="Download" :loading="exporting" @click="handleExportLedger">导出账户明细</el-button>
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
      <MetricCard title="余额修正" :value="flowStats.adjustmentAmount" :trend="flowStats.adjustmentAmount" description="不计入普通收支" :tone="flowStats.adjustmentAmount >= 0 ? 'success' : 'danger'" />
    </section>

    <section class="chart-grid">
      <div class="panel chart-panel">
        <div class="section-head"><h2>账户余额曲线</h2></div>
        <el-empty v-if="flowStats.dailyBalanceTrend.length === 0" description="暂无余额曲线数据" />
        <BaseChart v-else :option="trendOption" height="260px" />
      </div>
      <div class="panel chart-panel">
        <div class="section-head"><h2>支出分类占比</h2></div>
        <el-empty v-if="flowStats.categoryExpenseStats.length === 0" description="暂无支出分类数据" />
        <BaseChart v-else :option="categoryOption" height="260px" />
      </div>
    </section>

    <section class="panel filter-panel">
      <el-date-picker class="ledger-date-range" v-model="dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="reloadFromFirstPage" />
      <el-select class="ledger-type-select" v-model="typeFilter" clearable placeholder="全部类型" @change="reloadFromFirstPage">
        <el-option v-for="item in ledgerTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input class="ledger-keyword-input" v-model="keyword" placeholder="搜索标题、备注、分类或资产" clearable @change="reloadFromFirstPage" />
    </section>

    <section v-loading="loading" class="panel">
      <el-empty v-if="!loading && records.length === 0" description="暂无账户资金明细" />
      <template v-else>
        <el-table :data="records" stripe height="560">
          <el-table-column label="时间" min-width="170">
            <template #default="{ row }"><span class="nowrap-cell">{{ formatDateTime(row.transactionTime) }}</span></template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }"><StatusBadge :label="bizTypeLabel(row.bizType)" /></template>
          </el-table-column>
          <el-table-column label="标题" min-width="180">
            <template #default="{ row }"><span class="nowrap-cell">{{ row.title }}</span></template>
          </el-table-column>
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
            <template #default="{ row }">{{ sourceTypeLabel(row.sourceType) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><StatusBadge :label="row.status === 'REVOKED' ? '已撤销' : '正常'" /></template>
          </el-table-column>
          <el-table-column label="备注" min-width="180">
            <template #default="{ row }">{{ row.note || '-' }}</template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <el-pagination
            v-model:current-page="pageNo"
            v-model:page-size="pageSize"
            layout="total, sizes, prev, pager, next"
            :page-sizes="[10, 50, 100, 300]"
            :total="total"
            @size-change="reloadFromFirstPage"
            @current-change="loadLedger"
          />
        </div>
      </template>
    </section>

    <el-dialog v-model="adjustmentDialogVisible" class="xo-form-dialog account-adjustment-dialog" width="520px" top="24px">
      <template #header>
        <div class="xo-dialog-header-content">
          <span class="xo-dialog-kicker">账户对账</span>
          <h2>余额修正</h2>
          <p>记录非普通收支导致的余额校准，保留可追溯的业务发生时间。</p>
        </div>
      </template>
      <el-form class="xo-dialog-form" label-position="top" @submit.prevent="handleBalanceAdjustment">
        <section class="xo-dialog-section">
          <div class="xo-dialog-section-title">
            <strong>修正信息</strong>
            <span>不计入普通收入或支出</span>
          </div>
          <el-form-item label="修正后余额">
            <el-input-number v-model="adjustmentForm.afterBalance" class="full-width" :precision="2" :step="100" />
          </el-form-item>
          <el-form-item label="业务时间">
            <el-date-picker v-model="adjustmentForm.bizTime" class="full-width" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm" />
          </el-form-item>
          <el-form-item label="修正原因">
            <el-input v-model.trim="adjustmentForm.reason" type="textarea" :rows="2" placeholder="例如：补记利息、对账调整" />
          </el-form-item>
          <p class="dialog-tip">余额修正会生成专用调整记录，不计入普通收支，但会进入账户账本和余额曲线。</p>
        </section>
      </el-form>
      <template #footer>
        <div class="xo-dialog-footer">
          <el-button @click="adjustmentDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="adjustingBalance" @click="handleBalanceAdjustment">确认修正</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// 账户详情的图表和表格均来自后端聚合接口，避免前端重复拼普通流水和投资交易。
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { Download, Edit } from '@element-plus/icons-vue';
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
const adjustingBalance = ref(false);
const adjustmentDialogVisible = ref(false);
const account = ref<AccountItem | null>(null);
const summary = ref<AccountLedgerSummary>({ currentBalance: 0, initialBalance: 0, totalInflow: 0, totalOutflow: 0, netInflow: 0, transactionCount: 0 });
const flowStats = ref<AccountFlowStatistics>({ adjustmentAmount: 0, categoryExpenseStats: [], dailyBalanceTrend: [] });
const records = ref<AccountLedgerItem[]>([]);
const total = ref(0);
const pageNo = ref(1);
const pageSize = ref(10);
const typeFilter = ref<AccountLedgerBizType | ''>('');
const keyword = ref('');
const dateRange = ref<[string, string] | null>(null);
const adjustmentForm = ref({ afterBalance: 0, reason: '', bizTime: formatDateTimeInput(new Date()) });
const ledgerTypeOptions = [
  { label: '收入', value: 'INCOME' },
  { label: '支出', value: 'EXPENSE' },
  { label: '转账转出', value: 'TRANSFER_OUT' },
  { label: '转账转入', value: 'TRANSFER_IN' },
  { label: '退款', value: 'REFUND' },
  { label: '投资买入', value: 'INVEST_BUY' },
  { label: '投资卖出', value: 'INVEST_SELL' },
  { label: '余额修正', value: 'BALANCE_ADJUSTMENT' }
] as const;

onMounted(() => {
  loadAll();
});

const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0 },
  grid: { top: 24, left: 40, right: 20, bottom: 44 },
  xAxis: { type: 'category', data: flowStats.value.dailyBalanceTrend.map((item) => item.date) },
  yAxis: { type: 'value' },
  series: [
    { name: '日终余额', type: 'line', smooth: true, data: flowStats.value.dailyBalanceTrend.map((item) => item.endBalance) },
    { name: '余额修正', type: 'bar', data: flowStats.value.dailyBalanceTrend.map((item) => item.adjustmentAmount) }
  ]
}));
const categoryOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['42%', '70%'], data: flowStats.value.categoryExpenseStats.map((item) => ({ name: item.name, value: item.amount })) }]
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
    ElMessage.error(error instanceof Error ? error.message : '账户详情统计加载失败');
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

function openBalanceAdjustment() {
  adjustmentForm.value = {
    afterBalance: Number(summary.value.currentBalance || account.value?.balance || 0),
    reason: '',
    bizTime: formatDateTimeInput(new Date())
  };
  adjustmentDialogVisible.value = true;
}

async function handleBalanceAdjustment() {
  if (!Number.isFinite(Number(adjustmentForm.value.afterBalance))) {
    ElMessage.warning('请输入有效余额');
    return;
  }
  adjustingBalance.value = true;
  try {
    // 余额修正走专用接口，保证账本和余额曲线都能追溯调整原因。
    await accountApi.adjustBalance(accountId.value, {
      afterBalance: adjustmentForm.value.afterBalance,
      reason: adjustmentForm.value.reason || undefined,
      bizTime: adjustmentForm.value.bizTime || undefined
    });
    ElMessage.success('余额已修正');
    adjustmentDialogVisible.value = false;
    await loadAll();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '余额修正失败');
  } finally {
    adjustingBalance.value = false;
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

function sourceTypeLabel(type: AccountLedgerItem['sourceType']) {
  if (type === 'INVESTMENT') {
    return '投资交易';
  }
  if (type === 'ADJUSTMENT') {
    return '余额修正';
  }
  return '普通流水';
}

function formatDateTime(value: string) {
  return value.replace('T', ' ').slice(0, 16);
}

function formatDate(date: Date) {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

function formatDateTimeInput(date: Date) {
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  return `${formatDate(date)} ${hours}:${minutes}:${seconds}`;
}
</script>

<style scoped>
.account-info {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.account-info > div {
  padding: 14px;
  border-radius: var(--xo-radius-inner);
  background: #f8fbff;
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-panel {
  min-height: 330px;
  padding-bottom: 10px;
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
  /* 日期够用、类型保持窄列，搜索框吃掉剩余空间，符合账户明细高频检索场景。 */
  grid-template-columns: minmax(360px, 380px) minmax(130px, 150px) minmax(420px, 1fr);
  gap: 12px;
  padding: 16px;
  align-items: center;
}

:deep(.ledger-date-range),
:deep(.ledger-type-select),
:deep(.ledger-keyword-input) {
  width: 100% !important;
  min-width: 0;
}

@media (max-width: 1270px) {
  .filter-panel {
    grid-template-columns: 1fr;
  }
}

.dialog-tip {
  margin: 0;
  color: var(--xo-muted);
  font-size: 13px;
  line-height: 1.7;
}

.full-width {
  width: 100%;
}

.numeric-cell {
  display: inline-block;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.nowrap-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  justify-content: flex-end;
  padding: 14px 16px;
  border-top: 1px solid var(--xo-border);
  color: var(--xo-muted);
  font-size: 14px;
  background: rgba(248, 251, 255, 0.72);
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
