<!-- 攻略列表页 -->
<template>
  <div class="page-container guide-list-page">
    <GuideListToolbar
      :description="currentStateText"
      :sort-by="sortBy"
      @sort-change="changeSort"
      @reset="resetFilters"
    />

    <section class="category-bar premium-card">
      <p class="category-kicker">攻略主题</p>
      <div class="category-tags">
        <el-check-tag :checked="currentCategory === ''" @change="selectCategory('')">全部</el-check-tag>
        <el-check-tag
          v-for="cat in categories"
          :key="cat"
          :checked="currentCategory === cat"
          @change="selectCategory(cat)"
        >
          {{ cat }}
        </el-check-tag>
      </div>
    </section>

    <section v-loading="loading" class="guide-grid">
      <GuideCard
        v-for="guide in guideList"
        :key="guide.id"
        :guide="guide"
        @select="$router.push(`/guides/${guide.id}`)"
      />
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
import GuideCard from '@/modules/guide/components/GuideCard.vue'
import GuideListToolbar from '@/modules/guide/components/GuideListToolbar.vue'
import { getGuideList, getCategories } from '@/modules/guide/api.js'

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
  padding-top: 4px;
}

.category-bar {
  padding: 18px 20px;
}

.category-kicker {
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.guide-grid {
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
  .guide-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .guide-grid {
    grid-template-columns: 1fr;
  }
}
</style>
