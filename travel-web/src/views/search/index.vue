<template>
  <div class="page-container">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>搜索</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 搜索框 -->
    <div class="search-box">
      <el-input
        v-model="keyword"
        placeholder="搜索景点名称、城市..."
        size="large"
        clearable
        @keyup.enter="handleSearch"
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <!-- 搜索结果 -->
    <div v-if="searched">
      <p class="result-summary" v-if="total > 0">
        共找到 <strong>{{ total }}</strong> 个相关景点
      </p>

      <div v-loading="loading" class="result-list">
        <div
          v-for="spot in results"
          :key="spot.id"
          class="result-card card"
          @click="$router.push(`/spots/${spot.id}`)"
        >
          <img :src="getImageUrl(spot.coverImage)" class="result-img" alt="" />
          <div class="result-info">
            <h3 class="result-name">{{ spot.name }}</h3>
            <p class="result-region">{{ spot.regionName }} · {{ spot.categoryName }}</p>
            <p class="result-desc">{{ spot.intro || '暂无介绍' }}</p>
            <div class="result-bottom">
              <span class="star-text">★ {{ spot.avgRating || '-' }}</span>
              <span class="price">¥{{ spot.price }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && results.length === 0" description="未找到相关景点" />

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="doSearch"
        />
      </div>
    </div>

    <!-- 未搜索状态 -->
    <div v-else class="search-hint">
      <el-icon :size="64" color="#c0c4cc"><Search /></el-icon>
      <p>输入关键词搜索你想去的景点</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { searchSpots } from '@/api/spot'
import { getImageUrl } from '@/utils/request'

const keyword = ref('')
const results = ref([])
const loading = ref(false)
const searched = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)

const handleSearch = () => {
  if (!keyword.value.trim()) return
  page.value = 1
  doSearch()
}

const doSearch = async () => {
  loading.value = true
  searched.value = true
  try {
    const res = await searchSpots(keyword.value, page.value, pageSize)
    results.value = res.data?.list || res.data || []
    total.value = res.data?.total || 0
  } catch (e) { /* ignore */ }
  loading.value = false
}
</script>

<style lang="scss" scoped>
.search-box {
  margin: 20px 0;
}

.search-input {
  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }
}

.result-summary {
  font-size: 14px;
  color: #909399;
  margin-bottom: 16px;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}

.result-card {
  display: flex;
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  }
}

.result-img {
  width: 220px;
  height: 160px;
  object-fit: cover;
  flex-shrink: 0;
}

.result-info {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.result-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.result-region {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.result-desc {
  font-size: 13px;
  color: #606266;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.result-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #c0c4cc;

  p {
    margin-top: 16px;
    font-size: 16px;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 768px) {
  .result-card {
    flex-direction: column;
  }

  .result-img {
    width: 100%;
    height: 200px;
  }
}
</style>

