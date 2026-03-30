<!-- 首页 -->
<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-overlay">
        <div class="page-container hero-inner">
          <div class="hero-copy">
            <p class="hero-eyebrow">WayTrip</p>
            <h1 class="hero-title">发现旅途之美</h1>
            <p class="hero-subtitle">把热门景点、个性推荐、附近探索和攻略入口集中到一个首页里。</p>
            <div class="hero-search" @click="router.push('/search')">
              <el-icon><Search /></el-icon>
              <span>搜索景点、攻略...</span>
            </div>
          </div>
        </div>
      </div>

      <el-carousel class="hero-carousel" height="500px" :interval="5000" autoplay :pause-on-hover="false" arrow="never" indicator-position="none">
        <el-carousel-item v-for="banner in banners" :key="banner.id">
          <div class="hero-slide" :class="{ clickable: !!banner.spotId }" @click="handleBannerClick(banner)">
            <img :src="getImageUrl(banner.imageUrl)" class="hero-bg" alt="" />
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <div class="page-container home-content">
      <section class="quick-grid">
        <div v-for="item in quickActions" :key="item.id" class="quick-card card" @click="item.handler()">
          <div class="quick-icon" :class="item.theme">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h2 class="section-title">附近探索</h2>
          <el-button text type="primary" @click="goNearby">查看更多</el-button>
        </div>
        <div class="nearby-panel card">
          <div class="nearby-copy">
            <h3>{{ nearbyHeadline }}</h3>
            <p>{{ nearbySummary }}</p>
            <el-button type="primary" :loading="nearbyLoading" @click="goNearby">{{ nearbyActionText }}</el-button>
          </div>
          <div v-if="nearbySpots.length" class="nearby-list">
            <article v-for="spot in nearbySpots.slice(0, 3)" :key="spot.id" class="nearby-item" @click="router.push(`/spots/${spot.id}?source=nearby`)">
              <img :src="getImageUrl(spot.coverImage)" class="nearby-image" alt="" />
              <div class="nearby-info">
                <h4>{{ spot.name }}</h4>
                <p>{{ spot.regionName || '附近区域' }}</p>
                <span>{{ formatDistance(spot.distanceKm) }}</span>
              </div>
            </article>
          </div>
          <el-empty v-else description="暂未加载附近景点" :image-size="80" />
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h2 class="section-title">热门景点</h2>
          <el-button text type="primary" @click="router.push('/spots?sortBy=heat')">查看全部</el-button>
        </div>
        <div v-if="hotSpots.length" class="hot-grid">
          <article v-for="spot in hotSpots" :key="spot.id" class="hot-card card" @click="router.push(`/spots/${spot.id}?source=home`)">
            <img :src="getImageUrl(spot.coverImage)" class="hot-image" alt="" />
            <div class="hot-content">
              <div class="hot-top">
                <h3>{{ spot.name }}</h3>
                <span class="price">¥{{ spot.price }}</span>
              </div>
              <div class="hot-bottom">
                <span class="star-text">★ {{ spot.avgRating || '4.5' }}</span>
                <span>{{ spot.regionName }}</span>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无热门景点" />
      </section>

      <section class="section">
        <div class="section-header">
          <h2 class="section-title">{{ recommendationSectionTitle }}</h2>
          <div class="section-actions">
            <el-button text type="primary" @click="goRecommendations">查看更多</el-button>
            <el-button v-if="userStore.isLoggedIn" text type="primary" :loading="refreshing" @click="handleRefresh">换一批</el-button>
          </div>
        </div>

        <div v-if="needPreference && userStore.isLoggedIn" class="preference-tip card" @click="showPreferencePopup">
          <span>你还没有设置偏好，先选几类感兴趣的景点，推荐会更准确。</span>
          <el-icon><ArrowRight /></el-icon>
        </div>

        <div v-if="recommendations.length" class="recommend-list">
          <article v-for="spot in recommendations.slice(0, 4)" :key="spot.id" class="recommend-card card" @click="router.push(`/spots/${spot.id}?source=home`)">
            <img :src="getImageUrl(spot.coverImage)" class="recommend-image" alt="" />
            <div class="recommend-content">
              <div class="recommend-top">
                <h3>{{ spot.name }}</h3>
                <span class="star-text">★ {{ spot.avgRating || '4.5' }}</span>
              </div>
              <p>{{ spot.intro || '暂无介绍，点击查看详情。' }}</p>
              <div class="recommend-bottom">
                <span class="tag">{{ spot.categoryName || '景点' }}</span>
                <span class="price">¥{{ spot.price }}</span>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-else :description="userStore.isLoggedIn ? '暂无推荐景点' : '登录后查看推荐景点'">
          <el-button v-if="!userStore.isLoggedIn" type="primary" @click="router.push('/login')">去登录</el-button>
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
import { useUserStore } from '@/stores/user'
import { getBanners, getHotSpots, getNearbySpots } from '@/api/home'
import { useRecommendationFeed } from '@/composables/useRecommendationFeed'
import {
  getColdStartGuideState,
  markColdStartGuidePending,
  markColdStartGuideSkipped
} from '@/utils/cold-start-guide'
import { getLocationSnapshot, getCurrentLocation } from '@/utils/location'
import { getImageUrl } from '@/utils/request'

// 基础依赖与用户状态
const router = useRouter()
const userStore = useUserStore()

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
    return `你附近有 ${nearbySpots.value.length} 个景点，最近约 ${formatDistance(nearbySpots.value[0].distanceKm)}`
  }
  if (!userStore.isLoggedIn) return '登录后可按距离浏览附近景点'
  return '点击按钮后会调用浏览器定位并加载附近景点'
})

const nearbyActionText = computed(() => {
  if (nearbyLoading.value) return '加载中'
  if (!userStore.isLoggedIn) return '去登录'
  return '查看附近'
})

const quickActions = computed(() => ([
  { id: 'spots', title: '全部景点', desc: '按热度浏览热门景点', icon: MapLocation, theme: 'blue', handler: () => router.push('/spots?sortBy=heat') },
  { id: 'guides', title: '游玩攻略', desc: '查看最新旅行攻略', icon: Guide, theme: 'orange', handler: () => router.push('/guides') },
  { id: 'recommend', title: '推荐景点', desc: '集中浏览推荐结果', icon: Star, theme: 'amber', handler: () => goRecommendations() },
  { id: 'nearby', title: '附近探索', desc: '查看离你最近的景点', icon: Tickets, theme: 'emerald', handler: () => goNearby() }
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
    getHotSpots(6)
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
    router.push('/login')
    return
  }
  router.push('/recommendations')
}

const goNearby = async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
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

  router.push('/nearby')
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
  height: 500px;
  overflow: hidden;
  background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 50%, #38bdf8 100%);
}

.hero-carousel,
.hero-slide,
.hero-bg {
  width: 100%;
  height: 100%;
}

.hero-bg {
  object-fit: cover;
  filter: brightness(0.45);
}

.hero-slide.clickable {
  cursor: pointer;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  z-index: 2;
  background: linear-gradient(90deg, rgba(15, 23, 42, 0.68), rgba(15, 23, 42, 0.24));
}

.hero-inner {
  height: 100%;
  display: flex;
  align-items: center;
}

.hero-copy {
  max-width: 620px;
  color: #fff;
}

.hero-eyebrow {
  margin-bottom: 12px;
  letter-spacing: 0.3em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.hero-title {
  font-size: 54px;
  line-height: 1.1;
  margin-bottom: 16px;
}

.hero-subtitle {
  font-size: 18px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.86);
}

.hero-search {
  margin-top: 28px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #475569;
  cursor: pointer;
}

.home-content {
  display: flex;
  flex-direction: column;
  gap: 28px;
  margin-top: 28px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

.quick-card {
  padding: 20px;
  display: flex;
  gap: 16px;
  cursor: pointer;
  align-items: center;
}

.quick-card h3 {
  margin-bottom: 8px;
  font-size: 18px;
}

.quick-card p {
  color: #909399;
  line-height: 1.6;
}

.quick-icon {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.quick-icon.blue {
  background: #dbeafe;
  color: #2563eb;
}

.quick-icon.orange {
  background: #ffedd5;
  color: #ea580c;
}

.quick-icon.amber {
  background: #fef3c7;
  color: #d97706;
}

.quick-icon.emerald {
  background: #d1fae5;
  color: #059669;
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

.nearby-panel {
  padding: 22px;
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 20px;
  align-items: start;
}

.nearby-copy h3 {
  margin-bottom: 12px;
  font-size: 24px;
}

.nearby-copy p {
  margin-bottom: 18px;
  color: #606266;
  line-height: 1.7;
}

.nearby-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.nearby-item {
  background: #f8fafc;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
}

.nearby-image {
  width: 100%;
  height: 140px;
  object-fit: cover;
}

.nearby-info {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nearby-info p,
.nearby-info span {
  color: #64748b;
  font-size: 13px;
}

.hot-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

.hot-card,
.recommend-card {
  cursor: pointer;
}

.hot-image {
  width: 100%;
  height: 210px;
  object-fit: cover;
}

.hot-content {
  padding: 16px;
}

.hot-top,
.hot-bottom,
.recommend-top,
.recommend-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.hot-top h3,
.recommend-top h3 {
  font-size: 18px;
}

.hot-bottom {
  margin-top: 12px;
  color: #909399;
}

.preference-tip {
  padding: 14px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #409eff;
  cursor: pointer;
}

.recommend-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}

.recommend-card {
  display: flex;
}

.recommend-image {
  width: 210px;
  height: 170px;
  object-fit: cover;
  flex-shrink: 0;
}

.recommend-content {
  flex: 1;
  min-width: 0;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.recommend-content p {
  color: #606266;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 1024px) {
  .quick-grid,
  .hot-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .nearby-panel {
    grid-template-columns: 1fr;
  }

  .nearby-list,
  .recommend-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 36px;
  }

  .quick-grid,
  .hot-grid {
    grid-template-columns: 1fr;
  }

  .recommend-card {
    flex-direction: column;
  }

  .recommend-image {
    width: 100%;
    height: 220px;
  }
}
</style>
