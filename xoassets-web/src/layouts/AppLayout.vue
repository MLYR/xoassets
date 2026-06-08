<!-- 应用主布局：左侧导航、顶部标题栏、右侧业务内容区。 -->
<template>
  <div class="app-layout">
    <Sidebar />
    <section class="content-shell">
      <AppHeader />
      <main class="content-main">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
// 布局组件只组合结构，不承载业务数据。
import Sidebar from './Sidebar.vue';
import AppHeader from './Header.vue';
</script>

<style scoped>
/* 主布局固定满屏，浅蓝渐变由全局背景承载，内容区内部滚动。 */
.app-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: transparent;
}

.content-shell {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.content-main {
  flex: 1;
  min-width: 0;
  overflow: auto;
}
</style>
