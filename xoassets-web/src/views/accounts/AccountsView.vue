<!-- 账户管理页：展示账户总览、状态和余额明细。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">账户管理</h1>
        <p class="page-subtitle">统一查看银行卡、钱包和投资账户余额</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增账户</el-button>
    </div>

    <section class="grid-3">
      <MetricCard title="账户总余额" :value="totalBalance" :trend="2.1" description="较上月" tone="success" />
      <MetricCard title="流动资金" :value="cashBalance" :trend="1.4" description="较上周" tone="primary" />
      <MetricCard title="信用负债" :value="creditDebt" :trend="-3.2" description="较上月" tone="danger" />
    </section>

    <section v-loading="loading" class="panel panel-padding">
      <el-empty v-if="!loading && accounts.length === 0" description="还没有账户，创建第一个账户后即可开始记账" />
      <div v-else class="account-grid">
        <article v-for="account in accounts" :key="account.id" class="account-card">
          <div class="account-top">
            <div>
              <h3>{{ account.name }}</h3>
              <p>{{ account.type }}</p>
            </div>
            <StatusBadge :label="formatStatus(account.status)" />
          </div>
          <AmountText class="account-amount" :value="account.balance" />
          <div class="account-foot">
            <span>{{ account.currency }} · {{ account.remark || '暂无备注' }}</span>
            <div class="account-actions">
              <el-button link type="primary" @click="openEditDialog(account)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(account)">删除</el-button>
            </div>
          </div>
        </article>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="420px">
      <el-form label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="账户名称">
          <el-input v-model.trim="form.name" placeholder="请输入账户名称" />
        </el-form-item>
        <el-form-item label="账户类型">
          <el-select v-model="form.type" class="full-width" placeholder="请选择账户类型">
            <el-option v-for="item in accountTypes" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item :label="editingAccount ? '初始余额' : '初始余额 / 当前余额'">
          <el-input-number v-model="form.initialBalance" class="full-width" :precision="2" :step="100" :disabled="Boolean(editingAccount)" />
        </el-form-item>
        <el-form-item v-if="editingAccount" label="当前余额">
          <el-input-number v-model="form.balance" class="full-width" :precision="2" :step="100" />
        </el-form-item>
        <el-form-item label="币种">
          <el-input v-model.trim="form.currency" placeholder="CNY" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" class="full-width" :step="1" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model.trim="form.remark" type="textarea" :rows="3" placeholder="请输入备注（可选）" />
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
// 账户页读取真实后端账户接口，并在页面内计算汇总指标。
import { computed, onMounted, reactive, ref } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { accountApi, type AccountItem, type AccountRequest } from '@/services/accountApi';

// 账户类型选项保持原型中的常见分类，提交时直接传给后端。
const accountTypes = ['储蓄卡', '信用卡', '电子钱包', '投资账户', '现金账户', '其他账户'];
// 账户列表和加载状态由接口驱动。
const accounts = ref<AccountItem[]>([]);
const loading = ref(false);
const submitting = ref(false);
// 弹窗状态：editingAccount 为空表示新增，否则表示编辑。
const dialogVisible = ref(false);
const editingAccount = ref<AccountItem | null>(null);
// 表单模型和后端 AccountRequest 保持一致。
const form = reactive<AccountRequest>({
  name: '',
  type: '',
  initialBalance: 0,
  balance: 0,
  currency: 'CNY',
  status: 1,
  sortOrder: 0,
  remark: ''
});
// 账户余额汇总，用于顶部资产指标。
const totalBalance = computed(() => accounts.value.reduce((sum, item) => sum + Number(item.balance), 0));
// 正余额账户视为现金类资产。
const cashBalance = computed(() => accounts.value.filter((item) => Number(item.balance) > 0).reduce((sum, item) => sum + Number(item.balance), 0));
// 负余额账户视为信用负债，展示时取绝对值。
const creditDebt = computed(() => Math.abs(accounts.value.filter((item) => Number(item.balance) < 0).reduce((sum, item) => sum + Number(item.balance), 0)));
// 弹窗标题跟随新增或编辑状态变化。
const dialogTitle = computed(() => (editingAccount.value ? '编辑账户' : '新增账户'));

// 页面进入时拉取真实账户列表。
onMounted(() => {
  loadAccounts();
});

// 加载账户列表，失败时展示后端或网络错误。
async function loadAccounts() {
  loading.value = true;
  try {
    accounts.value = await accountApi.list();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '账户列表加载失败');
  } finally {
    loading.value = false;
  }
}

// 状态数字转成页面原有状态文案，继续复用 StatusBadge。
function formatStatus(status: number) {
  return status === 1 ? '正常' : '关注';
}

// 重置弹窗表单，新增和编辑共用同一个表单模型。
function resetForm(account?: AccountItem) {
  form.name = account?.name ?? '';
  form.type = account?.type ?? '';
  form.initialBalance = Number(account?.initialBalance ?? 0);
  form.balance = Number(account?.balance ?? account?.initialBalance ?? 0);
  form.currency = account?.currency ?? 'CNY';
  form.status = account?.status ?? 1;
  form.sortOrder = account?.sortOrder ?? 0;
  form.remark = account?.remark ?? '';
}

// 打开新增账户弹窗。
function openCreateDialog() {
  editingAccount.value = null;
  resetForm();
  dialogVisible.value = true;
}

// 打开编辑账户弹窗，并带入当前账户基础信息。
function openEditDialog(account: AccountItem) {
  editingAccount.value = account;
  resetForm(account);
  dialogVisible.value = true;
}

// 提交前做最基础的必填校验，避免无意义请求。
function validateForm() {
  if (!form.name) {
    ElMessage.warning('请输入账户名称');
    return false;
  }
  if (!form.type) {
    ElMessage.warning('请选择账户类型');
    return false;
  }
  if (!form.currency) {
    ElMessage.warning('请输入币种');
    return false;
  }
  return true;
}

// 新增或编辑账户，成功后重新加载列表保证页面和后端一致。
async function handleSubmit() {
  if (!validateForm()) {
    return;
  }
  submitting.value = true;
  try {
    if (editingAccount.value) {
      await accountApi.update(editingAccount.value.id, form);
      ElMessage.success('账户已更新');
    } else {
      await accountApi.create(form);
      ElMessage.success('账户已新增');
    }
    dialogVisible.value = false;
    await loadAccounts();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '账户保存失败');
  } finally {
    submitting.value = false;
  }
}

// 删除账户前二次确认；后端拒绝删除时直接展示接口错误提示。
async function handleDelete(account: AccountItem) {
  try {
    await ElMessageBox.confirm(`确认删除账户「${account.name}」吗？`, '删除账户', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    });
    await accountApi.remove(account.id);
    ElMessage.success('账户已删除');
    await loadAccounts();
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    ElMessage.error(error instanceof Error ? error.message : '账户删除失败');
  }
}
</script>

<style scoped>
/* 账户卡片采用网格排列，便于快速扫描各账户状态。 */
.account-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.account-card {
  padding: 18px;
  border: 1px solid var(--xo-border);
  border-radius: var(--xo-radius);
  background: #fff;
}

.account-top,
.account-foot {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.account-top h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.account-top p,
.account-foot span {
  margin: 0;
  color: var(--xo-muted);
  font-size: 13px;
}

/* 账户操作按钮和底部信息保持在同一行，延续卡片紧凑布局。 */
.account-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 6px;
}

.account-amount {
  display: block;
  margin: 22px 0;
  font-size: 28px;
}

.full-width {
  width: 100%;
}

@media (max-width: 1080px) {
  .account-grid {
    grid-template-columns: 1fr;
  }
}
</style>
