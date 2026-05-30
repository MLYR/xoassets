<!-- 顶部栏：搜索、提醒和用户入口，保持原型 64px 高度。 -->
<template>
  <header class="app-header">
    <el-input class="search" placeholder="搜索交易、账户..." :prefix-icon="Search" clearable />
    <div class="header-actions">
      <el-badge is-dot>
        <el-button :icon="Bell" circle />
      </el-badge>
      <span class="divider" />
      <el-button class="user-chip" :icon="UserFilled">张三</el-button>
    </div>
  </header>
</template>

<script setup lang="ts">
// 顶部栏暂不绑定真实搜索逻辑，先保留交互控件和视觉结构。
import { Bell, Search, UserFilled } from '@element-plus/icons-vue';
</script>

<style scoped>
/* Header 使用白底细边框，让内容区保持轻量工作台质感。 */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  border-bottom: 1px solid var(--xo-border);
  background: var(--xo-card);
}

.search {
  max-width: 420px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.divider {
  width: 1px;
  height: 24px;
  background: var(--xo-border);
}

.user-chip {
  border: 0;
  background: transparent;
}
</style>
