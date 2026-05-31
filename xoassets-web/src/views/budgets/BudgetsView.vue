<!-- 预算管理页：接入真实预算接口，展示总预算、分类预算和使用进度。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">预算管理</h1>
        <p class="page-subtitle">跟踪分类预算使用情况，提前发现超支风险</p>
      </div>
      <div class="header-actions">
        <el-date-picker v-model="selectedMonth" type="month" value-format="YYYY-MM" :clearable="false" />
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增预算</el-button>
      </div>
    </div>

    <section class="grid-3">
      <MetricCard title="预算总额" :value="summary.totalBudget" :trend="0" description="本月计划" tone="primary" />
      <MetricCard title="已使用" :value="summary.totalUsed" :trend="0" description="本月支出" tone="warning" />
      <MetricCard title="剩余额度" :value="summary.totalRemaining" :trend="0" description="可用预算" tone="success" />
    </section>

    <section v-loading="loading" class="budget-grid">
      <el-empty v-if="!loading && budgets.length === 0" class="panel empty-panel" description="还没有预算，新增总预算或分类预算后即可跟踪进度" />
      <article v-for="budget in budgets" :key="budget.id" class="panel panel-padding budget-card">
        <div class="budget-head">
          <div>
            <h3>{{ budgetTitle(budget) }}</h3>
            <p><AmountText :value="budget.usedAmount" muted /> / <AmountText :value="budget.amount" muted /></p>
          </div>
          <StatusBadge :label="budget.usageStatusLabel" />
        </div>
        <el-progress :percentage="progressPercent(budget.usedAmount, budget.amount)" :status="budget.usageStatus === 'OVER' ? 'exception' : budget.usageStatus === 'WARNING' ? 'warning' : 'success'" />
        <div class="budget-actions">
          <el-button link type="primary" @click="openEditDialog(budget)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(budget)">删除</el-button>
        </div>
      </article>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingBudget ? '编辑预算' : '新增预算'" width="440px">
      <el-form label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="月份">
          <el-date-picker v-model="form.month" type="month" value-format="YYYY-MM" class="full-width" :clearable="false" />
        </el-form-item>
        <el-form-item label="预算类型">
          <el-segmented v-model="form.budgetType" :options="budgetTypeOptions" />
        </el-form-item>
        <el-form-item v-if="form.budgetType === 'CATEGORY'" label="支出分类">
          <el-select v-model="form.categoryId" class="full-width" placeholder="选择支出分类">
            <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算金额">
          <el-input-number v-model="form.amount" class="full-width" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// 预算页使用后端实时汇总结果，转账不计入预算，退款由后端抵扣支出。
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { budgetApi, type BudgetItem, type BudgetRequest, type BudgetSummary, type BudgetType } from '@/services/budgetApi';
import { categoryApi, type CategoryItem } from '@/services/categoryApi';
import { progressPercent } from '@/utils/format';

const selectedMonth = ref(currentMonth());
const budgets = ref<BudgetItem[]>([]);
const categories = ref<CategoryItem[]>([]);
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const editingBudget = ref<BudgetItem | null>(null);
const form = reactive<BudgetRequest>({ month: selectedMonth.value, budgetType: 'TOTAL', categoryId: null, amount: 0, status: 1 });
const emptySummary = computed<BudgetSummary>(() => ({ month: selectedMonth.value, totalBudget: 0, totalUsed: 0, totalRemaining: 0, usageRate: 0, usageStatus: 'NORMAL', usageStatusLabel: '正常', items: [] }));
const summary = ref<BudgetSummary>(emptySummary.value);
const budgetTypeOptions = [
  { label: '总预算', value: 'TOTAL' },
  { label: '分类预算', value: 'CATEGORY' }
];

onMounted(() => {
  loadCategories();
  loadBudgets();
});

watch(selectedMonth, () => {
  loadBudgets();
});

watch(() => form.budgetType, (type) => {
  // 总预算不带分类，避免误把上一次分类选择提交给后端。
  if (type === 'TOTAL') {
    form.categoryId = null;
  }
});

async function loadBudgets() {
  loading.value = true;
  try {
    const [list, summaryData] = await Promise.all([budgetApi.list(selectedMonth.value), budgetApi.summary(selectedMonth.value)]);
    budgets.value = list;
    summary.value = summaryData;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '预算加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadCategories() {
  try {
    categories.value = (await categoryApi.list('EXPENSE')).filter((category) => category.status === 1);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '分类加载失败');
  }
}

function openCreateDialog() {
  editingBudget.value = null;
  resetForm('TOTAL');
  dialogVisible.value = true;
}

function openEditDialog(budget: BudgetItem) {
  editingBudget.value = budget;
  form.month = budget.month;
  form.budgetType = budget.budgetType;
  form.categoryId = budget.categoryId || null;
  form.amount = Number(budget.amount);
  form.status = budget.status;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (form.amount <= 0) {
    ElMessage.warning('预算金额必须大于0');
    return;
  }
  if (form.budgetType === 'CATEGORY' && !form.categoryId) {
    ElMessage.warning('请选择支出分类');
    return;
  }
  submitting.value = true;
  try {
    if (editingBudget.value) {
      await budgetApi.update(editingBudget.value.id, form);
    } else {
      await budgetApi.create(form);
    }
    dialogVisible.value = false;
    ElMessage.success('预算已保存');
    selectedMonth.value = form.month;
    await loadBudgets();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '预算保存失败');
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(budget: BudgetItem) {
  try {
    await ElMessageBox.confirm(`确认删除「${budgetTitle(budget)}」吗？`, '删除预算', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' });
    await budgetApi.remove(budget.id);
    ElMessage.success('预算已删除');
    await loadBudgets();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '预算删除失败');
    }
  }
}

function resetForm(type: BudgetType) {
  form.month = selectedMonth.value;
  form.budgetType = type;
  form.categoryId = null;
  form.amount = 0;
  form.status = 1;
}

function budgetTitle(budget: BudgetItem) {
  return budget.budgetType === 'TOTAL' ? '本月总预算' : budget.categoryName || '分类预算';
}

function currentMonth() {
  const date = new Date();
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}`;
}
</script>

<style scoped>
/* 顶部操作保持账户页同类布局，便于切换月份后快速新增预算。 */
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* 预算卡片强调进度条，便于快速识别超支项目。 */
.budget-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.budget-card {
  min-height: 166px;
}

.budget-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.budget-head h3 {
  margin: 0 0 8px;
}

.budget-head p {
  margin: 0;
  color: var(--xo-muted);
}

.budget-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}

.empty-panel {
  grid-column: 1 / -1;
}

.full-width {
  width: 100%;
}

@media (max-width: 780px) {
  .budget-grid {
    grid-template-columns: 1fr;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
