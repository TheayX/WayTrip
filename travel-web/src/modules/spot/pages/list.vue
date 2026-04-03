<!-- 景点列表页 -->
<template>
  <div class="page-container spot-list-page">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>景点列表</el-breadcrumb-item>
    </el-breadcrumb>

    <section class="search-strip card" @click="$router.push('/search')">
      <el-icon><Search /></el-icon>
      <span>搜索景点名称 / 城市</span>
    </section>

    <section class="state-card card">
      <div class="state-main">
        <h2 class="state-title">景点列表</h2>
        <p class="state-desc">{{ currentStateText }}</p>
      </div>
      <div class="state-actions">
        <el-button text :type="filters.sortBy === 'heat' ? 'primary' : 'default'" @click="changeSort('heat')">综合热度</el-button>
        <el-button text :type="filters.sortBy === 'rating' ? 'primary' : 'default'" @click="changeSort('rating')">评分最高</el-button>
        <el-button text :type="filters.sortBy === 'price_asc' ? 'primary' : 'default'" @click="changeSort('price_asc')">价格最低</el-button>
        <el-button text :type="filters.sortBy === 'price_desc' ? 'primary' : 'default'" @click="changeSort('price_desc')">价格最高</el-button>
        <el-button text @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section class="filter-bar card">
      <div class="filter-row">
        <div class="filter-group">
          <span class="filter-label">地区</span>
          <el-select v-model="filters.regionId" clearable placeholder="全部地区" @change="handleFilter">
            <el-option label="全部地区" value="" />
            <el-option v-for="item in flatRegions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </div>
        <div class="filter-group">
          <span class="filter-label">分类</span>
          <el-select v-model="filters.categoryId" clearable placeholder="全部分类" @change="handleFilter">
            <el-option label="全部分类" value="" />
            <el-option v-for="item in flatCategories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </div>
      </div>
    </section>

    <section class="active-filters" v-if="activeFilterTags.length">
      <span v-for="tag in activeFilterTags" :key="tag" class="tag-chip">{{ tag }}</span>
    </section>

    <section v-loading="loading" class="spot-grid">
      <article
        v-for="spot in spotList"
        :key="spot.id"
        class="spot-card card"
        @click="$router.push(`/spots/${spot.id}?source=list`)"
      >
        <div class="spot-image-box">
          <img :src="getImageUrl(spot.coverImage)" class="spot-image" alt="" />
          <div v-if="spot.avgRating" class="rating-badge">
            <span class="score">{{ spot.avgRating }}</span>
            <span class="unit">分</span>
          </div>
        </div>
        <div class="spot-content">
          <h3 class="spot-name">{{ spot.name }}</h3>
          <div class="spot-tags">
            <span class="tag-chip plain">{{ spot.regionName || '地区待补充' }}</span>
            <span class="tag-chip">{{ spot.categoryName || '分类待补充' }}</span>
          </div>
          <div class="spot-footer">
            <div class="price-box">
              <span class="symbol">¥</span>
              <span class="num">{{ spot.price }}</span>
              <span class="suffix">起</span>
            </div>
            <div class="meta-box">
              <span class="star-text">★ {{ spot.avgRating || '-' }}</span>
            </div>
          </div>
        </div>
      </article>
    </section>

    <el-empty v-if="!loading && spotList.length === 0" description="暂无相关景点">
      <el-button @click="resetFilters">清除筛选</el-button>
    </el-empty>

    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchSpotList"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getSpotList, getFilters } from '@/modules/spot/api.js'
import { getImageUrl } from '@/shared/api/client.js'

const SPOT_DETAIL_UPDATED_KEY = 'spot_detail_updated'

// 基础依赖与路由状态
const route = useRoute()
const router = useRouter()

// 页面数据状态
const loading = ref(false)
const page = ref(1)
const pageSize = 12
const total = ref(0)
const spotList = ref([])
const regionTree = ref([])
const categoryTree = ref([])

const filters = reactive({
  regionId: '',
  categoryId: '',
  sortBy: 'heat'
})

// 计算属性
const flatRegions = computed(() => {
  if (!regionTree.value.length) return []
  return regionTree.value.flatMap((item) => {
    if (Array.isArray(item.children) && item.children.length) {
      return [{ id: item.id, name: item.name }, ...item.children]
    }
    return [{ id: item.id, name: item.name }]
  })
})

const flatCategories = computed(() => {
  if (!categoryTree.value.length) return []
  return categoryTree.value.flatMap((item) => {
    if (Array.isArray(item.children) && item.children.length) {
      return [{ id: item.id, name: item.name }, ...item.children]
    }
    return [{ id: item.id, name: item.name }]
  })
})

const currentStateText = computed(() => {
  const region = flatRegions.value.find((item) => String(item.id) === String(filters.regionId))?.name || '全部地区'
  const category = flatCategories.value.find((item) => String(item.id) === String(filters.categoryId))?.name || '全部分类'
  const sortMap = {
    heat: '综合热度',
    rating: '评分最高',
    price_asc: '价格最低',
    price_desc: '价格最高'
  }
  return `${region} · ${category} · 共 ${total.value} 个结果 · ${sortMap[filters.sortBy] || '综合热度'}`
})

const activeFilterTags = computed(() => {
  const tags = []
  const region = flatRegions.value.find((item) => String(item.id) === String(filters.regionId))?.name
  const category = flatCategories.value.find((item) => String(item.id) === String(filters.categoryId))?.name
  if (region) tags.push(region)
  if (category) tags.push(category)
  if (filters.sortBy !== 'heat') {
    tags.push({
      rating: '评分最高',
      price_asc: '价格最低',
      price_desc: '价格最高'
    }[filters.sortBy])
  }
  return tags.filter(Boolean)
})

// 工具方法
const applyUpdatedSpot = () => {
  const raw = localStorage.getItem(SPOT_DETAIL_UPDATED_KEY)
  if (!raw) return

  const updatedSpot = JSON.parse(raw)
  const index = spotList.value.findIndex((item) => item.id === updatedSpot.id)
  if (index !== -1) {
    spotList.value[index] = { ...spotList.value[index], ...updatedSpot }
  }
  localStorage.removeItem(SPOT_DETAIL_UPDATED_KEY)
}

const syncRouteQuery = () => {
  router.replace({
    path: '/spots',
    query: {
      ...(filters.regionId ? { regionId: filters.regionId } : {}),
      ...(filters.categoryId ? { categoryId: filters.categoryId } : {}),
      ...(filters.sortBy ? { sortBy: filters.sortBy } : {})
    }
  })
}

// 数据加载方法
const fetchFilters = async () => {
  const res = await getFilters()
  regionTree.value = res.data?.regionTree?.length ? res.data.regionTree : (res.data?.regions || [])
  categoryTree.value = res.data?.categoryTree?.length ? res.data.categoryTree : (res.data?.categories || [])
}

const fetchSpotList = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize,
      sortBy: filters.sortBy
    }
    if (filters.regionId) params.regionId = filters.regionId
    if (filters.categoryId) params.categoryId = filters.categoryId

    const res = await getSpotList(params)
    spotList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

// 交互处理方法
const handleFilter = () => {
  page.value = 1
  syncRouteQuery()
  fetchSpotList()
}

const changeSort = (value) => {
  if (filters.sortBy === value) return
  filters.sortBy = value
  handleFilter()
}

const resetFilters = () => {
  filters.regionId = ''
  filters.categoryId = ''
  filters.sortBy = 'heat'
  handleFilter()
}

// 生命周期
onMounted(async () => {
  if (typeof route.query.regionId === 'string') {
    filters.regionId = route.query.regionId
  }
  if (typeof route.query.categoryId === 'string') {
    filters.categoryId = route.query.categoryId
  }
  if (typeof route.query.sortBy === 'string') {
    filters.sortBy = route.query.sortBy
  }

  await fetchFilters()
  await fetchSpotList()
  applyUpdatedSpot()
})
</script>

<style lang="scss" scoped>
.spot-list-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.search-strip {
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #909399;
  cursor: pointer;
}

.state-card {
  padding: 22px;
}

.state-main {
  margin-bottom: 16px;
}

.state-title {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 8px;
}

.state-desc {
  color: #909399;
  line-height: 1.7;
}

.state-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-bar {
  padding: 18px;
}

.filter-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-group {
  flex: 1 1 260px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  white-space: nowrap;
  color: #606266;
}

.active-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: #e8f1ff;
  color: #2563eb;
  font-size: 13px;
}

.tag-chip.plain {
  background: #f3f4f6;
  color: #4b5563;
}

.spot-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  min-height: 200px;
}

.spot-card {
  overflow: hidden;
  cursor: pointer;
}

.spot-image-box {
  position: relative;
}

.spot-image {
  width: 100%;
  height: 240px;
  object-fit: cover;
}

.rating-badge {
  position: absolute;
  left: 14px;
  bottom: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.score {
  color: #f59e0b;
  font-weight: 700;
}

.unit {
  color: #64748b;
  font-size: 12px;
}

.spot-content {
  padding: 16px;
}

.spot-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
}

.spot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.spot-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.price-box {
  display: flex;
  align-items: baseline;
  color: #ef4444;
}

.symbol {
  font-size: 14px;
}

.num {
  font-size: 28px;
  font-weight: 700;
}

.suffix,
.meta-box {
  color: #909399;
  font-size: 13px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

@media (max-width: 992px) {
  .spot-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .spot-grid {
    grid-template-columns: 1fr;
  }
}
</style>
