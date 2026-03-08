<template>
  <div class="category-page">
    <el-row :gutter="20">
      <!-- 左侧：一级分类 -->
      <el-col :span="6">
        <el-card shadow="never" class="left-card">
          <template #header>
            <div class="card-header">
              <span>一级分类</span>
              <el-button type="primary" link icon="Plus" @click="handleAddLevel1">新增</el-button>
            </div>
          </template>
          
          <ul class="parent-list" v-loading="loading1">
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
            <el-empty v-if="level1List.length === 0" description="暂无一级分类" :image-size="60" />
          </ul>
        </el-card>
      </el-col>

      <!-- 右侧：二级分类 -->
      <el-col :span="18">
        <el-card shadow="never" class="right-card">
          <template #header>
            <div class="card-header">
              <span>二级分类管理</span>
              <el-button type="primary" @click="handleAddLevel2" :disabled="!activeParentId">新增二级分类</el-button>
            </div>
          </template>

          <el-table :data="level2List" v-loading="loading2" stripe>
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
                <el-button type="primary" link @click="handleEditLevel2(row)">编辑</el-button>
                <el-button type="danger" link @click="handleDeleteLevel2(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="请选择左侧一级分类，或该分类下暂无子分类" />
            </template>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 弹窗 -->
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
import { getCategories, createCategory, updateCategory, deleteCategory } from '@/api/category'
import { useUserStore } from '@/stores/user'

const BASE_URL = 'http://localhost:8080'
const userStore = useUserStore()

// 上传配置
const uploadUrl = computed(() => `${BASE_URL}/api/admin/v1/upload/icon`)
const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${userStore.token}`
}))

// 获取完整图片URL
const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return BASE_URL + url
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

// 状态
const loading1 = ref(false)
const loading2 = ref(false)
const level1List = ref([])
const level2List = ref([])
const activeParentId = ref(null)

// 弹窗
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

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const dialogTitle = computed(() => {
  const levelStr = isLevel2.value ? '二级分类' : '一级分类'
  return isEdit.value ? `编辑${levelStr}` : `新增${levelStr}`
})

// 加载一级分类
const fetchLevel1 = async () => {
  loading1.value = true
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
    console.error('获取一级分类失败', e)
  } finally {
    loading1.value = false
  }
}

// 选中一级分类
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
    console.error('获取二级分类失败', e)
  } finally {
    loading2.value = false
  }
}

// 一级分类操作
const handleAddLevel1 = () => {
  isEdit.value = false
  isLevel2.value = false
  currentId.value = null
  Object.assign(form, { name: '', parentId: 0, iconUrl: '', sortOrder: 1 })
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
    await ElMessageBox.confirm('确定要删除该一级分类吗？如有子分类请先删除子分类。', '提示', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    if (activeParentId.value === row.id) {
      activeParentId.value = null
    }
    fetchLevel1()
  } catch (e) {}
}

// 二级分类操作
const handleAddLevel2 = () => {
  isEdit.value = false
  isLevel2.value = true
  currentId.value = null
  Object.assign(form, { name: '', parentId: activeParentId.value, iconUrl: '', sortOrder: 1 })
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
    await ElMessageBox.confirm('确定要删除该二级分类吗？', '提示', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    fetchLevel2(activeParentId.value)
  } catch (e) {}
}

// 图片上传
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

// 提交表单
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

onMounted(() => {
  fetchLevel1()
})
</script>

<style lang="scss" scoped>
.category-page {
  .left-card {
    min-height: 500px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .parent-list {
      list-style: none;
      padding: 0;
      margin: 0;
      
      .list-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 16px;
        margin-bottom: 8px;
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.3s;
        border: 1px solid transparent;
        
        &:hover {
          background-color: #f5f7fa;
          .item-actions {
            opacity: 1;
          }
        }
        
        &.active {
          background-color: #ecf5ff;
          color: #409eff;
          border-color: #b3d8ff;
          font-weight: bold;
        }

        .item-actions {
          opacity: 0;
          transition: opacity 0.3s;
          display: flex;
          gap: 12px;
          
          .action-icon {
            color: #909399;
            font-size: 16px;
            
            &:hover {
              color: #409eff;
            }
            &.danger:hover {
              color: #f56c6c;
            }
          }
        }
      }
    }
  }

  .right-card {
    min-height: 500px;
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
}

.upload-container {
  .avatar-uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      width: 80px;
      height: 80px;
      
      &:hover {
        border-color: #409eff;
      }
      
      .avatar-uploader-icon {
        font-size: 28px;
        color: #8c939d;
        width: 80px;
        height: 80px;
        line-height: 80px;
        text-align: center;
      }
      
      .avatar {
        width: 80px;
        height: 80px;
        display: block;
        object-fit: contain;
      }
    }
  }
  .upload-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 5px;
  }
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 20px;
}
</style>
