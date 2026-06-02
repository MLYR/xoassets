<template>
  <view class="register-page">
    <view class="brand-section">
      <text class="brand-logo">〇</text>
      <text class="brand-name">创建账号</text>
    </view>

    <view class="form-card">
      <view class="form-group">
        <text class="form-label">昵称</text>
        <input class="form-input" v-model="nickname" placeholder="给自己起个名字" placeholder-style="color:#C0C4CC" />
      </view>
      <view class="form-group">
        <text class="form-label">账号</text>
        <input class="form-input" v-model="username" placeholder="设置登录用户名" placeholder-style="color:#C0C4CC" />
      </view>
      <view class="form-group">
        <text class="form-label">密码</text>
        <input class="form-input" v-model="password" type="password" placeholder="设置登录密码" placeholder-style="color:#C0C4CC" />
      </view>
      <view class="form-group">
        <text class="form-label">确认密码</text>
        <input class="form-input" v-model="confirmPwd" type="password" placeholder="再次输入密码" placeholder-style="color:#C0C4CC" @confirm="handleRegister" />
      </view>

      <view class="btn-primary register-btn" @click="handleRegister" :class="{ disabled: btnDisabled }">
        <text>注册</text>
      </view>

      <text v-if="errorMsg" class="error-msg">{{ errorMsg }}</text>

      <view class="form-footer">
        <text class="link" @click="goLogin">已有账号？去登录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const nickname = ref('')
const username = ref('')
const password = ref('')
const confirmPwd = ref('')
const registering = ref(false)
const errorMsg = ref('')

const btnDisabled = computed(() => !username.value || !password.value || !confirmPwd.value)

async function handleRegister() {
  if (btnDisabled.value || registering.value) return
  if (password.value !== confirmPwd.value) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  registering.value = true
  errorMsg.value = ''
  try {
    await authStore.register(username.value, password.value, nickname.value || undefined)
    // 注册成功后自动登录
    await authStore.login(username.value, password.value)
    uni.switchTab({ url: '/pages/index/index' })
  } catch (e: any) {
    errorMsg.value = e.message || '注册失败'
  } finally {
    registering.value = false
  }
}

function goLogin() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #4A90D9 0%, #6BA5E7 40%, #F0F4F8 40%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 16vh;
}
.brand-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 48rpx;
}
.brand-logo { font-size: 72rpx; color: #fff; font-weight: 700; margin-bottom: 12rpx; }
.brand-name { font-size: 38rpx; color: #fff; font-weight: 700; }
.form-card {
  width: 630rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 8rpx 40rpx rgba(0,0,0,0.08);
}
.form-group { margin-bottom: 28rpx; }
.form-label { font-size: 28rpx; color: #303133; margin-bottom: 12rpx; display: block; }
.form-input {
  height: 88rpx; background: #F5F7FA; border-radius: 16rpx;
  padding: 0 24rpx; font-size: 30rpx; color: #303133;
}
.register-btn { margin-top: 40rpx; border-radius: 16rpx;
  &.disabled { background: #C0C4CC; }
}
.error-msg { display: block; text-align: center; margin-top: 24rpx; color: #FF4D4F; font-size: 26rpx; }
.form-footer { margin-top: 32rpx; text-align: center; }
.link { color: #4A90D9; font-size: 26rpx; }
</style>
