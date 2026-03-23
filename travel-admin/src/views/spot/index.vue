<template>
  <div class="spot-page">
    <el-card class="page-container" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>景点列表</span>
          <div class="header-actions">
            <el-button @click="handleRefreshAllRatings" :loading="refreshingAllRatings">同步全部评分</el-button>
            <el-button type="primary" @click="handleAdd">新增景点</el-button>
          </div>
        </div>
      </template>

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

      <el-table :data="tableData" v-loading="loading" stripe>
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
              <el-button link type="primary" @click="handleRefreshSpotRating(row)">同步评分</el-button>
              <el-button link :type="row.published ? 'warning' : 'success'" @click="handleTogglePublish(row)">
                {{ row.published ? '下架' : '发布' }}
              </el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

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

    <el-dialog v-model="heatDialogVisible" title="热度设置" width="420px">
      <el-form ref="heatFormRef" :model="heatForm" :rules="heatRules" label-width="110px">
        <el-form-item label="当前评分">
          <span>{{ heatForm.avgRating ?? 0 }}</span>
        </el-form-item>
        <el-form-item label="评价数">
          <span>{{ heatForm.ratingCount ?? 0 }}</span>
        </el-form-item>
        <el-form-item label="热度" prop="heatScore">
          <el-input-number v-model="heatForm.heatScore" :min="0" :precision="2" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  createSpot,
  deleteSpot,
  getFilters,
  getSpotDetail,
  getSpotList,
  refreshAllSpotRatings,
  refreshSpotRating,
  updatePublishStatus,
  updateSpot
} from '@/api/spot'
import { useUserStore } from '@/stores/user'

const BASE_URL = 'http://localhost:8080'
const userStore = useUserStore()

const uploadUrl = computed(() => `${BASE_URL}/api/admin/v1/upload/image`)
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))
const uploadData = computed(() => ({
  tag: form.name || ''
}))

const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return BASE_URL + url
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
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
const tableData = ref([])
const total = ref(0)
const regions = ref([])
const regionTree = ref([])
const categories = ref([])
const categoryTree = ref([])

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

const dialogVisible = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formRef = ref()
const heatDialogVisible = ref(false)
const heatSubmitting = ref(false)
const heatFormRef = ref()
const heatEditId = ref(null)

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
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

const heatForm = reactive({
  avgRating: 0,
  ratingCount: 0,
  heatScore: 0
})

const heatRules = {
  heatScore: [{ required: true, message: '请输入热度', trigger: 'blur' }]
}

onMounted(() => {
  loadFilters()
  loadData()
})

const loadFilters = async () => {
  try {
    const res = await getFilters()
    regions.value = res.data.regions || []
    regionTree.value = res.data.regionTree?.length ? res.data.regionTree : []
    categories.value = res.data.categories || []
    categoryTree.value = res.data.categoryTree?.length ? res.data.categoryTree : categories.value
  } catch (e) {}
}

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

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSpotList(queryParams)
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.page = 1
  syncFilters()
  loadData()
}

const handleFilterChange = () => {
  handleSearch()
}

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
    description: '',
    coverImage: '',
    images: [],
    published: false
  })
}

const handleAdd = () => {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  editId.value = row.id
  try {
    const res = await getSpotDetail(row.id)
    Object.assign(form, res.data)
    form.regionPath = findPathById(form.regionId, regionCascaderOptions.value)
    form.images = Array.isArray(res.data.images) ? [...res.data.images] : []
    form.parentCategoryId = categoryParentMap.value[form.categoryId] || null
    dialogVisible.value = true
  } catch (e) {}
}

const handleHeatEdit = async (row) => {
  heatEditId.value = row.id
  try {
    const res = await getSpotDetail(row.id)
    heatForm.avgRating = res.data.avgRating ?? 0
    heatForm.ratingCount = res.data.ratingCount ?? 0
    heatForm.heatScore = res.data.heatScore ?? 0
    heatDialogVisible.value = true
  } catch (e) {}
}

const handleRefreshSpotRating = async (row) => {
  await refreshSpotRating(row.id)
  ElMessage.success('评分已按评价表同步')
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

const handleParentCategoryChange = () => {
  form.categoryId = null
}

const buildSubmitPayload = () => ({
  name: form.name,
  description: form.description,
  price: form.price,
  openTime: form.openTime,
  address: form.address,
  latitude: form.latitude,
  longitude: form.longitude,
  coverImage: form.coverImage,
  regionId: form.regionPath?.length ? form.regionPath[form.regionPath.length - 1] : form.regionId,
  categoryId: form.categoryId,
  published: form.published,
  images: form.images
})

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

const handleHeatSubmit = async () => {
  await heatFormRef.value.validate()
  heatSubmitting.value = true
  try {
    await updateSpot(heatEditId.value, {
      heatScore: heatForm.heatScore
    })
    ElMessage.success('更新成功')
    heatDialogVisible.value = false
    loadData()
  } finally {
    heatSubmitting.value = false
  }
}

const handleTogglePublish = async (row) => {
  const action = row.published ? '下架' : '发布'
  await ElMessageBox.confirm(`确定要${action}该景点吗？`, '提示', { type: 'warning' })
  await updatePublishStatus(row.id, !row.published)
  ElMessage.success(`${action}成功`)
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该景点吗？', '提示', { type: 'warning' })
  await deleteSpot(row.id)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<style lang="scss" scoped>

.page-container {
  border: none !important;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04) !important;
  
  :deep(.el-card__header) {
    border-bottom: 1px solid #f0f2f5;
    padding: 20px 24px;
    
    .card-header {
      font-size: 16px;
      font-weight: 600;
      color: #1f2f3d;
      position: relative;
      padding-left: 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 4px;
        height: 16px;
        background: #1890ff;
        border-radius: 2px;
      }
    }
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.search-form {
  background: #f8f9fa;
  padding: 18px 24px 2px;
  border-radius: 8px;
  margin-bottom: 24px;
  border: 1px solid #f0f2f5;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.02);
  }
}

.pagination-wrapper, .pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px dashed #ebeef5;
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
</style>
