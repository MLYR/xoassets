<!-- AI报告页：展示报告列表、摘要和行动建议。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">AI报告</h1>
        <p class="page-subtitle">基于 mock 数据生成财务复盘和预算建议</p>
      </div>
      <el-button type="primary" :icon="DocumentAdd">生成报告</el-button>
    </div>

    <section class="reports-layout">
      <div class="panel report-list">
        <button v-for="report in reports" :key="report.id" :class="{ active: report.id === activeReport.id }" @click="activeId = report.id">
          <strong>{{ report.title }}</strong>
          <span>{{ report.createdAt }}</span>
          <StatusBadge :label="report.status" />
        </button>
      </div>

      <article class="panel panel-padding report-detail">
        <div class="report-head">
          <div>
            <h2>{{ activeReport.title }}</h2>
            <p>{{ activeReport.createdAt }}</p>
          </div>
          <StatusBadge :label="activeReport.status" />
        </div>
        <p class="summary">{{ activeReport.summary }}</p>
        <div class="insight-grid">
          <div>
            <span>消费提醒</span>
            <strong>购物预算接近上限</strong>
            <p>建议本周减少非必要购买，优先处理固定支出。</p>
          </div>
          <div>
            <span>资产观察</span>
            <strong>资产趋势稳定向上</strong>
            <p>现金流保持健康，可以继续维持当前储蓄节奏。</p>
          </div>
          <div>
            <span>下步行动</span>
            <strong>复核信用卡账单</strong>
            <p>信用账户需要重点关注还款日和单月额度使用率。</p>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
// 报告页通过 activeId 控制列表和详情联动。
import { computed, ref } from 'vue';
import { DocumentAdd } from '@element-plus/icons-vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { financeService } from '@/services/financeService';

// 报告列表来自 mock 服务。
const reports = financeService.getReports();
// 当前选中的报告 ID，默认选择第一份报告。
const activeId = ref(reports[0]?.id ?? 0);
// 根据 activeId 派生详情数据，异常时回退第一份报告。
const activeReport = computed(() => reports.find((item) => item.id === activeId.value) ?? reports[0]);
</script>

<style scoped>
/* 报告页采用左列表右详情，适合后续扩展报告历史。 */
.reports-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 24px;
}

.report-list {
  padding: 8px;
}

.report-list button {
  display: grid;
  width: 100%;
  gap: 8px;
  margin-bottom: 8px;
  padding: 14px;
  border: 0;
  border-radius: var(--xo-radius);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.report-list button.active,
.report-list button:hover {
  background: #eff6ff;
}

.report-list span,
.report-head p,
.summary,
.insight-grid p {
  color: var(--xo-muted);
}

.report-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.report-head h2 {
  margin: 0 0 6px;
}

.summary {
  margin: 0 0 24px;
  line-height: 1.8;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.insight-grid div {
  padding: 16px;
  border: 1px solid var(--xo-border);
  border-radius: var(--xo-radius);
}

.insight-grid span {
  color: var(--xo-primary);
  font-size: 13px;
  font-weight: 600;
}

.insight-grid strong {
  display: block;
  margin: 10px 0 6px;
}

@media (max-width: 980px) {
  .reports-layout,
  .insight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
