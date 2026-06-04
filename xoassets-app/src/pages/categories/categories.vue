<template>
  <view class="cat-page safe-bottom">
    <view class="section-title">支出分类</view>
    <view class="cat-grid">
      <view v-for="c in expenseCategories" :key="c.id" class="cat-item">
        <view class="cat-icon-small" :style="{ background: c.color || themeStore.currentTheme.colors.primary }">
          <AppIcon :name="categoryIconName(c, 'EXPENSE')" size="40rpx" />
        </view>
        <text class="cat-name">{{ c.name }}</text>
      </view>
    </view>

    <view class="section-title">收入分类</view>
    <view class="cat-grid">
      <view v-for="c in incomeCategories" :key="c.id" class="cat-item">
        <view class="cat-icon-small" :style="{ background: c.color || themeStore.currentTheme.colors.positive }">
          <AppIcon :name="categoryIconName(c, 'INCOME')" size="40rpx" />
        </view>
        <text class="cat-name">{{ c.name }}</text>
      </view>
    </view>

    <view v-if="!expenseCategories.length && !incomeCategories.length" class="empty-state">
      <text>暂无分类</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppIcon from '@/components/app/AppIcon.vue'
import { categoryApi, type CategoryItem } from '@/services/categoryApi'
import { useThemeStore } from '@/stores/theme'

const expenseCategories = ref<CategoryItem[]>([])
const incomeCategories = ref<CategoryItem[]>([])
const themeStore = useThemeStore()

const categoryIconRules = [
  { key: 'dining', match: ['餐饮', '吃饭', '外卖'] },
  { key: 'coffee', match: ['咖啡', '奶茶'] },
  { key: 'shopping', match: ['购物', '日用'] },
  { key: 'bus', match: ['公交'] },
  { key: 'subway', match: ['地铁'] },
  { key: 'transit', match: ['交通', '打车', '出行'] },
  { key: 'entertainment', match: ['娱乐'] },
  { key: 'movie', match: ['电影'] },
  { key: 'game', match: ['游戏'] },
  { key: 'medical', match: ['医疗', '药', '医院'] },
  { key: 'education', match: ['教育', '学习', '培训'] },
  { key: 'book', match: ['书'] },
  { key: 'bills', match: ['生活', '缴费', '水电'] },
  { key: 'internet', match: ['网络', '宽带'] },
  { key: 'salary', match: ['工资', '薪资'] },
  { key: 'bonus', match: ['奖金', '奖励'] },
  { key: 'refund', match: ['退款'] }
]

function categoryIconName(category: CategoryItem, type: 'EXPENSE' | 'INCOME') {
  const rule = categoryIconRules.find((item) => item.match.some((keyword) => category.name.includes(keyword)))
  if (rule) return `category.${rule.key}`
  return type === 'INCOME' ? 'home.income' : 'home.expense'
}

onMounted(async () => {
  try {
    const [exp, inc] = await Promise.all([
      categoryApi.list('EXPENSE'),
      categoryApi.list('INCOME')
    ])
    expenseCategories.value = exp
    incomeCategories.value = inc
  } catch {}
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.cat-page { min-height: 100vh; background: var(--xo-page-bg); padding: $spacing-sm; }
.section-title { font-size: $font-lg; font-weight: 700; color: var(--xo-text-primary); padding: $spacing-md 0 $spacing-sm; }
.cat-grid { display: flex; flex-wrap: wrap; }
.cat-item { width: 25%; display: flex; flex-direction: column; align-items: center; padding: 20rpx 0; }
.cat-icon-small {
  width: 72rpx; height: 72rpx; border-radius: 20rpx;
  display: flex; align-items: center; justify-content: center;
  color: var(--xo-white); font-size: 28rpx; font-weight: 600; margin-bottom: 8rpx;
}
.cat-name { font-size: $font-xs; color: var(--xo-text-regular); }
</style>
