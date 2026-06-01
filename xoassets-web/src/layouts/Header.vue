<!-- 顶部栏：搜索、提醒和用户中心入口，使用轻玻璃工具栏质感。 -->
<template>
  <header class="app-header">
    <el-input class="search" placeholder="搜索交易、账户..." :prefix-icon="Search" clearable />
    <div class="header-actions">
      <el-badge is-dot>
        <el-button :icon="Bell" circle />
      </el-badge>
      <span class="divider" />
      <el-dropdown trigger="click" @command="handleCommand">
        <el-button class="user-chip" :icon="UserFilled">{{ displayName }}</el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">修改名称</el-dropdown-item>
            <el-dropdown-item command="password">修改密码</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-dialog v-model="profileDialogVisible" title="修改名称" width="420px">
      <el-form label-position="top" @submit.prevent="handleUpdateProfile">
        <el-form-item label="账号">
          <el-input :model-value="user?.username || '-'" disabled />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model.trim="profileForm.nickname" placeholder="请输入名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="profileSubmitting" @click="handleUpdateProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px">
      <el-form label-position="top" @submit.prevent="handleChangePassword">
        <el-form-item label="旧密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少 6 位新密码" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="handleChangePassword">保存</el-button>
      </template>
    </el-dialog>
  </header>
</template>

<script setup lang="ts">
// Header 负责当前用户展示、资料维护和退出登录，不承载业务页面状态。
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Bell, Search, UserFilled } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ROUTES } from '@/constants/routes';
import { authApi, type AuthUser } from '@/services/authApi';
import { clearToken } from '@/services/token';

// 当前用户信息从后端 /api/auth/me 获取，避免继续使用硬编码名称。
const user = ref<AuthUser | null>(null);
const router = useRouter();
const profileDialogVisible = ref(false);
const passwordDialogVisible = ref(false);
const profileSubmitting = ref(false);
const passwordSubmitting = ref(false);
// 名称表单只提交 nickname，账号保持只读展示。
const profileForm = reactive({
  nickname: ''
});
// 修改密码表单包含确认密码，confirmPassword 只做前端校验。
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 顶部按钮优先展示昵称，昵称为空时回退到用户名。
const displayName = computed(() => user.value?.nickname || user.value?.username || '用户');

onMounted(() => {
  loadCurrentUser();
});

// 加载当前用户失败时交给 HTTP 拦截器处理 401，这里只展示非认证类错误。
async function loadCurrentUser() {
  try {
    user.value = await authApi.me();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '用户信息加载失败');
  }
}

// 处理用户中心菜单命令。
function handleCommand(command: string) {
  if (command === 'profile') {
    openProfileDialog();
  } else if (command === 'password') {
    openPasswordDialog();
  } else if (command === 'logout') {
    handleLogout();
  }
}

// 打开名称弹窗时带入当前名称，避免用户重复输入。
function openProfileDialog() {
  profileForm.nickname = displayName.value;
  profileDialogVisible.value = true;
}

// 打开密码弹窗时清空敏感输入。
function openPasswordDialog() {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  passwordDialogVisible.value = true;
}

// 保存昵称后刷新顶部展示。
async function handleUpdateProfile() {
  if (!profileForm.nickname) {
    ElMessage.warning('请输入名称');
    return;
  }
  profileSubmitting.value = true;
  try {
    user.value = await authApi.updateProfile({ nickname: profileForm.nickname });
    profileDialogVisible.value = false;
    ElMessage.success('名称已更新');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '名称修改失败');
  } finally {
    profileSubmitting.value = false;
  }
}

// 校验旧密码、新密码和确认密码后提交后端。
async function handleChangePassword() {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入旧密码');
    return;
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位');
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致');
    return;
  }
  passwordSubmitting.value = true;
  try {
    await authApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });
    passwordDialogVisible.value = false;
    ElMessage.success('密码已更新，请重新登录');
    logoutToLogin();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '密码修改失败');
  } finally {
    passwordSubmitting.value = false;
  }
}

// 退出登录前二次确认，避免误点导致工作流中断。
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出当前账号吗？', '退出登录', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    });
    logoutToLogin();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '退出失败');
    }
  }
}

// 清理本地 token 并回到登录页。
function logoutToLogin() {
  clearToken();
  router.replace(ROUTES.login);
}
</script>

<style scoped>
/* Header 使用半透明白底，保持内容区的轻量工作台质感。 */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 24px;
  border-bottom: 1px solid var(--xo-border);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: var(--xo-blur);
}

.search {
  max-width: 460px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.divider {
  width: 1px;
  height: 24px;
  background: var(--xo-border);
}

.user-chip {
  border-color: var(--xo-border);
  background: rgba(255, 255, 255, 0.72);
  color: var(--xo-text);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}
</style>
