<!-- 景点管理页面 -->
<template>
  <div class="spot-page">
    <el-card  shadow="hover">
      <!-- 卡片头部 -->
      <template #header>
        <div class="card-header">
          <span>景点列表</span>
          <div class="header-actions">
            <el-button @click="handleRefreshAllRatings" :loading="refreshingAllRatings">同步全部评分</el-button>
            <el-button @click="handleRefreshAllHeats" :loading="refreshingAllHeats">同步全部热度</el-button>
            <el-button type="primary" @click="handleAdd">新增景点</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索筛选表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="景点名称"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="地区">
          <el-cascader
            v-model="uiFilters.regionPath"
            :options="regionCascaderOptions"
            :props="regionCascaderProps"
            clearable
            style="width: 220px"
            placeholder="全部"
            @change="handleFilterChange"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-cascader
            v-model="uiFilters.categoryPath"
            :options="categoryCascaderOptions"
            :props="categoryCascaderProps"
            clearable
            style="width: 220px"
            placeholder="全部"
            @change="handleFilterChange"
            @clear="handleFilterChange"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="uiFilters.published"
            placeholder="全部"
            clearable
            style="width: 140px"
            @change="handleFilterChange"
            @clear="handleFilterChange"
          >
            <el-option label="已发布" value="1" />
            <el-option label="未发布" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 景点数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe :row-class-name="getRowClassName">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <el-image :src="getImageUrl(row.coverImage)" style="width: 60px; height: 60px" fit="cover" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="regionName" label="地区" width="100" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="avgRating" label="评分" width="80" />
        <el-table-column prop="ratingCount" label="评价数" width="90" />
        <el-table-column label="热度档位" width="110">
          <template #default="{ row }">
            <el-tag :type="getHeatLevelTagType(row.heatLevel)">
              {{ getHeatLevelLabel(row.heatLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="heatScore" label="热度" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.published ? 'success' : 'info'">
              {{ row.published ? '已发布' : '未发布' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="修改时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="handleHeatEdit(row)">热度</el-button>
              <el-button link type="primary" @click="handleRefreshSpotHeat(row)">同步热度</el-button>
              <el-button link type="primary" @click="handleRefreshSpotRating(row)">同步评分</el-button>
              <el-button link :type="row.published ? 'warning' : 'success'" @click="handleTogglePublish(row)">
                {{ row.published ? '下架' : '发布' }}
              </el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 新增/编辑景点对话框 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑景点' : '新增景点'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入景点名称" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="地区" prop="regionPath">
          <el-cascader
            v-model="form.regionPath"
            :options="regionCascaderOptions"
            :props="regionCascaderProps"
            clearable
            placeholder="请选择地区"
          />
        </el-form-item>
        <el-form-item label="父分类" prop="parentCategoryId">
          <el-select v-model="form.parentCategoryId" placeholder="请选择父分类" @change="handleParentCategoryChange">
            <el-option v-for="item in parentCategoryOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="子分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择子分类" :disabled="!form.parentCategoryId">
            <el-option v-for="item in childCategoryOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="经纬度">
          <el-input-number v-model="form.latitude" placeholder="纬度" style="width: 150px" />
          <el-input-number v-model="form.longitude" placeholder="经度" style="width: 150px; margin-left: 10px" />
        </el-form-item>
        <el-form-item label="开放时间">
          <el-input v-model="form.openTime" placeholder="如：08:30-17:00" />
        </el-form-item>
        <el-form-item label="热度档位" prop="heatLevel">
          <el-select v-model="form.heatLevel" placeholder="请选择热度档位">
            <el-option
              v-for="item in heatLevelOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入景点简介" />
        </el-form-item>
        <el-form-item label="封面图">
          <div class="upload-container">
            <el-upload
              class="image-uploader"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :data="uploadData"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-image
                v-if="form.coverImage"
                :src="getImageUrl(form.coverImage)"
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
        <el-form-item label="景点图片">
          <div class="gallery-container">
            <el-upload
              class="gallery-uploader"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :data="uploadData"
              :show-file-list="false"
              :on-success="handleGalleryUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-button type="primary" plain>上传图片</el-button>
            </el-upload>
            <div class="gallery-list" v-if="form.images?.length">
              <div class="gallery-item" v-for="(img, index) in form.images" :key="`${img}-${index}`">
                <el-image :src="getImageUrl(img)" fit="cover" class="gallery-image" />
                <el-button link type="danger" @click="removeGalleryImage(index)">删除</el-button>
              </div>
            </div>
            <div class="upload-tip">可上传多张详情图，按当前顺序保存</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 热度设置对话框 -->
    <el-dialog v-model="heatDialogVisible" title="热度设置" width="420px">
      <el-form ref="heatFormRef" :model="heatForm" :rules="heatRules" label-width="110px">
        <el-form-item label="当前评分">
          <span>{{ heatForm.avgRating ?? 0 }}</span>
        </el-form-item>
        <el-form-item label="评价数">
          <span>{{ heatForm.ratingCount ?? 0 }}</span>
        </el-form-item>
        <el-form-item label="当前总热度">
          <span>{{ heatForm.heatScore ?? 0 }}</span>
        </el-form-item>
        <el-form-item label="热度档位" prop="heatLevel">
          <el-select v-model="heatForm.heatLevel" style="width: 100%" placeholder="请选择热度档位">
            <el-option
              v-for="item in heatLevelOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="heatDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleHeatSubmit" :loading="heatSubmitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
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
} from '@/api/spot'
import { useUserStore } from '@/stores/user'
import { getAdminUploadUrl, getResourceUrl } from '@/utils/resource'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 上传相关配置
const uploadUrl = computed(() => getAdminUploadUrl('image'))
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))
const uploadData = computed(() => ({
  tag: form.name || ''
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
const formRef = ref()
const heatDialogVisible = ref(false)
const heatSubmitting = ref(false)
const heatFormRef = ref()
const heatEditId = ref(null)
const heatSpotDetail = ref(null)

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
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
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
  router.replace({ path: route.path, query: nextQuery })
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

// 打开热度设置对话框
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
  ElMessage.success('热度已按档位和行为数据同步')
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
    ElMessage.success('全部景点热度已按档位和行为数据同步')
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
  await formRef.value.validate()
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

// 提交热度设置
const handleHeatSubmit = async () => {
  await heatFormRef.value.validate()
  if (!heatSpotDetail.value) {
    ElMessage.error('景点详情加载失败，请重新打开热度设置')
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
  await ElMessageBox.confirm(`确定要${action}该景点吗？`, '提示', { type: 'warning' })
  await updatePublishStatus(row.id, !row.published)
  ElMessage.success(`${action}成功`)
  loadData()
}

// 删除景点
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该景点吗？', '提示', { type: 'warning' })
  await deleteSpot(row.id)
  ElMessage.success('删除成功')
  loadData()
}

watch(
  () => [route.query.keyword, route.query.spotId],
  () => {
    applyRouteQuery()
    loadData()
  }
)
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.search-form {
  margin-bottom: 20px;
}

.table-actions {
  white-space: nowrap;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
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
    width: 150px;
    height: 150px;
    display: block;
  }

  .upload-placeholder {
    width: 150px;
    height: 150px;
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

.gallery-container {
  width: 100%;
}

.gallery-list {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}

.gallery-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.gallery-image {
  width: 100%;
  height: 90px;
}

:deep(.spot-highlight-row) {
  --el-table-tr-bg-color: #fdf6ec;

  td {
    background-color: #fdf6ec !important;
  }
}
</style>
