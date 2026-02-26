<template>
  <div class="page-container">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>旅行攻略</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 分类筛选 -->
    <div class="category-bar">
      <el-check-tag
        :checked="currentCategory === ''"
        @change="selectCategory('')"
      >全部</el-check-tag>
      <el-check-tag
        v-for="cat in categories"
        :key="cat"
        :checked="currentCategory === cat"
        @change="selectCategory(cat)"
      >{{ cat }}</el-check-tag>
    </div>

    <!-- 攻略列表 -->
    <div v-loading="loading" class="guide-grid">
      <div
        v-for="guide in guideList"
        :key="guide.id"
        class="guide-card card"
        @click="$router.push(`/guides/${guide.id}`)"
      >
        <div class="guide-img-wrapper">
          <img :src="getImageUrl(guide.coverImage)" class="guide-img" alt="" />
        </div>
        <div class="guide-info">
          <h3 class="guide-title">{{ guide.title }}</h3>
          <p class="guide-summary">{{ guide.summary }}</p>
          <div class="guide-meta">
            <span class="tag">{{ guide.category }}</span>
            <span class="guide-views">👁 {{ guide.viewCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && guideList.length === 0" description="暂无攻略" />

    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchGuideList"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getGuideList, getCategories } from '@/api/guide'
import { getImageUrl } from '@/utils/request'

const categories = ref([])
const currentCategory = ref('')
const guideList = ref([])
const page = ref(1)
const pageSize = 12
const total = ref(0)
const loading = ref(false)

const fetchCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (e) { /* ignore */ }
}

const fetchGuideList = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize, sortBy: 'time' }
    if (currentCategory.value) params.category = currentCategory.value
    const res = await getGuideList(params)
    guideList.value = res.data?.list || res.data || []
    total.value = res.data?.total || 0
  } catch (e) { /* ignore */ }
  loading.value = false
}

const selectCategory = (cat) => {
  currentCategory.value = cat
  page.value = 1
  fetchGuideList()
}

onMounted(() => {
  fetchCategories()
  fetchGuideList()
})
</script>

<style lang="scss" scoped>
.category-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 24px;
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  min-height: 200px;
}

.guide-card {
  cursor: pointer;
  border-radius: 12px;
}

.guide-img-wrapper {
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border-radius: 12px 12px 0 0;
}

.guide-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;

  .guide-card:hover & {
    transform: scale(1.05);
  }
}

.guide-info {
  padding: 14px;
}

.guide-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-summary {
  font-size: 13px;
  color: #909399;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
  margin-bottom: 10px;
}

.guide-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.guide-views {
  font-size: 13px;
  color: #c0c4cc;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 992px) {
  .guide-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 576px) {
  .guide-grid {
    grid-template-columns: 1fr;
  }
}
</style>

