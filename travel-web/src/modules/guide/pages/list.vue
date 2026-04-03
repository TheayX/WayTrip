<!-- 攻略列表页 -->
<template>
  <div class="page-container guide-list-page">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>旅行攻略</el-breadcrumb-item>
    </el-breadcrumb>

    <section class="state-card card">
      <div class="state-main">
        <h2 class="state-title">攻略列表</h2>
        <p class="state-desc">{{ currentStateText }}</p>
      </div>
      <div class="state-actions">
        <el-button text :type="sortBy === 'time' ? 'primary' : 'default'" @click="changeSort('time')">最新优先</el-button>
        <el-button text :type="sortBy === 'category' ? 'primary' : 'default'" @click="changeSort('category')">分类排序</el-button>
        <el-button text @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section class="category-bar card">
      <el-check-tag :checked="currentCategory === ''" @change="selectCategory('')">全部</el-check-tag>
      <el-check-tag
        v-for="cat in categories"
        :key="cat"
        :checked="currentCategory === cat"
        @change="selectCategory(cat)"
      >
        {{ cat }}
      </el-check-tag>
    </section>

    <section v-loading="loading" class="guide-grid">
      <article
        v-for="guide in guideList"
        :key="guide.id"
        class="guide-card card"
        @click="$router.push(`/guides/${guide.id}`)"
      >
        <img :src="getImageUrl(guide.coverImage)" class="guide-image" alt="" />
        <div class="guide-content">
          <h3 class="guide-title">{{ guide.title }}</h3>
          <p class="guide-summary">{{ guide.summary || '带上好心情，发现更多旅行灵感。' }}</p>
          <div class="guide-meta">
            <span class="tag">{{ guide.category || '攻略' }}</span>
            <span class="guide-views">👁 {{ guide.viewCount || 0 }}</span>
          </div>
        </div>
      </article>
    </section>

    <el-empty v-if="!loading && guideList.length === 0" description="暂无攻略">
      <el-button @click="resetFilters">清空筛选</el-button>
    </el-empty>

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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGuideList, getCategories } from '@/modules/guide/api.js'
import { getImageUrl } from '@/shared/api/client.js'

const GUIDE_DETAIL_UPDATED_KEY = 'guide_detail_updated'

// 基础依赖与路由状态
const route = useRoute()
const router = useRouter()

// 页面数据状态
const categories = ref([])
const currentCategory = ref('')
const guideList = ref([])
const page = ref(1)
const pageSize = 12
const total = ref(0)
const loading = ref(false)
const sortBy = ref('time')

// 计算属性
const currentStateText = computed(() => {
  const categoryText = currentCategory.value || '全部分类'
  const sortText = sortBy.value === 'category' ? '分类排序' : '最新优先'
  return `${categoryText} · 共 ${total.value} 条 · ${sortText}`
})

// 工具方法
const syncRouteQuery = () => {
  router.replace({
    path: '/guides',
    query: {
      ...(currentCategory.value ? { category: currentCategory.value } : {}),
      ...(sortBy.value ? { sortBy: sortBy.value } : {})
    }
  })
}

const applyUpdatedGuide = () => {
  const raw = localStorage.getItem(GUIDE_DETAIL_UPDATED_KEY)
  if (!raw) return

  const updatedGuide = JSON.parse(raw)
  const index = guideList.value.findIndex((item) => item.id === updatedGuide.id)
  if (index !== -1) {
    guideList.value[index] = { ...guideList.value[index], ...updatedGuide }
  }
  localStorage.removeItem(GUIDE_DETAIL_UPDATED_KEY)
}

// 数据加载方法
const fetchCategories = async () => {
  const res = await getCategories()
  categories.value = res.data || []
}

const fetchGuideList = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize, sortBy: sortBy.value }
    if (currentCategory.value) params.category = currentCategory.value
    const res = await getGuideList(params)
    guideList.value = res.data?.list || res.data || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

// 交互处理方法
const selectCategory = (cat) => {
  currentCategory.value = cat
  page.value = 1
  syncRouteQuery()
  fetchGuideList()
}

const changeSort = (value) => {
  if (sortBy.value === value) return
  sortBy.value = value
  page.value = 1
  syncRouteQuery()
  fetchGuideList()
}

const resetFilters = () => {
  currentCategory.value = ''
  sortBy.value = 'time'
  page.value = 1
  syncRouteQuery()
  fetchGuideList()
}

// 生命周期
onMounted(async () => {
  if (typeof route.query.category === 'string' && route.query.category) {
    currentCategory.value = route.query.category
  }
  if (route.query.sortBy === 'time' || route.query.sortBy === 'category') {
    sortBy.value = route.query.sortBy
  }

  await fetchCategories()
  await fetchGuideList()
  applyUpdatedGuide()
})
</script>

<style lang="scss" scoped>
.guide-list-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
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
  gap: 8px;
}

.category-bar {
  padding: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  min-height: 200px;
}

.guide-card {
  cursor: pointer;
}

.guide-image {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.guide-content {
  padding: 16px;
}

.guide-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 10px;
}

.guide-summary {
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 12px;
}

.guide-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.guide-views {
  color: #909399;
  font-size: 13px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

@media (max-width: 992px) {
  .guide-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .guide-grid {
    grid-template-columns: 1fr;
  }
}
</style>
