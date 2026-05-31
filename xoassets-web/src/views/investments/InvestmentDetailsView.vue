<!-- 投资明细页：按资产类型分页查看具体持仓。 -->
<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">投资明细</h1>
        <p class="page-subtitle">按基金、股票、虚拟货币查看每一项持仓</p>
      </div>
      <el-button @click="$router.push(ROUTES.investments)">返回投资主页</el-button>
    </div>

    <section class="panel filter-panel">
      <el-segmented v-model="activeType" :options="typeOptions" @change="resetPage" />
      <el-input v-model="keyword" placeholder="搜索名称或代码" clearable @change="resetPage" />
    </section>

    <section v-loading="loading" class="panel">
      <el-empty v-if="!loading && pagedHoldings.length === 0" description="暂无符合条件的投资明细" />
      <template v-else>
        <el-table :data="pagedHoldings" stripe>
          <el-table-column label="持仓" min-width="180">
            <template #default="{ row }">
              <strong>{{ row.assetName || '-' }}</strong>
              <small class="muted-line">{{ row.symbol || '-' }} · {{ row.currency || '-' }}</small>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }"><StatusBadge :label="typeLabel(row.assetType)" /></template>
          </el-table-column>
          <el-table-column label="数量" align="right">
            <template #default="{ row }">{{ formatQuantity(row.quantity) }}</template>
          </el-table-column>
          <el-table-column label="当前价" align="right">
            <template #default="{ row }"><AmountText :value="row.latestPrice || 0" /></template>
          </el-table-column>
          <el-table-column label="市值" align="right">
            <template #default="{ row }"><AmountText :value="row.marketValue" /></template>
          </el-table-column>
          <el-table-column label="总成本" align="right">
            <template #default="{ row }"><AmountText :value="row.totalCost" /></template>
          </el-table-column>
          <el-table-column label="浮动盈亏" align="right">
            <template #default="{ row }"><AmountText :value="row.floatingProfit" with-sign /></template>
          </el-table-column>
          <el-table-column label="收益率">
            <template #default="{ row }"><TrendValue :value="row.floatingProfitRate" /></template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <span>共 {{ filteredHoldings.length }} 条明细</span>
          <el-pagination
            v-model:current-page="pageNo"
            v-model:page-size="pageSize"
            layout="total, sizes, prev, pager, next"
            :page-sizes="[10, 20, 50]"
            :total="filteredHoldings.length"
          />
        </div>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
// 第一版明细页复用持仓列表接口，在前端完成类型筛选和分页。
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AmountText from '@/components/finance/AmountText.vue';
import StatusBadge from '@/components/finance/StatusBadge.vue';
import TrendValue from '@/components/finance/TrendValue.vue';
import { ROUTES } from '@/constants/routes';
import { investmentApi, type AssetType, type HoldingItem } from '@/services/investmentApi';

const holdings = ref<HoldingItem[]>([]);
const loading = ref(false);
const activeType = ref<AssetType | 'ALL'>('ALL');
const keyword = ref('');
const pageNo = ref(1);
const pageSize = ref(10);
const typeOptions = [
  { label: '全部', value: 'ALL' },
  { label: '基金', value: 'FUND' },
  { label: '股票', value: 'STOCK' },
  { label: '虚拟货币', value: 'CRYPTO' }
];

onMounted(() => {
  loadHoldings();
});

const filteredHoldings = computed(() => holdings.value.filter((item) => {
  const matchedType = activeType.value === 'ALL' || item.assetType === activeType.value;
  const keywordText = keyword.value.trim().toLowerCase();
  const matchedKeyword = !keywordText || `${item.assetName || ''} ${item.symbol || ''}`.toLowerCase().includes(keywordText);
  return matchedType && matchedKeyword;
}));
const pagedHoldings = computed(() => {
  const start = (pageNo.value - 1) * pageSize.value;
  return filteredHoldings.value.slice(start, start + pageSize.value);
});

async function loadHoldings() {
  loading.value = true;
  try {
    holdings.value = await investmentApi.listHoldings();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '投资明细加载失败');
  } finally {
    loading.value = false;
  }
}

function resetPage() {
  pageNo.value = 1;
}

function typeLabel(type?: string | null) {
  return ({ FUND: '基金', STOCK: '股票', CRYPTO: '虚拟货币', OTHER: '其他' } as Record<string, string>)[type || ''] || '-';
}

function formatQuantity(value: number) {
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 10 });
}
</script>

<style scoped>
/* 明细页提供更宽的表格空间，避免主页承载过多筛选。 */
.filter-panel {
  display: grid;
  grid-template-columns: auto minmax(220px, 320px);
  gap: 12px;
  padding: 16px;
}

.muted-line {
  display: block;
  margin-top: 4px;
  color: var(--xo-muted);
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
}
</style>
