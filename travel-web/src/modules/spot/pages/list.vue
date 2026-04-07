<!-- 景点列表页 -->
<template>
  <div class="page-container spot-list-page">
    <section class="search-strip premium-card" @click="$router.push('/search')">
      <el-icon><Search /></el-icon>
      <span>搜索景点名称、城市或分类</span>
    </section>

    <SpotListToolbar
      :description="currentStateText"
      :sort-by="filters.sortBy"
      @sort-change="changeSort"
      @reset="resetFilters"
    />

    <SpotFilterBar
      v-model:region-id="filters.regionId"
      v-model:category-id="filters.categoryId"
      :regions="flatRegions"
      :categories="flatCategories"
      @change="handleFilter"
    />

    <section class="active-filters premium-card" v-if="activeFilterTags.length">
      <p class="filter-kicker">当前已选</p>
      <div class="tag-row">
        <span v-for="tag in activeFilterTags" :key="tag" class="tag-chip">{{ tag }}</span>
      </div>
    </section>

    <section v-loading="loading" class="spot-grid">
      <SpotCard
        v-for="spot in spotList"
        :key="spot.id"
        :spot="spot"
        @select="router.push(buildSpotDetailRoute(spot.id, SPOT_DETAIL_SOURCE.LIST))"
      />
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
import SpotCard from '@/modules/spot/components/SpotCard.vue'
import SpotFilterBar from '@/modules/spot/components/SpotFilterBar.vue'
import SpotListToolbar from '@/modules/spot/components/SpotListToolbar.vue'
import { getSpotList, getFilters } from '@/modules/spot/api.js'
import { buildSpotDetailRoute, SPOT_DETAIL_SOURCE } from '@/shared/constants/spot-detail.js'

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
  padding-top: 4px;
}

.search-strip {
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #475569;
  cursor: pointer;
}

.active-filters {
  padding: 18px 20px;
}

.filter-kicker {
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f8fafc;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.spot-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  min-height: 200px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

@media (max-width: 992px) {
  .spot-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .spot-grid {
    grid-template-columns: 1fr;
  }
}
</style>
