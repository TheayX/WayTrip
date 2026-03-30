<!-- 发现页 -->
<template>
  <div class="page-container discover-page">
    <section class="discover-hero card">
      <div>
        <h2 class="page-title">发现</h2>
        <p class="page-subtitle">用一个页面联动浏览景点与攻略。</p>
      </div>
      <div class="hero-search" @click="$router.push('/search')">
        <el-icon><Search /></el-icon>
        <span>搜索景点、攻略...</span>
      </div>
    </section>

    <section class="quick-panel">
      <div class="quick-card card" @click="$router.push('/spots?sortBy=heat')">全部景点</div>
      <div class="quick-card card" @click="$router.push('/guides')">游玩攻略</div>
      <div class="quick-card card" @click="goRecommendations">推荐景点</div>
    </section>

    <section class="tab-panel card">
      <el-radio-group v-model="activeTab" @change="handleTabChange">
        <el-radio-button label="all">综合</el-radio-button>
        <el-radio-button label="spot">景点</el-radio-button>
        <el-radio-button label="guide">攻略</el-radio-button>
      </el-radio-group>
    </section>

    <section class="filters card">
      <div v-if="showSpotFilters" class="filter-group">
        <span class="filter-label">地区</span>
        <el-select v-model="selectedRegionId" clearable placeholder="全部地区" @change="handleSpotFilter">
          <el-option label="全部" value="" />
          <el-option v-for="item in regions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </div>
      <div v-if="showSpotFilters" class="filter-group">
        <span class="filter-label">分类</span>
        <el-select v-model="selectedSpotCategoryId" clearable placeholder="全部分类" @change="handleSpotFilter">
          <el-option label="全部" value="" />
          <el-option v-for="item in spotCategories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </div>
      <div v-if="showGuideFilters" class="filter-group">
        <span class="filter-label">主题</span>
        <el-select v-model="selectedGuideCategory" clearable placeholder="全部主题" @change="handleGuideFilter">
          <el-option label="全部" value="" />
          <el-option v-for="item in guideCategories" :key="item" :label="item" :value="item" />
        </el-select>
      </div>
    </section>

    <section v-if="showSpotSection" class="content-section">
      <div class="section-head">
        <h3>探索景点</h3>
        <el-button text type="primary" @click="$router.push('/spots')">查看全部</el-button>
      </div>
      <div v-if="spotList.length" class="spot-grid">
        <article v-for="spot in spotList" :key="spot.id" class="spot-card card" @click="$router.push(`/spots/${spot.id}?source=discover`)">
          <img :src="getImageUrl(spot.coverImage)" class="spot-image" alt="" />
          <div class="spot-content">
            <div class="spot-top">
              <h4 class="spot-name">{{ spot.name }}</h4>
              <span class="price">¥{{ spot.price }}</span>
            </div>
            <p class="spot-desc">{{ spot.intro || '暂无介绍' }}</p>
            <div class="spot-meta">
              <span class="tag">{{ spot.regionName || '地区待补充' }}</span>
              <span class="tag">{{ spot.categoryName || '分类待补充' }}</span>
            </div>
          </div>
        </article>
      </div>
      <el-empty v-else description="当前条件暂无景点" />
    </section>

    <section v-if="showGuideSection" class="content-section">
      <div class="section-head">
        <h3>精华攻略</h3>
        <el-button text type="primary" @click="$router.push('/guides')">查看全部</el-button>
      </div>
      <div v-if="guideList.length" class="guide-grid">
        <article v-for="guide in guideList" :key="guide.id" class="guide-card card" @click="$router.push(`/guides/${guide.id}`)">
          <img :src="getImageUrl(guide.coverImage)" class="guide-image" alt="" />
          <div class="guide-content">
            <h4 class="guide-title">{{ guide.title }}</h4>
            <p class="guide-desc">{{ guide.summary || '带上好心情，发现更多旅行灵感。' }}</p>
            <div class="guide-meta">
              <span class="tag">{{ guide.category || '攻略' }}</span>
              <span>👁 {{ guide.viewCount || 0 }}</span>
            </div>
          </div>
        </article>
      </div>
      <el-empty v-else description="当前条件暂无攻略" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getGuideList, getCategories } from '@/api/guide'
import { getSpotList, getFilters } from '@/api/spot'
import { getImageUrl } from '@/utils/request'
import { useUserStore } from '@/stores/user'

const DISCOVER_STATE_KEY = 'discover_state'

// 基础依赖与用户状态
const userStore = useUserStore()
const router = useRouter()

// 页面数据状态
const activeTab = ref('all')
const regions = ref([])
const spotCategories = ref([])
const guideCategories = ref([])
const selectedRegionId = ref('')
const selectedSpotCategoryId = ref('')
const selectedGuideCategory = ref('')
const spotList = ref([])
const guideList = ref([])

// 计算属性
const showSpotFilters = computed(() => activeTab.value === 'all' || activeTab.value === 'spot')
const showGuideFilters = computed(() => activeTab.value === 'all' || activeTab.value === 'guide')
const showSpotSection = computed(() => activeTab.value === 'all' || activeTab.value === 'spot')
const showGuideSection = computed(() => activeTab.value === 'all' || activeTab.value === 'guide')

// 工具方法
const persistState = () => {
  localStorage.setItem(DISCOVER_STATE_KEY, JSON.stringify({
    tab: activeTab.value,
    selectedRegionId: selectedRegionId.value,
    selectedSpotCategoryId: selectedSpotCategoryId.value,
    selectedGuideCategory: selectedGuideCategory.value
  }))
}

const restoreState = () => {
  const raw = localStorage.getItem(DISCOVER_STATE_KEY)
  if (!raw) return

  const state = JSON.parse(raw)
  activeTab.value = ['all', 'spot', 'guide'].includes(state.tab) ? state.tab : 'all'
  selectedRegionId.value = state.selectedRegionId || ''
  selectedSpotCategoryId.value = state.selectedSpotCategoryId || ''
  selectedGuideCategory.value = state.selectedGuideCategory || ''
}

// 数据加载方法
const fetchSpotFilters = async () => {
  const res = await getFilters()
  regions.value = res.data?.regions || []
  spotCategories.value = res.data?.categories || []
}

const fetchGuideCategories = async () => {
  const res = await getCategories()
  guideCategories.value = res.data || []
}

const fetchSpotPreview = async () => {
  const params = { page: 1, pageSize: 6, sortBy: 'heat' }
  if (selectedRegionId.value) params.regionId = selectedRegionId.value
  if (selectedSpotCategoryId.value) params.categoryId = selectedSpotCategoryId.value
  const res = await getSpotList(params)
  spotList.value = res.data?.list || []
}

const fetchGuidePreview = async () => {
  const params = { page: 1, pageSize: 6, sortBy: 'time' }
  if (selectedGuideCategory.value) params.category = selectedGuideCategory.value
  const res = await getGuideList(params)
  guideList.value = res.data?.list || []
}

const refreshDiscover = async () => {
  await Promise.all([fetchSpotPreview(), fetchGuidePreview()])
}

// 交互处理方法
const handleTabChange = async () => {
  persistState()
  await refreshDiscover()
}

const handleSpotFilter = async () => {
  persistState()
  await fetchSpotPreview()
}

const handleGuideFilter = async () => {
  persistState()
  await fetchGuidePreview()
}

const goRecommendations = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后可查看推荐景点')
    return
  }
  router.push('/recommendations')
}

watch([activeTab, selectedRegionId, selectedSpotCategoryId, selectedGuideCategory], persistState)

// 生命周期
onMounted(async () => {
  restoreState()
  await Promise.all([fetchSpotFilters(), fetchGuideCategories()])
  await refreshDiscover()
})
</script>

<style lang="scss" scoped>
.discover-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.discover-hero {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.page-title {
  font-size: 30px;
  font-weight: 700;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #909399;
}

.hero-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px;
  border-radius: 999px;
  background: #f5f7fa;
  color: #606266;
  cursor: pointer;
}

.quick-panel {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.quick-card {
  padding: 18px;
  text-align: center;
  cursor: pointer;
  font-weight: 600;
}

.tab-panel,
.filters {
  padding: 18px 20px;
}

.filters {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1 1 240px;
}

.filter-label {
  color: #606266;
  white-space: nowrap;
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.spot-grid,
.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.spot-card,
.guide-card {
  cursor: pointer;
}

.spot-image,
.guide-image {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.spot-content,
.guide-content {
  padding: 16px;
}

.spot-top,
.spot-meta,
.guide-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.spot-name,
.guide-title {
  font-size: 17px;
  font-weight: 600;
}

.spot-desc,
.guide-desc {
  margin: 12px 0;
  line-height: 1.6;
  color: #606266;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 900px) {
  .quick-panel,
  .spot-grid,
  .guide-grid {
    grid-template-columns: 1fr;
  }

  .discover-hero {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
