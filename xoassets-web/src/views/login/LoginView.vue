<!-- 登录页：保留原型左侧品牌展示和右侧表单结构。 -->
<template>
  <div class="login-page">
    <section class="login-brand">
      <div class="login-logo">
        <span>{{ BRAND_SYMBOL }}</span>
        <div>
          <strong>{{ BRAND_NAME }}</strong>
          <small>{{ BRAND_SHORT_NAME }}</small>
        </div>
      </div>

      <div class="login-copy">
        <h1>看清每一笔花费，<br />掌握你的财富全貌</h1>
        <p>记录资产变化、复盘消费趋势，用清晰的数据管理每一次财务决策。</p>
        <div class="preview-cards">
          <div class="preview-card">
            <span>总资产</span>
            <AmountText :value="254180" />
            <small>较昨日 +¥128.50</small>
          </div>
          <div class="preview-card">
            <span>本月结余</span>
            <AmountText :value="8320" />
            <small>较上月 +12.5%</small>
          </div>
        </div>
      </div>

      <p class="copyright">© 2026 {{ BRAND_NAME }}. All rights reserved.</p>
    </section>

    <section class="login-form-panel">
      <div class="mobile-logo">
        <span>{{ BRAND_SYMBOL }}</span>
        <strong>{{ BRAND_NAME }}</strong>
      </div>
      <div class="form-card">
        <h2>欢迎回来</h2>
        <p>登录您的账户，继续管理资产和流水</p>
        <el-form label-position="top" @submit.prevent="handleLogin">
          <el-form-item label="账号">
            <el-input v-model="form.username" placeholder="请输入账号" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" placeholder="请输入密码" :type="showPassword ? 'text' : 'password'">
              <template #suffix>
                <el-icon class="password-icon" @click="showPassword = !showPassword">
                  <component :is="showPassword ? Hide : View" />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>
          <div class="form-row">
            <el-checkbox v-model="remember">记住密码</el-checkbox>
            <el-button link type="primary">忘记密码？</el-button>
          </div>
          <el-button class="login-button" type="primary" native-type="submit">登录</el-button>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
// 登录行为当前直接进入仪表盘，后续可替换为真实认证服务。
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Hide, View } from '@element-plus/icons-vue';
import { BRAND_NAME, BRAND_SHORT_NAME, BRAND_SYMBOL } from '@/constants/brand';
import { ROUTES } from '@/constants/routes';
import AmountText from '@/components/finance/AmountText.vue';

// 路由实例用于登录后跳转。
const router = useRouter();
// 记住密码和密码可见性为本地表单状态。
const remember = ref(false);
const showPassword = ref(false);
// 登录表单模型，后续接入后端时直接作为登录请求体基础。
const form = reactive({
  username: '',
  password: ''
});

// 当前 MVP 不做真实鉴权，提交后进入首页。
function handleLogin() {
  router.push(ROUTES.dashboard);
}
</script>

<style scoped>
/* 登录页采用左右分屏，移动端隐藏品牌展示区。 */
.login-page {
  display: flex;
  min-height: 100vh;
  background: var(--xo-card);
}

.login-brand {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: 48px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}

.login-brand::after {
  position: absolute;
  inset: auto 0 0;
  height: 48%;
  background: linear-gradient(0deg, rgba(59, 130, 246, 0.18), rgba(59, 130, 246, 0));
  content: "";
}

.login-logo,
.mobile-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.login-logo span,
.mobile-logo span {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 12px;
  background: var(--xo-primary);
  color: #fff;
  font-weight: 700;
}

.login-logo strong,
.mobile-logo strong {
  display: block;
  color: var(--xo-text);
  font-size: 20px;
}

.login-logo small {
  display: block;
  margin-top: 4px;
  color: var(--xo-muted);
}

.login-copy,
.copyright,
.login-logo {
  position: relative;
  z-index: 1;
}

.login-copy {
  max-width: 560px;
}

.login-copy h1 {
  margin: 0 0 16px;
  color: var(--xo-text);
  font-size: 40px;
  line-height: 1.25;
}

.login-copy p {
  margin: 0;
  color: var(--xo-muted);
  font-size: 18px;
  line-height: 1.7;
}

.preview-cards {
  display: flex;
  gap: 16px;
  margin-top: 32px;
}

.preview-card {
  min-width: 180px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: var(--xo-radius);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--xo-shadow);
}

.preview-card span,
.preview-card small {
  display: block;
  color: var(--xo-muted);
  font-size: 13px;
}

.preview-card small {
  margin-top: 6px;
  color: var(--xo-success);
}

.copyright {
  margin: 0;
  color: var(--xo-muted);
  font-size: 12px;
}

.login-form-panel {
  display: flex;
  width: 480px;
  align-items: center;
  justify-content: center;
  padding: 32px;
}

.form-card {
  width: 100%;
  max-width: 360px;
}

.form-card h2 {
  margin: 0 0 8px;
  text-align: center;
  font-size: 24px;
}

.form-card > p {
  margin: 0 0 32px;
  text-align: center;
  color: var(--xo-muted);
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
  height: 44px;
}

.password-icon {
  cursor: pointer;
}

.mobile-logo {
  display: none;
  justify-content: center;
  margin-bottom: 32px;
}

@media (max-width: 960px) {
  .login-brand {
    display: none;
  }

  .login-form-panel {
    width: 100%;
  }

  .mobile-logo {
    display: flex;
  }
}
</style>
