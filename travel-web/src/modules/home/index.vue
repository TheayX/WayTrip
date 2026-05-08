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
            <h1 class="hero-title">选一个方向，开始这次旅行。</h1>
            <p class="hero-subtitle">热门景点、个性推荐、附近探索与实用攻略，帮你更快找到想去的地方。</p>
          </div>
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

      <HomeNearbySection
        :headline="nearbyHeadline"
        :summary="nearbySummary"
        :action-text="nearbyActionText"
        :loading="nearbyLoading"
        :spots="nearbySpots"
        :format-distance="formatDistance"
        @more="goNearby"
        @action="goNearby"
        @select="router.push(buildSpotDetailRoute($event.id, SPOT_DETAIL_SOURCE.NEARBY))"
      />

      <section class="section page-section">
        <div class="section-header">
          <div>
            <p class="section-kicker">Popular Spots</p>
            <h2 class="section-title">热门景点精选</h2>
            <p class="section-subtitle">优先展示当前最值得继续点开的景点。</p>
          </div>
          <button type="button" class="section-link" @click="router.push(`${APP_ROUTE_PATHS.spots}?sortBy=heat`)">查看全部</button>
        </div>

        <div v-if="hotSpots.length" class="spot-grid">
          <SpotCard
            v-for="spot in hotSpots"
            :key="spot.id"
            :spot="spot"
            @select="router.push(buildSpotDetailRoute(spot.id, SPOT_DETAIL_SOURCE.HOME))"
          />
        </div>
        <el-empty v-else description="暂无热门景点" />
      </section>

      <section class="section page-section">
        <div class="section-header">
          <div>
            <p class="section-kicker">Recommendations</p>
            <h2 class="section-title">{{ recommendationSectionTitle }}</h2>
            <p class="section-subtitle">根据你的浏览偏好，优先整理值得继续查看的景点。</p>
          </div>
          <div class="section-actions">
            <button type="button" class="section-link" @click="goRecommendations">查看更多</button>
            <button v-if="userStore.isLoggedIn" type="button" class="section-link" :disabled="refreshing" @click="handleRefresh">换一批</button>
          </div>
        </div>

        <div v-if="needPreference && userStore.isLoggedIn" class="preference-tip premium-card" @click="showPreferencePopup">
          <div>
            <strong>还没有设置偏好分类</strong>
            <p>先选几类感兴趣的景点，推荐会更稳定。</p>
          </div>
          <el-icon><ArrowRight /></el-icon>
        </div>

        <div v-if="recommendations.length" class="recommend-grid">
          <SpotCard
            v-for="spot in recommendations.slice(0, 4)"
            :key="spot.id"
            :spot="spot"
            @select="router.push(buildSpotDetailRoute(spot.id, SPOT_DETAIL_SOURCE.HOME))"
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
import { computed, defineAsyncComponent, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Guide, MapLocation, Search, Star, Tickets } from '@element-plus/icons-vue'
import HomeQuickActions from '@/modules/home/components/HomeQuickActions.vue'
import SpotCard from '@/modules/spot/components/SpotCard.vue'
import { useUserStore } from '@/modules/account/store/user.js'
import { APP_NAME } from '@/shared/constants/app.js'
import { SEARCH_HOT_KEYWORDS } from '@/shared/constants/search.js'
import { APP_ROUTE_PATHS, AUTH_ROUTE_PATHS } from '@/shared/constants/route-paths.js'
import { buildSpotDetailRoute, SPOT_DETAIL_SOURCE } from '@/shared/constants/spot-detail.js'
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

// 附近模块改为异步加载，避免首页主包首屏携带地图相关渲染开销。
const HomeNearbySection = defineAsyncComponent(() => import('@/modules/home/components/HomeNearbySection.vue'))

const router = useRouter()
const userStore = useUserStore()
const hotKeywords = SEARCH_HOT_KEYWORDS

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
  rotateRecommendationList,
  openPreferenceDialog,
  savePreferences
} = useRecommendationFeed(12)

const recommendationSectionTitle = computed(() => (userStore.isLoggedIn ? recommendType.value : '推荐景点'))

const nearbyHeadline = computed(() => {
  if (nearbyLoading.value) return '定位中'
  if (nearbyStatus.value === 'ready') return '附近可探索'
  if (!userStore.isLoggedIn) return '登录后查看'
  return '开启定位'
})

const nearbySummary = computed(() => {
  if (nearbyLoading.value) return '正在获取附近景点。'
  if (nearbyStatus.value === 'ready' && nearbySpots.value.length) {
    return `附近有 ${nearbySpots.value.length} 个地点可看，最近约 ${formatDistance(nearbySpots.value[0].distanceKm)}。`
  }
  if (!userStore.isLoggedIn) return '登录后可按距离查看附近内容。'
  return '允许浏览器定位后即可加载附近内容。'
})

const nearbyActionText = computed(() => {
  if (nearbyLoading.value) return '加载中'
  if (!userStore.isLoggedIn) return '去登录'
  return '进入发现'
})

const quickActions = computed(() => ([
  { id: 'discover', title: '发现灵感', desc: '浏览推荐、附近与精选内容', icon: Star, theme: 'amber', handler: () => router.push(APP_ROUTE_PATHS.discover) },
  { id: 'spots', title: '全部景点', desc: '按热度进入景点列表', icon: MapLocation, theme: 'blue', handler: () => router.push(`${APP_ROUTE_PATHS.spots}?sortBy=heat`) },
  { id: 'guides', title: '游玩攻略', desc: '快速查看最新攻略', icon: Guide, theme: 'orange', handler: () => router.push(APP_ROUTE_PATHS.guides) },
  { id: 'orders', title: '行程订单', desc: '管理当前与历史订单', icon: Tickets, theme: 'emerald', handler: () => goOrders() }
]))

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

const fetchHomeBasics = async () => {
  const [bannerResult, hotResult] = await Promise.allSettled([
    getBanners(),
    getHotSpots(4)
  ])
  banners.value = bannerResult.status === 'fulfilled' ? (bannerResult.value.data?.list || []) : []
  hotSpots.value = hotResult.status === 'fulfilled' ? (hotResult.value.data?.list || []) : []
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
    await rotateRecommendationList()
    ElMessage.success('已换一批推荐')
  } catch {
    ElMessage.error('换一批失败，请稍后重试')
  } finally {
    refreshing.value = false
  }
}

const handleBannerClick = (banner) => {
  if (banner?.spotId) {
    router.push(buildSpotDetailRoute(banner.spotId, SPOT_DETAIL_SOURCE.HOME))
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

  router.push({ path: APP_ROUTE_PATHS.discover, query: { tab: 'spot', scene: 'nearby' } })
}

const goOrders = () => {
  if (!userStore.isLoggedIn) {
    router.push(AUTH_ROUTE_PATHS.login)
    return
  }

  router.push('/account/orders')
}

onMounted(async () => {
  await fetchHomeBasics()

  // 推荐和附近探索属于次级区块，不阻塞首屏主内容显示。
  fetchRecommendationList()
    .then(() => {
      maybeShowColdStartGuide()
    })
    .catch(() => {})
  void tryLoadNearbyAutomatically()
})
</script>

<style lang="scss" scoped>
.hero {
  position: relative;
  min-height: 560px;
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
  pointer-events: none;
  background:
    linear-gradient(100deg, rgba(15, 23, 42, 0.74) 0%, rgba(15, 23, 42, 0.42) 42%, rgba(15, 23, 42, 0.14) 100%);
}

.hero-inner {
  min-height: 560px;
  padding-top: 56px;
  padding-bottom: 64px;
  display: flex;
  align-items: end;
  justify-content: center;
}

.hero-copy {
  max-width: 900px;
  color: #fff;
  text-align: center;
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
  max-width: 620px;
  margin: 20px auto 0;
  font-size: 16px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.86);
}

.home-content {
  margin-top: 28px;
  padding-bottom: 18px;
  display: flex;
  flex-direction: column;
  gap: 24px;
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

.section-link {
  border: none;
  background: transparent;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s ease;
}

.section-link:hover:not(:disabled) {
  color: #0f172a;
}

.section-link:disabled {
  color: #94a3b8;
  cursor: default;
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
  .spot-grid,
  .recommend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 992px) {
  .hero {
    min-height: auto;
  }

  .hero-inner {
    min-height: auto;
    align-items: center;
  }

  .hero-title {
    font-size: 46px;
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

  .spot-grid,
  .recommend-grid {
    grid-template-columns: 1fr;
  }
}
</style>
