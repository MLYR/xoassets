<!-- 资产目标页：展示目标进度、截止日期和完成状态。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">资产目标</h1>
        <p class="page-subtitle">规划储蓄、投资和专项资金目标</p>
      </div>
      <el-button type="primary" :icon="Plus">新增目标</el-button>
    </div>

    <section class="grid-3">
      <MetricCard title="目标总额" :value="totalTarget" :trend="0" description="规划中" tone="primary" />
      <MetricCard title="已完成金额" :value="totalCurrent" :trend="9.6" description="较上月" tone="success" />
      <MetricCard title="待完成金额" :value="totalTarget - totalCurrent" :trend="-3.8" description="较上月" tone="warning" />
    </section>

    <section class="goal-grid">
      <article v-for="goal in goals" :key="goal.id" class="panel panel-padding goal-card">
        <div class="goal-head">
          <div>
            <h3>{{ goal.name }}</h3>
            <p>截止 {{ goal.deadline }}</p>
          </div>
          <StatusBadge :label="goal.status" />
        </div>
        <div class="goal-amounts">
          <AmountText :value="goal.current" />
          <span>/</span>
          <AmountText :value="goal.target" muted />
        </div>
        <el-progress :percentage="progressPercent(goal.current, goal.target)" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
// 目标页根据 mock 目标列表计算总目标和完成进度。
import { computed } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { financeService } from '@/services/financeService';
import { progressPercent } from '@/utils/format';

// 目标数据目前使用本地 mock。
const goals = financeService.getGoals();
// 所有目标金额合计。
const totalTarget = computed(() => goals.reduce((sum, item) => sum + item.target, 0));
// 所有目标已完成金额合计。
const totalCurrent = computed(() => goals.reduce((sum, item) => sum + item.current, 0));
</script>

<style scoped>
/* 目标卡片保留明确金额层级和进度条，适合后续加入编辑操作。 */
.goal-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.goal-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.goal-head h3 {
  margin: 0 0 8px;
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
  font-size: 22px;
}

.goal-amounts span {
  color: var(--xo-muted);
}

@media (max-width: 1080px) {
  .goal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
