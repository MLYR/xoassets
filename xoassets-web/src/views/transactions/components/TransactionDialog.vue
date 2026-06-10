<!-- 流水弹窗：账户和分类全部来自后端，支持收入、支出和转账。 -->
<template>
  <el-dialog v-model="visible" class="xo-form-dialog transaction-form-dialog" width="640px" top="12px" @open="resetForm">
    <template #header>
      <div class="xo-dialog-header-content">
        <span class="xo-dialog-kicker">普通流水</span>
        <h2>{{ dialogTitle }}</h2>
        <p>{{ dialogSubtitle }}</p>
      </div>
    </template>
    <el-form :model="form" class="xo-dialog-form" label-position="top" @submit.prevent>
      <section class="xo-dialog-section">
        <div class="xo-dialog-section-title">
          <strong>交易信息</strong>
          <span>{{ currentTypeHint }}</span>
        </div>
        <div class="transaction-main-grid">
          <el-form-item label="类型">
            <el-segmented v-model="form.type" :options="typeOptions" class="type-segmented" />
          </el-form-item>
          <el-form-item label="金额">
            <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="10" class="full-input amount-input" />
          </el-form-item>
        </div>
        <template v-if="form.type === 'TRANSFER'">
          <div class="transaction-account-grid">
            <el-form-item label="转出账户">
              <el-select v-model="form.accountId" placeholder="请选择转出账户" class="full-input" :loading="loading">
                <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="转入账户">
              <el-select v-model="form.targetAccountId" placeholder="请选择转入账户" class="full-input" :loading="loading">
                <el-option v-for="item in targetAccountOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </div>
        </template>

        <template v-else>
          <div class="transaction-account-grid">
            <el-form-item label="账户">
              <el-select v-model="form.accountId" placeholder="请选择账户" class="full-input" :loading="loading">
                <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" placeholder="请选择分类" class="full-input" :loading="loading">
                <el-option v-for="item in availableCategories" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
              <el-alert v-if="!loading && availableCategories.length === 0" class="form-tip" type="info" :closable="false" show-icon>
                <template #title>暂无可用分类，新用户注册后会自动初始化默认分类</template>
              </el-alert>
            </el-form-item>
          </div>
        </template>
      </section>

      <section class="xo-dialog-section">
        <div class="xo-dialog-section-title">
          <strong>补充信息</strong>
          <span>时间、备注和凭证图片</span>
        </div>
        <div class="transaction-extra-grid">
          <el-form-item label="日期时间">
            <el-date-picker v-model="form.transactionTime" type="datetime" class="full-input" />
          </el-form-item>
          <el-form-item label="图片">
            <div class="image-upload">
              <label v-if="!form.imageUrl" class="upload-trigger">
                <input type="file" accept="image/*" @change="handleImageChange" />
                <span>选择图片</span>
                <small>仅 1 张，10MB 内</small>
              </label>
              <el-button v-if="form.imageUrl" link type="danger" @click="removeImage">移除图片</el-button>
            </div>
            <img v-if="form.imageUrl" class="preview-image" :src="form.imageUrl" alt="流水图片预览" />
          </el-form-item>
          <el-form-item class="full-row" label="备注">
            <el-input v-model.trim="form.note" type="textarea" :rows="2" placeholder="记录这笔流水的说明" />
          </el-form-item>
        </div>
      </section>
    </el-form>
    <template #footer>
      <div class="xo-dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
// 弹窗只负责表单交互和基础校验，真实保存由父页面调用接口。
import { computed, reactive, watch } from 'vue';
import { ElMessage } from 'element-plus';
import type { AccountItem } from '@/services/accountApi';
import type { CategoryItem } from '@/services/categoryApi';
import type { TransactionApiType, TransactionItem, TransactionRequest } from '@/services/transactionApi';

const props = defineProps<{
  modelValue: boolean;
  accounts: AccountItem[];
  categories: CategoryItem[];
  transaction: TransactionItem | null;
  loading: boolean;
  submitting: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  submit: [payload: TransactionRequest];
}>();

// 图片以 Data URL 存入流水字段；单张最大 10MB，避免用户一次上传多张或超大凭证。
const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;

// 将父组件的 v-model 映射为弹窗内部可读写状态。
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});

// 后端流水类型和中文标签保持一处映射。
const typeOptions = [
  { label: '支出', value: 'EXPENSE' },
  { label: '收入', value: 'INCOME' },
  { label: '转账', value: 'TRANSFER' },
  { label: '退款', value: 'REFUND' }
];

// 表单模型直接对应后端 TransactionRequest 字段。
const form = reactive({
  amount: 0,
  type: 'EXPENSE' as TransactionApiType,
  accountId: undefined as string | undefined,
  targetAccountId: undefined as string | undefined,
  categoryId: undefined as string | undefined,
  transactionTime: new Date(),
  note: '',
  imageUrl: null as string | null
});

// 弹窗标题区分新增和编辑，减少用户误操作。
const dialogTitle = computed(() => (props.transaction ? '编辑记账' : '新增记账'));
const dialogSubtitle = computed(() => (props.transaction ? '调整已有流水，保存后会由后端同步修正账户余额。' : '记录收入、支出、退款或转账，保存后会立即刷新账户余额和首页统计。'));
const currentTypeLabel = computed(() => typeOptions.find((item) => item.value === form.type)?.label || '流水');
const currentTypeHint = computed(() => {
  if (form.type === 'TRANSFER') {
    return '转账只影响账户余额，不进入普通收支统计';
  }
  if (form.type === 'REFUND') {
    return '退款用于抵扣支出，避免虚增收入';
  }
  return `${currentTypeLabel.value}会进入普通收支统计`;
});
// 收入和退款展示收入分类，支出展示支出分类；转账不需要分类。
const availableCategories = computed(() => {
  const categoryType = form.type === 'EXPENSE' ? 'EXPENSE' : 'INCOME';
  return props.categories.filter((item) => item.type === categoryType && item.status === 1);
});
// 转入账户排除当前转出账户，防止同账户转账。
const targetAccountOptions = computed(() => props.accounts.filter((item) => item.id !== form.accountId));

// 切换类型时清理不适用字段，避免提交旧分类或旧转入账户。
watch(
  () => form.type,
  () => {
    form.categoryId = undefined;
    form.targetAccountId = undefined;
  }
);

// 打开弹窗或切换编辑对象时重置表单。
function resetForm() {
  const transaction = props.transaction;
  form.amount = Number(transaction?.amount ?? 0);
  form.type = transaction?.type ?? 'EXPENSE';
  form.accountId = transaction?.accountId;
  form.targetAccountId = transaction?.targetAccountId ?? undefined;
  form.categoryId = transaction?.categoryId ?? undefined;
  form.transactionTime = transaction?.transactionTime ? new Date(transaction.transactionTime) : new Date();
  form.note = transaction?.note ?? '';
  form.imageUrl = transaction?.imageUrl ?? null;
}

// 金额和必选项在前端先拦截，后端仍保留权威校验。
function validateForm() {
  if (!form.amount || form.amount <= 0) {
    ElMessage.warning('请输入大于 0 的金额');
    return false;
  }
  if (!form.accountId) {
    ElMessage.warning(form.type === 'TRANSFER' ? '请选择转出账户' : '请选择账户');
    return false;
  }
  if (form.type === 'TRANSFER') {
    if (!form.targetAccountId) {
      ElMessage.warning('请选择转入账户');
      return false;
    }
    if (form.accountId === form.targetAccountId) {
      ElMessage.warning('转入账户不能和转出账户相同');
      return false;
    }
    return true;
  }
  if (!form.categoryId) {
    ElMessage.warning('请选择分类');
    return false;
  }
  return true;
}

// 转成后端 LocalDateTime 可接收的无时区字符串。
function formatDateTime(date: Date) {
  const pad = (value: number) => `${value}`.padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

// 提交表单给父页面，成功关闭由父页面控制。
function submitForm() {
  if (!validateForm()) {
    return;
  }
  emit('submit', {
    type: form.type,
    amount: form.amount,
    accountId: form.accountId!,
    targetAccountId: form.type === 'TRANSFER' ? form.targetAccountId : null,
    categoryId: form.type === 'TRANSFER' ? null : form.categoryId,
    transactionTime: formatDateTime(form.transactionTime),
    note: form.note,
    imageUrl: form.imageUrl
  });
}

// 移除后才重新显示选择入口，保证一笔流水只能保留一张凭证图片。
function removeImage() {
  form.imageUrl = null;
}

// 第一版把单张图片转成 Data URL 随流水保存，后续可替换为对象存储上传后的 URL。
function handleImageChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件');
    input.value = '';
    return;
  }
  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    ElMessage.warning('图片不能超过 10MB');
    input.value = '';
    return;
  }
  const reader = new FileReader();
  reader.onload = () => {
    form.imageUrl = typeof reader.result === 'string' ? reader.result : null;
    input.value = '';
  };
  reader.readAsDataURL(file);
}
</script>

<style scoped>
/* 表单控件保持通栏宽度，弹窗内部按统一分区卡片组织复杂字段。 */
.full-input {
  width: 100%;
}

.transaction-main-grid,
.transaction-account-grid,
.transaction-extra-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.full-row {
  grid-column: 1 / -1;
}

.type-segmented {
  width: 100%;
}

.amount-input :deep(.el-input__inner) {
  font-size: 18px;
  font-weight: 800;
}

.form-tip {
  margin-top: 8px;
}

.image-upload {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.upload-trigger {
  display: grid;
  flex: 1;
  gap: 4px;
  min-height: 56px;
  place-items: center;
  border: 1px dashed rgba(37, 99, 235, 0.32);
  border-radius: var(--xo-radius-inner);
  background: var(--xo-input-muted);
  color: var(--xo-primary);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.upload-trigger:hover {
  border-color: var(--xo-primary);
  background: var(--xo-primary-softer);
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.08);
}

.upload-trigger input {
  display: none;
}

.upload-trigger span {
  font-weight: 800;
}

.upload-trigger small {
  color: var(--xo-muted);
}

.preview-image {
  display: block;
  width: 168px;
  max-width: 100%;
  max-height: 96px;
  margin-top: 10px;
  border: 1px solid var(--xo-border);
  border-radius: var(--xo-radius-inner);
  box-shadow: var(--xo-shadow);
  object-fit: cover;
}

@media (max-width: 720px) {
  .transaction-main-grid,
  .transaction-account-grid,
  .transaction-extra-grid {
    grid-template-columns: 1fr;
  }
}
</style>
