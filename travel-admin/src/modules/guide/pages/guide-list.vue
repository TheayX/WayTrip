<!-- 攻略管理页面 -->
<template>
  <div class="guide-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">攻略内容管理</p>
        <h1 class="page-title">攻略管理</h1>
        <p class="page-subtitle">维护攻略内容、发布状态与关联景点。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="loadData">刷新数据</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">当前结果</div>
        <div class="summary-value">{{ total }}</div>
        <div class="summary-desc">符合筛选条件的攻略数量</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">已发布</div>
        <div class="summary-value">{{ publishedCount }}</div>
        <div class="summary-desc">正在前台展示的攻略内容</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">待处理</div>
        <div class="summary-value">{{ unpublishedCount }}</div>
        <div class="summary-desc">仍需完善或尚未发布的攻略</div>
      </el-card>
    </section>

    <el-card shadow="hover" class="management-card">
      <template #header>
        <div class="card-header">
          <span>攻略列表</span>
          <div class="header-actions">
            <el-button type="primary" @click="handleAdd">新增攻略</el-button>
          </div>
        </div>
      </template>

      <GuideFilterBar
        :query-params="queryParams"
        :ui-filters="uiFilters"
        :categories="categories"
        @search="handleSearch"
        @reset="handleReset"
        @filter-change="handleFilterChange"
      />

      <div v-if="errorMessage" class="error-state page-error-state">
        <el-result icon="error" title="攻略管理加载失败" :sub-title="errorMessage">
          <template #extra>
            <el-button type="primary" @click="loadData">重新加载</el-button>
          </template>
        </el-result>
      </div>

      <GuideTable
        v-else
        :table-data="tableData"
        :loading="loading"
        :get-image-url="getImageUrl"
        :format-date="formatDate"
        :get-row-class-name="getRowClassName"
        @selection-change="handleSelectionChange"
        @view="handleView"
        @edit="handleEdit"
        @toggle-publish="handleTogglePublish"
        @delete="handleDelete"
      />

      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <GuideFormDrawer
      ref="formDrawerRef"
      v-model:visible="dialogVisible"
      :edit-id="editId"
      :form="form"
      :rules="rules"
      :categories="categories"
      :merged-spot-options="mergedSpotOptions"
      :upload-url="uploadUrl"
      :upload-headers="uploadHeaders"
      :upload-data="uploadData"
      :before-upload="beforeUpload"
      :handle-upload-success="handleUploadSuccess"
      :handle-upload-error="handleUploadError"
      :get-image-url="getImageUrl"
      :submitting="submitting"
      @submit="handleSubmit"
    />

    <GuideDetailDrawer
      v-model:visible="drawerVisible"
      :detail="guideDetail"
      :get-image-url="getImageUrl"
      :format-date="formatDate"
    />

    <transition name="el-zoom-in-bottom">
      <div v-show="selectedGuides.length > 0" class="floating-action-bar">
        <div class="floating-action-summary">
          已选择 <span class="text-primary font-bold px-1" style="color: var(--el-color-primary)">{{ selectedGuides.length }}</span> 项
        </div>
        <div class="floating-action-actions">
          <el-button type="success" size="small" @click="handleBatchPublish(true)">批量发布</el-button>
          <el-button type="warning" size="small" @click="handleBatchPublish(false)">批量下架</el-button>
          <el-button type="danger" size="small" @click="handleBatchDelete">批量删除</el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import GuideFilterBar from '@/modules/guide/components/GuideFilterBar.vue'
import GuideTable from '@/modules/guide/components/GuideTable.vue'
import GuideFormDrawer from '@/modules/guide/components/GuideFormDrawer.vue'
import GuideDetailDrawer from '@/modules/guide/components/GuideDetailDrawer.vue'
import {
  createGuide,
  deleteGuide,
  getCategories,
  getGuideDetail,
  getGuideList,
  updateGuide,
  updatePublishStatus
} from '@/modules/guide/api.js'
import { useUserStore } from '@/app/store/user.js'
import { getAdminUploadUrl, getResourceUrl } from '@/shared/lib/resource.js'
import { fetchAllSpotOptions } from '@/modules/spot/composables/useSpotOptions.js'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 上传相关配置
const uploadUrl = computed(() => getAdminUploadUrl('image'))
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))
const uploadData = computed(() => ({
  tag: form.title || ''
}))

// 补全图片访问地址
const getImageUrl = (url) => {
  return getResourceUrl(url)
}

// 上传前校验
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

// 上传成功后回填封面，保持抽屉内即时可见。
const handleUploadSuccess = (response) => {
  if (response.code === 0) {
    form.coverImage = response.data.url
    ElMessage.success('上传成功')
    return
  }
  ElMessage.error(response.message || '上传失败')
}

const handleUploadError = () => {
  ElMessage.error('上传失败，请重试')
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const categories = ref([])
const spotList = ref([])
const spotOptions = ref([])
const errorMessage = ref('')
const selectedGuides = ref([])
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const submitting = ref(false)
const editId = ref(null)
const activeGuideId = ref(null)
const autoOpenedGuideId = ref(null)
const guideDetail = ref(null)
const formDrawerRef = ref()
const skipNextRouteLoad = ref(false)

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  category: '',
  published: null
})

const uiFilters = reactive({
  published: ''
})

const form = reactive({
  title: '',
  category: '',
  coverImage: '',
  content: '',
  published: false,
  spotIds: []
})

const rules = {
  title: [{ required: true, message: '请输入攻略标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择或输入攻略分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入攻略内容', trigger: 'blur' }]
}
const publishedCount = computed(() => tableData.value.filter((item) => item.published).length)
const unpublishedCount = computed(() => tableData.value.filter((item) => !item.published).length)

// 合并列表景点和编辑回显景点，避免旧关联缺失时无法展示。
const mergedSpotOptions = computed(() => {
  const map = new Map()
  spotList.value.forEach((spot) => {
    map.set(spot.id, {
      id: spot.id,
      name: spot.name,
      published: spot.published,
      isDeleted: spot.isDeleted
    })
  })
  spotOptions.value.forEach((spot) => {
    if (!map.has(spot.id)) {
      map.set(spot.id, spot)
    }
  })
  return Array.from(map.values())
})

onMounted(async () => {
  applyRouteQuery()
  await loadCategories()
  await loadSpots()
  await loadData()
})

const loadCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (e) {}
}

const loadSpots = async () => {
  try {
    spotList.value = await fetchAllSpotOptions()
  } catch (e) {}
}

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getGuideList(queryParams)
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
    await openGuideFromRoute()
  } catch (error) {
    tableData.value = []
    total.value = 0
    errorMessage.value = error?.response?.data?.message || error?.message || '请稍后重试或检查接口返回。'
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  queryParams.published = uiFilters.published === '' || uiFilters.published == null
    ? null
    : Number(uiFilters.published)
  syncRouteQuery()
  loadData()
}

const handleFilterChange = () => {
  handleSearch()
}

const handleReset = () => {
  queryParams.keyword = ''
  queryParams.category = ''
  queryParams.published = null
  uiFilters.published = ''
  handleSearch()
}

const syncRouteQuery = (guideId = activeGuideId.value) => {
  const nextQuery = {}
  if (queryParams.keyword) {
    nextQuery.keyword = queryParams.keyword
  }
  if (guideId) {
    nextQuery.guideId = String(guideId)
  }
  const currentQuery = {}
  if (typeof route.query.keyword === 'string' && route.query.keyword) {
    currentQuery.keyword = route.query.keyword
  }
  if (typeof route.query.guideId === 'string' && route.query.guideId) {
    currentQuery.guideId = route.query.guideId
  }
  const changed = JSON.stringify(currentQuery) !== JSON.stringify(nextQuery)
  if (changed) {
    skipNextRouteLoad.value = true
    router.replace({ path: route.path, query: nextQuery })
  }
  return changed
}

const normalizeRouteGuideId = (value) => {
  const guideId = Number(value)
  return Number.isInteger(guideId) && guideId > 0 ? guideId : null
}

const applyRouteQuery = () => {
  queryParams.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  const nextGuideId = normalizeRouteGuideId(route.query.guideId)
  if (nextGuideId !== activeGuideId.value) {
    autoOpenedGuideId.value = null
  }
  activeGuideId.value = nextGuideId
}

const getRowClassName = ({ row }) => {
  return Number(row.id) === activeGuideId.value ? 'guide-highlight-row' : ''
}

const resetForm = () => {
  Object.assign(form, {
    title: '',
    category: '',
    coverImage: '',
    content: '',
    published: false,
    spotIds: []
  })
  spotOptions.value = []
}

const handleAdd = () => {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  editId.value = row.id
  try {
    const res = await getGuideDetail(row.id)
    Object.assign(form, {
      title: res.data.title,
      category: res.data.category,
      coverImage: res.data.coverImage,
      content: res.data.content,
      published: res.data.published,
      spotIds: Array.isArray(res.data.spotIds) ? [...res.data.spotIds] : []
    })
    spotOptions.value = res.data.spotOptions || []
    dialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取攻略详情失败')
  }
}

// 详情抽屉复用详情接口，并补齐列表态字段供展示使用。
const openGuideDetail = async (row) => {
  const res = await getGuideDetail(row.id)
  guideDetail.value = {
    ...res.data,
    id: row.id,
    title: row.title,
    category: res.data.category || row.category,
    coverImage: res.data.coverImage || row.coverImage,
    published: res.data.published ?? row.published,
    viewCount: row.viewCount,
    createdAt: row.createdAt,
    updatedAt: row.updatedAt,
    spotOptions: res.data.spotOptions || []
  }
  drawerVisible.value = true
}

const handleView = async (row) => {
  try {
    activeGuideId.value = row.id
    syncRouteQuery(row.id)
    await openGuideDetail(row)
  } catch (e) {
    ElMessage.error('无法获取攻略详情')
  }
}

const openGuideFromRoute = async () => {
  if (!activeGuideId.value || autoOpenedGuideId.value === activeGuideId.value) {
    return
  }
  const targetRow = tableData.value.find((item) => Number(item.id) === activeGuideId.value)
  if (!targetRow) {
    return
  }
  autoOpenedGuideId.value = activeGuideId.value
  try {
    await openGuideDetail(targetRow)
  } catch (e) {
    autoOpenedGuideId.value = null
  }
}

const buildSubmitPayload = () => ({
  title: form.title,
  category: form.category,
  coverImage: form.coverImage,
  content: form.content,
  published: form.published,
  spotIds: Array.isArray(form.spotIds) ? form.spotIds : []
})

const handleSubmit = async (options = {}) => {
  if (typeof options.published === 'boolean') {
    form.published = options.published
  }
  await formDrawerRef.value?.validate()
  submitting.value = true
  try {
    if (editId.value) {
      await updateGuide(editId.value, buildSubmitPayload())
      ElMessage.success('更新成功')
    } else {
      await createGuide(buildSubmitPayload())
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadData()
    await loadCategories()
  } finally {
    submitting.value = false
  }
}

const handleTogglePublish = async (row) => {
  const action = row.published ? '下架' : '发布'
  await ElMessageBox.confirm(`确定要${action}该攻略吗？`, '状态确认', { type: 'warning' })
  await updatePublishStatus(row.id, !row.published)
  ElMessage.success(`${action}成功`)
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该攻略吗？', '删除确认', { type: 'warning' })
  await deleteGuide(row.id)
  ElMessage.success('删除成功')
  if (activeGuideId.value === row.id) {
    activeGuideId.value = null
    guideDetail.value = null
    drawerVisible.value = false
    syncRouteQuery(null)
  }
  loadData()
}

const handleSelectionChange = (selection) => {
  selectedGuides.value = selection
}

// 批量操作统一汇总执行结果，避免逐条串行导致等待时间过长且失败反馈不清晰。
const runBatchAction = async ({ rows, requestFactory, successMessage, afterSuccess }) => {
  if (!rows.length) {
    return
  }

  loading.value = true
  try {
    const results = await Promise.allSettled(rows.map((item) => requestFactory(item)))
    const successCount = results.filter((item) => item.status === 'fulfilled').length
    const failedCount = results.length - successCount

    if (failedCount === 0) {
      ElMessage.success(successMessage)
    } else if (successCount > 0) {
      ElMessage.warning(`已成功 ${successCount} 项，失败 ${failedCount} 项`)
    } else {
      ElMessage.error('批量操作失败')
    }

    if (typeof afterSuccess === 'function') {
      afterSuccess()
    }

    selectedGuides.value = []
    await loadData()
  } finally {
    loading.value = false
  }
}

const handleBatchPublish = async (status) => {
  if (!selectedGuides.value.length) return
  const action = status ? '发布' : '下架'
  await ElMessageBox.confirm(`确定要批量${action}选中的 ${selectedGuides.value.length} 篇攻略吗？`, '状态确认', { type: 'warning' })
  const targetRows = selectedGuides.value.filter((item) => item.published !== status)
  if (!targetRows.length) {
    ElMessage.info(`选中攻略均已${status ? '发布' : '下架'}`)
    return
  }
  await runBatchAction({
    rows: targetRows,
    requestFactory: (item) => updatePublishStatus(item.id, status),
    successMessage: `批量${action}成功`
  })
}

const handleBatchDelete = async () => {
  if (!selectedGuides.value.length) return
  await ElMessageBox.confirm(`确定要批量删除选中的 ${selectedGuides.value.length} 篇攻略吗？(此操作不可恢复)`, '删除确认', { type: 'error' })
  await runBatchAction({
    rows: selectedGuides.value,
    requestFactory: (item) => deleteGuide(item.id),
    successMessage: '批量删除成功',
    afterSuccess: () => {
      if (selectedGuides.value.some((item) => item.id === activeGuideId.value)) {
        activeGuideId.value = null
        guideDetail.value = null
        drawerVisible.value = false
        syncRouteQuery(null)
      }
    }
  })
}

watch(
  () => drawerVisible.value,
  (visible) => {
    if (!visible) {
      activeGuideId.value = null
      guideDetail.value = null
      syncRouteQuery(null)
    }
  }
)

watch(
  () => [route.query.keyword, route.query.guideId],
  () => {
    applyRouteQuery()
    if (skipNextRouteLoad.value) {
      skipNextRouteLoad.value = false
      return
    }
    loadData()
  }
)
</script>

<style lang="scss" scoped>
.guide-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  border-radius: 20px;
}

.summary-label {
  font-size: 13px;
  color: var(--wt-text-secondary);
  font-weight: 600;
}

.summary-value {
  margin-top: 8px;
  font-size: 28px;
  line-height: 1.1;
  font-weight: 700;
  color: var(--wt-text-primary);
}

.summary-desc {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--wt-text-regular);
}

.management-card {
  border-radius: 22px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.pagination {
  justify-content: flex-end;
}

.floating-action-bar {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  align-items: center;
  border-radius: 999px;
  overflow: hidden;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(18px);
}

.floating-action-summary {
  background: var(--wt-text-primary);
  color: var(--wt-surface-elevated);
  padding: 12px 16px;
  font-weight: 600;
}

.floating-action-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--wt-surface-elevated);
}

:deep(.guide-highlight-row) {
  --el-table-tr-bg-color: var(--el-color-primary-light-9);

  td {
    background-color: var(--el-color-primary-light-9) !important;
  }
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .hero-actions {
    width: 100%;
  }

  .hero-actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
