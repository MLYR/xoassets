<!-- 注册页：沿用登录页左右分屏视觉，表单接入真实注册接口。 -->
<template>
  <div class="register-page">
    <section class="register-brand">
      <div class="register-logo">
        <span>{{ BRAND_SYMBOL }}</span>
        <div>
          <strong>{{ BRAND_NAME }}</strong>
          <small>{{ BRAND_SHORT_NAME }}</small>
        </div>
      </div>

      <div class="register-copy">
        <h1>从第一笔记录开始，<br />建立你的资产秩序</h1>
        <p>创建账号后即可管理账户、分类和收支流水，逐步沉淀自己的财务数据。</p>
        <div class="preview-cards">
          <div class="preview-card">
            <span>账户资产</span>
            <AmountText :value="128600" />
            <small>多账户统一管理</small>
          </div>
          <div class="preview-card">
            <span>本月支出</span>
            <AmountText :value="5630" />
            <small>分类趋势清晰可见</small>
          </div>
        </div>
      </div>

      <p class="copyright">© 2026 {{ BRAND_NAME }}. All rights reserved.</p>
    </section>

    <section class="register-form-panel">
      <div class="mobile-logo">
        <span>{{ BRAND_SYMBOL }}</span>
        <strong>{{ BRAND_NAME }}</strong>
      </div>
      <div class="form-card">
        <h2>创建账号</h2>
        <p>注册 XOAssets 小〇财迹，开始记录你的资产变化</p>
        <el-form label-position="top" @submit.prevent="handleRegister">
          <el-form-item label="账号">
            <el-input v-model.trim="form.username" placeholder="请输入账号" />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model.trim="form.nickname" placeholder="请输入昵称（可选）" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" placeholder="至少 6 位密码" :type="showPassword ? 'text' : 'password'">
              <template #suffix>
                <el-icon class="password-icon" @click="showPassword = !showPassword">
                  <component :is="showPassword ? Hide : View" />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="form.confirmPassword" placeholder="请再次输入密码" :type="showConfirmPassword ? 'text' : 'password'">
              <template #suffix>
                <el-icon class="password-icon" @click="showConfirmPassword = !showConfirmPassword">
                  <component :is="showConfirmPassword ? Hide : View" />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-button class="register-button" type="primary" native-type="submit" :loading="loading">注册</el-button>
        </el-form>
        <div class="switch-entry">
          <span>已有账号？</span>
          <el-button link type="primary" @click="router.push(ROUTES.login)">返回登录</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
// 注册页只负责创建账号，注册成功后回到登录页，由用户再完成登录闭环。
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Hide, View } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { BRAND_NAME, BRAND_SHORT_NAME, BRAND_SYMBOL } from '@/constants/brand';
import { ROUTES } from '@/constants/routes';
import AmountText from '@/components/finance/AmountText.vue';
import { authApi } from '@/services/authApi';

// 路由实例用于注册成功后跳转登录页。
const router = useRouter();
// 密码可见性和提交状态为本地 UI 状态。
const showPassword = ref(false);
const showConfirmPassword = ref(false);
const loading = ref(false);
// 注册表单模型，其中 confirmPassword 只做前端校验，不提交给后端。
const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
});

// 注册前做基础校验，避免明显无效请求进入后端。
function validateForm() {
  if (!form.username) {
    ElMessage.warning('请输入账号');
    return false;
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少 6 位');
    return false;
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致');
    return false;
  }
  return true;
}

// 调用真实注册接口，成功后回登录页继续登录。
async function handleRegister() {
  if (!validateForm()) {
    return;
  }
  loading.value = true;
  try {
    await authApi.register({
      username: form.username,
      nickname: form.nickname || undefined,
      password: form.password
    });
    ElMessage.success('注册成功，请登录');
    router.push(ROUTES.login);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '注册失败');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
/* 注册页采用与登录页一致的左右分屏和品牌信息布局。 */
.register-page {
  display: flex;
  min-height: 100vh;
  background: var(--xo-card);
}

.register-brand {
  position: relative;
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: 48px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}

.register-brand::after {
  position: absolute;
  inset: auto 0 0;
  height: 48%;
  background: linear-gradient(0deg, rgba(59, 130, 246, 0.18), rgba(59, 130, 246, 0));
  content: "";
}

.register-logo,
.mobile-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.register-logo span,
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

.register-logo strong,
.mobile-logo strong {
  display: block;
  color: var(--xo-text);
  font-size: 20px;
}

.register-logo small {
  display: block;
  margin-top: 4px;
  color: var(--xo-muted);
}

.register-copy,
.copyright,
.register-logo {
  position: relative;
  z-index: 1;
}

.register-copy {
  max-width: 560px;
}

.register-copy h1 {
  margin: 0 0 16px;
  color: var(--xo-text);
  font-size: 40px;
  line-height: 1.25;
}

.register-copy p {
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

.register-form-panel {
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

.register-button {
  width: 100%;
  height: 44px;
}

/* 注册页底部入口只负责回到登录页，避免和提交按钮混淆。 */
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
  .register-brand {
    display: none;
  }

  .register-form-panel {
    width: 100%;
  }

  .mobile-logo {
    display: flex;
  }
}
</style>
