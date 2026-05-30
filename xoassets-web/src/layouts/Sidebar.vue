<!-- 左侧导航：延续原型深色侧栏、蓝色激活项和紧凑菜单。 -->
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
      text-color="#cbd5e1"
      active-text-color="#ffffff"
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
/* 侧边栏尺寸和配色与原型保持一致。 */
.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: var(--xo-sidebar);
  color: #fff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  padding: 0 24px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.brand-mark {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: var(--xo-radius);
  background: var(--xo-primary);
  font-weight: 700;
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
  color: rgba(226, 232, 240, 0.62);
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
  border-radius: var(--xo-radius);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--xo-primary);
}

.sidebar-menu :deep(.el-menu-item:not(.is-active):hover) {
  background: var(--xo-sidebar-soft);
}
</style>
