// 应用入口：注册 Vue、Pinia、Router 和 Element Plus。
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';
import App from './App.vue';
import router from './router';
import { useThemeStore } from './stores/theme';
import './styles/index.css';

// 创建应用实例后按插件依赖顺序挂载。
const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
// Element Plus 全局使用中文，确保日期选择器面板、按钮和星期月份文案一致。
app.use(ElementPlus, { locale: zhCn });
// 主题在挂载前初始化，避免首屏出现日间/夜间闪烁。
useThemeStore(pinia).initTheme();
app.mount('#app');
