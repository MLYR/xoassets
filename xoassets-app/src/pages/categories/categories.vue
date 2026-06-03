<template>
  <view class="cat-page safe-bottom">
    <view class="section-title">支出分类</view>
    <view class="cat-grid">
      <view v-for="c in expenseCategories" :key="c.id" class="cat-item">
        <view class="cat-icon-small" :style="{ background: c.color || themeStore.currentTheme.colors.primary }">
          <text>{{ c.icon || expenseFallbackIcon }}</text>
        </view>
        <text class="cat-name">{{ c.name }}</text>
      </view>
    </view>

    <view class="section-title">收入分类</view>
    <view class="cat-grid">
      <view v-for="c in incomeCategories" :key="c.id" class="cat-item">
        <view class="cat-icon-small" :style="{ background: c.color || themeStore.currentTheme.colors.positive }">
          <text>{{ c.icon || incomeFallbackIcon }}</text>
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
import { categoryApi, type CategoryItem } from '@/services/categoryApi'
import { useThemeStore } from '@/stores/theme'
import { getCategoryFallbackIcon, getThemeIconText } from '@/theme/helpers'

const expenseCategories = ref<CategoryItem[]>([])
const incomeCategories = ref<CategoryItem[]>([])
const themeStore = useThemeStore()
const expenseFallbackIcon = getThemeIconText(getCategoryFallbackIcon(themeStore.currentThemeName, 'EXPENSE'), '支')
const incomeFallbackIcon = getThemeIconText(getCategoryFallbackIcon(themeStore.currentThemeName, 'INCOME'), '收')

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
