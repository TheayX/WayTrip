<!-- 景点管理页面 -->
<template>
  <div class="spot-page admin-page-shell">
    <section class="page-hero">
      <div class="hero-left">
        <p class="page-kicker">景点内容管理</p>
        <h1 class="page-title">景点管理</h1>
        <div class="subtitle-row">
          <p class="page-subtitle">维护景点信息、上架状态与展示质量。</p>
        </div>
      </div>
      <div class="hero-actions hero-actions-stack">
        <el-button :loading="loading" @click="loadData">刷新数据</el-button>
        <div class="hero-action-row hero-action-offset">
          <el-button :loading="refreshingAllRatings" @click="handleRefreshAllRatings">同步全部评分</el-button>
          <el-button :loading="refreshingAllHeats" @click="handleRefreshAllHeats">同步全部热度分数</el-button>
          <el-button type="primary" @click="handleAdd">新增景点</el-button>
        </div>
      </div>
    </section>

    <section class="summary-grid metric-cards--order">
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

    <el-card shadow="hover" class="management-card admin-management-card">


      <SpotFilterBar
        :query-params="queryParams"
        :ui-filters="uiFilters"
        :region-cascader-options="regionCascaderOptions"
        :category-cascader-options="categoryCascaderOptions"
        :region-cascader-props="regionCascaderProps"
        :category-cascader-props="categoryCascaderProps"
        :heat-level-options="heatLevelOptions"
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
        @sort-change="handleSortChange"
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
      :get-heat-level-label="getHeatLevelLabel"
      :get-heat-level-tag-type="getHeatLevelTagType"
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
import { applySortChange } from '@/shared/composables/useTableSort.js'

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

// 将后台返回的资源路径补全为完整访问地址。
const getImageUrl = (url) => {
  return getResourceUrl(url)
}

// 格式化后台时间字段用于表格展示。
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

// 将热度档位值转换为展示文案。
const getHeatLevelLabel = (level) => {
  return heatLevelOptions.find((item) => item.value === Number(level))?.label || '普通'
}

// 将热度档位值转换为标签样式。
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

// 校验上传文件是否满足图片类型和大小限制。
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

// 处理封面图上传成功回调。
const handleUploadSuccess = (response) => {
  if (response.code === 0) {
    form.coverImage = response.data.url
    ElMessage.success('上传成功')
    return
  }
  ElMessage.error(response.message || '上传失败')
}

// 处理图集上传成功回调。
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

// 删除图集中的单张图片。
const removeGalleryImage = (index) => {
  if (!Array.isArray(form.images)) return
  form.images.splice(index, 1)
}

// 统一处理上传失败提示。
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
  heatLevel: null,
  published: null,
  sortBy: '',
  sortOrder: ''
})
const uiFilters = reactive({
  regionPath: [],
  categoryPath: [],
  heatLevel: '',
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
  reviewCount: 0,
  heatLevel: 0,
  heatScore: 0
})

const heatRules = {
  heatLevel: [{ required: true, message: '请选择热度档位', trigger: 'change' }]
}

// 页面挂载后初始化路由筛选、筛选项和首屏列表。
onMounted(async () => {
  applyRouteQuery()
  await loadFilters()
  applyRouteQuery()
  await loadData()
})

// 加载地区和分类筛选项。
const loadFilters = async () => {
  try {
    const res = await getFilters()
    regions.value = res.data.regions || []
    regionTree.value = res.data.regionTree?.length ? res.data.regionTree : []
    categories.value = res.data.categories || []
    categoryTree.value = res.data.categoryTree?.length ? res.data.categoryTree : categories.value
  } catch (e) {}
}

// 将界面上的筛选状态同步回查询参数。
const syncFilters = () => {
  const selectedRegionId = uiFilters.regionPath?.length
    ? uiFilters.regionPath[uiFilters.regionPath.length - 1]
    : null
  const selectedCategoryId = uiFilters.categoryPath?.length
    ? uiFilters.categoryPath[uiFilters.categoryPath.length - 1]
    : null
  queryParams.regionId = selectedRegionId ? Number(selectedRegionId) : null
  queryParams.categoryId = selectedCategoryId ? Number(selectedCategoryId) : null
  queryParams.heatLevel = uiFilters.heatLevel === '' || uiFilters.heatLevel == null
    ? null
    : Number(uiFilters.heatLevel)
  queryParams.published = uiFilters.published === '' || uiFilters.published == null
    ? null
    : Number(uiFilters.published)
}

// 根据节点 ID 反查级联选择路径。
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

// 按当前筛选条件加载景点列表。
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

// 按当前关键字和筛选项执行搜索。
const handleSearch = () => {
  queryParams.page = 1
  syncFilters()
  syncRouteQuery()
  loadData()
}

// 筛选项变化后立即重新查询列表。
const handleFilterChange = () => {
  handleSearch()
}

// 同步表格排序并重新加载列表。
const handleSortChange = (sortPayload) => {
  applySortChange(queryParams, sortPayload)
  loadData()
}

// 清空搜索条件并恢复默认筛选状态。
const handleReset = () => {
  queryParams.keyword = ''
  queryParams.regionId = null
  queryParams.categoryId = null
  queryParams.heatLevel = null
  queryParams.published = null
  uiFilters.regionPath = []
  uiFilters.categoryPath = []
  uiFilters.heatLevel = ''
  uiFilters.published = ''
  handleSearch()
}

// 将当前筛选状态同步到路由 query。
const syncRouteQuery = () => {
  const nextQuery = {}
  if (queryParams.keyword) {
    nextQuery.keyword = queryParams.keyword
  }
  if (queryParams.regionId != null) {
    nextQuery.regionId = String(queryParams.regionId)
  }
  if (queryParams.categoryId != null) {
    nextQuery.categoryId = String(queryParams.categoryId)
  }
  if (queryParams.heatLevel != null) {
    nextQuery.heatLevel = String(queryParams.heatLevel)
  }
  if (queryParams.published != null) {
    nextQuery.published = String(queryParams.published)
  }
  const currentQuery = {}
  if (typeof route.query.keyword === 'string' && route.query.keyword) {
    currentQuery.keyword = route.query.keyword
  }
  if (typeof route.query.regionId === 'string' && route.query.regionId) {
    currentQuery.regionId = route.query.regionId
  }
  if (typeof route.query.categoryId === 'string' && route.query.categoryId) {
    currentQuery.categoryId = route.query.categoryId
  }
  if (typeof route.query.heatLevel === 'string' && route.query.heatLevel) {
    currentQuery.heatLevel = route.query.heatLevel
  }
  if (typeof route.query.published === 'string' && route.query.published) {
    currentQuery.published = route.query.published
  }
  const changed = JSON.stringify(currentQuery) !== JSON.stringify(nextQuery)
  if (changed) {
    skipNextRouteLoad.value = true
    router.replace({ path: route.path, query: nextQuery })
  }
  return changed
}

// 将路由中的景点 ID 规范化为有效整数。
const normalizeRouteSpotId = (value) => {
  const spotId = Number(value)
  return Number.isInteger(spotId) && spotId > 0 ? spotId : null
}

// 根据路由 query 回填当前筛选状态和高亮景点。
const applyRouteQuery = () => {
  queryParams.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  queryParams.regionId = typeof route.query.regionId === 'string' ? Number(route.query.regionId) : null
  queryParams.categoryId = typeof route.query.categoryId === 'string' ? Number(route.query.categoryId) : null
  queryParams.heatLevel = typeof route.query.heatLevel === 'string' ? Number(route.query.heatLevel) : null
  queryParams.published = typeof route.query.published === 'string' ? Number(route.query.published) : null
  uiFilters.regionPath = findPathById(queryParams.regionId, regionCascaderOptions.value)
  uiFilters.categoryPath = findPathById(queryParams.categoryId, categoryCascaderOptions.value)
  uiFilters.heatLevel = queryParams.heatLevel == null ? '' : String(queryParams.heatLevel)
  uiFilters.published = queryParams.published == null ? '' : String(queryParams.published)
  const nextSpotId = normalizeRouteSpotId(route.query.spotId)
  if (nextSpotId !== activeSpotId.value) {
    autoOpenedSpotId.value = null
  }
  activeSpotId.value = nextSpotId
}

// 为当前高亮景点行附加样式类名。
const getRowClassName = ({ row }) => {
  return Number(row.id) === activeSpotId.value ? 'spot-highlight-row' : ''
}

// 重置景点编辑表单为默认值。
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

// 打开新增景点对话框。
const handleAdd = () => {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

// 加载并打开景点编辑表单。
const handleEdit = async (row) => {
  editId.value = row.id
  try {
    const res = await getSpotDetail(row.id)
    applySpotDetail(res.data)
  } catch (e) {}
}

// 将景点详情数据回填到编辑表单。
const applySpotDetail = (detail) => {
  Object.assign(form, detail)
  form.regionPath = findPathById(form.regionId, regionCascaderOptions.value)
  form.images = Array.isArray(detail.images) ? [...detail.images] : []
  form.parentCategoryId = categoryParentMap.value[form.categoryId] || null
  dialogVisible.value = true
}

// 根据路由参数自动打开指定景点。
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

// 加载并打开热度档位设置对话框。
const handleHeatEdit = async (row) => {
  heatEditId.value = row.id
  heatSpotDetail.value = null
  try {
    const res = await getSpotDetail(row.id)
    heatSpotDetail.value = res.data
    heatForm.avgRating = res.data.avgRating ?? 0
    heatForm.reviewCount = res.data.reviewCount ?? 0
    heatForm.heatLevel = res.data.heatLevel ?? 0
    heatForm.heatScore = res.data.heatScore ?? 0
    heatDialogVisible.value = true
  } catch (e) {}
}

// 重新同步单个景点的评分数据。
const handleRefreshSpotRating = async (row) => {
  await refreshSpotRating(row.id)
  ElMessage.success('评分已按评价表同步')
  loadData()
}

// 重新同步单个景点的热度分数。
const handleRefreshSpotHeat = async (row) => {
  await refreshSpotHeat(row.id)
  ElMessage.success('热度分数已按档位和行为数据同步')
  loadData()
}

// 批量同步全部景点评分。
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

// 批量同步全部景点热度分数。
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

// 切换父分类后清空已选子分类。
const handleParentCategoryChange = () => {
  form.categoryId = null
}

// 构建景点创建与更新共用的提交参数。
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

// 基于当前表单构建提交参数。
const buildSubmitPayload = () => buildSpotPayload(form)

// 提交景点新增或编辑表单。
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

// 提交热度档位调整。
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

// 切换单个景点的上架状态。
const handleTogglePublish = async (row) => {
  const action = row.published ? '下架' : '发布'
  await ElMessageBox.confirm(`确定要${action}该景点吗？`, '状态确认', { type: 'warning' })
  await updatePublishStatus(row.id, !row.published)
  ElMessage.success(`${action}成功`)
  loadData()
}

// 删除单个景点。
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该景点吗？', '删除确认', { type: 'warning' })
  await deleteSpot(row.id)
  ElMessage.success('删除成功')
  loadData()
}

// 同步表格批量选中结果。
const handleSelectionChange = (selection) => {
  selectedSpots.value = selection
}

// 打开景点详情抽屉。
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

// 监听外部路由筛选变化并刷新列表。
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


:deep(.spot-highlight-row) {
  --el-table-tr-bg-color: var(--el-color-primary-light-9);

  td {
    background-color: var(--el-color-primary-light-9) !important;
  }
}
</style>
