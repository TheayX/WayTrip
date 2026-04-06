<!-- 分类管理页面 -->
<template>
  <div class="category-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">内容结构管理</p>
        <h1 class="page-title">分类管理</h1>
        <p class="page-subtitle">维护景点与攻略的分类层级。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading1 || loading2" @click="handleRefresh">刷新数据</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">一级分类</div>
        <div class="summary-value">{{ level1List.length }}</div>
        <div class="summary-desc">当前已创建的一级分类数量</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">当前子分类</div>
        <div class="summary-value">{{ level2List.length }}</div>
        <div class="summary-desc">当前选中一级分类下的二级分类数量</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">当前工作区</div>
        <div class="summary-value summary-value--sm">{{ activeParentName }}</div>
        <div class="summary-desc">未选择时默认展示第一项或空状态</div>
      </el-card>
    </section>

    <el-row :gutter="20">
      <!-- 左侧：一级分类 -->
      <el-col :span="6">
        <el-card shadow="hover" class="left-card">
          <template #header>
            <div class="card-header">
              <span>一级分类</span>
              <el-button type="primary" link icon="Plus" @click="handleAddLevel1">新增</el-button>
            </div>
          </template>
          
          <div v-if="errorMessage" class="panel-error page-error-state">
            <el-result icon="error" title="分类管理加载失败" :sub-title="errorMessage">
              <template #extra>
                <el-button type="primary" @click="handleRefresh">重新加载</el-button>
              </template>
            </el-result>
          </div>

          <ul v-else class="parent-list" v-loading="loading1" element-loading-text="正在加载分类数据...">
            <li 
              v-for="item in level1List" 
              :key="item.id" 
              :class="['list-item', { active: activeParentId === item.id }]"
              @click="handleSelectParent(item.id)"
            >
              <div class="item-name">
                <span style="vertical-align: middle;">{{ item.name }}</span>
                <el-image 
                  v-if="item.iconUrl"
                  :src="getImageUrl(item.iconUrl)" 
                  fit="contain"
                  style="width: 20px; height: 20px; margin-left: 8px; vertical-align: middle;"
                />
              </div>
              <div class="item-actions">
                <el-icon @click.stop="handleEditLevel1(item)" class="action-icon"><Edit /></el-icon>
                <el-icon @click.stop="handleDeleteLevel1(item)" class="action-icon danger"><Delete /></el-icon>
              </div>
            </li>
            <el-empty v-if="level1List.length === 0" description="当前暂无一级分类数据" :image-size="60" />
          </ul>
        </el-card>
      </el-col>

      <!-- 右侧：二级分类 -->
      <el-col :span="18">
        <el-card shadow="hover" class="right-card">
          <template #header>
            <div class="card-header">
              <span>二级分类</span>
              <el-button type="primary" @click="handleAddLevel2" :disabled="!activeParentId">新增二级分类</el-button>
            </div>
          </template>

          <el-table
            :data="level2List"
            v-loading="loading2"
            element-loading-text="正在加载分类数据..."
            class="content-table borderless-table"
          >
            <el-table-column label="图标" width="100">
              <template #default="{ row }">
                <el-image 
                  v-if="row.iconUrl"
                  :src="getImageUrl(row.iconUrl)" 
                  fit="contain"
                  style="width: 40px; height: 40px; border-radius: 4px;"
                >
                  <template #error>
                    <div class="image-slot">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <span v-else>无</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="分类名称" />
            <el-table-column prop="sortOrder" label="排序" width="100" />
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <div style="white-space: nowrap;">
                  <el-button type="primary" link @click="handleEditLevel2(row)">编辑</el-button>
                  <el-button type="danger" link @click="handleDeleteLevel2(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="请先选择左侧一级分类，或当前暂无子分类数据" />
            </template>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle"
      width="500px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        
        <el-form-item label="分类图标" prop="iconUrl">
          <div class="upload-container">
            <el-upload
              class="avatar-uploader"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :data="uploadData"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <img v-if="form.iconUrl" :src="getImageUrl(form.iconUrl)" class="avatar" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">建议尺寸：40x40 或等比例。可为空。</div>
          </div>
        </el-form-item>

        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="1" :max="999" />
          <div class="form-tip">默认取当前层级最后一位；如填写已有序号，系统会自动顺延后续分类。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Picture } from '@element-plus/icons-vue'
import { getCategories, createCategory, updateCategory, deleteCategory } from '@/modules/category/api.js'
import { useUserStore } from '@/app/store/user.js'
import { getAdminUploadUrl, getResourceUrl } from '@/shared/lib/resource.js'

const userStore = useUserStore()

// 上传相关配置
const uploadUrl = computed(() => getAdminUploadUrl('icon'))
const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${userStore.token}`
}))
const uploadData = computed(() => ({
  name: form.name || ''
}))

// 补全图片访问地址
const getImageUrl = (url) => {
  return getResourceUrl(url)
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

// 列表状态
const loading1 = ref(false)
const loading2 = ref(false)
const level1List = ref([])
const level2List = ref([])
const activeParentId = ref(null)
const errorMessage = ref('')
const activeParentName = computed(() => level1List.value.find((item) => item.id === activeParentId.value)?.name || '未选择')

// 对话框与表单状态
const dialogVisible = ref(false)
const isEdit = ref(false)
const isLevel2 = ref(false)
const submitting = ref(false)
const currentId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '',
  parentId: 0,
  iconUrl: '',
  sortOrder: 1
})

// 表单校验规则
const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const dialogTitle = computed(() => {
  const levelStr = isLevel2.value ? '二级分类' : '一级分类'
  return isEdit.value ? `编辑${levelStr}` : `新增${levelStr}`
})

// 当前层级默认排序取现有最大值后一位，减少手工调整成本。
const getNextSortOrder = (list) => {
  const maxSortOrder = list.reduce((max, item) => Math.max(max, Number(item.sortOrder) || 0), 0)
  return maxSortOrder + 1
}

// 加载一级分类
const fetchLevel1 = async () => {
  loading1.value = true
  errorMessage.value = ''
  try {
    const res = await getCategories({ parentId: 0 })
    level1List.value = res.data || []
    if (level1List.value.length > 0 && !activeParentId.value) {
      handleSelectParent(level1List.value[0].id)
    } else if (level1List.value.length === 0) {
      activeParentId.value = null
      level2List.value = []
    }
  } catch (e) {
    level1List.value = []
    level2List.value = []
    activeParentId.value = null
    errorMessage.value = e?.response?.data?.message || e?.message || '请稍后重试或检查接口返回。'
  } finally {
    loading1.value = false
  }
}

// 选择一级分类
const handleSelectParent = (id) => {
  activeParentId.value = id
  fetchLevel2(id)
}

// 加载二级分类
const fetchLevel2 = async (parentId) => {
  loading2.value = true
  try {
    const res = await getCategories({ parentId })
    level2List.value = res.data || []
  } catch (e) {
    level2List.value = []
    ElMessage.error(e?.response?.data?.message || e?.message || '获取二级分类失败')
  } finally {
    loading2.value = false
  }
}

const handleRefresh = () => {
  fetchLevel1()
}

// 一级分类相关操作
const handleAddLevel1 = () => {
  isEdit.value = false
  isLevel2.value = false
  currentId.value = null
  Object.assign(form, { name: '', parentId: 0, iconUrl: '', sortOrder: getNextSortOrder(level1List.value) })
  dialogVisible.value = true
}

const handleEditLevel1 = (row) => {
  isEdit.value = true
  isLevel2.value = false
  currentId.value = row.id
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const handleDeleteLevel1 = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该一级分类吗？如有子分类请先删除子分类。', '删除确认', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    if (activeParentId.value === row.id) {
      activeParentId.value = null
    }
    fetchLevel1()
  } catch (e) {}
}

// 二级分类相关操作
const handleAddLevel2 = () => {
  isEdit.value = false
  isLevel2.value = true
  currentId.value = null
  Object.assign(form, { name: '', parentId: activeParentId.value, iconUrl: '', sortOrder: getNextSortOrder(level2List.value) })
  dialogVisible.value = true
}

const handleEditLevel2 = (row) => {
  isEdit.value = true
  isLevel2.value = true
  currentId.value = row.id
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const handleDeleteLevel2 = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该二级分类吗？', '删除确认', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    fetchLevel2(activeParentId.value)
  } catch (e) {}
}

// 图片上传相关方法
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) ElMessage.error('只能上传图片文件!')
  return isImage
}

const handleUploadSuccess = (response) => {
  if (response.code === 0) {
    form.iconUrl = response.data.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 提交分类表单
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCategory(currentId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createCategory(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    
    if (isLevel2.value) {
      fetchLevel2(activeParentId.value)
    } else {
      fetchLevel1()
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// 页面初始化
onMounted(() => {
  fetchLevel1()
})
</script>

<style lang="scss" scoped>
.category-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .summary-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }

  .summary-card {
    border-radius: 20px;
  }

  .summary-label {
    font-size: 12px;
    color: var(--wt-text-secondary);
    font-weight: 600;
  }

  .summary-value {
    margin-top: 8px;
    font-size: 24px;
    line-height: 1.15;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .summary-value--sm {
    font-size: 20px;
    line-height: 1.3;
  }

  .summary-desc {
    margin-top: 6px;
    font-size: 12px;
    line-height: 1.55;
    color: var(--wt-text-regular);
  }

  .left-card {
    min-height: 520px;
    border-radius: 22px;
    .parent-list {
      list-style: none;
      padding: 0;
      margin: 0;
      .list-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 16px;
        margin-bottom: 4px;
        border-radius: 10px;
        cursor: pointer;
        transition: all 0.2s ease;
        border: 1px solid transparent;
        &:hover {
          background-color: var(--wt-fill-hover);
          .item-actions { opacity: 1; }
        }
        &.active {
          background-color: var(--el-color-primary-light-9);
          color: var(--el-color-primary);
          border-color: var(--el-color-primary-light-7);
          font-weight: 600;
        }
        .item-actions {
          opacity: 0;
          transition: opacity 0.2s;
          display: flex;
          gap: 12px;
          .action-icon {
            color: var(--wt-text-secondary);
            font-size: 30px;
            cursor: pointer;
            padding: 4px;
            border-radius: 6px;
            transition: all 0.2s;
            &:hover { color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
            &.danger:hover { color: #ef4444; background: #fef2f2; }
          }
        }
      }
    }
  }
  .right-card {
    min-height: 520px;
    border-radius: 22px;
  }
}

.content-table {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.content-table th.el-table__cell) {
  background: var(--wt-fill-hover);
  color: var(--wt-text-secondary);
  font-weight: 600;
}

:deep(.borderless-table .el-table__inner-wrapper::before) {
  display: none;
}

:deep(.borderless-table td.el-table__cell),
:deep(.borderless-table th.el-table__cell.is-leaf) {
  border-bottom: 1px solid var(--wt-divider-faint);
}

:deep(.content-table .el-table__row:hover > td.el-table__cell) {
  background: var(--wt-row-gradient-hover) !important;
}
.upload-container {
  .avatar-uploader {
    :deep(.el-upload) {
      border: 2px dashed var(--wt-border-default);
      border-radius: 12px;
      cursor: pointer;
      overflow: hidden;
      width: 80px;
      height: 80px;
      transition: all 0.3s;
      &:hover { border-color: var(--el-color-primary); background: var(--wt-fill-hover); }
      .avatar-uploader-icon {
        font-size: 24px;
        color: var(--wt-text-secondary);
        width: 80px; height: 80px;
        line-height: 80px;
        text-align: center;
      }
      .avatar {
        width: 80px; height: 80px;
        display: block;
        object-fit: contain;
      }
    }
  }
  .upload-tip {
    font-size: 12px;
    color: var(--wt-text-secondary);
    margin-top: 8px;
  }
}
.form-tip {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--wt-text-secondary);
}
.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%; height: 100%;
  background: var(--wt-fill-hover);
  color: var(--wt-text-secondary);
  font-size: 20px;
  border-radius: 8px;
}

@media (max-width: 1200px) {
  .category-page {
    .summary-grid {
      grid-template-columns: 1fr;
    }
  }
}

</style>
