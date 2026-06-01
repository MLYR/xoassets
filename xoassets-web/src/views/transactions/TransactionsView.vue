<!-- 记账流水页：从后端读取账户、分类和流水，支持新增、编辑、删除。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">记账流水</h1>
        <p class="page-subtitle">查看和管理您的所有交易记录</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增流水</el-button>
    </div>

    <section class="panel filter-panel">
      <el-input v-model="keyword" placeholder="搜索交易记录..." :prefix-icon="Search" clearable @change="reloadFromFirstPage" />
      <el-select v-model="typeFilter" placeholder="全部类型" clearable @change="reloadFromFirstPage">
        <el-option label="收入" value="INCOME" />
        <el-option label="支出" value="EXPENSE" />
        <el-option label="转账" value="TRANSFER" />
        <el-option label="退款" value="REFUND" />
      </el-select>
      <el-select v-model="accountFilter" placeholder="全部账户" clearable @change="reloadFromFirstPage">
        <el-option v-for="account in accounts" :key="account.id" :label="account.name" :value="account.id" />
      </el-select>
      <el-button :icon="Filter">更多筛选</el-button>
      <el-button :icon="Download" :loading="exporting" @click="handleExport">导出</el-button>
    </section>

    <section v-loading="loading" class="panel">
      <el-empty v-if="!loading && transactions.length === 0" description="还没有流水，点击新增流水记录第一笔收支" />
      <template v-else>
        <el-table :data="transactions" stripe>
          <el-table-column label="日期时间" min-width="160">
            <template #default="{ row }">{{ formatDateTime(row.transactionTime) }}</template>
          </el-table-column>
          <el-table-column label="类型" width="90">
            <template #default="{ row }"><StatusBadge :label="formatType(row.type)" /></template>
          </el-table-column>
          <el-table-column label="分类">
            <template #default="{ row }">{{ formatCategory(row) }}</template>
          </el-table-column>
          <el-table-column label="账户" min-width="170">
            <template #default="{ row }">{{ formatAccount(row) }}</template>
          </el-table-column>
          <el-table-column label="金额" align="right">
            <template #default="{ row }"><AmountText :value="formatAmountValue(row)" with-sign /></template>
          </el-table-column>
          <el-table-column label="备注">
            <template #default="{ row }">{{ row.note || '-' }}</template>
          </el-table-column>
          <el-table-column label="图片" width="90" align="center">
            <template #default="{ row }">
              <el-image v-if="row.imageUrl" class="transaction-image" :src="row.imageUrl" :preview-src-list="[row.imageUrl]" preview-teleported fit="cover" />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态">
            <template #default="{ row }"><StatusBadge :label="row.status === 1 ? '已完成' : '待确认'" /></template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="120">
            <template #default="{ row }">
              <el-button link :icon="Edit" @click="openEditDialog(row)" />
              <el-button link type="danger" :icon="Delete" @click="handleDelete(row)" />
            </template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <span>共 {{ total }} 条记录</span>
          <el-pagination
            v-model:current-page="pageNo"
            v-model:page-size="pageSize"
            layout="total, sizes, prev, pager, next"
            :page-sizes="[8, 15, 30, 50]"
            :total="total"
            @size-change="reloadFromFirstPage"
            @current-change="loadTransactions"
          />
        </div>
      </template>
    </section>

    <TransactionDialog
      v-model="dialogVisible"
      :accounts="accounts"
      :categories="categories"
      :transaction="editingTransaction"
      :loading="optionsLoading"
      :submitting="submitting"
      @submit="handleSubmit"
    />
  </div>
</template>

<script setup lang="ts">
// 记账页所有新增、编辑和删除都调用后端，账户余额联动由后端事务保证。
import { onMounted, ref } from 'vue';
import { Delete, Download, Edit, Filter, Plus, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import AmountText from '@/components/finance/AmountText.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { accountApi, type AccountItem } from '@/services/accountApi';
import { categoryApi, type CategoryItem } from '@/services/categoryApi';
import { transactionApi, type TransactionApiType, type TransactionItem, type TransactionRequest } from '@/services/transactionApi';
import { exportApi } from '@/services/exportApi';
import TransactionDialog from './components/TransactionDialog.vue';

// 远程数据状态。
const transactions = ref<TransactionItem[]>([]);
const accounts = ref<AccountItem[]>([]);
const categories = ref<CategoryItem[]>([]);
const total = ref(0);
// 页面加载、选项加载和提交状态分开控制，避免互相阻塞。
const loading = ref(false);
const optionsLoading = ref(false);
const submitting = ref(false);
const exporting = ref(false);
// 弹窗状态：editingTransaction 为空表示新增，否则表示编辑。
const dialogVisible = ref(false);
const editingTransaction = ref<TransactionItem | null>(null);
// 筛选条件直接映射后端查询参数。
const keyword = ref('');
const typeFilter = ref<TransactionApiType | ''>('');
const accountFilter = ref<string | ''>('');
const pageNo = ref(1);
const pageSize = ref(8);

// 页面进入时并行加载账户、分类和流水。
onMounted(() => {
  loadOptions();
  loadTransactions();
});

// 加载账户和分类，新增流水弹窗必须使用这些后端数据。
async function loadOptions() {
  optionsLoading.value = true;
  try {
    const [accountList, categoryList] = await Promise.all([accountApi.list(), categoryApi.list()]);
    accounts.value = accountList;
    categories.value = categoryList;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '账户或分类加载失败');
  } finally {
    optionsLoading.value = false;
  }
}

// 按当前筛选条件加载流水分页。
async function loadTransactions() {
  loading.value = true;
  try {
    const result = await transactionApi.page({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      type: typeFilter.value || undefined,
      accountId: accountFilter.value || undefined
    });
    transactions.value = result.records;
    total.value = result.total;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '流水列表加载失败');
  } finally {
    loading.value = false;
  }
}

// 筛选条件变化时回到第一页，避免新条件下页码越界。
function reloadFromFirstPage() {
  pageNo.value = 1;
  loadTransactions();
}

// 打开新增弹窗前刷新账户和分类，保证表单选项来自后端最新数据。
async function openCreateDialog() {
  await loadOptions();
  if (accounts.value.length === 0) {
    ElMessage.info('请先创建账户后再记录流水');
    return;
  }
  editingTransaction.value = null;
  dialogVisible.value = true;
}

// 打开编辑弹窗前刷新选项，避免分类或账户被其他操作改动后仍使用旧数据。
async function openEditDialog(transaction: TransactionItem) {
  await loadOptions();
  editingTransaction.value = transaction;
  dialogVisible.value = true;
}

// 新增或编辑流水后刷新流水和账户，保证余额展示同步。
async function handleSubmit(payload: TransactionRequest) {
  submitting.value = true;
  try {
    if (editingTransaction.value) {
      await transactionApi.update(editingTransaction.value.id, payload);
      ElMessage.success('流水已更新');
    } else {
      await transactionApi.create(payload);
      ElMessage.success('流水已新增');
    }
    dialogVisible.value = false;
    await Promise.all([loadTransactions(), loadOptions()]);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '流水保存失败');
  } finally {
    submitting.value = false;
  }
}

// 删除流水前二次确认；删除后刷新账户余额和流水列表。
async function handleDelete(transaction: TransactionItem) {
  try {
    await ElMessageBox.confirm('确认删除这笔流水吗？删除后账户余额会同步恢复。', '删除流水', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    });
    await transactionApi.remove(transaction.id);
    ElMessage.success('流水已删除');
    await Promise.all([loadTransactions(), loadOptions()]);
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    ElMessage.error(error instanceof Error ? error.message : '流水删除失败');
  }
}

// 导出当前筛选条件下的普通流水，不包含投资交易。
async function handleExport() {
  exporting.value = true;
  try {
    await exportApi.transactions({
      keyword: keyword.value || undefined,
      type: typeFilter.value || undefined,
      accountId: accountFilter.value || undefined
    });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '流水导出失败');
  } finally {
    exporting.value = false;
  }
}

// 后端类型映射为页面中文标签。
function formatType(type: TransactionApiType) {
  const map: Record<TransactionApiType, string> = {
    INCOME: '收入',
    EXPENSE: '支出',
    TRANSFER: '转账',
    REFUND: '退款'
  };
  return map[type];
}

// 转账没有分类，展示固定文案。
function formatCategory(row: TransactionItem) {
  return row.type === 'TRANSFER' ? '账户转账' : row.categoryName || '-';
}

// 转账展示转出和转入账户，收入支出展示主账户。
function formatAccount(row: TransactionItem) {
  if (row.type === 'TRANSFER') {
    return `${row.accountName || '-'} → ${row.targetAccountName || '-'}`;
  }
  return row.accountName || '-';
}

// 支出和转账在列表中按流出展示负数，收入和退款展示正数。
function formatAmountValue(row: TransactionItem) {
  const amount = Number(row.amount);
  return row.type === 'EXPENSE' || row.type === 'TRANSFER' ? -amount : amount;
}

// 后端返回 ISO 日期字符串，页面展示为本地易读格式。
function formatDateTime(value: string) {
  return value.replace('T', ' ').slice(0, 16);
}
</script>

<style scoped>
/* 筛选区沿用原型横向工具栏，白色玻璃底让表格入口更轻。 */
.filter-panel {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 160px 180px auto auto;
  gap: 12px;
  padding: 16px;
  align-items: center;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-top: 1px solid var(--xo-border);
  color: var(--xo-muted);
  font-size: 14px;
  background: rgba(248, 251, 255, 0.72);
}

.transaction-image {
  width: 42px;
  height: 42px;
  border-radius: 10px;
}

@media (max-width: 980px) {
  .filter-panel {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
