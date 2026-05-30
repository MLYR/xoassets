<!-- 根组件只负责承载路由，页面结构交给 layout 和 views。 -->
<template>
  <router-view />
</template>

<script setup lang="ts">
// 当前根组件无需业务逻辑，保留脚本块便于后续接入全局初始化。
</script>
