<!-- 发现页 -->
<template>
  <div class="page-container discover-page">
    <section class="discover-hero premium-card">
      <div class="discover-hero-main">
        <p class="hero-eyebrow">Discover More</p>
        <h2 class="page-title">发现灵感</h2>
        <p class="page-subtitle">从推荐、附近、景点和攻略里继续缩小范围，找到更适合这次出行的选择。</p>

        <div class="hero-search-row">
          <button type="button" class="hero-search" @click="$router.push(APP_ROUTE_PATHS.search)">
            <el-icon><Search /></el-icon>
            <span>搜索景点、攻略或目的地</span>
          </button>
          <button type="button" class="hero-link" @click="router.push(APP_ROUTE_PATHS.guides)">浏览攻略</button>
        </div>
      </div>

      <div class="hero-status">
        <article class="status-card">
          <strong>{{ recommendations.length || 0 }}</strong>
          <span>推荐结果</span>
        </article>
        <article class="status-card">
          <strong>{{ nearbySpots.length || 0 }}</strong>
          <span>附近地点</span>
        </article>
        <article class="status-card">
          <strong>{{ spotList.length + guideList.length }}</strong>
          <span>当前内容</span>
        </article>
      </div>
    </section>

    <ExploreKeywordGroup
      title="热门搜索"
      :items="hotKeywords"
      @select="handleKeywordSelect"
    />

    <section class="scene-grid">
      <article
        v-for="item in sceneEntries"
        :key="item.key"
        class="scene-card premium-card"
        :class="{ active: activeScene === item.key }"
        @click="activateScene(item.key)"
      >
        <div class="scene-top">
          <strong>{{ item.title }}</strong>
          <span>{{ item.badge }}</span>
        </div>
        <p>{{ item.desc }}</p>
      </article>
    </section>

    <section class="tab-panel premium-card">
      <div class="tab-panel-main">
        <div>
          <p class="panel-kicker">浏览内容</p>
          <el-radio-group v-model="activeTab" @change="handleTabChange">
            <el-radio-button label="all">综合浏览</el-radio-button>
            <el-radio-button label="spot">景点</el-radio-button>
            <el-radio-button label="guide">攻略</el-radio-button>
          </el-radio-group>
        </div>
        <button type="button" class="section-link" @click="clearScene">清除场景</button>
      </div>
      <p class="tab-panel-desc">{{ currentSceneDescription }}</p>
    </section>

    <section class="filters premium-card">
      <div class="filters-head">
        <div>
          <p class="panel-kicker">筛选条件</p>
          <h3>当前筛选条件</h3>
        </div>
        <p class="filters-summary">{{ filterSummary }}</p>
      </div>

      <div class="filters-grid">
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
      </div>
    </section>

    <section v-if="showRecommendationSection" class="content-section">
      <div class="section-head">
        <div>
          <p class="panel-kicker">推荐</p>
          <h3>{{ userStore.isLoggedIn ? recommendType : '推荐景点' }}</h3>
          <p>根据你的浏览偏好，优先整理值得继续查看的景点。</p>
        </div>
        <div class="section-actions">
          <button type="button" class="section-link" @click="router.push(APP_ROUTE_PATHS.recommendations)">查看全部</button>
          <button v-if="userStore.isLoggedIn" type="button" class="section-link" :disabled="refreshing" @click="handleRefresh">换一批</button>
        </div>
      </div>

      <div v-if="needPreference && userStore.isLoggedIn" class="hint-banner premium-card" @click="showPreferencePopup">
        <div>
          <strong>还没有设置偏好分类</strong>
          <p>先选几类感兴趣的景点，推荐结果会更贴近你的出游偏好。</p>
        </div>
        <el-icon><ArrowRight /></el-icon>
      </div>

      <div v-if="recommendations.length" class="spot-grid">
        <SpotCard
          v-for="spot in recommendations.slice(0, 6)"
          :key="spot.id"
          :spot="spot"
          @select="$router.push(buildSpotDetailRoute(spot.id, SPOT_DETAIL_SOURCE.RECOMMENDATION))"
        />
      </div>
      <el-empty v-else :description="userStore.isLoggedIn ? '当前暂无推荐景点' : '登录后查看推荐景点'">
        <el-button v-if="!userStore.isLoggedIn" type="primary" @click="router.push(AUTH_ROUTE_PATHS.login)">去登录</el-button>
      </el-empty>
    </section>

    <section v-if="showNearbySection" class="content-section">
      <div class="section-head">
        <div>
          <p class="panel-kicker">附近</p>
          <h3>附近探索</h3>
          <p>结合当前位置，快速看看周边有哪些值得去的地方。</p>
        </div>
        <div class="section-actions">
          <el-button :loading="nearbyLoading" type="primary" @click="handleLocate">{{ nearbyLoading ? '定位中' : '重新定位' }}</el-button>
          <button type="button" class="section-link" @click="router.push(APP_ROUTE_PATHS.nearby)">独立页查看</button>
        </div>
      </div>

      <div v-if="nearbySpots.length" class="spot-grid">
        <SpotCard
          v-for="spot in nearbySpots"
          :key="spot.id"
          :spot="spot"
          @select="$router.push(buildSpotDetailRoute(spot.id, SPOT_DETAIL_SOURCE.NEARBY))"
        />
      </div>
      <el-empty v-else :description="nearbyEmptyText">
        <el-button type="primary" @click="handleLocate">开启定位</el-button>
      </el-empty>
    </section>

    <section v-if="showSpotSection" class="content-section">
      <div class="section-head">
        <div>
          <p class="panel-kicker">景点</p>
          <h3>精选景点</h3>
          <p>按地区和分类继续缩小范围，先挑出值得继续查看的目的地。</p>
        </div>
        <button type="button" class="section-link" @click="$router.push(APP_ROUTE_PATHS.spots)">查看全部</button>
      </div>
      <div v-if="spotList.length" class="spot-grid">
        <SpotCard
          v-for="spot in spotList"
          :key="spot.id"
          :spot="spot"
          @select="$router.push(buildSpotDetailRoute(spot.id, SPOT_DETAIL_SOURCE.DISCOVER))"
        />
      </div>
      <el-empty v-else description="当前条件暂无景点" />
    </section>

    <section v-if="showGuideSection" class="content-section">
      <div class="section-head">
        <div>
          <p class="panel-kicker">攻略</p>
          <h3>精选攻略</h3>
          <p>从路线、玩法和经验里找到更适合自己的出行灵感。</p>
        </div>
        <button type="button" class="section-link" @click="$router.push(APP_ROUTE_PATHS.guides)">查看全部</button>
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

    <el-dialog v-model="preferenceVisible" title="选择你感兴趣的景点分类" width="520px" :close-on-click-modal="false">
      <div class="preference-tags">
        <el-check-tag
          v-for="category in categories"
          :key="category.id"
          :checked="selectedCategories.includes(category.id)"
          @change="toggleCategory(category.id)"
        >
          {{ category.name }}
        </el-check-tag>
      </div>
      <template #footer>
        <el-button @click="preferenceVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPref" @click="handleSavePreference">保存设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import GuideCard from '@/modules/guide/components/GuideCard.vue'
import { getGuideList, getCategories } from '@/modules/guide/api.js'
import { getNearbySpots } from '@/modules/home/api.js'
import { useRecommendationFeed } from '@/modules/recommendation/composables/useRecommendationFeed.js'
import SpotCard from '@/modules/spot/components/SpotCard.vue'
import { getSpotList, getFilters } from '@/modules/spot/api.js'
import { useUserStore } from '@/modules/account/store/user.js'
import { getCurrentLocation, getLocationSnapshot } from '@/shared/lib/location.js'
import { SEARCH_HOT_KEYWORDS } from '@/shared/constants/search.js'
import { APP_ROUTE_PATHS, AUTH_ROUTE_PATHS } from '@/shared/constants/route-paths.js'
import { buildSpotDetailRoute, SPOT_DETAIL_SOURCE } from '@/shared/constants/spot-detail.js'
import ExploreKeywordGroup from '@/shared/ui/ExploreKeywordGroup.vue'

const DISCOVER_STATE_KEY = 'discover_state'
const DISCOVER_TABS = ['all', 'spot', 'guide']
const DISCOVER_SCENES = ['all', 'recommend', 'nearby']
const hotKeywords = SEARCH_HOT_KEYWORDS

// 基础依赖与用户状态
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

// 页面数据状态
const activeTab = ref('all')
const activeScene = ref('all')
const regions = ref([])
const spotCategories = ref([])
const guideCategories = ref([])
const selectedRegionId = ref('')
const selectedSpotCategoryId = ref('')
const selectedGuideCategory = ref('')
const spotList = ref([])
const guideList = ref([])
const nearbySpots = ref([])
const nearbyLoading = ref(false)
const nearbyStatus = ref('idle')
const refreshing = ref(false)
const savingPref = ref(false)
const {
  recommendations,
  needPreference,
  categories,
  selectedCategories,
  preferenceVisible,
  recommendType,
  fetchRecommendationList,
  rotateRecommendationList,
  openPreferenceDialog,
  savePreferences
} = useRecommendationFeed(12)

// 计算属性
const showSpotFilters = computed(() => activeTab.value === 'all' || activeTab.value === 'spot')
const showGuideFilters = computed(() => activeTab.value === 'all' || activeTab.value === 'guide')
const showSpotSection = computed(() => activeTab.value === 'all' || activeTab.value === 'spot')
const showGuideSection = computed(() => activeTab.value === 'all' || activeTab.value === 'guide')
const showRecommendationSection = computed(() => activeScene.value === 'all' || activeScene.value === 'recommend')
const showNearbySection = computed(() => activeScene.value === 'all' || activeScene.value === 'nearby')
const currentSceneDescription = computed(() => {
  if (activeScene.value === 'recommend') return '优先浏览更贴近你兴趣的推荐内容。'
  if (activeScene.value === 'nearby') return '优先查看距离更近、适合立刻出发的地点。'
  return '综合浏览会同时展示推荐、附近、景点与攻略内容。'
})
const nearbyEmptyText = computed(() => {
  if (!userStore.isLoggedIn) return '登录后查看附近景点'
  if (nearbyStatus.value === 'empty') return '附近暂时没有景点'
  return '还没有加载附近景点'
})
const filterSummary = computed(() => {
  const segments = []
  if (selectedRegionId.value) {
    const region = regions.value.find(item => item.id === selectedRegionId.value)
    segments.push(`地区：${region?.name || '已选择'}`)
  }
  if (selectedSpotCategoryId.value) {
    const category = spotCategories.value.find(item => item.id === selectedSpotCategoryId.value)
    segments.push(`分类：${category?.name || '已选择'}`)
  }
  if (selectedGuideCategory.value) {
    segments.push(`主题：${selectedGuideCategory.value}`)
  }
  return segments.length ? segments.join(' / ') : '当前为默认筛选'
})
const sceneEntries = computed(() => ([
  { key: 'all', title: '综合探索', desc: '同时查看推荐、附近、景点和攻略。', badge: '全部' },
  { key: 'recommend', title: '为你推荐', desc: userStore.isLoggedIn ? '优先浏览更贴近兴趣的内容。' : '登录后查看更贴近兴趣的推荐。', badge: '推荐' },
  { key: 'nearby', title: '附近探索', desc: '基于定位快速浏览周边值得去的地方。', badge: '附近' }
]))

// 工具方法
// 发现页承担多种探索场景，持久化场景和筛选状态可以避免用户来回切页时丢上下文。
const persistState = () => {
  localStorage.setItem(DISCOVER_STATE_KEY, JSON.stringify({
    tab: activeTab.value,
    scene: activeScene.value,
    selectedRegionId: selectedRegionId.value,
    selectedSpotCategoryId: selectedSpotCategoryId.value,
    selectedGuideCategory: selectedGuideCategory.value
  }))
}

const syncRouteQuery = () => {
  router.replace({
    path: APP_ROUTE_PATHS.discover,
    query: {
      ...(activeTab.value !== 'all' ? { tab: activeTab.value } : {}),
      ...(activeScene.value !== 'all' ? { scene: activeScene.value } : {}),
      ...(selectedRegionId.value ? { regionId: selectedRegionId.value } : {}),
      ...(selectedSpotCategoryId.value ? { categoryId: selectedSpotCategoryId.value } : {}),
      ...(selectedGuideCategory.value ? { guideCategory: selectedGuideCategory.value } : {})
    }
  })
}

const restoreState = () => {
  const raw = localStorage.getItem(DISCOVER_STATE_KEY)
  if (!raw) return

  const state = JSON.parse(raw)
  activeTab.value = DISCOVER_TABS.includes(state.tab) ? state.tab : 'all'
  activeScene.value = DISCOVER_SCENES.includes(state.scene) ? state.scene : 'all'
  selectedRegionId.value = state.selectedRegionId || ''
  selectedSpotCategoryId.value = state.selectedSpotCategoryId || ''
  selectedGuideCategory.value = state.selectedGuideCategory || ''
}

const applyRoutePreset = () => {
  const tab = typeof route.query.tab === 'string' ? route.query.tab : ''
  const scene = typeof route.query.scene === 'string' ? route.query.scene : ''
  if (DISCOVER_TABS.includes(tab)) {
    activeTab.value = tab
  }
  if (DISCOVER_SCENES.includes(scene)) {
    activeScene.value = scene
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

const toggleCategory = (id) => {
  const index = selectedCategories.value.indexOf(id)
  if (index > -1) {
    selectedCategories.value.splice(index, 1)
  } else {
    selectedCategories.value.push(id)
  }
}

const showPreferencePopup = async () => {
  await openPreferenceDialog()
}

const handleKeywordSelect = (value) => {
  router.push({ path: APP_ROUTE_PATHS.search, query: { keyword: value } })
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

const fetchNearbyPreview = async (location) => {
  if (!location) return
  const res = await getNearbySpots(location.latitude, location.longitude, 6)
  nearbySpots.value = res.data?.list || []
  nearbyStatus.value = nearbySpots.value.length ? 'ready' : 'empty'
}

const tryLoadNearbyAutomatically = async () => {
  if (!userStore.isLoggedIn) {
    nearbySpots.value = []
    nearbyStatus.value = 'idle'
    return
  }

  const snapshot = await getLocationSnapshot()
  if (snapshot.current) {
    await fetchNearbyPreview(snapshot.current)
  }
}

const refreshDiscover = async () => {
  await Promise.all([
    fetchSpotPreview(),
    fetchGuidePreview(),
    fetchRecommendationList(),
    tryLoadNearbyAutomatically()
  ])
}

// 交互处理方法
const handleTabChange = async () => {
  syncRouteQuery()
  await refreshDiscover()
}

const handleSpotFilter = async () => {
  syncRouteQuery()
  await fetchSpotPreview()
}

const handleGuideFilter = async () => {
  syncRouteQuery()
  await fetchGuidePreview()
}

const activateScene = async (scene) => {
  if (activeScene.value === scene) return
  activeScene.value = scene
  syncRouteQuery()

  // 场景切换时只补拉对应核心内容，避免每次都让整个发现页重新进入重加载状态。
  if (scene === 'recommend') {
    await fetchRecommendationList()
    return
  }
  if (scene === 'nearby') {
    await handleLocate()
  }
}

const clearScene = () => {
  activeScene.value = 'all'
  syncRouteQuery()
}

const handleRefresh = async () => {
  refreshing.value = true
  try {
    await rotateRecommendationList()
    ElMessage.success('已换一批推荐')
  } catch {
    ElMessage.error('换一批失败，请稍后重试')
  } finally {
    refreshing.value = false
  }
}

const handleSavePreference = async () => {
  savingPref.value = true
  try {
    await savePreferences()
    ElMessage.success(selectedCategories.value.length ? '偏好设置成功' : '已清空偏好')
    await handleRefresh()
  } finally {
    savingPref.value = false
  }
}

const handleLocate = async () => {
  if (!userStore.isLoggedIn) {
    router.push(AUTH_ROUTE_PATHS.login)
    return
  }

  nearbyLoading.value = true
  try {
    const location = await getCurrentLocation()
    await fetchNearbyPreview(location)
  } catch (_error) {
    nearbyStatus.value = 'empty'
    ElMessage.warning('请先允许浏览器定位，再查看附近景点')
  } finally {
    nearbyLoading.value = false
  }
}

watch([activeTab, activeScene, selectedRegionId, selectedSpotCategoryId, selectedGuideCategory], persistState)

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
  padding-top: 4px;
}

.discover-hero {
  padding: 28px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 20px;
  background:
    radial-gradient(circle at top right, rgba(200, 169, 91, 0.14), transparent 28%),
    linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
}

.hero-eyebrow,
.panel-kicker {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #64748b;
  text-transform: uppercase;
  font-weight: 700;
}

.page-title {
  font-size: 38px;
  line-height: 1.08;
  font-weight: 700;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.page-subtitle,
.tab-panel-desc,
.section-head p,
.filters-summary {
  color: #64748b;
  line-height: 1.8;
}

.hero-search-row {
  margin-top: 22px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-search {
  min-height: 52px;
  padding: 0 18px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #475569;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.hero-link,
.section-link {
  border: none;
  padding: 0;
  background: transparent;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease;
}

.hero-link:hover,
.section-link:hover:not(:disabled) {
  color: #0f172a;
}

.hero-status {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.status-card {
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, #ffffff 100%);
  border: 1px solid #eef2f7;
}

.status-card strong {
  display: block;
  font-size: 30px;
  line-height: 1;
  color: #0f172a;
}

.status-card span {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}

.scene-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.scene-card {
  padding: 20px;
  cursor: pointer;
  border: 1px solid transparent;
}

.scene-card.active {
  border-color: rgba(200, 169, 91, 0.42);
  background: linear-gradient(180deg, #fffdf7 0%, #ffffff 100%);
}

.scene-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.scene-top strong {
  font-size: 18px;
  color: #0f172a;
}

.scene-top span {
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  color: #8a6a2f;
  background: #f7f0db;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 700;
}

.scene-card p {
  color: #64748b;
  line-height: 1.8;
}

.tab-panel,
.filters,
.hint-banner {
  padding: 20px 22px;
}

.tab-panel-main {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.tab-panel-desc {
  margin-top: 14px;
}

.filters-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.filters-head h3 {
  font-size: 24px;
  color: #0f172a;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.filter-label {
  color: #475569;
  font-weight: 600;
  font-size: 13px;
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-head,
.section-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-head h3 {
  margin-bottom: 6px;
  font-size: 30px;
  color: #0f172a;
  letter-spacing: -0.03em;
}

.section-link:disabled {
  color: #94a3b8;
  cursor: default;
}

.hint-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  cursor: pointer;
}

.hint-banner strong {
  color: #0f172a;
}

.hint-banner p {
  margin-top: 6px;
  color: #64748b;
  line-height: 1.75;
}

.spot-grid,
.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 1100px) {
  .discover-hero,
  .filters-grid,
  .spot-grid,
  .guide-grid,
  .scene-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .discover-hero,
  .tab-panel-main,
  .filters-head,
  .section-head {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }

  .page-title {
    font-size: 32px;
  }

  .hero-search-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
