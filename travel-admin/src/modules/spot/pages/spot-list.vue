<!-- 景点管理页面 -->
<template>
  <div class="spot-page admin-page-shell">
    <section class="page-hero">
      <div>
        <p class="page-kicker">景点内容管理</p>
        <h1 class="page-title">景点管理</h1>
        <p class="page-subtitle">维护景点信息、上架状态与展示质量。</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="loading" @click="loadData">刷新数据</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">当前结果</div>
        <div class="summary-value">{{ total }}</div>
        <div class="summary-desc">符合筛选条件的景点数量</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">已上架</div>
        <div class="summary-value">{{ publishedCount }}</div>
        <div class="summary-desc">当前列表中正在展示的景点</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-label">待处理</div>
        <div class="summary-value">{{ unpublishedCount }}</div>
        <div class="summary-desc">尚未上架或待继续完善的内容</div>
      </el-card>
    </section>

    <el-card shadow="hover" class="management-card">
      <!-- 卡片头部 -->
      <template #header>
        <div class="card-header">
          <span>景点管理</span>
          <div class="header-actions">
            <el-button @click="handleRefreshAllRatings" :loading="refreshingAllRatings">同步全部评分</el-button>
            <el-button @click="handleRefreshAllHeats" :loading="refreshingAllHeats">同步全部热度分数</el-button>
            <el-button type="primary" @click="handleAdd">新增景点</el-button>
          </div>
        </div>
      </template>

      <SpotFilterBar
        :query-params="queryParams"
        :ui-filters="uiFilters"
        :region-cascader-options="regionCascaderOptions"
        :category-cascader-options="categoryCascaderOptions"
        :region-cascader-props="regionCascaderProps"
        :category-cascader-props="categoryCascaderProps"
        @search="handleSearch"
        @reset="handleReset"
        @filter-change="handleFilterChange"
      />

      <SpotTable
        :table-data="tableData"
        :loading="loading"
        :get-row-class-name="getRowClassName"
        :get-image-url="getImageUrl"
        :format-date="formatDate"
        :get-heat-level-label="getHeatLevelLabel"
        :get-heat-level-tag-type="getHeatLevelTagType"
        @selection-change="handleSelectionChange"
        @view="handleView"
        @edit="handleEdit"
        @heat-edit="handleHeatEdit"
        @refresh-heat="handleRefreshSpotHeat"
        @refresh-rating="handleRefreshSpotRating"
        @toggle-publish="handleTogglePublish"
        @delete="handleDelete"
      />

      <!-- 分页器 -->
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
        class="pagination"
      />
    </el-card>

    <SpotFormDialog
      ref="formDialogRef"
      v-model:visible="dialogVisible"
      :edit-id="editId"
      :form="form"
      :rules="rules"
      :region-cascader-options="regionCascaderOptions"
      :region-cascader-props="regionCascaderProps"
      :parent-category-options="parentCategoryOptions"
      :child-category-options="childCategoryOptions"
      :heat-level-options="heatLevelOptions"
      :upload-url="uploadUrl"
      :upload-headers="uploadHeaders"
      :cover-upload-data="coverUploadData"
      :gallery-upload-data="galleryUploadData"
      :before-upload="beforeUpload"
      :handle-upload-success="handleUploadSuccess"
      :handle-gallery-upload-success="handleGalleryUploadSuccess"
      :handle-upload-error="handleUploadError"
      :get-image-url="getImageUrl"
      :submitting="submitting"
      @submit="handleSubmit"
      @parent-category-change="handleParentCategoryChange"
      @remove-gallery-image="removeGalleryImage"
    />

    <SpotHeatDialog
      ref="heatDialogRef"
      v-model:visible="heatDialogVisible"
      :heat-form="heatForm"
      :heat-rules="heatRules"
      :heat-level-options="heatLevelOptions"
      :heat-submitting="heatSubmitting"
      @submit="handleHeatSubmit"
    />

    <SpotDetailDrawer
      v-model:visible="drawerVisible"
      :detail="spotDetail"
      :get-image-url="getImageUrl"
      :format-date="formatDate"
    />

    <!-- Batch Floating Action Bar -->
    <transition name="el-zoom-in-bottom">
      <div v-show="selectedSpots.length > 0" class="floating-action-bar">
        <div class="floating-action-summary">
          已选择 <span class="text-primary font-bold px-1" style="color: var(--el-color-primary)">{{ selectedSpots.length }}</span> 项
        </div>
        <div class="floating-action-actions">
          <el-button type="success" size="small" @click="handleBatchPublish(true)">批量上架</el-button>
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
import SpotFilterBar from '@/modules/spot/components/SpotFilterBar.vue'
import SpotTable from '@/modules/spot/components/SpotTable.vue'
import SpotFormDialog from '@/modules/spot/components/SpotFormDialog.vue'
import SpotHeatDialog from '@/modules/spot/components/SpotHeatDialog.vue'
import SpotDetailDrawer from '@/modules/spot/components/SpotDetailDrawer.vue'
import {
  createSpot,
  deleteSpot,
  getFilters,
  getSpotDetail,
  getSpotList,
  refreshAllSpotHeat,
  refreshAllSpotRatings,
  refreshSpotHeat,
  refreshSpotRating,
  updatePublishStatus,
  updateSpot
} from '@/modules/spot/api.js'
import { useUserStore } from '@/app/store/user.js'
import { getAdminUploadUrl, getResourceUrl } from '@/shared/lib/resource.js'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 上传相关配置
const uploadUrl = computed(() => getAdminUploadUrl('image'))
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))
const coverUploadData = computed(() => ({
  scene: 'spot',
  assetType: 'cover',
  name: form.name || ''
}))
const galleryUploadData = computed(() => ({
  scene: 'spot',
  assetType: 'gallery',
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

// 热度档位选项
const heatLevelOptions = [
  { value: 0, label: '普通' },
  { value: 1, label: '推荐' },
  { value: 2, label: '重点推荐' },
  { value: 3, label: '强推' }
]

const getHeatLevelLabel = (level) => {
  return heatLevelOptions.find((item) => item.value === Number(level))?.label || '普通'
}

const getHeatLevelTagType = (level) => {
  switch (Number(level)) {
    case 1:
      return 'success'
    case 2:
      return 'warning'
    case 3:
      return 'danger'
    default:
      return 'info'
  }
}

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

const handleUploadSuccess = (response) => {
  if (response.code === 0) {
    form.coverImage = response.data.url
    ElMessage.success('上传成功')
    return
  }
  ElMessage.error(response.message || '上传失败')
}

const handleGalleryUploadSuccess = (response) => {
  if (response.code === 0) {
    if (!Array.isArray(form.images)) {
      form.images = []
    }
    form.images.push(response.data.url)
    ElMessage.success('上传成功')
    return
  }
  ElMessage.error(response.message || '上传失败')
}

const removeGalleryImage = (index) => {
  if (!Array.isArray(form.images)) return
  form.images.splice(index, 1)
}

const handleUploadError = () => {
  ElMessage.error('上传失败，请重试')
}

const loading = ref(false)
const refreshingAllRatings = ref(false)
const refreshingAllHeats = ref(false)
const tableData = ref([])
const total = ref(0)
const activeSpotId = ref(null)
const autoOpenedSpotId = ref(null)
const regions = ref([])
const regionTree = ref([])
const categories = ref([])
const categoryTree = ref([])

// 扁平化分类树，便于建立父子映射
const flattenCategories = (nodes = [], level = 0) => {
  return nodes.reduce((acc, node) => {
    const hasChildren = Array.isArray(node.children) && node.children.length > 0
    acc.push({
      id: node.id,
      name: node.name,
      parentId: node.parentId,
      label: `${'  '.repeat(level)}${level > 0 ? '└ ' : ''}${node.name}`,
      hasChildren
    })
    if (hasChildren) {
      acc.push(...flattenCategories(node.children, level + 1))
    }
    return acc
  }, [])
}

const categoryOptions = computed(() => flattenCategories(categoryTree.value))
const leafCategoryOptions = computed(() => categoryOptions.value.filter((item) => !item.hasChildren))
const categoryCascaderOptions = computed(() => categoryTree.value)
const regionCascaderOptions = computed(() => {
  if (regionTree.value.length) {
    return regionTree.value
  }
  return regions.value.map((item) => ({ ...item, children: [] }))
})
const regionCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: true
}
const categoryCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: true
}
const parentCategoryOptions = computed(() => categoryTree.value.filter((item) => item.children?.length))
const childCategoryOptions = computed(() => {
  if (!form.parentCategoryId) {
    return []
  }
  const parent = categoryTree.value.find((item) => item.id === form.parentCategoryId)
  return parent?.children || []
})
const categoryParentMap = computed(() => {
  return leafCategoryOptions.value.reduce((acc, item) => {
    acc[item.id] = item.parentId || null
    return acc
  }, {})
})
const publishedCount = computed(() => tableData.value.filter((item) => item.published).length)
const unpublishedCount = computed(() => tableData.value.filter((item) => !item.published).length)

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  regionId: null,
  categoryId: null,
  published: null
})
const uiFilters = reactive({
  regionPath: [],
  categoryPath: [],
  published: ''
})

// 对话框与表单状态
const dialogVisible = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formDialogRef = ref()
const heatDialogVisible = ref(false)
const heatSubmitting = ref(false)
const heatDialogRef = ref()
const heatEditId = ref(null)
const heatSpotDetail = ref(null)

const selectedSpots = ref([])
const drawerVisible = ref(false)
const spotDetail = ref(null)
const skipNextRouteLoad = ref(false)

// 景点编辑表单
const form = reactive({
  name: '',
  price: 0,
  regionId: null,
  regionPath: [],
  parentCategoryId: null,
  categoryId: null,
  address: '',
  latitude: null,
  longitude: null,
  openTime: '',
  heatLevel: 0,
  description: '',
  coverImage: '',
  images: [],
  published: false
})

const rules = {
  name: [{ required: true, message: '请输入景点名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  regionPath: [{ required: true, message: '请选择地区', trigger: 'change' }],
  parentCategoryId: [{ required: true, message: '请选择父分类', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择子分类', trigger: 'change' }],
  heatLevel: [{ required: true, message: '请选择热度档位', trigger: 'change' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  coverImage: [{ required: true, message: '请上传封面图', trigger: 'change' }]
}

const heatForm = reactive({
  avgRating: 0,
  ratingCount: 0,
  heatLevel: 0,
  heatScore: 0
})

const heatRules = {
  heatLevel: [{ required: true, message: '请选择热度档位', trigger: 'change' }]
}

// 页面初始化
onMounted(async () => {
  applyRouteQuery()
  await loadFilters()
  await loadData()
})

// 加载筛选项
const loadFilters = async () => {
  try {
    const res = await getFilters()
    regions.value = res.data.regions || []
    regionTree.value = res.data.regionTree?.length ? res.data.regionTree : []
    categories.value = res.data.categories || []
    categoryTree.value = res.data.categoryTree?.length ? res.data.categoryTree : categories.value
  } catch (e) {}
}

// 将界面筛选值同步到查询参数
const syncFilters = () => {
  const selectedRegionId = uiFilters.regionPath?.length
    ? uiFilters.regionPath[uiFilters.regionPath.length - 1]
    : null
  const selectedCategoryId = uiFilters.categoryPath?.length
    ? uiFilters.categoryPath[uiFilters.categoryPath.length - 1]
    : null
  queryParams.regionId = selectedRegionId ? Number(selectedRegionId) : null
  queryParams.categoryId = selectedCategoryId ? Number(selectedCategoryId) : null
  queryParams.published = uiFilters.published === '' || uiFilters.published == null
    ? null
    : Number(uiFilters.published)
}

// 根据节点 ID 查找级联路径
const findPathById = (targetId, tree) => {
  if (!targetId || !Array.isArray(tree) || !tree.length) {
    return []
  }
  const stack = tree.map((node) => ({ node, path: [node.id] }))
  while (stack.length) {
    const current = stack.pop()
    if (!current) continue
    if (current.node.id === targetId) {
      return current.path
    }
    if (Array.isArray(current.node.children) && current.node.children.length) {
      for (const child of current.node.children) {
        stack.push({ node: child, path: [...current.path, child.id] })
      }
    }
  }
  return []
}

// 加载景点列表
const loadData = async () => {
  loading.value = true
  try {
    const res = await getSpotList(queryParams)
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
    await openSpotFromRoute()
  } finally {
    loading.value = false
  }
}

// 搜索操作
const handleSearch = () => {
  queryParams.page = 1
  syncFilters()
  syncRouteQuery()
  loadData()
}

const handleFilterChange = () => {
  handleSearch()
}

// 重置搜索条件
const handleReset = () => {
  queryParams.keyword = ''
  queryParams.regionId = null
  queryParams.categoryId = null
  queryParams.published = null
  uiFilters.regionPath = []
  uiFilters.categoryPath = []
  uiFilters.published = ''
  handleSearch()
}

const syncRouteQuery = () => {
  const nextQuery = {}
  if (queryParams.keyword) {
    nextQuery.keyword = queryParams.keyword
  }
  const currentQuery = route.query.keyword ? { keyword: route.query.keyword } : {}
  const changed = JSON.stringify(currentQuery) !== JSON.stringify(nextQuery)
  if (changed) {
    skipNextRouteLoad.value = true
    router.replace({ path: route.path, query: nextQuery })
  }
  return changed
}

const normalizeRouteSpotId = (value) => {
  const spotId = Number(value)
  return Number.isInteger(spotId) && spotId > 0 ? spotId : null
}

// 同步路由参数
const applyRouteQuery = () => {
  queryParams.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  const nextSpotId = normalizeRouteSpotId(route.query.spotId)
  if (nextSpotId !== activeSpotId.value) {
    autoOpenedSpotId.value = null
  }
  activeSpotId.value = nextSpotId
}

const getRowClassName = ({ row }) => {
  return Number(row.id) === activeSpotId.value ? 'spot-highlight-row' : ''
}

// 重置景点表单
const resetForm = () => {
  Object.assign(form, {
    name: '',
    price: 0,
    regionId: null,
    regionPath: [],
    parentCategoryId: null,
    categoryId: null,
    address: '',
    latitude: null,
    longitude: null,
    openTime: '',
    heatLevel: 0,
    description: '',
    coverImage: '',
    images: [],
    published: false
  })
}

// 新增景点
const handleAdd = () => {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

// 编辑景点
const handleEdit = async (row) => {
  editId.value = row.id
  try {
    const res = await getSpotDetail(row.id)
    applySpotDetail(res.data)
  } catch (e) {}
}

const applySpotDetail = (detail) => {
  Object.assign(form, detail)
  form.regionPath = findPathById(form.regionId, regionCascaderOptions.value)
  form.images = Array.isArray(detail.images) ? [...detail.images] : []
  form.parentCategoryId = categoryParentMap.value[form.categoryId] || null
  dialogVisible.value = true
}

// 根据路由定位景点
const openSpotFromRoute = async () => {
  if (!activeSpotId.value || autoOpenedSpotId.value === activeSpotId.value) {
    return
  }
  autoOpenedSpotId.value = activeSpotId.value
  editId.value = activeSpotId.value
  try {
    const res = await getSpotDetail(activeSpotId.value)
    applySpotDetail(res.data)
  } catch (e) {
    autoOpenedSpotId.value = null
  }
}

// 打开热度档位设置对话框
const handleHeatEdit = async (row) => {
  heatEditId.value = row.id
  heatSpotDetail.value = null
  try {
    const res = await getSpotDetail(row.id)
    heatSpotDetail.value = res.data
    heatForm.avgRating = res.data.avgRating ?? 0
    heatForm.ratingCount = res.data.ratingCount ?? 0
    heatForm.heatLevel = res.data.heatLevel ?? 0
    heatForm.heatScore = res.data.heatScore ?? 0
    heatDialogVisible.value = true
  } catch (e) {}
}

const handleRefreshSpotRating = async (row) => {
  await refreshSpotRating(row.id)
  ElMessage.success('评分已按评价表同步')
  loadData()
}

const handleRefreshSpotHeat = async (row) => {
  await refreshSpotHeat(row.id)
  ElMessage.success('热度分数已按档位和行为数据同步')
  loadData()
}

const handleRefreshAllRatings = async () => {
  refreshingAllRatings.value = true
  try {
    await refreshAllSpotRatings()
    ElMessage.success('全部景点评分已按评价表同步')
    loadData()
  } finally {
    refreshingAllRatings.value = false
  }
}

const handleRefreshAllHeats = async () => {
  refreshingAllHeats.value = true
  try {
    await refreshAllSpotHeat()
    ElMessage.success('全部景点热度分数已按档位和行为数据同步')
    loadData()
  } finally {
    refreshingAllHeats.value = false
  }
}

const handleParentCategoryChange = () => {
  form.categoryId = null
}

const buildSpotPayload = (source) => ({
  name: source.name,
  description: source.description,
  price: source.price,
  openTime: source.openTime,
  heatLevel: source.heatLevel,
  address: source.address,
  latitude: source.latitude,
  longitude: source.longitude,
  coverImage: source.coverImage,
  regionId: source.regionPath?.length ? source.regionPath[source.regionPath.length - 1] : source.regionId,
  categoryId: source.categoryId,
  published: source.published,
  images: Array.isArray(source.images) ? source.images : []
})

// 构建接口提交参数
const buildSubmitPayload = () => buildSpotPayload(form)

// 提交景点表单
const handleSubmit = async () => {
  await formDialogRef.value?.validate()
  submitting.value = true
  try {
    if (editId.value) {
      await updateSpot(editId.value, buildSubmitPayload())
      ElMessage.success('更新成功')
    } else {
      await createSpot(buildSubmitPayload())
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// 提交热度档位设置
const handleHeatSubmit = async () => {
  await heatDialogRef.value?.validate()
  if (!heatSpotDetail.value) {
    ElMessage.error('景点详情加载失败，请重新打开热度档位设置')
    return
  }
  heatSubmitting.value = true
  try {
    await updateSpot(heatEditId.value, buildSpotPayload({
      ...heatSpotDetail.value,
      heatLevel: heatForm.heatLevel
    }))
    ElMessage.success('更新成功')
    heatDialogVisible.value = false
    heatSpotDetail.value = null
    loadData()
  } finally {
    heatSubmitting.value = false
  }
}

// 切换发布状态
const handleTogglePublish = async (row) => {
  const action = row.published ? '下架' : '发布'
  await ElMessageBox.confirm(`确定要${action}该景点吗？`, '状态确认', { type: 'warning' })
  await updatePublishStatus(row.id, !row.published)
  ElMessage.success(`${action}成功`)
  loadData()
}

// 删除景点
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该景点吗？', '删除确认', { type: 'warning' })
  await deleteSpot(row.id)
  ElMessage.success('删除成功')
  loadData()
}

// --- 批量选中与详情查看逻辑 ---
const handleSelectionChange = (selection) => {
  selectedSpots.value = selection
}

const handleView = async (row) => {
  try {
    const res = await getSpotDetail(row.id)
    spotDetail.value = res.data
    drawerVisible.value = true
  } catch (e) {
    ElMessage.error('无法获取景点详情')
  }
}

// 批量操作统一汇总执行结果，避免逐条串行导致等待时间过长且失败反馈不清晰。
const runBatchAction = async ({ rows, requestFactory, successMessage }) => {
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

    selectedSpots.value = []
    await loadData()
  } finally {
    loading.value = false
  }
}

const handleBatchPublish = async (status) => {
  if (!selectedSpots.value.length) return
  const action = status ? '发布' : '下架'
  await ElMessageBox.confirm(`确定要批量${action}选中的 ${selectedSpots.value.length} 个景点吗？`, '状态确认', { type: 'warning' })
  const targetRows = selectedSpots.value.filter((item) => item.published !== status)
  if (!targetRows.length) {
    ElMessage.info(`选中景点均已${status ? '上架' : '下架'}`)
    return
  }
  await runBatchAction({
    rows: targetRows,
    requestFactory: (item) => updatePublishStatus(item.id, status),
    successMessage: `批量${action}成功`
  })
}

const handleBatchDelete = async () => {
  if (!selectedSpots.value.length) return
  await ElMessageBox.confirm(`确定要批量删除选中的 ${selectedSpots.value.length} 个景点吗？(此操作不可恢复)`, '删除确认', { type: 'error' })
  await runBatchAction({
    rows: selectedSpots.value,
    requestFactory: (item) => deleteSpot(item.id),
    successMessage: '批量删除成功'
  })
}


watch(
  () => [route.query.keyword, route.query.spotId],
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
.spot-page {
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

:deep(.spot-highlight-row) {
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

</style>
