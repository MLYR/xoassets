<!-- 左侧导航：白色侧栏、浅蓝激活态和清晰图标文字对齐。 -->
<template>
  <aside class="sidebar">
    <RouterLink class="brand" :to="ROUTES.dashboard">
      <div class="brand-mark">{{ BRAND_SYMBOL }}</div>
      <div class="brand-text">
        <strong>{{ BRAND_NAME }}</strong>
        <span>{{ BRAND_SHORT_NAME }}</span>
      </div>
    </RouterLink>

    <el-menu
      :default-active="route.path"
      class="sidebar-menu"
      background-color="transparent"
      text-color="#475569"
      active-text-color="#2563eb"
      router
    >
      <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup lang="ts">
// 菜单项从 constants 读取，避免导航文案散落在组件内。
import { useRoute } from 'vue-router';
import { BRAND_NAME, BRAND_SHORT_NAME, BRAND_SYMBOL } from '@/constants/brand';
import { menuItems } from '@/constants/menu';
import { ROUTES } from '@/constants/routes';

// 当前路由用于驱动侧边栏激活态。
const route = useRoute();
</script>

<style scoped>
/* 侧边栏使用白色玻璃面，当前菜单以浅蓝背景突出。 */
.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: var(--xo-sidebar);
  border-right: 1px solid var(--xo-border);
  color: var(--xo-text);
  box-shadow: 12px 0 36px rgba(15, 23, 42, 0.04);
  backdrop-filter: var(--xo-blur);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  padding: 0 24px;
  border-bottom: 1px solid var(--xo-border);
}

.brand-mark {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  font-weight: 700;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.18);
}

.brand-text {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.brand-text strong {
  overflow: hidden;
  font-size: 14px;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-text span {
  margin-top: 4px;
  color: var(--xo-muted);
  font-size: 12px;
  line-height: 1;
}

.sidebar-menu {
  border-right: 0;
  padding: 16px 12px;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 42px;
  margin-bottom: 4px;
  border-radius: 14px;
  font-weight: 600;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #eff6ff;
  box-shadow: inset 3px 0 0 var(--xo-primary);
}

.sidebar-menu :deep(.el-menu-item:not(.is-active):hover) {
  background: var(--xo-sidebar-soft);
  color: var(--xo-primary);
}
</style>
