<!-- 预算分析：展示所选月份预算汇总和分类使用情况。 -->
<template>
  <div class="analysis-grid">
    <section class="panel panel-padding budget-summary wide">
      <div>
        <span class="budget-month">{{ selectedMonth }}</span>
        <h3>预算概览</h3>
      </div>
      <div class="budget-summary-grid">
        <div class="budget-stat">
          <span>预算总额</span>
          <AmountText :value="budgetSummary.totalBudget" />
        </div>
        <div class="budget-stat">
          <span>已用金额</span>
          <AmountText :value="budgetSummary.totalUsed" />
        </div>
        <div class="budget-stat">
          <span>剩余金额</span>
          <AmountText :value="budgetSummary.totalRemaining" with-sign />
        </div>
        <div class="budget-stat">
          <span>使用率</span>
          <strong>{{ usageRateText }}</strong>
        </div>
      </div>
      <el-progress :percentage="safePercent(budgetSummary.usageRate)" :status="progressStatus" />
    </section>

    <section class="panel panel-padding wide">
      <h3>预算分类列表</h3>
      <el-empty v-if="!loading && budgetSummary.items.length === 0" description="暂无预算分类数据" />
      <div v-else class="budget-list">
        <div v-for="item in budgetSummary.items" :key="item.id" class="budget-row">
          <div>
            <strong>{{ item.categoryName || (item.budgetType === 'TOTAL' ? '总预算' : '未分类') }}</strong>
            <span>{{ item.usageStatusLabel }}</span>
          </div>
          <el-progress :percentage="safePercent(item.usageRate)" :status="item.usageStatus === 'OVER' ? 'exception' : item.usageStatus === 'WARNING' ? 'warning' : undefined" :show-text="false" />
          <div class="budget-row-values">
            <AmountText :value="item.usedAmount" />
            <span>/</span>
            <AmountText :value="item.amount" muted />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
// 预算 Tab 只展示 selectedMonth 对应的 budgetProgress 结果。
import { computed } from 'vue';
import AmountText from '@/components/finance/AmountText.vue';
import type { BudgetSummary } from '@/services/budgetApi';

const props = defineProps<{
  loading: boolean;
  selectedMonth: string;
  budgetSummary: BudgetSummary;
}>();

const usageRateText = computed(() => `${safePercent(props.budgetSummary.usageRate).toFixed(1)}%`);
const progressStatus = computed(() => (props.budgetSummary.usageStatus === 'OVER' ? 'exception' : props.budgetSummary.usageStatus === 'WARNING' ? 'warning' : undefined));

// 预算使用率可能为空或超过 100，进度条展示层需要限制范围。
function safePercent(value: number | null | undefined) {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return 0;
  }
  return Math.max(0, Math.min(100, Number(value)));
}
</script>

<style scoped>
/* 预算概览使用四列摘要，和 KPI 区形成上下层级。 */
.budget-summary {
  display: grid;
  gap: 18px;
}

.budget-month {
  color: var(--xo-primary);
  font-size: 13px;
  font-weight: 800;
}

.budget-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.budget-stat {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: var(--xo-radius-inner);
  background: var(--xo-input-muted);
}

.budget-stat span {
  color: var(--xo-muted);
  font-size: 13px;
}

.budget-stat strong {
  color: var(--xo-text);
  font-size: 20px;
  font-variant-numeric: tabular-nums;
}

.budget-list {
  display: grid;
  gap: 14px;
}

.budget-row {
  display: grid;
  grid-template-columns: minmax(110px, 200px) minmax(120px, 1fr) minmax(170px, auto);
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-radius: var(--xo-radius-inner);
  background: var(--xo-input-muted);
}

.budget-row strong {
  display: block;
  margin-bottom: 4px;
  color: var(--xo-text);
}

.budget-row span {
  color: var(--xo-muted);
  font-size: 13px;
}

.budget-row-values {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 860px) {
  .budget-summary-grid,
  .budget-row {
    grid-template-columns: 1fr;
  }

  .budget-row-values {
    justify-content: flex-start;
  }
}
</style>
