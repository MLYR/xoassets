<!-- 分类管理页：管理当前用户自己的收入和支出分类。 -->
<template>
  <div class="page">
    <section class="panel panel-padding">
      <div class="category-nav-row">
        <el-tabs v-model="activeType" class="category-tabs" @tab-change="loadCategories">
          <el-tab-pane label="支出分类" name="EXPENSE" />
          <el-tab-pane label="收入分类" name="INCOME" />
        </el-tabs>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增分类</el-button>
      </div>

      <el-table v-loading="loading" class="category-table" :data="categories" stripe>
        <template #empty>
          <el-empty description="暂无分类，新用户注册后会自动初始化默认分类，也可以手动新增" />
        </template>
        <el-table-column prop="name" label="分类名称" min-width="160" />
        <el-table-column prop="icon" label="图标" width="110">
          <template #default="{ row }"><span class="icon-cell">{{ row.icon || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="颜色" width="120">
          <template #default="{ row }">
            <span class="color-cell">
              <span class="color-dot" :style="{ backgroundColor: row.color || '#d1d5db' }" />
              {{ row.color || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-switch :model-value="row.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" @change="(value: number) => handleStatusChange(row, value)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" class="xo-form-dialog category-form-dialog" width="560px" top="12px">
      <template #header>
        <div class="xo-dialog-header-content">
          <span class="xo-dialog-kicker">收支分类</span>
          <h2>{{ dialogTitle }}</h2>
          <p>{{ editingCategory ? '调整分类名称、图标和状态；分类方向保持不变，避免影响历史流水。' : '新增收入或支出分类，用于记账筛选、预算和统计分析。' }}</p>
        </div>
      </template>
      <el-form class="xo-dialog-form" label-position="top" @submit.prevent="handleSubmit">
        <section class="xo-dialog-section">
          <div class="xo-dialog-section-title">
            <strong>分类信息</strong>
            <span>方向、名称和排序</span>
          </div>
          <div class="category-form-grid">
            <el-form-item label="分类方向">
              <el-segmented v-model="form.type" :options="typeOptions" :disabled="Boolean(editingCategory)" class="full-width" />
            </el-form-item>
            <el-form-item label="分类名称">
              <el-input v-model.trim="form.name" placeholder="请输入分类名称" />
            </el-form-item>
            <el-form-item label="图标">
              <el-input v-model.trim="form.icon" placeholder="例如：🍜 或 food" />
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" class="full-width" :step="1" />
            </el-form-item>
          </div>
        </section>

        <section class="xo-dialog-section">
          <div class="xo-dialog-section-title">
            <strong>视觉与状态</strong>
            <span>用于列表和记账入口展示</span>
          </div>
          <div class="category-visual-row">
            <el-form-item label="颜色">
              <div class="color-picker-row">
                <el-color-picker v-model="form.color" />
                <span>{{ form.color }}</span>
              </div>
            </el-form-item>
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </div>
        </section>
      </el-form>
      <template #footer>
        <div class="xo-dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// 分类管理页所有操作都通过后端接口完成，前端不传 userId。
import { computed, onMounted, reactive, ref } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { categoryApi, type CategoryItem, type CategoryRequest, type CategoryType } from '@/services/categoryApi';

// 分类方向 tab，默认展示支出分类。
const activeType = ref<CategoryType>('EXPENSE');
const categories = ref<CategoryItem[]>([]);
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const editingCategory = ref<CategoryItem | null>(null);
// Element Plus 分段控件需要 label/value 对象数组。
const typeOptions = [
  { label: '支出', value: 'EXPENSE' },
  { label: '收入', value: 'INCOME' }
];
// 表单模型与后端 CategoryRequest 保持一致。
const form = reactive<CategoryRequest>({
  name: '',
  type: 'EXPENSE',
  icon: '',
  color: '#3b82f6',
  status: 1,
  sortOrder: 0
});

const dialogTitle = computed(() => (editingCategory.value ? '编辑分类' : '新增分类'));

onMounted(() => {
  loadCategories();
});

// 分类列表来自 xo_category 表，按当前 tab 的 type 过滤。
async function loadCategories() {
  loading.value = true;
  try {
    categories.value = await categoryApi.list(activeType.value);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '分类加载失败');
  } finally {
    loading.value = false;
  }
}

// 重置表单；编辑时 type 不允许变更，避免历史流水方向被混淆。
function resetForm(category?: CategoryItem) {
  form.name = category?.name ?? '';
  form.type = category?.type ?? activeType.value;
  form.icon = category?.icon ?? '';
  form.color = category?.color ?? '#3b82f6';
  form.status = category?.status ?? 1;
  form.sortOrder = category?.sortOrder ?? 0;
}

// 打开新增分类弹窗。
function openCreateDialog() {
  editingCategory.value = null;
  resetForm();
  dialogVisible.value = true;
}

// 打开编辑分类弹窗。
function openEditDialog(category: CategoryItem) {
  editingCategory.value = category;
  resetForm(category);
  dialogVisible.value = true;
}

// 前端只做必要必填校验，重复名和归属校验以后端为准。
function validateForm() {
  if (!form.name) {
    ElMessage.warning('请输入分类名称');
    return false;
  }
  return true;
}

// 新增或编辑分类后刷新当前 tab 列表。
async function handleSubmit() {
  if (!validateForm()) {
    return;
  }
  submitting.value = true;
  try {
    if (editingCategory.value) {
      await categoryApi.update(editingCategory.value.id, form);
      ElMessage.success('分类已更新');
    } else {
      await categoryApi.create(form);
      ElMessage.success('分类已新增');
    }
    activeType.value = form.type;
    dialogVisible.value = false;
    await loadCategories();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '分类保存失败');
  } finally {
    submitting.value = false;
  }
}

// 分类删除前二次确认；被流水使用时展示后端提示，引导用户停用。
async function handleDelete(category: CategoryItem) {
  try {
    await ElMessageBox.confirm(`确认删除分类「${category.name}」吗？`, '删除分类', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    });
    await categoryApi.remove(category.id);
    ElMessage.success('分类已删除');
    await loadCategories();
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return;
    }
    ElMessage.error(error instanceof Error ? error.message : '分类删除失败');
  }
}

// 启用或停用分类；失败时重新加载列表恢复真实状态。
async function handleStatusChange(category: CategoryItem, status: number) {
  try {
    await categoryApi.updateStatus(category.id, status);
    ElMessage.success(status === 1 ? '分类已启用' : '分类已停用');
    await loadCategories();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '状态更新失败');
    await loadCategories();
  }
}
</script>

<style scoped>
/* 分类页补齐视觉基线，色块和图标使用轻量卡片式细节。 */
.category-nav-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--xo-border);
}

.category-tabs {
  min-width: 0;
  flex: 1;
}

.category-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.category-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.category-table {
  margin-top: 12px;
}

.color-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--xo-muted);
  font-size: 13px;
}

.color-dot {
  width: 18px;
  height: 18px;
  border: 1px solid var(--xo-border);
  border-radius: 7px;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.08);
}

.icon-cell {
  display: inline-grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 12px;
  background: var(--xo-primary-softer);
  color: var(--xo-primary);
  font-weight: 800;
}

.full-width {
  width: 100%;
}

.category-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.category-visual-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(160px, 0.7fr);
  gap: 0 14px;
}

.color-picker-row {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--xo-muted);
  font-size: 13px;
}

@media (max-width: 720px) {
  .category-nav-row {
    flex-direction: column;
    align-items: stretch;
  }

  .category-form-grid,
  .category-visual-row {
    grid-template-columns: 1fr;
  }
}
</style>
