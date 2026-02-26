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
          <el-select v-model="filters.regionId" placeholder="全部地区" clearable @change="handleFilter">
            <el-option-group v-for="province in regionTree" :key="province.id" :label="province.name">
              <el-option :label="province.name + ' (全部)'" :value="province.id" />
              <el-option v-for="city in province.children" :key="city.id" :label="city.name" :value="city.id" />
            </el-option-group>
          </el-select>
        </div>
        <div class="filter-group">
          <span class="filter-label">分类：</span>
          <el-select v-model="filters.categoryId" placeholder="全部分类" clearable @change="handleFilter">
            <el-option-group v-for="parent in categoryTree" :key="parent.id" :label="parent.name">
              <el-option :label="parent.name + ' (全部)'" :value="parent.id" />
              <el-option v-for="child in parent.children" :key="child.id" :label="child.name" :value="child.id" />
            </el-option-group>
          </el-select>
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
import { ref, reactive, onMounted } from 'vue'
import { getSpotList, getFilters } from '@/api/spot'
import { getImageUrl } from '@/utils/request'

const spotList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 12
const total = ref(0)

const regionTree = ref([])
const categoryTree = ref([])

const filters = reactive({
  regionId: '',
  categoryId: '',
  sortBy: ''
})

const fetchFilters = async () => {
  try {
    const res = await getFilters()
    regionTree.value = res.data?.regions || []
    categoryTree.value = res.data?.categories || []
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
  gap: 24px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
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
  .spot-grid {
    grid-template-columns: 1fr;
  }
}
</style>

