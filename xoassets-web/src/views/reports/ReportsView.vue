<!-- AI报告页：展示模板化报告列表、摘要和数据复盘。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">AI报告</h1>
        <p class="page-subtitle">基于真实数据生成财务复盘，不提供投资买卖建议</p>
      </div>
      <div class="header-actions">
        <el-select v-model="reportType" class="type-select">
          <el-option label="日报" value="DAILY" />
          <el-option label="周报" value="WEEKLY" />
          <el-option label="月报" value="MONTHLY" />
        </el-select>
        <el-button type="primary" :icon="DocumentAdd" :loading="generating" @click="handleGenerate">生成报告</el-button>
      </div>
    </div>

    <section v-loading="loading" class="reports-layout">
      <div class="panel report-list">
        <el-empty v-if="reports.length === 0" description="暂无报告，点击生成报告创建第一份复盘" />
        <button v-for="report in reports" v-else :key="report.id" :class="{ active: report.id === activeId }" @click="activeId = report.id">
          <strong>{{ report.title }}</strong>
          <span>{{ formatDateTime(report.createdAt) }}</span>
          <StatusBadge :label="report.statusLabel" />
        </button>
      </div>

      <article v-if="activeReport" class="panel panel-padding report-detail">
        <div class="report-head">
          <div>
            <h2>{{ activeReport.title }}</h2>
            <p>{{ formatDateTime(activeReport.createdAt) }}</p>
          </div>
          <StatusBadge :label="activeReport.statusLabel" />
        </div>
        <p class="summary">{{ activeReport.content }}</p>
        <div class="insight-grid">
          <div v-for="item in summaryItems" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.description }}</p>
          </div>
        </div>
      </article>
      <article v-else class="panel panel-padding report-detail">
        <el-empty description="选择或生成一份报告查看详情" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
// 报告页使用模板化报告接口；后续接真实 AI 时保持同一展示结构。
import { computed, onMounted, ref } from 'vue';
import { DocumentAdd } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { reportApi, type AiReportItem, type ReportType } from '@/services/reportApi';

const reports = ref<AiReportItem[]>([]);
const activeId = ref<string>('');
const reportType = ref<ReportType>('DAILY');
const loading = ref(false);
const generating = ref(false);

onMounted(() => {
  loadReports();
});

const activeReport = computed(() => reports.value.find((item) => item.id === activeId.value) || reports.value[0] || null);
const summaryItems = computed(() => parseSummary(activeReport.value?.summaryJson));

async function loadReports() {
  loading.value = true;
  try {
    reports.value = await reportApi.list();
    activeId.value = activeId.value || reports.value[0]?.id || '';
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '报告加载失败');
  } finally {
    loading.value = false;
  }
}

async function handleGenerate() {
  generating.value = true;
  try {
    const report = await reportApi.generatePreview({ reportType: reportType.value });
    ElMessage.success('报告已生成');
    await loadReports();
    activeId.value = report.id;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '报告生成失败');
  } finally {
    generating.value = false;
  }
}

function parseSummary(summaryJson?: string | null) {
  if (!summaryJson) {
    return [];
  }
  try {
    const summary = JSON.parse(summaryJson) as Record<string, number | string>;
    return [
      { label: '净资产', value: `${summary.netAssets ?? 0}`, description: '当前没有负债模型时，净资产按总资产展示。' },
      { label: '预算使用率', value: `${summary.budgetUsageRate ?? 0}%`, description: `预算状态：${summary.budgetStatus ?? '-'}` },
      { label: '投资浮动盈亏', value: `${summary.investmentFloatingProfit ?? 0}`, description: '仅做数据观察，不构成买入或卖出建议。' }
    ];
  } catch {
    return [];
  }
}

function formatDateTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
</script>

<style scoped>
/* 报告页采用左列表右详情，适合后续扩展报告历史。 */
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.type-select {
  width: 120px;
}

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
  white-space: pre-line;
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
