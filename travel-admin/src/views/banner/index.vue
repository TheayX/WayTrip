<template>
  <div class="banner-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>轮播图管理</span>
          <el-button type="primary" @click="handleAdd">新增轮播图</el-button>
        </div>
      </template>

      <!-- 轮播图列表 -->
      <el-table :data="bannerList" v-loading="loading" stripe>
        <el-table-column label="预览" width="200">
          <template #default="{ row }">
            <el-image 
              :src="getImageUrl(row.imageUrl)" 
              :preview-src-list="[getImageUrl(row.imageUrl)]"
              fit="cover"
              style="width: 160px; height: 80px; border-radius: 4px;"
            />
          </template>
        </el-table-column>
        <el-table-column prop="spotName" label="关联景点" min-width="150">
          <template #default="{ row }">
            {{ row.spotName || '无' }}
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch 
              :model-value="row.enabled === 1"
              @change="handleToggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="修改时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑轮播图' : '新增轮播图'"
      width="500px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="轮播图片" prop="imageUrl">
          <div class="upload-container">
            <el-upload
              class="image-uploader"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-image 
                v-if="form.imageUrl" 
                :src="getImageUrl(form.imageUrl)" 
                fit="cover"
                class="uploaded-image"
              />
              <div v-else class="upload-placeholder">
                <el-icon><Plus /></el-icon>
                <span>点击上传</span>
              </div>
            </el-upload>
            <div class="upload-tip">支持 jpg、png 格式，大小不超过 5MB</div>
          </div>
        </el-form-item>
        <el-form-item label="关联景点">
          <el-select 
            v-model="form.spotId" 
            placeholder="请选择景点（可选）" 
            clearable
            filterable
            style="width: 100%"
          >
            <el-option 
              v-for="spot in spotList" 
              :key="spot.id" 
              :label="spot.name" 
              :value="spot.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getBannerList, createBanner, updateBanner, deleteBanner, toggleBannerEnabled } from '@/api/banner'
import { getSpotList } from '@/api/spot'

const BASE_URL = 'http://localhost:8080'

// 上传配置
const uploadUrl = computed(() => `${BASE_URL}/api/admin/v1/upload/image`)
const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${localStorage.getItem('admin_token')}`
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

// 列表数据
const loading = ref(false)
const bannerList = ref([])
const spotList = ref([])

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const currentId = ref(null)

const form = reactive({
  imageUrl: '',
  spotId: null,
  sortOrder: 0,
  enabled: 1
})

const rules = {
  imageUrl: [{ required: true, message: '请上传轮播图片', trigger: 'change' }]
}

// 上传前校验
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

// 上传成功
const handleUploadSuccess = (response) => {
  if (response.code === 0) {
    form.imageUrl = response.data.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 上传失败
const handleUploadError = () => {
  ElMessage.error('上传失败，请重试')
}

// 获取轮播图列表
const fetchBannerList = async () => {
  loading.value = true
  try {
    const res = await getBannerList()
    bannerList.value = res.data.list || []
  } catch (e) {
    console.error('获取轮播图列表失败', e)
  } finally {
    loading.value = false
  }
}

// 获取景点列表
const fetchSpotList = async () => {
  try {
    const res = await getSpotList({ pageSize: 100 })
    spotList.value = res.data.list || []
  } catch (e) {
    console.error('获取景点列表失败', e)
  }
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  currentId.value = null
  Object.assign(form, {
    imageUrl: '',
    spotId: null,
    sortOrder: 0,
    enabled: 1
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  currentId.value = row.id
  Object.assign(form, {
    imageUrl: row.imageUrl,
    spotId: row.spotId,
    sortOrder: row.sortOrder,
    enabled: row.enabled
  })
  dialogVisible.value = true
}

// 提交
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await updateBanner(currentId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createBanner(form)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    fetchBannerList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  } finally {
    submitting.value = false
  }
}

// 切换状态
const handleToggle = async (row) => {
  try {
    await toggleBannerEnabled(row.id)
    ElMessage.success('状态已更新')
    fetchBannerList()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该轮播图吗？', '提示', {
      type: 'warning'
    })
    await deleteBanner(row.id)
    ElMessage.success('删除成功')
    fetchBannerList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchBannerList()
  fetchSpotList()
})
</script>

<style lang="scss" scoped>
.banner-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.upload-container {
  .image-uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: border-color 0.3s;

      &:hover {
        border-color: #409eff;
      }
    }
  }

  .uploaded-image {
    width: 200px;
    height: 100px;
    display: block;
  }

  .upload-placeholder {
    width: 200px;
    height: 100px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #8c939d;
    
    .el-icon {
      font-size: 28px;
      margin-bottom: 8px;
    }

    span {
      font-size: 12px;
    }
  }

  .upload-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
  }
}
</style>
