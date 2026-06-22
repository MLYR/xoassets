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
          <el-button class="login-button" type="primary" native-type="submit" :loading="loading">登录</el-button>
        </el-form>
        <div class="switch-entry">
          <span>还没有账号？</span>
          <el-button link type="primary" @click="router.push(ROUTES.register)">立即注册</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
// 登录页调用真实后端接口，成功后保存 token 并进入业务页面。
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Hide, View } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { BRAND_NAME, BRAND_SHORT_NAME, BRAND_SYMBOL } from '@/constants/brand';
import { ROUTES } from '@/constants/routes';
import AmountText from '@/components/finance/AmountText.vue';
import { authApi } from '@/services/authApi';
import { setTokens } from '@/services/token';

// 路由实例用于登录后跳转。
const router = useRouter();
const route = useRoute();
// 记住密码和密码可见性为本地表单状态。
const remember = ref(false);
const showPassword = ref(false);
const loading = ref(false);
// 登录表单模型，后续接入后端时直接作为登录请求体基础。
const form = reactive({
  username: '',
  password: ''
});

// 登录成功后保存 JWT；redirect 存在时回到原目标页，否则进入首页。
async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码');
    return;
  }
  loading.value = true;
  try {
    const result = await authApi.login({
      username: form.username,
      password: form.password
    });
    setTokens(result.accessToken, result.refreshToken);
    ElMessage.success('登录成功');
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ROUTES.dashboard;
    router.push(redirect);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
/* 登录页采用原型同款左右分屏，左侧用城市氛围和指标卡强化金融感。 */
.login-page {
  display: flex;
  min-height: 100dvh;
  background:
    radial-gradient(circle at 74% 18%, var(--xo-bg-spot-primary), transparent 26%),
    var(--xo-bg-canvas);
}

.login-brand {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: 64px 64px 54px;
  background:
    radial-gradient(circle at 22% 12%, var(--xo-bg-spot-primary), transparent 28%),
    linear-gradient(135deg, var(--xo-bg-soft) 0%, var(--xo-card) 100%);
}

.login-brand::after {
  position: absolute;
  inset: auto -10% 0 -8%;
  height: 50%;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0), var(--xo-bg-soft)),
    url("data:image/svg+xml,%3Csvg width='900' height='360' viewBox='0 0 900 360' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' stroke='%232563eb' stroke-opacity='.18'%3E%3Cpath d='M28 330h844'/%3E%3Cpath d='M90 330V210h52v120M180 330V160h72v170M306 330V118h56v212M410 330V76h92v254M550 330V138h62v192M664 330V96h82v234M790 330V184h52v146'/%3E%3Cpath d='M0 260c120 14 194-14 292-10 110 5 190 42 314 32 112-8 176-54 294-42'/%3E%3C/g%3E%3C/svg%3E") bottom center / cover no-repeat;
  content: "";
  opacity: 0.82;
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
  background: var(--xo-brand-gradient);
  color: #fff;
  font-weight: 700;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.22);
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
  font-size: clamp(42px, 5vw, 62px);
  line-height: 1.18;
  font-weight: 900;
}

.login-copy p {
  margin: 0;
  color: var(--xo-muted);
  font-size: 18px;
  line-height: 1.85;
}

.preview-cards {
  display: flex;
  gap: 16px;
  margin-top: 38px;
}

.preview-card {
  min-width: 190px;
  padding: 18px;
  border: 1px solid var(--xo-border);
  border-radius: var(--xo-radius);
  background: var(--xo-card);
  box-shadow: var(--xo-shadow);
  backdrop-filter: var(--xo-blur);
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
  width: min(48vw, 620px);
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.form-card {
  width: 100%;
  max-width: 430px;
  padding: 46px;
  border: 1px solid var(--xo-border);
  border-radius: 28px;
  background: var(--xo-card-elevated);
  box-shadow: var(--xo-shadow-lg);
  backdrop-filter: var(--xo-blur);
}

.form-card h2 {
  margin: 0 0 8px;
  text-align: center;
  font-size: 30px;
  font-weight: 800;
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
  height: 48px;
  border-radius: 14px;
  font-size: 16px;
}

/* 登录页底部入口只负责切换到注册页，不影响原登录表单布局。 */
.switch-entry {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 18px;
  color: var(--xo-muted);
  font-size: 14px;
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
