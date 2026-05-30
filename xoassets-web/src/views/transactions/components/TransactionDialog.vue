<!-- 新增流水弹窗：使用 Element Plus 表单，提交后将数据回传给父页面。 -->
<template>
  <el-dialog v-model="visible" title="新增流水" width="460px">
    <el-form :model="form" label-position="top" @submit.prevent>
      <el-form-item label="金额">
        <el-input-number v-model="form.amount" :min="0" :precision="2" :step="10" class="full-input" />
      </el-form-item>
      <el-form-item label="类型">
        <el-segmented v-model="form.type" :options="['支出', '收入']" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="form.category" placeholder="请选择分类" class="full-input">
          <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="账户">
        <el-select v-model="form.account" placeholder="请选择账户" class="full-input">
          <el-option v-for="item in accounts" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker v-model="form.date" type="date" class="full-input" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.note" type="textarea" :rows="3" placeholder="记录这笔流水的说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
// 弹窗内部只管理临时表单状态，真正列表数据由父组件决定。
import { computed, reactive } from 'vue';
import type { TransactionType } from '@/types/finance';

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  submit: [payload: { amount: number; type: TransactionType; category: string; account: string; date: Date; note: string }];
}>();

// 将父组件的 v-model 映射为弹窗内部可读写状态。
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});

// 第一版先内置常用分类和账户，后续接入接口替换。
const categories = ['餐饮', '购物', '交通', '娱乐', '工资', '理财收益'];
const accounts = ['招商银行信用卡', '工商银行储蓄卡', '支付宝', '微信'];
// 弹窗临时表单数据。
const form = reactive({
  amount: 0,
  type: '支出' as TransactionType,
  category: '',
  account: '',
  date: new Date(),
  note: ''
});

// 提交表单并关闭弹窗，父组件负责后续保存动作。
function submitForm() {
  emit('submit', { ...form });
  visible.value = false;
}
</script>

<style scoped>
/* 表单控件保持通栏宽度，避免弹窗内容左右错位。 */
.full-input {
  width: 100%;
}
</style>
