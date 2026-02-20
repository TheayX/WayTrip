<template>
  <div class="spot-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>景点列表</span>
          <el-button type="primary" @click="handleAdd">新增景点</el-button>
        </div>
      </template>
      
      <!-- 搜索筛选 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="景点名称" clearable />
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
          <el-select v-model="uiFilters.categoryId" placeholder="全部" clearable style="width: 200px" @change="handleFilterChange" @clear="handleFilterChange">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.label" :value="String(item.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="uiFilters.published" placeholder="全部" clearable style="width: 140px" @change="handleFilterChange" @clear="handleFilterChange">
            <el-option label="已发布" value="1" />
            <el-option label="未发布" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
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
        <el-table-column prop="heatScore" label="热度" width="80" />
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
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleRatingEdit(row)">评分/热度</el-button>
            <el-button link :type="row.published ? 'warning' : 'success'" @click="handleTogglePublish(row)">
              {{ row.published ? '下架' : '发布' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 新增/编辑弹窗 -->
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

    <!-- 评分/热度设置 -->
    <el-dialog v-model="ratingDialogVisible" title="评分/热度设置" width="420px">
      <el-form ref="ratingFormRef" :model="ratingForm" :rules="ratingRules" label-width="110px">
        <el-form-item label="评分" prop="avgRating">
          <el-input-number v-model="ratingForm.avgRating" :min="0" :max="5" :precision="1" :step="0.1" />
        </el-form-item>
        <el-form-item label="评价数" prop="ratingCount">
          <el-input-number v-model="ratingForm.ratingCount" :min="0" :precision="0" />
        </el-form-item>
        <el-form-item label="热度" prop="heatScore">
          <el-input-number v-model="ratingForm.heatScore" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ratingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRatingSubmit" :loading="ratingSubmitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getSpotList, getSpotDetail, createSpot, updateSpot, updatePublishStatus, deleteSpot, getFilters } from '@/api/spot'

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
    form.coverImage = response.data.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleGalleryUploadSuccess = (response) => {
  if (response.code === 0) {
    if (!Array.isArray(form.images)) {
      form.images = []
    }
    form.images.push(response.data.url)
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const removeGalleryImage = (index) => {
  if (!Array.isArray(form.images)) return
  form.images.splice(index, 1)
}

// 上传失败
const handleUploadError = () => {
  ElMessage.error('上传失败，请重试')
}

const loading = ref(false)
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
      label: `${'　'.repeat(level)}${level > 0 ? '└ ' : ''}${node.name}`,
      hasChildren
    })

    if (hasChildren) {
      acc.push(...flattenCategories(node.children, level + 1))
    }

    return acc
  }, [])
}

const categoryOptions = computed(() => flattenCategories(categoryTree.value))
const leafCategoryOptions = computed(() => categoryOptions.value.filter(item => !item.hasChildren))
const regionCascaderOptions = computed(() => {
  if (regionTree.value.length) {
    return regionTree.value
  }
  return regions.value.map(item => ({ ...item, children: [] }))
})
const regionCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: true
}
const parentCategoryOptions = computed(() => categoryTree.value.filter(item => item.children?.length))
const childCategoryOptions = computed(() => {
  if (!form.parentCategoryId) {
    return []
  }
  const parent = categoryTree.value.find(item => item.id === form.parentCategoryId)
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
  categoryId: '',
  published: ''
})

// 弹窗相关
const dialogVisible = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formRef = ref()
const ratingDialogVisible = ref(false)
const ratingSubmitting = ref(false)
const ratingFormRef = ref()
const ratingEditId = ref(null)

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
  images: []
})

const rules = {
  name: [{ required: true, message: '请输入景点名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  regionPath: [{ required: true, message: '请选择地区', trigger: 'change' }],
  parentCategoryId: [{ required: true, message: '请选择父分类', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择子分类', trigger: 'change' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

const ratingForm = reactive({
  avgRating: 0,
  ratingCount: 0,
  heatScore: 0
})

const ratingRules = {
  avgRating: [{ required: true, message: '请输入评分', trigger: 'blur' }],
  ratingCount: [{ required: true, message: '请输入评价数', trigger: 'blur' }],
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
  queryParams.regionId = selectedRegionId ? Number(selectedRegionId) : null
  queryParams.categoryId = uiFilters.categoryId ? Number(uiFilters.categoryId) : null
  queryParams.published = uiFilters.published == null || uiFilters.published === ''
    ? null
    : Number(uiFilters.published)
}

const findRegionPathById = (targetId, tree) => {
  if (!targetId || !Array.isArray(tree) || !tree.length) {
    return []
  }
  const stack = tree.map(node => ({ node, path: [node.id] }))
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
    tableData.value = res.data.list
    total.value = res.data.total
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
  uiFilters.categoryId = ''
  uiFilters.published = ''
  handleSearch()
}

const handleAdd = () => {
  editId.value = null
  Object.assign(form, { name: '', price: 0, regionId: null, regionPath: [], parentCategoryId: null, categoryId: null, address: '', latitude: null, longitude: null, openTime: '', description: '', coverImage: '', images: [] })
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  editId.value = row.id
  try {
    const res = await getSpotDetail(row.id)
    Object.assign(form, res.data)
    form.regionPath = findRegionPathById(form.regionId, regionCascaderOptions.value)
    form.images = Array.isArray(res.data.images) ? [...res.data.images] : []
    form.parentCategoryId = categoryParentMap.value[form.categoryId] || null
    dialogVisible.value = true
  } catch (e) {}
}

const handleRatingEdit = async (row) => {
  ratingEditId.value = row.id
  try {
    const res = await getSpotDetail(row.id)
    ratingForm.avgRating = res.data.avgRating ?? 0
    ratingForm.ratingCount = res.data.ratingCount ?? 0
    ratingForm.heatScore = res.data.heatScore ?? 0
    ratingDialogVisible.value = true
  } catch (e) {}
}

const handleParentCategoryChange = () => {
  form.categoryId = null
}

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

const handleRatingSubmit = async () => {
  await ratingFormRef.value.validate()
  ratingSubmitting.value = true
  try {
    await updateSpot(ratingEditId.value, {
      avgRating: ratingForm.avgRating,
      ratingCount: ratingForm.ratingCount,
      heatScore: ratingForm.heatScore
    })
    ElMessage.success('更新成功')
    ratingDialogVisible.value = false
    loadData()
  } finally {
    ratingSubmitting.value = false
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

const buildSubmitPayload = () => ({
  name: form.name,
  description: form.description,
  price: form.price,
  openTime: form.openTime,
  address: form.address,
  latitude: form.latitude,
  longitude: form.longitude,
  coverImage: form.coverImage,
  regionId: form.regionPath?.length
    ? form.regionPath[form.regionPath.length - 1]
    : form.regionId,
  categoryId: form.categoryId,
  published: form.published,
  images: form.images
})
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
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
