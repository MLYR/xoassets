<!-- 账户管理页：展示账户总览、状态和余额明细。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">账户管理</h1>
        <p class="page-subtitle">统一查看银行卡、钱包和投资账户余额</p>
      </div>
      <el-button type="primary" :icon="Plus">新增账户</el-button>
    </div>

    <section class="grid-3">
      <MetricCard title="账户总余额" :value="totalBalance" :trend="2.1" description="较上月" tone="success" />
      <MetricCard title="流动资金" :value="cashBalance" :trend="1.4" description="较上周" tone="primary" />
      <MetricCard title="信用负债" :value="creditDebt" :trend="-3.2" description="较上月" tone="danger" />
    </section>

    <section class="panel panel-padding account-grid">
      <article v-for="account in accounts" :key="account.id" class="account-card">
        <div class="account-top">
          <div>
            <h3>{{ account.name }}</h3>
            <p>{{ account.type }}</p>
          </div>
          <StatusBadge :label="account.status" />
        </div>
        <AmountText class="account-amount" :value="account.balance" />
        <div class="account-foot">
          <span>更新于 {{ account.updatedAt }}</span>
          <el-button link type="primary">查看明细</el-button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
// 账户页通过 mock 服务读取账户列表，并在页面内计算汇总指标。
import { computed } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import AmountText from '@/components/finance/AmountText.vue';
import MetricCard from '@/components/finance/MetricCard.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import { financeService } from '@/services/financeService';

// 当前页面直接读取 mock 账户数据。
const accounts = financeService.getAccounts();
// 账户余额汇总，用于顶部资产指标。
const totalBalance = computed(() => accounts.reduce((sum, item) => sum + item.balance, 0));
// 正余额账户视为现金类资产。
const cashBalance = computed(() => accounts.filter((item) => item.balance > 0).reduce((sum, item) => sum + item.balance, 0));
// 负余额账户视为信用负债，展示时取绝对值。
const creditDebt = computed(() => Math.abs(accounts.filter((item) => item.balance < 0).reduce((sum, item) => sum + item.balance, 0)));
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

.account-amount {
  display: block;
  margin: 22px 0;
  font-size: 28px;
}

@media (max-width: 1080px) {
  .account-grid {
    grid-template-columns: 1fr;
  }
}
</style>
