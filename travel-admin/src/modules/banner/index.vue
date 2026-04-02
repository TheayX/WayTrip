<!-- 轮播图管理页面 -->
<template>
  <div class="banner-page">
    <section class="page-hero">
      <div>
        <p class="page-kicker">Content Workspace</p>
        <h1 class="page-title">轮播图管理</h1>
        <p class="page-subtitle">维护首页轮播内容、关联景点和展示顺序。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="fetchBannerList">刷新数据</el-button>
      </div>
    </section>

    <el-card shadow="hover">
      <!-- 卡片头部 -->
      <template #header>
        <div class="card-header">
          <span>轮播图管理</span>
          <div class="header-actions">
            <el-button type="primary" @click="handleAdd">新增轮播图</el-button>
          </div>
        </div>
      </template>

      <div v-if="errorMessage" class="error-state">
        <el-result icon="error" title="轮播图管理加载失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="fetchBannerList">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <!-- 轮播图列表 -->
      <el-table v-else :data="bannerList" v-loading="loading" class="banner-table borderless-table" empty-text="当前暂无轮播图数据">
        <el-table-column label="预览" width="200">
          <template #default="{ row }">
            <el-image
              :src="getImageUrl(row.imageUrl)"
              :preview-src-list="[getImageUrl(row.imageUrl)]"
              fit="cover"
              class="banner-preview"
            />
          </template>
        </el-table-column>
        <el-table-column prop="spotName" label="关联景点" min-width="180" align="left">
          <template #default="{ row }">
            <div class="spot-name-cell">
              <template v-if="row.spotId && row.spotName">
                <el-button link type="primary" class="spot-name-link" @click="handleOpenSpot(row)">
                  {{ row.spotName }}
                </el-button>
              </template>
              <template v-else>
                <span class="text-muted">未关联景点</span>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="100" align="center" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <div class="banner-status">
              <el-tag effect="light" round :type="row.enabled === 1 ? 'success' : 'info'">
                {{ row.enabled === 1 ? '已启用' : '未启用' }}
              </el-tag>
              <el-switch
                :model-value="row.enabled === 1"
                @change="handleToggle(row)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="修改时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑轮播图' : '新增轮播图'"
      width="500px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <div class="form-section">
          <div class="form-section-head">
            <h3 class="form-section-title">基础信息</h3>
            <p class="form-section-desc">维护轮播图片、展示顺序与启用状态。</p>
          </div>

          <el-form-item label="轮播图片" prop="imageUrl">
            <div class="upload-container">
              <el-upload
                class="image-uploader"
                :action="uploadUrl"
                :headers="uploadHeaders"
                :data="{ tag: 'banner' }"
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
            <el-input-number v-model="form.sortOrder" :min="1" :max="999" />
            <div class="form-tip">默认取最后一位；如填写已有序号，系统会自动顺延后续轮播图。</div>
          </el-form-item>

          <el-form-item label="启用状态">
            <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </div>
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getBannerList, createBanner, updateBanner, deleteBanner, toggleBannerEnabled } from '@/modules/banner/api.js'
import { useUserStore } from '@/app/store/user.js'
import { isMessageBoxDismissed } from '@/shared/lib/message-box.js'
import { getAdminUploadUrl, getResourceUrl } from '@/shared/lib/resource.js'
import { fetchAllSpotOptions } from '@/modules/spot/composables/useSpotOptions.js'

const router = useRouter()
const userStore = useUserStore()

// 上传相关配置
const uploadUrl = computed(() => getAdminUploadUrl('image'))
const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${userStore.token}`
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
const loading = ref(false)
const bannerList = ref([])
const spotList = ref([])
const errorMessage = ref('')

// 对话框与表单状态
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const currentId = ref(null)

const form = reactive({
  imageUrl: '',
  spotId: null,
  sortOrder: 1,
  enabled: 1
})

// 默认排序取当前轮播图列表最后一位，减少手工维护成本。
const getNextSortOrder = () => {
  const maxSortOrder = bannerList.value.reduce((max, item) => Math.max(max, Number(item.sortOrder) || 0), 0)
  return maxSortOrder + 1
}

// 表单校验规则
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

// 加载轮播图列表
const fetchBannerList = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getBannerList()
    bannerList.value = res.data.list || []
  } catch (e) {
    bannerList.value = []
    errorMessage.value = e?.response?.data?.message || e?.message || '请稍后重试或检查接口返回。'
  } finally {
    loading.value = false
  }
}

// 加载景点选项
const fetchSpotList = async () => {
  try {
    spotList.value = await fetchAllSpotOptions()
  } catch (e) {
    console.error('获取景点列表失败', e)
  }
}

// 新增轮播图
const handleAdd = () => {
  isEdit.value = false
  currentId.value = null
  Object.assign(form, {
    imageUrl: '',
    spotId: null,
    sortOrder: getNextSortOrder(),
    enabled: 1
  })
  dialogVisible.value = true
}

// 编辑轮播图
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

// 提交轮播图表单
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
    if (!isMessageBoxDismissed(e)) {
      ElMessage.error('操作失败')
    }
  } finally {
    submitting.value = false
  }
}

// 切换启用状态
const handleToggle = async (row) => {
  try {
    await toggleBannerEnabled(row.id)
    ElMessage.success('状态已更新')
    fetchBannerList()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 跳转景点页，并复用景点管理页的自动定位与详情打开能力。
const handleOpenSpot = (row) => {
  router.push({
    path: '/spot',
    query: {
      keyword: row.spotName || '',
      spotId: row.spotId || ''
    }
  })
}

// 删除轮播图
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该轮播图吗？', '删除确认', {
      type: 'warning'
    })
    await deleteBanner(row.id)
    ElMessage.success('删除成功')
    fetchBannerList()
  } catch (e) {
    if (!isMessageBoxDismissed(e)) {
      ElMessage.error('删除失败')
    }
  }
}

// 页面初始化
onMounted(() => {
  fetchBannerList()
  fetchSpotList()
})
</script>

<style lang="scss" scoped>
.banner-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 4px 2px;
}

.page-kicker {
  margin: 0 0 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-title {
  margin: 0;
  color: #0f172a;
  font-size: 30px;
  line-height: 1.2;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #64748b;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions,
.table-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.error-state {
  padding: 8px 0 16px;
}

.banner-preview {
  width: 160px;
  height: 80px;
  border-radius: 8px;
}

.banner-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.text-muted {
  color: #94a3b8;
}

.banner-table {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.banner-table th.el-table__cell) {
  background: #f8fafc;
  color: #64748b;
  font-weight: 600;
}

:deep(.borderless-table .el-table__inner-wrapper::before) {
  display: none;
}

:deep(.borderless-table td.el-table__cell),
:deep(.borderless-table th.el-table__cell.is-leaf) {
  border-bottom: 1px solid #f8fafc;
}

:deep(.banner-table .el-table__row:hover > td.el-table__cell) {
  background: linear-gradient(90deg, rgba(248, 250, 252, 0.5) 0%, #f1f5f9 50%, rgba(248, 250, 252, 0.5) 100%) !important;
}

.form-section {
  padding: 4px 4px 0;
}

.form-section-head {
  margin-bottom: 16px;
}

.form-section-title {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.form-section-desc {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.upload-container {
  .image-uploader {
    :deep(.el-upload) {
      border: 2px dashed #e2e8f0;
      border-radius: 12px;
      cursor: pointer;
      overflow: hidden;
      transition: all 0.3s;
      &:hover { border-color: var(--el-color-primary); background: #f8fafc; }
    }
  }
  .uploaded-image {
    width: 200px; height: 100px;
    display: block;
    border-radius: 8px;
  }
  .upload-placeholder {
    width: 200px; height: 100px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #94a3b8;
    .el-icon { font-size: 28px; margin-bottom: 8px; }
    span { font-size: 12px; }
  }
  .upload-tip {
    font-size: 12px;
    color: #94a3b8;
    margin-top: 8px;
  }
}
.form-tip {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: #94a3b8;
}

.spot-name-cell {
  display: flex;
  align-items: center;
  min-height: 32px;
}

.spot-name-link {
  padding: 0;
  min-width: 0;
  height: auto;
  justify-content: flex-start;
  font-weight: 600;
  color: #1e293b;

  &:hover {
    color: var(--el-color-primary);
  }
}

:deep(.spot-name-link.el-button.is-link) {
  padding-left: 0;
  padding-right: 0;
}

:deep(.spot-name-link .el-button__text) {
  text-align: left;
}

@media (max-width: 960px) {
  .page-hero {
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
  }

  .hero-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
