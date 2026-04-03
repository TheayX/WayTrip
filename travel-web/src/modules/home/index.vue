<!-- 首页 -->
<template>
  <div class="home-page">
    <section class="hero">
      <el-carousel class="hero-carousel" height="620px" :interval="5000" autoplay :pause-on-hover="false" arrow="never" indicator-position="none">
        <el-carousel-item v-for="banner in banners" :key="banner.id">
          <div class="hero-slide" :class="{ clickable: !!banner.spotId }" @click="handleBannerClick(banner)">
            <img :src="getImageUrl(banner.imageUrl)" class="hero-bg" alt="" />
          </div>
        </el-carousel-item>
      </el-carousel>

      <div class="hero-overlay">
        <div class="page-container hero-inner">
          <div class="hero-copy">
            <p class="hero-eyebrow">{{ APP_NAME }} Curated Travel</p>
            <h1 class="hero-title">把灵感、景点与攻略整理成一条更高级的探索路径。</h1>
            <p class="hero-subtitle">首页只保留真正高频的入口，把推荐、附近和精选内容收束成更干净的旅行探索体验。</p>

            <div class="hero-actions">
              <button type="button" class="hero-search glass-panel" @click="router.push(APP_ROUTE_PATHS.search)">
                <el-icon><Search /></el-icon>
                <span>搜索景点、攻略与旅行灵感</span>
              </button>
              <el-button type="primary" @click="router.push(APP_ROUTE_PATHS.discover)">进入发现</el-button>
            </div>

            <div class="hero-metrics">
              <article class="hero-metric glass-panel">
                <strong>{{ hotSpots.length || 0 }}</strong>
                <span>热门景点精选</span>
              </article>
              <article class="hero-metric glass-panel">
                <strong>{{ recommendations.length || 0 }}</strong>
                <span>推荐内容预览</span>
              </article>
              <article class="hero-metric glass-panel">
                <strong>{{ nearbySpots.length || 0 }}</strong>
                <span>附近可探索地点</span>
              </article>
            </div>
          </div>

          <aside class="hero-aside glass-panel">
            <p class="hero-aside-kicker">Today's Flow</p>
            <div class="hero-aside-item">
              <span class="hero-aside-index">01</span>
              <div>
                <strong>先搜你想去的目的地</strong>
                <p>从关键字、地区和分类切入，快速缩小范围。</p>
              </div>
            </div>
            <div class="hero-aside-item">
              <span class="hero-aside-index">02</span>
              <div>
                <strong>在发现页切换探索场景</strong>
                <p>推荐、附近与内容浏览在同一条路径里切换。</p>
              </div>
            </div>
            <div class="hero-aside-item">
              <span class="hero-aside-index">03</span>
              <div>
                <strong>从详情继续下钻</strong>
                <p>景点、攻略和相关推荐形成连续浏览关系。</p>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </section>

    <div class="page-container home-content">
      <HomeQuickActions :items="quickActions" />

      <ExploreKeywordGroup
        title="热门搜索"
        :items="hotKeywords"
        @select="handleKeywordSelect"
      />

      <section class="curation-strip premium-card">
        <article class="curation-item">
          <span class="curation-label">For You</span>
          <h3>个性推荐已从独立入口收进发现流</h3>
          <p>登录后会基于偏好与行为刷新推荐，让首页只保留精简预览。</p>
        </article>
        <article class="curation-item">
          <span class="curation-label">Nearby</span>
          <h3>附近探索保留即时能力，但不再打散主导航</h3>
          <p>地理位置相关内容统一进入发现体系，减少入口噪音。</p>
        </article>
        <article class="curation-item">
          <span class="curation-label">Editorial</span>
          <h3>攻略与景点重新建立更自然的浏览关系</h3>
          <p>从首页预览，到列表筛选，再到详情沉浸阅读，层级更加清晰。</p>
        </article>
      </section>

      <HomeNearbySection
        :headline="nearbyHeadline"
        :summary="nearbySummary"
        :action-text="nearbyActionText"
        :loading="nearbyLoading"
        :spots="nearbySpots"
        :format-distance="formatDistance"
        @more="goNearby"
        @action="goNearby"
        @select="router.push(`/spots/${$event.id}?source=nearby`)"
      />

      <section class="section page-section">
        <div class="section-header">
          <div>
            <p class="section-kicker">Popular Spots</p>
            <h2 class="section-title">热门景点精选</h2>
            <p class="section-subtitle">控制数量，只保留最值得继续浏览的内容，让首页更像经过策展的旅行入口。</p>
          </div>
          <el-button text type="primary" @click="router.push(`${APP_ROUTE_PATHS.spots}?sortBy=heat`)">查看全部</el-button>
        </div>

        <div v-if="hotSpots.length" class="spot-grid">
          <SpotCard
            v-for="spot in hotSpots"
            :key="spot.id"
            :spot="spot"
            @select="router.push(`/spots/${spot.id}?source=home`)"
          />
        </div>
        <el-empty v-else description="暂无热门景点" />
      </section>

      <section class="section page-section">
        <div class="section-header">
          <div>
            <p class="section-kicker">Recommendations</p>
            <h2 class="section-title">{{ recommendationSectionTitle }}</h2>
            <p class="section-subtitle">把个性推荐作为高质量预览保留在首页，完整浏览和切换继续交给发现页承接。</p>
          </div>
          <div class="section-actions">
            <el-button text type="primary" @click="goRecommendations">查看更多</el-button>
            <el-button v-if="userStore.isLoggedIn" text type="primary" :loading="refreshing" @click="handleRefresh">换一批</el-button>
          </div>
        </div>

        <div v-if="needPreference && userStore.isLoggedIn" class="preference-tip premium-card" @click="showPreferencePopup">
          <div>
            <strong>你还没有设置偏好分类</strong>
            <p>先选择几类感兴趣的景点，推荐结果会明显更稳定。</p>
          </div>
          <el-icon><ArrowRight /></el-icon>
        </div>

        <div v-if="recommendations.length" class="recommend-grid">
          <SpotCard
            v-for="spot in recommendations.slice(0, 4)"
            :key="spot.id"
            :spot="spot"
            @select="router.push(`/spots/${spot.id}?source=home`)"
          />
        </div>
        <el-empty v-else :description="userStore.isLoggedIn ? '暂无推荐景点' : '登录后查看推荐景点'">
          <el-button v-if="!userStore.isLoggedIn" type="primary" @click="router.push(AUTH_ROUTE_PATHS.login)">去登录</el-button>
        </el-empty>
      </section>
    </div>

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
        <el-button @click="handleSkipColdStart">稍后再说</el-button>
        <el-button type="primary" :loading="savingPref" @click="handleSavePreference">保存设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Guide, MapLocation, Search, Star, Tickets } from '@element-plus/icons-vue'
import HomeNearbySection from '@/modules/home/components/HomeNearbySection.vue'
import HomeQuickActions from '@/modules/home/components/HomeQuickActions.vue'
import SpotCard from '@/modules/spot/components/SpotCard.vue'
import { useUserStore } from '@/modules/account/store/user.js'
import { APP_NAME } from '@/shared/constants/app.js'
import { SEARCH_HOT_KEYWORDS } from '@/shared/constants/search.js'
import { APP_ROUTE_PATHS, AUTH_ROUTE_PATHS } from '@/shared/constants/route-paths.js'
import ExploreKeywordGroup from '@/shared/ui/ExploreKeywordGroup.vue'
import { getBanners, getHotSpots, getNearbySpots } from '@/modules/home/api.js'
import { useRecommendationFeed } from '@/modules/recommendation/composables/useRecommendationFeed.js'
import {
  getColdStartGuideState,
  markColdStartGuidePending,
  markColdStartGuideSkipped
} from '@/shared/lib/cold-start-guide.js'
import { getLocationSnapshot, getCurrentLocation } from '@/shared/lib/location.js'
import { getImageUrl } from '@/shared/api/client.js'
import { ArrowRight } from '@element-plus/icons-vue'

// 基础依赖与用户状态
const router = useRouter()
const userStore = useUserStore()
const hotKeywords = SEARCH_HOT_KEYWORDS

// 页面数据状态
const banners = ref([])
const hotSpots = ref([])
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
  refreshRecommendationList,
  openPreferenceDialog,
  savePreferences
} = useRecommendationFeed(12)

// 计算属性
const recommendationSectionTitle = computed(() => (userStore.isLoggedIn ? recommendType.value : '推荐景点'))

const nearbyHeadline = computed(() => {
  if (nearbyLoading.value) return '定位中'
  if (nearbyStatus.value === 'ready') return '附近可探索'
  if (!userStore.isLoggedIn) return '登录后查看'
  return '开启定位'
})

const nearbySummary = computed(() => {
  if (nearbyLoading.value) return '正在获取你周边的景点'
  if (nearbyStatus.value === 'ready' && nearbySpots.value.length) {
    return `你附近有 ${nearbySpots.value.length} 个景点，最近约 ${formatDistance(nearbySpots.value[0].distanceKm)}。`
  }
  if (!userStore.isLoggedIn) return '登录后可按距离浏览附近景点。'
  return '点击按钮后会调用浏览器定位并加载附近景点。'
})

const nearbyActionText = computed(() => {
  if (nearbyLoading.value) return '加载中'
  if (!userStore.isLoggedIn) return '去登录'
  return '进入发现'
})

// 首页只保留高频一级入口，把推荐和附近收进发现页承接，避免继续平铺更多导航位。
const quickActions = computed(() => ([
  { id: 'discover', title: '发现灵感', desc: '集中浏览推荐、附近与精选内容', icon: Star, theme: 'amber', handler: () => router.push(APP_ROUTE_PATHS.discover) },
  { id: 'spots', title: '全部景点', desc: '按热度浏览热门景点', icon: MapLocation, theme: 'blue', handler: () => router.push(`${APP_ROUTE_PATHS.spots}?sortBy=heat`) },
  { id: 'guides', title: '游玩攻略', desc: '查看最新旅行攻略', icon: Guide, theme: 'orange', handler: () => router.push(APP_ROUTE_PATHS.guides) },
  { id: 'orders', title: '行程订单', desc: '查看进行中与已完成订单', icon: Tickets, theme: 'emerald', handler: () => goOrders() }
]))

// 工具方法
const formatDistance = (value) => {
  const distance = Number(value)
  if (!Number.isFinite(distance)) return '-- km'
  return distance < 1 ? `${Math.max(100, Math.round(distance * 1000))} m` : `${distance.toFixed(1)} km`
}

const maybeShowColdStartGuide = () => {
  if (!userStore.isLoggedIn || !needPreference.value || preferenceVisible.value) return

  const currentUserId = userStore.userInfo?.id
  const currentPreferenceIds = userStore.userInfo?.preferenceCategoryIds || []
  if (!currentUserId || currentPreferenceIds.length) return

  const state = getColdStartGuideState(currentUserId)
  if (!state.pending && !state.completed && !state.skipped) {
    markColdStartGuidePending(currentUserId)
  }
  if (state.skipped || state.completed) return

  openPreferenceDialog()
}

// 数据加载方法
const fetchHomeBasics = async () => {
  const [bannerRes, hotRes] = await Promise.all([
    getBanners(),
    getHotSpots(4)
  ])
  banners.value = bannerRes.data?.list || []
  hotSpots.value = hotRes.data?.list || []
}

const fetchNearbyPreview = async (location) => {
  if (!location) return
  nearbyLoading.value = true
  try {
    const res = await getNearbySpots(location.latitude, location.longitude, 3)
    nearbySpots.value = res.data?.list || []
    nearbyStatus.value = nearbySpots.value.length ? 'ready' : 'empty'
  } finally {
    nearbyLoading.value = false
  }
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

// 交互处理方法
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

const handleSkipColdStart = () => {
  markColdStartGuideSkipped(userStore.userInfo?.id)
  preferenceVisible.value = false
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

const handleRefresh = async () => {
  refreshing.value = true
  try {
    await refreshRecommendationList()
    ElMessage.success('推荐已刷新')
  } finally {
    refreshing.value = false
  }
}

const handleBannerClick = (banner) => {
  if (banner?.spotId) {
    router.push(`/spots/${banner.spotId}?source=home`)
  }
}

const goRecommendations = () => {
  if (!userStore.isLoggedIn) {
    router.push(AUTH_ROUTE_PATHS.login)
    return
  }
  router.push(APP_ROUTE_PATHS.recommendations)
}

const goNearby = async () => {
  if (!userStore.isLoggedIn) {
    router.push(AUTH_ROUTE_PATHS.login)
    return
  }

  if (!nearbySpots.value.length) {
    try {
      const location = await getCurrentLocation()
      await fetchNearbyPreview(location)
    } catch (_error) {
      ElMessage.warning('请先允许浏览器定位，再查看附近景点')
      return
    }
  }

  // 附近探索继续保留能力，但主承接页切到发现页，统一 Web 端探索路径。
  router.push({ path: APP_ROUTE_PATHS.discover, query: { tab: 'spot', scene: 'nearby' } })
}

const goOrders = () => {
  if (!userStore.isLoggedIn) {
    router.push(AUTH_ROUTE_PATHS.login)
    return
  }

  router.push('/account/orders')
}

// 生命周期
onMounted(async () => {
  await fetchHomeBasics()
  await fetchRecommendationList()
  maybeShowColdStartGuide()
  await tryLoadNearbyAutomatically()
})
</script>

<style lang="scss" scoped>
.hero {
  position: relative;
  min-height: 620px;
  overflow: hidden;
  border-bottom-left-radius: 36px;
  border-bottom-right-radius: 36px;
}

.hero-carousel,
.hero-slide,
.hero-bg {
  width: 100%;
  height: 100%;
}

.hero-bg {
  object-fit: cover;
  filter: brightness(0.42);
}

.hero-slide.clickable {
  cursor: pointer;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  z-index: 2;
  background:
    linear-gradient(100deg, rgba(15, 23, 42, 0.82) 0%, rgba(15, 23, 42, 0.54) 44%, rgba(15, 23, 42, 0.28) 100%);
}

.hero-inner {
  min-height: 620px;
  padding-top: 44px;
  padding-bottom: 64px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 28px;
  align-items: end;
}

.hero-copy {
  max-width: 760px;
  color: #fff;
}

.hero-eyebrow {
  margin-bottom: 16px;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  font-weight: 700;
}

.hero-title {
  font-size: 64px;
  line-height: 1.02;
  letter-spacing: -0.05em;
}

.hero-subtitle {
  max-width: 660px;
  margin-top: 18px;
  font-size: 17px;
  line-height: 1.95;
  color: rgba(255, 255, 255, 0.86);
}

.hero-actions {
  margin-top: 28px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-search {
  min-height: 56px;
  padding: 0 18px;
  border-radius: 999px;
  border: none;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.hero-metrics {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.hero-metric {
  padding: 16px 18px;
  border-radius: 22px;
}

.hero-metric strong {
  display: block;
  font-size: 28px;
  line-height: 1;
}

.hero-metric span {
  display: block;
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.76);
  font-size: 13px;
}

.hero-aside {
  padding: 22px;
  border-radius: 28px;
  color: #ffffff;
}

.hero-aside-kicker {
  margin-bottom: 18px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.hero-aside-item + .hero-aside-item {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.hero-aside-item {
  display: flex;
  gap: 14px;
}

.hero-aside-index {
  color: #c8a95b;
  font-size: 14px;
  font-weight: 700;
}

.hero-aside-item strong {
  display: block;
  margin-bottom: 6px;
}

.hero-aside-item p {
  color: rgba(255, 255, 255, 0.72);
  line-height: 1.75;
  font-size: 13px;
}

.home-content {
  margin-top: 28px;
  padding-bottom: 18px;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.curation-strip {
  padding: 22px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.curation-item {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #eef2f7;
}

.curation-label {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.curation-item h3 {
  margin-top: 14px;
  font-size: 20px;
  line-height: 1.35;
  color: #0f172a;
}

.curation-item p {
  margin-top: 10px;
  color: #64748b;
  line-height: 1.8;
  font-size: 14px;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-header,
.section-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-kicker {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.spot-grid,
.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.preference-tip {
  padding: 18px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  cursor: pointer;
}

.preference-tip strong {
  color: #0f172a;
}

.preference-tip p {
  margin-top: 6px;
  color: #64748b;
  line-height: 1.75;
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 1200px) {
  .hero-inner,
  .spot-grid,
  .recommend-grid,
  .curation-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-inner {
    align-items: start;
  }
}

@media (max-width: 992px) {
  .hero {
    min-height: auto;
  }

  .hero-inner {
    min-height: auto;
    grid-template-columns: 1fr;
  }

  .hero-title {
    font-size: 46px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .hero {
    border-bottom-left-radius: 28px;
    border-bottom-right-radius: 28px;
  }

  .hero-inner {
    padding-top: 28px;
    padding-bottom: 40px;
  }

  .hero-title {
    font-size: 36px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .spot-grid,
  .recommend-grid,
  .curation-strip {
    grid-template-columns: 1fr;
  }
}
</style>
