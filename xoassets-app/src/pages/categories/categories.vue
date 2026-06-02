<template>
  <view class="cat-page safe-bottom">
    <view class="section-title">支出分类</view>
    <view class="cat-grid">
      <view v-for="c in expenseCategories" :key="c.id" class="cat-item">
        <view class="cat-icon-small" :style="{ background: c.color || '#4A90D9' }">
          <text>{{ c.icon || c.name[0] }}</text>
        </view>
        <text class="cat-name">{{ c.name }}</text>
      </view>
    </view>

    <view class="section-title">收入分类</view>
    <view class="cat-grid">
      <view v-for="c in incomeCategories" :key="c.id" class="cat-item">
        <view class="cat-icon-small" :style="{ background: c.color || '#52C41A' }">
          <text>{{ c.icon || c.name[0] }}</text>
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

const expenseCategories = ref<CategoryItem[]>([])
const incomeCategories = ref<CategoryItem[]>([])

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
.cat-page { min-height: 100vh; background: $bg-color; padding: $spacing-sm; }
.section-title { font-size: $font-lg; font-weight: 700; color: $text-primary; padding: $spacing-md 0 $spacing-sm; }
.cat-grid { display: flex; flex-wrap: wrap; }
.cat-item { width: 25%; display: flex; flex-direction: column; align-items: center; padding: 20rpx 0; }
.cat-icon-small {
  width: 72rpx; height: 72rpx; border-radius: 20rpx;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 28rpx; font-weight: 600; margin-bottom: 8rpx;
}
.cat-name { font-size: $font-xs; color: $text-regular; }
</style>
