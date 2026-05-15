<!-- 首页 -->
<template>
  <div class="home-page">
    <section class="hero">
      <el-carousel class="hero-carousel" height="620px" :interval="5000" autoplay :pause-on-hover="false" arrow="never">
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
            <p class="hero-subtitle">从热门景点、个性推荐、附近探索和实用攻略里，先找到这次想去的地方。</p>
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
          </div>
          <div class="section-actions">
            <button type="button" class="section-link" @click="goRecommendations">查看更多</button>
            <button v-if="userStore.isLoggedIn" type="button" class="section-link" :disabled="refreshing" @click="handleRefresh">换一批</button>
          </div>
        </div>

        <div v-if="needPreference && userStore.isLoggedIn" class="preference-tip premium-card" @click="showPreferencePopup">
          <div>
            <strong>还没有设置偏好分类</strong>
            <p>先选几类感兴趣的景点，推荐结果会更贴近你的兴趣。</p>
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
  { id: 'discover', title: '发现灵感', desc: '集中看推荐、附近和精选内容', icon: Star, theme: 'amber', handler: () => router.push(APP_ROUTE_PATHS.discover) },
  { id: 'spots', title: '全部景点', desc: '按热度浏览全部景点', icon: MapLocation, theme: 'blue', handler: () => router.push(`${APP_ROUTE_PATHS.spots}?sortBy=heat`) },
  { id: 'guides', title: '游玩攻略', desc: '查看最新游玩攻略', icon: Guide, theme: 'orange', handler: () => router.push(APP_ROUTE_PATHS.guides) },
  { id: 'orders', title: '行程订单', desc: '查看当前和历史订单', icon: Tickets, theme: 'emerald', handler: () => goOrders() }
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
  min-height: 520px;
  overflow: hidden;
  border-bottom-left-radius: 20px;
  border-bottom-right-radius: 20px;
}

.hero-carousel,
.hero-slide,
.hero-bg {
  width: 100%;
  height: 100%;
}

.hero-carousel :deep(.el-carousel__indicators) {
  bottom: 22px;
  transform: none;
}

.hero-carousel :deep(.el-carousel__button) {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.46);
  transition: width 0.24s ease, background-color 0.24s ease, opacity 0.24s ease;
}

.hero-carousel :deep(.el-carousel__indicator:hover .el-carousel__button) {
  background: rgba(255, 255, 255, 0.72);
}

.hero-carousel :deep(.el-carousel__indicator.is-active .el-carousel__button) {
  width: 28px;
  background: #ffffff;
}

.hero-bg {
  object-fit: cover;
  filter: brightness(0.48);
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
  min-height: 520px;
  padding-top: 48px;
  padding-bottom: 52px;
  display: flex;
  align-items: end;
  justify-content: center;
}

.hero-copy {
  max-width: 780px;
  color: #fff;
  text-align: center;
}

.hero-eyebrow {
  margin-bottom: 14px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  font-weight: 700;
}

.hero-title {
  font-size: 52px;
  line-height: 1.04;
  letter-spacing: 0;
}

.hero-subtitle {
  max-width: 560px;
  margin: 16px auto 0;
  font-size: 15px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.86);
}

.home-content {
  margin-top: 20px;
  padding-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  gap: 16px;
}

.preference-tip {
  padding: 16px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
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
    font-size: 40px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .hero {
    border-bottom-left-radius: 16px;
    border-bottom-right-radius: 16px;
  }

  .hero-carousel :deep(.el-carousel__indicators) {
    bottom: 14px;
  }

  .hero-inner {
    padding-top: 24px;
    padding-bottom: 30px;
  }

  .hero-title {
    font-size: 32px;
  }

  .spot-grid,
  .recommend-grid {
    grid-template-columns: 1fr;
  }
}
</style>
