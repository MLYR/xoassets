<!-- 记账流水页：筛选区、表格、分页和新增流水弹窗。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">记账流水</h1>
        <p class="page-subtitle">查看和管理您的所有交易记录</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="dialogVisible = true">新增流水</el-button>
    </div>

    <section class="panel filter-panel">
      <el-input v-model="keyword" placeholder="搜索交易记录..." :prefix-icon="Search" clearable />
      <el-select v-model="typeFilter" placeholder="全部类型" clearable>
        <el-option label="收入" value="收入" />
        <el-option label="支出" value="支出" />
      </el-select>
      <el-select v-model="accountFilter" placeholder="全部账户" clearable>
        <el-option v-for="account in accountOptions" :key="account" :label="account" :value="account" />
      </el-select>
      <el-button :icon="Filter">更多筛选</el-button>
      <el-button :icon="Download">导出</el-button>
    </section>

    <section class="panel">
      <el-table :data="filteredTransactions" stripe>
        <el-table-column prop="date" label="日期时间" min-width="160" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }"><StatusBadge :label="row.type" /></template>
        </el-table-column>
        <el-table-column prop="category" label="分类" />
        <el-table-column prop="account" label="账户" min-width="150" />
        <el-table-column label="金额" align="right">
          <template #default="{ row }"><AmountText :value="row.amount" with-sign /></template>
        </el-table-column>
        <el-table-column prop="note" label="备注" />
        <el-table-column label="状态">
          <template #default="{ row }"><StatusBadge :label="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="120">
          <template #default>
            <el-button link :icon="Edit" />
            <el-button link type="danger" :icon="Delete" />
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span>共 {{ filteredTransactions.length }} 条记录</span>
        <el-pagination layout="prev, pager, next" :total="filteredTransactions.length" :page-size="8" />
      </div>
    </section>

    <TransactionDialog v-model="dialogVisible" @submit="handleSubmit" />
  </div>
</template>

<script setup lang="ts">
// 页面本地完成筛选交互，新增弹窗提交后仅提示成功。
import { computed, ref } from 'vue';
import { Delete, Download, Edit, Filter, Plus, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import AmountText from '@/components/finance/AmountText.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { financeService } from '@/services/financeService';
import TransactionDialog from './components/TransactionDialog.vue';

// 流水数据来自本地 mock。
const transactions = financeService.getTransactions();
// 新增流水弹窗显示状态。
const dialogVisible = ref(false);
// 筛选条件为页面本地状态。
const keyword = ref('');
const typeFilter = ref('');
const accountFilter = ref('');

// 从流水中提取账户筛选项。
const accountOptions = computed(() => [...new Set(transactions.map((item) => item.account))]);

// 根据关键词、类型和账户过滤流水列表。
const filteredTransactions = computed(() =>
  transactions.filter((item) => {
    const matchKeyword = !keyword.value || `${item.category}${item.account}${item.note}`.includes(keyword.value);
    const matchType = !typeFilter.value || item.type === typeFilter.value;
    const matchAccount = !accountFilter.value || item.account === accountFilter.value;
    return matchKeyword && matchType && matchAccount;
  })
);

// 当前 MVP 只保留提交入口，不直接改写 mock 数据。
function handleSubmit() {
  ElMessage.success('流水已保存到本地 mock 列表预留入口');
}
</script>

<style scoped>
/* 筛选区沿用原型横向工具栏，窄屏自动换行。 */
.filter-panel {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 160px 180px auto auto;
  gap: 12px;
  padding: 16px;
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

@media (max-width: 980px) {
  .filter-panel {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
