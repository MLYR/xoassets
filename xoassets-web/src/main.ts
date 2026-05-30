// 应用入口：注册 Vue、Pinia、Router 和 Element Plus。
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router';
import './styles/index.css';

// 创建应用实例后按插件依赖顺序挂载。
const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(ElementPlus);
app.mount('#app');
