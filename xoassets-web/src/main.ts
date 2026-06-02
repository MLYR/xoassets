// 应用入口：注册 Vue、Pinia、Router 和 Element Plus。
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router';
import './styles/index.css';

// 创建应用实例后按插件依赖顺序挂载。
const app = createApp(App);

app.use(createPinia());
app.use(router);
// Element Plus 全局使用中文，确保日期选择器面板、按钮和星期月份文案一致。
app.use(ElementPlus, { locale: zhCn });
app.mount('#app');
