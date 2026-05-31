<!-- 状态标签组件：收入支出和业务状态都通过统一视觉输出。 -->
<template>
  <el-tag :type="tagType" effect="light" size="small" round>{{ label }}</el-tag>
</template>

<script setup lang="ts">
// Element Plus 标签类型由业务文案映射，避免页面重复判断。
import { computed } from 'vue';

const props = defineProps<{
  label: string;
}>();

// 将业务状态映射为 Element Plus 标签类型。
const tagType = computed(() => {
  if (['收入', '已完成', '正常', '达成'].includes(props.label)) {
    return 'success';
  }
  if (['支出', '已超支'].includes(props.label)) {
    return 'danger';
  }
  if (['进行中', '待确认', '关注', '预警'].includes(props.label)) {
    return 'warning';
  }
  return 'info';
});
</script>
