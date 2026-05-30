<!-- 预算管理页：展示分类预算、使用进度和状态。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">预算管理</h1>
        <p class="page-subtitle">跟踪分类预算使用情况，提前发现超支风险</p>
      </div>
      <el-button type="primary" :icon="Plus">新增预算</el-button>
    </div>

    <section class="grid-3">
      <MetricCard title="预算总额" :value="totalLimit" :trend="0" description="本月计划" tone="primary" />
      <MetricCard title="已使用" :value="totalUsed" :trend="-2.4" description="较上月" tone="warning" />
      <MetricCard title="剩余额度" :value="totalLimit - totalUsed" :trend="6.2" description="较上月" tone="success" />
    </section>

    <section class="budget-grid">
      <article v-for="budget in budgets" :key="budget.id" class="panel panel-padding budget-card">
        <div class="budget-head">
          <div>
            <h3>{{ budget.category }}</h3>
            <p><AmountText :value="budget.used" muted /> / <AmountText :value="budget.limit" muted /></p>
          </div>
          <StatusBadge :label="budget.status" />
        </div>
        <el-progress :percentage="progressPercent(budget.used, budget.limit)" :status="budget.status === '已超支' ? 'exception' : undefined" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
// 预算页按分类展示进度，金额展示统一使用 AmountText。
import { computed } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { financeService } from '@/services/financeService';
import { progressPercent } from '@/utils/format';

// 预算数据目前使用本地 mock。
const budgets = financeService.getBudgets();
// 预算总额用于顶部指标卡。
const totalLimit = computed(() => budgets.reduce((sum, item) => sum + item.limit, 0));
// 已使用预算用于展示整体消耗。
const totalUsed = computed(() => budgets.reduce((sum, item) => sum + item.used, 0));
</script>

<style scoped>
/* 预算卡片强调进度条，便于快速识别超支项目。 */
.budget-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
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

@media (max-width: 780px) {
  .budget-grid {
    grid-template-columns: 1fr;
  }
}
</style>
