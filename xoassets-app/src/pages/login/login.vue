<template>
  <view class="login-page">
    <!-- 顶部品牌区 -->
    <view class="brand-section">
      <text class="brand-logo">〇</text>
      <text class="brand-name">小〇财迹</text>
      <text class="brand-desc">你的个人资产管理助手</text>
    </view>

    <!-- 登录表单 -->
    <view class="form-card">
      <view class="form-group">
        <text class="form-label">账号</text>
        <input
          class="form-input"
          v-model="username"
          placeholder="请输入用户名"
          placeholder-style="color:#C0C4CC"
        />
      </view>
      <view class="form-group">
        <text class="form-label">密码</text>
        <input
          class="form-input"
          v-model="password"
          type="password"
          placeholder="请输入密码"
          placeholder-style="color:#C0C4CC"
          @confirm="handleLogin"
        />
      </view>

      <view class="btn-primary login-btn" @click="handleLogin" :class="{ disabled: btnDisabled }">
        <text v-if="!logging">登录</text>
        <text v-else>登录中…</text>
      </view>

      <text v-if="errorMsg" class="error-msg">{{ errorMsg }}</text>

      <view class="form-footer">
        <text class="link" @click="goRegister">还没有账号？立即注册</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const username = ref('')
const password = ref('')
const logging = ref(false)
const errorMsg = ref('')

const btnDisabled = computed(() => !username.value || !password.value)

async function handleLogin() {
  if (btnDisabled.value || logging.value) return
  logging.value = true
  errorMsg.value = ''
  try {
    await authStore.login(username.value, password.value)
    uni.switchTab({ url: '/pages/index/index' })
  } catch (e: any) {
    errorMsg.value = e.message || '登录失败'
  } finally {
    logging.value = false
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/register/register' })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #4A90D9 0%, #6BA5E7 40%, #F0F4F8 40%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 20vh;
}

.brand-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 60rpx;
}
.brand-logo {
  font-size: 80rpx;
  color: #fff;
  font-weight: 700;
  margin-bottom: 16rpx;
}
.brand-name {
  font-size: 44rpx;
  color: #fff;
  font-weight: 700;
  margin-bottom: 12rpx;
}
.brand-desc {
  font-size: 26rpx;
  color: rgba(255,255,255,0.8);
}

.form-card {
  width: 630rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 8rpx 40rpx rgba(0,0,0,0.08);
}

.form-group {
  margin-bottom: 32rpx;
}
.form-label {
  font-size: 28rpx;
  color: #303133;
  margin-bottom: 12rpx;
  display: block;
}
.form-input {
  height: 88rpx;
  background: #F5F7FA;
  border-radius: 16rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: #303133;
}

.login-btn {
  margin-top: 40rpx;
  border-radius: 16rpx;
  &.disabled {
    background: #C0C4CC;
  }
}

.error-msg {
  display: block;
  text-align: center;
  margin-top: 24rpx;
  color: #FF4D4F;
  font-size: 26rpx;
}

.form-footer {
  margin-top: 32rpx;
  text-align: center;
}
.link {
  color: #4A90D9;
  font-size: 26rpx;
}
</style>
