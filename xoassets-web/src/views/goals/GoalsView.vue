<!-- 资产目标页：接入真实目标接口，展示目标进度、截止日期和完成状态。 -->
<template>
  <div class="page">
    <div class="page-actions">
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增目标</el-button>
    </div>

    <section class="grid-3">
      <MetricCard title="目标总额" :value="summary.totalTargetAmount" :trend="0" description="规划中" tone="primary" />
      <MetricCard title="已完成金额" :value="summary.totalCurrentAmount" :trend="summary.overallCompletionRate" description="完成率" tone="success" />
      <MetricCard title="待完成金额" :value="summary.totalRemainingAmount" :trend="0" description="待增长" tone="warning" />
    </section>

    <section v-loading="loading" class="goal-grid">
      <el-empty v-if="!loading && goals.length === 0" class="panel empty-panel" description="还没有资产目标，新增目标后即可追踪进度" />
      <article v-for="goal in goals" :key="goal.id" class="panel panel-padding goal-card">
        <div class="goal-head">
          <div>
            <h3>{{ goal.name }}</h3>
            <p>截止 {{ goal.targetDate || '未设置' }} · 剩余 {{ goal.daysLeft }} 天</p>
          </div>
          <StatusBadge :label="goal.statusLabel" />
        </div>
        <div class="goal-amounts">
          <AmountText :value="goal.currentAmount" />
          <span>/</span>
          <AmountText :value="goal.targetAmount" muted />
        </div>
        <el-progress :percentage="progressPercent(goal.currentAmount, goal.targetAmount)" />
        <div class="goal-extra">
          <span>剩余 <AmountText :value="goal.remainingAmount" muted /></span>
          <span>月需 <AmountText :value="goal.monthlyRequiredAmount" muted /></span>
        </div>
        <div class="goal-actions">
          <el-button link type="primary" @click="openEditDialog(goal)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(goal)">删除</el-button>
        </div>
      </article>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingGoal ? '编辑目标' : '新增目标'" width="440px">
      <el-form label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="目标名称"><el-input v-model.trim="form.name" placeholder="例如：年底净资产目标" /></el-form-item>
        <el-form-item label="目标金额"><el-input-number v-model="form.targetAmount" class="full-width" :min="0.01" :precision="2" /></el-form-item>
        <el-form-item label="当前金额">
          <el-input-number v-model="form.currentAmount" class="full-width" :min="0" :precision="2" :disabled="form.useCurrentNetAssets" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.useCurrentNetAssets">使用当前净资产作为当前金额</el-checkbox>
        </el-form-item>
        <el-form-item label="目标日期"><el-date-picker v-model="form.targetDate" value-format="YYYY-MM-DD" class="full-width" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" class="full-width">
            <el-option label="进行中" value="ACTIVE" />
            <el-option label="已完成" value="DONE" />
          </el-select>
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
// 目标页使用真实接口，当前金额可以手动填，也可以由后端按净资产口径计算。
import { onMounted, reactive, ref } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { goalApi, type GoalItem, type GoalRequest, type GoalSummary } from '@/services/goalApi';
import { progressPercent } from '@/utils/format';

const goals = ref<GoalItem[]>([]);
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const editingGoal = ref<GoalItem | null>(null);
const summary = ref<GoalSummary>({ totalTargetAmount: 0, totalCurrentAmount: 0, totalRemainingAmount: 0, overallCompletionRate: 0, activeGoalCount: 0, completedGoalCount: 0 });
const form = reactive<GoalRequest>({ name: '', targetAmount: 0, currentAmount: 0, targetDate: null, status: 'ACTIVE', useCurrentNetAssets: false });

onMounted(() => {
  loadGoals();
});

async function loadGoals() {
  loading.value = true;
  try {
    const [items, summaryData] = await Promise.all([goalApi.list(), goalApi.summary()]);
    goals.value = items;
    summary.value = summaryData;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '目标加载失败');
  } finally {
    loading.value = false;
  }
}

function openCreateDialog() {
  editingGoal.value = null;
  resetForm();
  dialogVisible.value = true;
}

function openEditDialog(goal: GoalItem) {
  editingGoal.value = goal;
  form.name = goal.name;
  form.targetAmount = Number(goal.targetAmount);
  form.currentAmount = Number(goal.currentAmount);
  form.targetDate = goal.targetDate || null;
  form.status = goal.status;
  form.useCurrentNetAssets = false;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!form.name || form.targetAmount <= 0) {
    ElMessage.warning('请输入目标名称和有效目标金额');
    return;
  }
  submitting.value = true;
  try {
    if (editingGoal.value) {
      await goalApi.update(editingGoal.value.id, form);
    } else {
      await goalApi.create(form);
    }
    dialogVisible.value = false;
    ElMessage.success('目标已保存');
    await loadGoals();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '目标保存失败');
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(goal: GoalItem) {
  try {
    await ElMessageBox.confirm(`确认删除目标「${goal.name}」吗？`, '删除目标', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' });
    await goalApi.remove(goal.id);
    ElMessage.success('目标已删除');
    await loadGoals();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '目标删除失败');
    }
  }
}

function resetForm() {
  form.name = '';
  form.targetAmount = 0;
  form.currentAmount = 0;
  form.targetDate = null;
  form.status = 'ACTIVE';
  form.useCurrentNetAssets = false;
}
</script>

<style scoped>
/* 目标卡片延续金融 SaaS 卡片基线，强调金额、进度和长期状态。 */
.goal-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.goal-card {
  position: relative;
  overflow: hidden;
  transition: box-shadow 0.2s ease, transform 0.2s ease, border-color 0.2s ease;
}

.goal-card::after {
  position: absolute;
  right: -38px;
  bottom: -48px;
  width: 126px;
  height: 126px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  content: "";
}

.goal-card:hover {
  border-color: rgba(37, 99, 235, 0.24);
  box-shadow: var(--xo-shadow-hover);
  transform: translateY(-2px);
}

.goal-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.goal-head h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 800;
}

.goal-head p {
  margin: 0;
  color: var(--xo-muted);
}

.goal-amounts {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 18px;
  font-size: 24px;
}

.goal-amounts span {
  color: var(--xo-muted);
}

.goal-extra,
.goal-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  color: var(--xo-muted);
  font-size: 13px;
}

.goal-extra {
  padding: 12px;
  border-radius: var(--xo-radius-inner);
  background: #f8fbff;
}

.goal-actions {
  justify-content: flex-end;
}

.empty-panel {
  grid-column: 1 / -1;
}

.full-width {
  width: 100%;
}

@media (max-width: 1080px) {
  .goal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
