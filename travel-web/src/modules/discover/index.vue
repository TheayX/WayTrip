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

    <DiscoverQuickPanel :items="quickEntries" />

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
        <SpotCard
          v-for="spot in spotList"
          :key="spot.id"
          :spot="spot"
          @select="$router.push(`/spots/${spot.id}?source=discover`)"
        />
      </div>
      <el-empty v-else description="当前条件暂无景点" />
    </section>

    <section v-if="showGuideSection" class="content-section">
      <div class="section-head">
        <h3>精华攻略</h3>
        <el-button text type="primary" @click="$router.push('/guides')">查看全部</el-button>
      </div>
      <div v-if="guideList.length" class="guide-grid">
        <GuideCard
          v-for="guide in guideList"
          :key="guide.id"
          :guide="guide"
          @select="$router.push(`/guides/${guide.id}`)"
        />
      </div>
      <el-empty v-else description="当前条件暂无攻略" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DiscoverQuickPanel from '@/modules/discover/components/DiscoverQuickPanel.vue'
import GuideCard from '@/modules/guide/components/GuideCard.vue'
import { getGuideList, getCategories } from '@/modules/guide/api.js'
import SpotCard from '@/modules/spot/components/SpotCard.vue'
import { getSpotList, getFilters } from '@/modules/spot/api.js'
import { useUserStore } from '@/modules/account/store/user.js'

const DISCOVER_STATE_KEY = 'discover_state'

// 基础依赖与用户状态
const userStore = useUserStore()
const route = useRoute()
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
const quickEntries = computed(() => ([
  { key: 'spots', label: '全部景点', handler: () => router.push('/spots?sortBy=heat') },
  { key: 'guides', label: '游玩攻略', handler: () => router.push('/guides') },
  { key: 'recommend', label: '推荐景点', handler: () => goRecommendations() }
]))

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

const applyRoutePreset = () => {
  const tab = typeof route.query.tab === 'string' ? route.query.tab : ''
  if (['all', 'spot', 'guide'].includes(tab)) {
    activeTab.value = tab
  }
  if (typeof route.query.regionId === 'string') {
    selectedRegionId.value = route.query.regionId
  }
  if (typeof route.query.categoryId === 'string') {
    selectedSpotCategoryId.value = route.query.categoryId
  }
  if (typeof route.query.guideCategory === 'string') {
    selectedGuideCategory.value = route.query.guideCategory
  }
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
  applyRoutePreset()
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

@media (max-width: 900px) {
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
