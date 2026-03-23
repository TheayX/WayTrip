<template>
  <div class="page-container">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>景点列表</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 筛选栏 -->
    <div class="filter-bar card">
      <div class="filter-row">
        <div class="filter-group">
          <span class="filter-label">地区：</span>
          <el-popover
            v-model:visible="regionPopoverVisible"
            placement="bottom-start"
            :width="420"
            trigger="click"
            @show="initRegionPanel"
          >
            <template #reference>
              <button class="filter-trigger" type="button">
                <span>{{ selectedRegionLabel }}</span>
                <span class="filter-trigger-arrow">▼</span>
              </button>
            </template>
            <div class="dual-panel">
              <div class="panel-left">
                <button
                  v-for="region in regionTree"
                  :key="region.id"
                  class="panel-item"
                  :class="{ active: activeRegionParentId === region.id }"
                  type="button"
                  @click="selectRegionParent(region.id)"
                >
                  {{ region.name }}
                </button>
              </div>
              <div class="panel-right">
                <button
                  v-for="child in activeRegionChildren"
                  :key="child.id"
                  class="panel-item"
                  type="button"
                  @click="selectRegion(child.id)"
                >
                  {{ child.name }}
                </button>
                <div v-if="!activeRegionChildren.length" class="panel-empty">暂无子地区</div>
              </div>
            </div>
          </el-popover>
        </div>
        <div class="filter-group">
          <span class="filter-label">分类：</span>
          <el-popover
            v-model:visible="categoryPopoverVisible"
            placement="bottom-start"
            :width="420"
            trigger="click"
            @show="initCategoryPanel"
          >
            <template #reference>
              <button class="filter-trigger" type="button">
                <span>{{ selectedCategoryLabel }}</span>
                <span class="filter-trigger-arrow">▼</span>
              </button>
            </template>
            <div class="dual-panel">
              <div class="panel-left">
                <button
                  v-for="category in categoryTree"
                  :key="category.id"
                  class="panel-item"
                  :class="{ active: activeCategoryParentId === category.id }"
                  type="button"
                  @click="selectCategoryParent(category.id)"
                >
                  {{ category.name }}
                </button>
              </div>
              <div class="panel-right">
                <button
                  v-for="child in activeCategoryChildren"
                  :key="child.id"
                  class="panel-item"
                  type="button"
                  @click="selectCategory(child.id)"
                >
                  {{ child.name }}
                </button>
                <div v-if="!activeCategoryChildren.length" class="panel-empty">暂无子分类</div>
              </div>
            </div>
          </el-popover>
        </div>
        <div class="filter-group">
          <span class="filter-label">排序：</span>
          <el-select v-model="filters.sortBy" @change="handleFilter">
            <el-option label="综合排序" value="" />
            <el-option label="价格从低到高" value="price_asc" />
            <el-option label="价格从高到低" value="price_desc" />
            <el-option label="评分最高" value="rating" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 景点列表 -->
    <div v-loading="loading" class="spot-grid">
      <div
        v-for="spot in spotList"
        :key="spot.id"
        class="spot-card card"
        @click="$router.push(`/spots/${spot.id}`)"
      >
        <div class="spot-img-wrapper">
          <img :src="getImageUrl(spot.coverImage)" class="spot-img" alt="" />
        </div>
        <div class="spot-info">
          <h3 class="spot-name">{{ spot.name }}</h3>
          <p class="spot-region">{{ spot.regionName }} · {{ spot.categoryName }}</p>
          <p class="spot-desc">{{ spot.intro || '暂无介绍' }}</p>
          <div class="spot-bottom">
            <span class="star-text">★ {{ spot.avgRating || '-' }}</span>
            <span class="price">¥{{ spot.price }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && spotList.length === 0" description="暂无景点" />

    <!-- 分页 -->
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
import { computed, ref, reactive, onMounted } from 'vue'
import { getSpotList, getFilters } from '@/api/spot'
import { getImageUrl } from '@/utils/request'

const spotList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 12
const total = ref(0)

const regionTree = ref([])
const categoryTree = ref([])
const activeRegionParentId = ref(null)
const activeCategoryParentId = ref(null)
const regionPopoverVisible = ref(false)
const categoryPopoverVisible = ref(false)

const filters = reactive({
  regionId: '',
  categoryId: '',
  sortBy: ''
})

const activeRegionParent = computed(() =>
  regionTree.value.find(item => item.id === activeRegionParentId.value) || null
)

const activeCategoryParent = computed(() =>
  categoryTree.value.find(item => item.id === activeCategoryParentId.value) || null
)

const activeRegionChildren = computed(() => activeRegionParent.value?.children || [])
const activeCategoryChildren = computed(() => activeCategoryParent.value?.children || [])

const selectedRegionLabel = computed(() => findNodeName(filters.regionId, regionTree.value, '全部地区'))
const selectedCategoryLabel = computed(() => findNodeName(filters.categoryId, categoryTree.value, '全部分类'))

const fetchFilters = async () => {
  try {
    const res = await getFilters()
    regionTree.value = res.data?.regionTree || res.data?.regions || []
    categoryTree.value = res.data?.categoryTree || res.data?.categories || []
  } catch (e) { /* ignore */ }
}

const fetchSpotList = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize,
      ...filters
    }
    // 清理空值
    Object.keys(params).forEach(key => {
      if (!params[key] && params[key] !== 0) delete params[key]
    })
    const res = await getSpotList(params)
    spotList.value = res.data?.list || res.data || []
    total.value = res.data?.total || 0
  } catch (e) { /* ignore */ }
  loading.value = false
}

const handleFilter = () => {
  page.value = 1
  fetchSpotList()
}

const findNodeName = (id, tree, fallback) => {
  if (!id) return fallback
  const stack = [...tree]
  while (stack.length) {
    const node = stack.pop()
    if (!node) continue
    if (node.id === id) return node.name
    if (Array.isArray(node.children) && node.children.length) {
      stack.push(...node.children)
    }
  }
  return fallback
}

const initRegionPanel = () => {
  activeRegionParentId.value = filters.regionId
    ? findParentId(filters.regionId, regionTree.value)
    : regionTree.value[0]?.id || null
}

const initCategoryPanel = () => {
  activeCategoryParentId.value = filters.categoryId
    ? findParentId(filters.categoryId, categoryTree.value)
    : categoryTree.value[0]?.id || null
}

const findParentId = (id, tree) => {
  for (const node of tree) {
    if (node.id === id) return node.id
    if (Array.isArray(node.children) && node.children.some(child => child.id === id)) {
      return node.id
    }
  }
  return tree[0]?.id || null
}

const selectRegion = (id) => {
  filters.regionId = id
  regionPopoverVisible.value = false
  handleFilter()
}

const selectRegionParent = (id) => {
  activeRegionParentId.value = id
  selectRegion(id)
}

const selectCategory = (id) => {
  filters.categoryId = id
  categoryPopoverVisible.value = false
  handleFilter()
}

const selectCategoryParent = (id) => {
  activeCategoryParentId.value = id
  selectCategory(id)
}

const clearRegionFilter = () => {
  filters.regionId = ''
  regionPopoverVisible.value = false
  handleFilter()
}

const clearCategoryFilter = () => {
  filters.categoryId = ''
  categoryPopoverVisible.value = false
  handleFilter()
}

onMounted(() => {
  fetchFilters()
  fetchSpotList()
})
</script>

<style lang="scss" scoped>
.filter-bar {
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 12px;
}

.filter-row {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1 1 260px;
  min-width: 260px;
}

.filter-group :deep(.el-select) {
  width: 100%;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.filter-trigger {
  width: 100%;
  min-height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 14px;
  font-size: 14px;
  cursor: pointer;
}

.filter-trigger:hover {
  border-color: #409eff;
}

.filter-trigger-arrow {
  color: #909399;
  font-size: 12px;
}

.dual-panel {
  display: flex;
  min-height: 260px;
}

.panel-left {
  width: 160px;
  border-right: 1px solid #ebeef5;
  padding-right: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.panel-right {
  flex: 1;
  padding-left: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.panel-item {
  border: 0;
  background: #fff;
  text-align: left;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: #303133;
  font-size: 14px;
}

.panel-item:hover,
.panel-item.active {
  background: #f5f7fa;
}

.panel-empty {
  padding: 10px 12px;
  color: #909399;
  font-size: 13px;
}

.spot-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  min-height: 200px;
}

.spot-card {
  cursor: pointer;
  border-radius: 12px;
}

.spot-img-wrapper {
  aspect-ratio: 16 / 10;
  overflow: hidden;
  border-radius: 12px 12px 0 0;
}

.spot-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;

  .spot-card:hover & {
    transform: scale(1.05);
  }
}

.spot-info {
  padding: 14px;
}

.spot-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spot-region {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.spot-desc {
  font-size: 13px;
  color: #606266;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
  margin-bottom: 10px;
}

.spot-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 992px) {
  .spot-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 576px) {
  .filter-group {
    min-width: 100%;
  }

  .spot-grid {
    grid-template-columns: 1fr;
  }
}
</style>

