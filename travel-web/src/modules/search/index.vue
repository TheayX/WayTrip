<!-- 综合搜索页 -->
<template>
  <div class="page-container search-page">
    <section class="search-hero card">
      <div>
        <p class="hero-eyebrow">Search</p>
        <h2 class="page-title">综合搜索</h2>
        <p class="page-subtitle">同一关键词下同时浏览景点和攻略，更符合 Web 端的信息检索习惯。</p>
      </div>
    </section>

    <section class="search-box card">
      <el-input
        v-model="keyword"
        placeholder="搜索景点名称、城市、攻略标题..."
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
      <div class="search-meta">
        <el-radio-group v-model="activeTab" @change="handleTabChange">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="spot">景点</el-radio-button>
          <el-radio-button label="guide">攻略</el-radio-button>
        </el-radio-group>
        <span v-if="searched" class="result-total">{{ totalText }}</span>
      </div>
    </section>

    <template v-if="searched">
      <section v-if="showSpotSection" class="result-section">
        <div class="section-head">
          <div>
            <h3>景点结果</h3>
            <p>共找到 {{ spotTotal }} 个相关景点</p>
          </div>
          <el-button v-if="activeTab === 'all' && spotTotal > 0" text type="primary" @click="activeTab = 'spot'">只看景点</el-button>
        </div>

        <div v-loading="spotLoading" class="result-stack">
          <article
            v-for="spot in spotResults"
            :key="spot.id"
            class="result-card card"
            @click="$router.push(`/spots/${spot.id}?source=search`)"
          >
            <img :src="getImageUrl(spot.coverImage)" class="result-img" alt="" />
            <div class="result-info">
              <h4 class="result-name">{{ spot.name }}</h4>
              <p class="result-region">{{ spot.regionName }} · {{ spot.categoryName }}</p>
              <p class="result-desc">{{ spot.intro || '暂无介绍' }}</p>
              <div class="result-bottom">
                <span class="star-text">★ {{ spot.avgRating || '-' }}</span>
                <span class="price">¥{{ spot.price }}</span>
              </div>
            </div>
          </article>
        </div>

        <el-empty v-if="!spotLoading && spotResults.length === 0" description="未找到相关景点" />

        <div class="pagination" v-if="activeTab !== 'all' && activeTab === 'spot' && spotTotal > 0">
          <el-pagination
            v-model:current-page="spotPage"
            :page-size="spotPageSize"
            :total="spotTotal"
            layout="prev, pager, next"
            @current-change="fetchSpotResults"
          />
        </div>
      </section>

      <section v-if="showGuideSection" class="result-section">
        <div class="section-head">
          <div>
            <h3>攻略结果</h3>
            <p>共找到 {{ guideTotal }} 篇相关攻略</p>
          </div>
          <el-button v-if="activeTab === 'all' && guideTotal > 0" text type="primary" @click="activeTab = 'guide'">只看攻略</el-button>
        </div>

        <div v-loading="guideLoading" class="guide-grid">
          <article
            v-for="guide in guideResults"
            :key="guide.id"
            class="guide-card card"
            @click="$router.push(`/guides/${guide.id}`)"
          >
            <img :src="getImageUrl(guide.coverImage)" class="guide-image" alt="" />
            <div class="guide-content">
              <div class="guide-top">
                <h4 class="guide-title">{{ guide.title }}</h4>
                <span class="guide-tag">{{ guide.category || '攻略' }}</span>
              </div>
              <p class="guide-summary">{{ guide.summary || '暂无摘要' }}</p>
              <div class="guide-bottom">
                <span>浏览 {{ guide.viewCount || 0 }}</span>
                <span>{{ guide.createdAt }}</span>
              </div>
            </div>
          </article>
        </div>

        <el-empty v-if="!guideLoading && guideResults.length === 0" description="未找到相关攻略" />

        <div class="pagination" v-if="activeTab !== 'all' && activeTab === 'guide' && guideTotal > 0">
          <el-pagination
            v-model:current-page="guidePage"
            :page-size="guidePageSize"
            :total="guideTotal"
            layout="prev, pager, next"
            @current-change="fetchGuideResults"
          />
        </div>
      </section>

      <el-empty
        v-if="!spotLoading && !guideLoading && spotResults.length === 0 && guideResults.length === 0"
        description="没有找到匹配的景点或攻略"
      />
    </template>

    <section v-else class="search-hint card">
      <el-icon :size="64" color="#c0c4cc"><Search /></el-icon>
      <p>输入关键词后，同时查看景点和攻略结果。</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getGuideList } from '@/modules/guide/api.js'
import { searchSpots } from '@/modules/spot/api.js'
import { APP_ROUTE_PATHS } from '@/shared/constants/route-paths.js'
import { getImageUrl } from '@/shared/api/client.js'

const SEARCH_TABS = ['all', 'spot', 'guide']

// 基础依赖与路由状态
const route = useRoute()
const router = useRouter()

// 页面数据状态
const keyword = ref('')
const activeTab = ref('all')
const searched = ref(false)
const spotLoading = ref(false)
const guideLoading = ref(false)
const spotResults = ref([])
const guideResults = ref([])
const spotPage = ref(1)
const guidePage = ref(1)
const spotPageSize = 8
const guidePageSize = 6
const spotTotal = ref(0)
const guideTotal = ref(0)

// 计算属性
const showSpotSection = computed(() => activeTab.value === 'all' || activeTab.value === 'spot')
const showGuideSection = computed(() => activeTab.value === 'all' || activeTab.value === 'guide')
const totalText = computed(() => `共找到 ${spotTotal.value} 个景点，${guideTotal.value} 篇攻略`)

// 工具方法
const syncRouteQuery = () => {
  router.replace({
    path: APP_ROUTE_PATHS.search,
    query: {
      ...(keyword.value.trim() ? { keyword: keyword.value.trim() } : {}),
      ...(activeTab.value !== 'all' ? { tab: activeTab.value } : {})
    }
  })
}

// 数据加载方法
const fetchSpotResults = async () => {
  if (!keyword.value.trim()) {
    spotResults.value = []
    spotTotal.value = 0
    return
  }

  spotLoading.value = true
  try {
    const res = await searchSpots(keyword.value.trim(), spotPage.value, spotPageSize)
    spotResults.value = res.data?.list || res.data || []
    spotTotal.value = res.data?.total || 0
  } finally {
    spotLoading.value = false
  }
}

const fetchGuideResults = async () => {
  if (!keyword.value.trim()) {
    guideResults.value = []
    guideTotal.value = 0
    return
  }

  guideLoading.value = true
  try {
    const res = await getGuideList({
      keyword: keyword.value.trim(),
      page: guidePage.value,
      pageSize: guidePageSize,
      sortBy: 'time'
    })
    guideResults.value = res.data?.list || res.data || []
    guideTotal.value = res.data?.total || 0
  } finally {
    guideLoading.value = false
  }
}

// 交互处理方法
const handleSearch = async () => {
  if (!keyword.value.trim()) return

  searched.value = true
  spotPage.value = 1
  guidePage.value = 1
  syncRouteQuery()

  // 综合搜索页默认并行拉两类结果，避免切 tab 时才首次加载造成体验割裂。
  await Promise.all([fetchSpotResults(), fetchGuideResults()])
}

const handleTabChange = async () => {
  syncRouteQuery()
  if (!searched.value) return

  if (activeTab.value === 'spot') {
    await fetchSpotResults()
    return
  }
  if (activeTab.value === 'guide') {
    await fetchGuideResults()
    return
  }

  await Promise.all([fetchSpotResults(), fetchGuideResults()])
}

// 生命周期
onMounted(async () => {
  if (typeof route.query.tab === 'string' && SEARCH_TABS.includes(route.query.tab)) {
    activeTab.value = route.query.tab
  }

  if (typeof route.query.keyword === 'string' && route.query.keyword.trim()) {
    keyword.value = route.query.keyword.trim()
    await handleSearch()
  }
})
</script>

<style lang="scss" scoped>
.search-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-hero,
.search-box,
.search-hint {
  padding: 24px;
}

.search-hero {
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.12), transparent 26%),
    linear-gradient(135deg, #f8fafc 0%, #eef6ff 100%);
}

.hero-eyebrow {
  margin-bottom: 10px;
  font-size: 12px;
  letter-spacing: 0.24em;
  color: #64748b;
  text-transform: uppercase;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 10px;
}

.page-subtitle,
.section-head p,
.result-total {
  color: #64748b;
}

.search-meta {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.result-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.section-head h3 {
  margin-bottom: 6px;
  font-size: 24px;
}

.result-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 120px;
}

.result-card {
  display: flex;
  cursor: pointer;
  overflow: hidden;
}

.result-img {
  width: 240px;
  height: 170px;
  object-fit: cover;
  flex-shrink: 0;
}

.result-info {
  flex: 1;
  padding: 18px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.result-name,
.guide-title {
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
}

.result-region,
.guide-bottom,
.guide-summary {
  color: #64748b;
}

.result-desc,
.guide-summary {
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-bottom,
.guide-bottom,
.guide-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
  min-height: 120px;
}

.guide-card {
  cursor: pointer;
  overflow: hidden;
}

.guide-image {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.guide-content {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.guide-tag {
  padding: 4px 10px;
  border-radius: 999px;
  color: #1d4ed8;
  background: #dbeafe;
  font-size: 12px;
  white-space: nowrap;
}

.pagination {
  display: flex;
  justify-content: center;
}

.search-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  color: #94a3b8;
}

.search-hint p {
  margin-top: 16px;
}

@media (max-width: 992px) {
  .guide-grid {
    grid-template-columns: 1fr;
  }

  .search-meta,
  .section-head {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 768px) {
  .result-card {
    flex-direction: column;
  }

  .result-img {
    width: 100%;
    height: 220px;
  }
}
</style>
