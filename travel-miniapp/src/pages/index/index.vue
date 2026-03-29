<!-- 首页 -->
<template>
  <view class="ios-page">
    <!-- 顶部区域 -->
    <view class="ios-header">
      <view class="header-top">
        <view>
          <text class="large-title">首页</text>
          <text class="sub-title">推荐内容、快捷入口和附近探索都集中在这里</text>
        </view>
        <image class="avatar-sm" :src="getAvatarUrl(userInfo?.avatar)" @click="goMine" />
      </view>
      <view class="search-bar" @click="goSearch">
        <uni-search-bar
          :modelValue="''"
          placeholder="搜索景点、攻略..."
          :clearButton="'none'"
          :cancelButton="'none'"
          :radius="20"
          :readonly="true"
          bgColor="#E9EEF5"
        />
      </view>
    </view>

    <!-- 轮播区域 -->
    <view class="banner-container" v-if="banners.length">
      <swiper class="banner" indicator-dots indicator-active-color="#fff" autoplay circular>
        <swiper-item v-for="banner in banners" :key="banner.id" @click="handleBannerClick(banner)">
          <image class="banner-image" :src="getContentImageUrl(banner.imageUrl)" mode="aspectFill" />
        </swiper-item>
      </swiper>
    </view>

    <!-- 快捷入口 -->
    <view class="section">
      <view class="section-header">
        <view>
          <text class="section-title">快捷导航</text>
          <text class="section-desc">先保留高频入口，其余功能后续继续补齐</text>
        </view>
      </view>
      <view class="quick-grid">
        <view
          v-for="action in displayQuickActions"
          :key="action.id"
          class="quick-item"
          @click="handleQuickAction(action)"
        >
          <view class="quick-icon" :class="`theme-${action.theme}`">
            <uni-icons :type="action.icon" size="24" color="#1F2937" />
          </view>
          <text class="quick-title">{{ action.title }}</text>
          <text class="quick-note">{{ action.note }}</text>
        </view>
      </view>
    </view>

    <view v-if="!isLoggedIn" class="guest-banner" @click="goMine">
      <view class="guest-copy">
        <text class="guest-title">登录后可保存偏好，拿到更稳定的推荐景点</text>
        <text class="guest-subtitle">同步收藏、订单和推荐内容</text>
      </view>
      <text class="guest-action">去登录</text>
    </view>

    <!-- 推荐区域 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">{{ recommendationSectionTitle }}</text>
        <view v-if="isLoggedIn" class="section-actions">
          <text class="section-link" @click="handleRefresh">换一批</text>
          <text class="section-link" @click="goRecommendationSpots">查看更多</text>
        </view>
      </view>

      <view class="recommend-list" v-if="isLoggedIn && recommendPreview.length">
        <view class="recommend-card" v-for="spot in recommendPreview" :key="spot.id" @click="goSpotDetail(spot.id)">
          <image class="rec-img" :src="getContentImageUrl(spot.coverImage)" mode="aspectFill" />
          <view class="rec-content">
            <view class="rec-header">
              <text class="rec-name">{{ spot.name }}</text>
              <text class="rec-rating">★ {{ spot.avgRating || '4.5' }}</text>
            </view>
            <text class="rec-desc">{{ spot.intro || '暂无介绍，点击查看详情。' }}</text>
            <view class="rec-footer">
              <text class="rec-tag">{{ spot.categoryName || '景点' }}</text>
              <text class="rec-price">￥{{ spot.price }}</text>
            </view>
          </view>
        </view>
      </view>
      <view class="empty-tip" v-else>
        <text>{{ isLoggedIn ? '当前暂无推荐景点' : '登录后查看推荐景点' }}</text>
      </view>
    </view>

    <!-- 热门与附近区域 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">热门景点</text>
        <text class="section-link" @click="goSpotList">查看全部</text>
      </view>

      <view class="hot-nearby-grid">
        <view v-if="primaryPopularSpot" class="feature-card hot-feature-card" @click="goSpotDetail(primaryPopularSpot.id)">
          <image class="feature-image" :src="getContentImageUrl(primaryPopularSpot.coverImage)" mode="aspectFill" />
          <view class="feature-overlay">
            <text class="feature-eyebrow">热门景点</text>
            <text class="feature-title">{{ primaryPopularSpot.name }}</text>
            <text class="feature-subtitle">
              {{ primaryPopularSpot.categoryName || '精选景点' }} · ￥{{ primaryPopularSpot.price }}
            </text>
            <view class="feature-foot">
              <text class="feature-pill">评分 {{ primaryPopularSpot.avgRating || '4.6' }}</text>
              <text class="feature-pill subtle">热度 {{ primaryPopularSpot.heatScore || '--' }}</text>
            </view>
          </view>
        </view>
        <view v-else class="feature-card feature-empty">
          <text>当前暂无热门景点</text>
        </view>

        <view class="feature-card nearby-card" @click="handleNearbyCardClick">
          <view class="nearby-top">
            <view>
              <text class="feature-eyebrow nearby-eyebrow">附近景点</text>
              <text class="nearby-status">{{ nearbyHeadline }}</text>
            </view>
            <text class="nearby-link">{{ nearbyActionText }}</text>
          </view>

          <view class="nearby-map-shell">
            <map
              v-if="canShowNearbyMap"
              class="nearby-map"
              :latitude="nearbyMapCenter.latitude"
              :longitude="nearbyMapCenter.longitude"
              :scale="12"
              :markers="nearbyMarkers"
              :show-location="true"
              :enable-scroll="false"
              :enable-zoom="false"
              :enable-rotate="false"
              :enable-overlooking="false"
              @markertap="handleNearbyMarkerTap"
            />
            <view v-else class="nearby-map nearby-placeholder">
              <view class="placeholder-grid"></view>
              <view class="placeholder-pin pin-a"></view>
              <view class="placeholder-pin pin-b"></view>
              <view class="placeholder-pin pin-c"></view>
              <text class="placeholder-copy">{{ nearbyPlaceholderText }}</text>
            </view>
          </view>

          <view class="nearby-copy">
            <text class="nearby-summary">{{ nearbySummary }}</text>
            <text class="nearby-caption">{{ nearbyCaption }}</text>
          </view>

          <view v-if="displayNearbySpots.length" class="nearby-tags">
            <text v-for="spot in displayNearbySpots.slice(0, 2)" :key="spot.id" class="nearby-tag">
              {{ spot.name }}
            </text>
          </view>
        </view>
      </view>

      <scroll-view class="hot-scroll" scroll-x :show-scrollbar="false" v-if="remainingPopularSpots.length">
        <view class="hot-card compact" v-for="spot in remainingPopularSpots" :key="spot.id" @click="goSpotDetail(spot.id)">
          <image class="hot-img" :src="getContentImageUrl(spot.coverImage)" mode="aspectFill" />
          <view class="hot-overlay">
            <text class="hot-name">{{ spot.name }}</text>
            <view class="hot-meta">
              <text class="hot-badge">{{ spot.categoryName || '热门' }}</text>
              <text class="hot-price">￥{{ spot.price }}</text>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 偏好设置弹层 -->
    <view class="preference-popup" v-if="preferenceVisible" @click.self="preferenceVisible = false">
      <view class="preference-content">
        <PreferenceCategorySelector
          v-model="selectedCategories"
          :categories="categories"
          eyebrow="偏好冷启动"
          title="选择你感兴趣的景点分类"
          subtitle="先选几类你想看的景点，推荐会立刻从热门冷启动切到偏好冷启动。"
          primary-text="立即开启"
          secondary-text="跳过"
          @submit="savePreferences"
          @secondary="skipColdStartGuide"
          @limit-exceed="handleLimitExceed"
        />
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getBanners, getHotSpots, getNearbySpots } from '@/api/home'
import { promptLogin } from '@/utils/auth'
import {
  getColdStartGuideState,
  markColdStartGuideSkipped
} from '@/utils/cold-start-guide'
import { getAuthorizedLocation, getLocationSnapshot } from '@/utils/location'
import PreferenceCategorySelector from '@/components/PreferenceCategorySelector.vue'
import { useRecommendationFeed } from '@/composables/useRecommendationFeed'
import { getAvatarUrl, getContentImageUrl } from '@/utils/request'
import { useUserStore } from '@/stores/user'

// 基础依赖与用户状态
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isLoggedIn = computed(() => userStore.isLoggedIn)

// 页面数据状态
const banners = ref([])
const popularSpots = ref([])
const {
  recommendations,
  recommendationType,
  needPreference,
  preferenceVisible,
  categories,
  selectedCategories,
  recommendType,
  ensureCategoriesLoaded,
  fetchRecommendationList,
  refreshRecommendationList,
  openPreferenceDialog,
  savePreferences: persistPreferences,
  resetRecommendationState
} = useRecommendationFeed(12)
const preferencePopupTriggered = ref(false)

const nearbySpots = ref([])
const nearbyLocation = ref(null)
const nearbyLoading = ref(false)
const locationStatus = ref('idle')
const nearbySessionToken = ref('')

// 常量配置
const markerIcon = '/static/tabbar/spot-active.png'

const quickActions = [
  { id: 'spots', title: '景点列表', note: '全部景点', icon: 'location-filled', theme: 'blue', action: 'spot-list' },
  { id: 'guides', title: '攻略列表', note: '游玩攻略', icon: 'paperplane-filled', theme: 'orange', action: 'guide-list' },
  { id: 'recommend', title: '推荐景点', note: '个性推荐', icon: 'star-filled', theme: 'amber', action: 'recommend-spots' },
  { id: 'nearby', title: '附近景点', note: '周边探索', icon: 'map-filled', theme: 'emerald', action: 'nearby-spots' }
]

const pendingQuickActions = Array.from({ length: 4 }, (_, index) => ({
  id: `pending-${index + 1}`,
  title: '待定入口',
  note: '后续补齐',
  icon: 'more-filled',
  theme: 'slate',
  action: 'pending'
}))

const displayQuickActions = [...quickActions, ...pendingQuickActions]

// 计算属性
const recommendationSectionTitle = computed(() => (isLoggedIn.value ? recommendType.value : '推荐景点'))
const recommendPreview = computed(() => recommendations.value.slice(0, 4))
const primaryPopularSpot = computed(() => popularSpots.value[0] || null)
const remainingPopularSpots = computed(() => popularSpots.value.slice(1))
const MAX_NEARBY_DISTANCE_KM = 100

const toFiniteNumber = (value) => {
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

const isValidLatitude = (value) => value !== null && value >= -90 && value <= 90
const isValidLongitude = (value) => value !== null && value >= -180 && value <= 180
const isValidCoordinate = (latitude, longitude) => isValidLatitude(latitude) && isValidLongitude(longitude)

const normalizedNearbySpots = computed(() => {
  return nearbySpots.value
    .map((spot) => {
      const latitude = toFiniteNumber(spot.latitude)
      const longitude = toFiniteNumber(spot.longitude)
      return {
        ...spot,
        latitude,
        longitude,
        distanceKm: toFiniteNumber(spot.distanceKm)
      }
    })
    .filter(spot => isValidCoordinate(spot.latitude, spot.longitude))
})

const hasReasonableNearbySpots = computed(() => {
  if (!normalizedNearbySpots.value.length) return false
  const nearestDistance = normalizedNearbySpots.value[0]?.distanceKm
  return nearestDistance === null || nearestDistance <= MAX_NEARBY_DISTANCE_KM
})

const displayNearbySpots = computed(() => {
  return hasReasonableNearbySpots.value ? normalizedNearbySpots.value : []
})

const canShowNearbyMap = computed(() => {
  if (locationStatus.value !== 'ready' || !displayNearbySpots.value.length) return false
  const latitude = toFiniteNumber(nearbyLocation.value?.latitude)
  const longitude = toFiniteNumber(nearbyLocation.value?.longitude)
  return isValidCoordinate(latitude, longitude)
})

const nearbyMapCenter = computed(() => {
  const locationLatitude = toFiniteNumber(nearbyLocation.value?.latitude)
  const locationLongitude = toFiniteNumber(nearbyLocation.value?.longitude)
  const useLocation = isValidCoordinate(locationLatitude, locationLongitude)
  const base = useLocation
    ? { latitude: locationLatitude, longitude: locationLongitude }
    : displayNearbySpots.value[0]
  return {
    latitude: base?.latitude ?? 39.9042,
    longitude: base?.longitude ?? 116.4074
  }
})

const nearbyMarkers = computed(() => {
  const markers = displayNearbySpots.value.slice(0, 3).map((spot, index) => ({
    id: Number(spot.id),
    latitude: spot.latitude,
    longitude: spot.longitude,
    iconPath: markerIcon,
    width: 26,
    height: 32,
    zIndex: 10 + index,
    callout: {
      content: spot.name,
      display: 'BYCLICK',
      padding: 6,
      borderRadius: 10
    }
  }))

  const locationLatitude = toFiniteNumber(nearbyLocation.value?.latitude)
  const locationLongitude = toFiniteNumber(nearbyLocation.value?.longitude)
  if (isValidCoordinate(locationLatitude, locationLongitude)) {
    markers.push({
      id: -1,
      latitude: locationLatitude,
      longitude: locationLongitude,
      iconPath: markerIcon,
      width: 18,
      height: 22,
      alpha: 0.7,
      zIndex: 9
    })
  }

  return markers
})

const nearbyHeadline = computed(() => {
  if (nearbyLoading.value) return '定位中'
  if (locationStatus.value === 'ready') return '附近可探索'
  if (locationStatus.value === 'empty') return '附近暂无结果'
  if (!isLoggedIn.value) return '登录后查看'
  return '开启定位'
})

const nearbyActionText = computed(() => {
  if (nearbyLoading.value) return '加载中'
  if (locationStatus.value === 'ready' && displayNearbySpots.value.length) return '查看景点'
  if (!isLoggedIn.value) return '去登录'
  return '开启定位'
})

const nearbySummary = computed(() => {
  if (nearbyLoading.value) return '正在获取你周边的景点'
  if (locationStatus.value === 'ready' && displayNearbySpots.value.length) {
    const nearest = displayNearbySpots.value[0]
    return `你附近有 ${displayNearbySpots.value.length} 个景点，最近约 ${formatDistance(nearest.distanceKm)}`
  }
  if (locationStatus.value === 'empty') return '附近暂时没有可展示的景点'
  if (!isLoggedIn.value) return '登录后可按距离查看你附近的景点'
  return '点击卡片后先授权定位，再加载附近景点'
})

const nearbyCaption = computed(() => {
  if (locationStatus.value === 'ready' && displayNearbySpots.value.length) {
    return `${displayNearbySpots.value[0].regionName || '周边区域'} · 点击进入附近景点页`
  }
  if (locationStatus.value === 'empty') return '你可以先看看热门景点'
  if (!isLoggedIn.value) return '附近景点需要登录后使用'
  return '流程：登录 → 授权 → 定位 → 加载景点'
})

const nearbyPlaceholderText = computed(() => {
  if (nearbyLoading.value) return '定位中...'
  if (!isLoggedIn.value) return '登录后开启'
  if (locationStatus.value === 'empty') return '暂无附近景点'
  return '点击开启定位'
})

// 工具方法
const formatDistance = (value) => {
  const distance = Number(value)
  if (!Number.isFinite(distance)) return '-- km'
  return distance < 1 ? `${Math.max(100, Math.round(distance * 1000))} m` : `${distance.toFixed(1)} km`
}

const resetNearbyState = () => {
  nearbySpots.value = []
  nearbyLocation.value = null
  nearbyLoading.value = false
  locationStatus.value = 'idle'
  nearbySessionToken.value = ''
}

// 数据加载方法
const fetchBanners = async () => {
  try {
    const res = await getBanners()
    banners.value = res.data?.list || []
  } catch (error) {
    console.error('获取轮播图失败', error)
  }
}

const fetchHotSpots = async () => {
  try {
    const res = await getHotSpots(6)
    popularSpots.value = res.data?.list || []
  } catch (error) {
    console.error('获取热门景点失败', error)
  }
}

const fetchRecommendations = async () => {
  const data = await fetchRecommendationList()
  if (!userStore.token) {
    preferencePopupTriggered.value = false
    return
  }
  if (data) {
    maybeShowColdStartGuide()
  }
}

const fetchNearbyByLocation = async (latitude, longitude, limit = 3) => {
  nearbyLoading.value = true
  try {
    nearbyLocation.value = { latitude, longitude }
    const res = await getNearbySpots(latitude, longitude, limit)
    nearbySpots.value = res.data?.list || []
    locationStatus.value = hasReasonableNearbySpots.value ? 'ready' : 'empty'
    nearbySessionToken.value = userStore.token || ''
    return displayNearbySpots.value
  } catch (error) {
    if (error?.code === 10002) {
      resetNearbyState()
      throw error
    }
    nearbySpots.value = []
    locationStatus.value = 'empty'
    console.error('获取附近景点失败', error)
    throw error
  } finally {
    nearbyLoading.value = false
  }
}

const ensureNearbyAccess = async () => {
  if (!promptLogin('登录后可查看附近景点，是否现在去登录？')) {
    return null
  }

  try {
    const position = await getAuthorizedLocation()
    await fetchNearbyByLocation(position.latitude, position.longitude, 3)
    return position
  } catch (error) {
    if (error?.code === 10002) {
      return null
    }
    if (error?.message !== 'LOCATION_PERMISSION_DENIED') {
      uni.showToast({ title: '定位失败，请稍后重试', icon: 'none' })
    }
    return null
  }
}

const tryLoadNearbyAutomatically = async () => {
  if (!userStore.token) {
    resetNearbyState()
    return
  }

  if (nearbyLoading.value || locationStatus.value === 'ready') {
    return
  }

  try {
    const snapshot = await getLocationSnapshot()
    const position = snapshot.current
    if (!position) {
      if (locationStatus.value === 'idle') {
        locationStatus.value = 'idle'
      }
      return
    }
    await fetchNearbyByLocation(position.latitude, position.longitude, 3)
  } catch (error) {
    if (error?.code === 10002) {
      return
    }
    console.error('自动加载附近景点失败', error)
  }
}

// 交互处理方法
const handleRefresh = async () => {
  if (!promptLogin('登录后可刷新推荐，是否现在去登录？')) {
    return
  }

  uni.showLoading({ title: '加载中...' })
  try {
    await refreshRecommendationList()
    uni.showToast({ title: '已刷新', icon: 'none' })
  } catch (error) {
    console.error('刷新推荐失败', error)
    uni.showToast({ title: '刷新失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

const showPreferencePopup = async () => {
  if (!promptLogin('登录后可设置推荐偏好，是否现在去登录？')) {
    return
  }
  await openPreferenceDialog()
}

const maybeShowColdStartGuide = async () => {
  if (!userStore.isLoggedIn) return
  if (!needPreference.value || preferenceVisible.value || preferencePopupTriggered.value) return

  const currentUserId = userStore.userInfo?.id
  const currentPreferenceIds = userStore.userInfo?.preferenceCategoryIds || []
  if (!currentUserId || currentPreferenceIds.length) {
    return
  }

  const guideState = getColdStartGuideState(currentUserId)
  if (!guideState.pending || guideState.skipped || guideState.completed) {
    return
  }

  await ensureCategoriesLoaded()
  selectedCategories.value = []
  preferencePopupTriggered.value = true
  preferenceVisible.value = true
}

const handleLimitExceed = () => {
  uni.showToast({ title: '最多选择 5 个', icon: 'none' })
}

const savePreferences = async () => {
  if (!selectedCategories.value.length) {
    uni.showToast({ title: '请至少选择一个', icon: 'none' })
    return
  }

  try {
    await persistPreferences()
    uni.showToast({ title: '设置成功', icon: 'success' })
    await handleRefresh()
  } catch (error) {
    console.error('保存偏好失败', error)
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

const skipColdStartGuide = async () => {
  markColdStartGuideSkipped(userStore.userInfo?.id)
  preferenceVisible.value = false
  needPreference.value = false
  recommendationType.value = 'hot'
  await fetchHotSpots()
  recommendations.value = popularSpots.value.slice(0, 4).map(item => ({
    id: item.id,
    name: item.name,
    coverImage: item.coverImage,
    price: item.price,
    avgRating: item.avgRating,
    categoryName: item.categoryName,
    intro: item.description || ''
  }))
  uni.showToast({ title: '已跳过，后续可在我的-偏好设置里设置', icon: 'none' })
}

const handleBannerClick = (banner) => {
  if (banner.spotId) {
    goSpotDetail(banner.spotId)
  }
}

const handleQuickAction = (action) => {
  switch (action.action) {
    case 'spot-list':
      goSpotList()
      break
    case 'guide-list':
      goGuideList()
      break
    case 'recommend-spots':
      goRecommendationSpots()
      break
    case 'nearby-spots':
      handleNearbyCardClick()
      break
    case 'pending':
      uni.showToast({ title: '入口功能待补齐', icon: 'none' })
      break
    default:
      break
  }
}

const handleNearbyCardClick = async () => {
  if (nearbyLocation.value) {
    goNearbyList()
    return
  }

  await ensureNearbyAccess()
}

const handleNearbyMarkerTap = (event) => {
  const spot = displayNearbySpots.value.find(item => Number(item.id) === Number(event.detail.markerId))
  if (spot) {
    goSpotDetail(spot.id)
  }
}

// 页面跳转方法
const goSpotDetail = (spotId) => {
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/spot/detail?id=${spotId}&source=home` })
}

const goSpotList = () => {
  uni.navigateTo({ url: '/pages/spot/list?sortBy=heat' })
}

const goGuideList = () => {
  uni.navigateTo({ url: '/pages/guide/list?sortBy=time' })
}

const goRecommendationSpots = () => {
  if (!promptLogin('登录后可查看推荐景点，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: '/pages/recommendation/index' })
}

const goNearbyList = (position = nearbyLocation.value) => {
  if (position) {
    uni.navigateTo({
      url: `/pages/spot/nearby?latitude=${position.latitude}&longitude=${position.longitude}`
    })
    return
  }
  uni.navigateTo({ url: '/pages/spot/nearby' })
}

const goSearch = () => {
  uni.navigateTo({ url: '/pages/spot/search' })
}

const goMine = () => {
  uni.switchTab({ url: '/pages/mine/index' })
}

const refreshHome = async () => {
  await Promise.all([fetchBanners(), fetchHotSpots(), fetchRecommendations()])
}

// 生命周期
onPullDownRefresh(async () => {
  await refreshHome()
  uni.stopPullDownRefresh()
})

onShow(() => {
  if (!userStore.token || nearbySessionToken.value !== userStore.token) {
    resetNearbyState()
    preferencePopupTriggered.value = false
    if (!userStore.token) {
      resetRecommendationState()
    }
  }
  refreshHome()
  tryLoadNearbyAutomatically()
})
</script>

<style scoped>
.ios-page {
  min-height: 100vh;
  background: #f4f6fb;
  padding-bottom: 48rpx;
}

.ios-header {
  padding: 88rpx 32rpx 24rpx;
  background: linear-gradient(180deg, #ffffff 0%, #eef4ff 100%);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.large-title {
  display: block;
  font-size: 56rpx;
  font-weight: 800;
  color: #111827;
}

.sub-title {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #6b7280;
}

.avatar-sm {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #dbe4f0;
}

.search-bar {
  pointer-events: auto;
}

:deep(.search-bar .uni-searchbar) {
  padding: 0;
  background: transparent;
}

:deep(.search-bar .uni-searchbar__box) {
  height: 80rpx;
  border-radius: 20rpx;
}

.banner-container {
  padding: 28rpx 32rpx 12rpx;
}

.banner,
.banner-image {
  width: 100%;
  height: 320rpx;
  border-radius: 28rpx;
}

.banner {
  overflow: hidden;
  box-shadow: 0 10rpx 28rpx rgba(31, 41, 55, 0.12);
}

.guest-banner {
  margin: 16rpx 32rpx 0;
  padding: 24rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 6rpx 16rpx rgba(31, 41, 55, 0.06);
}

.guest-title {
  display: block;
  font-size: 28rpx;
  line-height: 1.5;
  color: #1f2937;
}

.guest-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #6b7280;
}

.guest-action {
  font-size: 24rpx;
  font-weight: 600;
  color: #2563eb;
}

.section {
  margin-top: 28rpx;
}

.section-header {
  padding: 0 32rpx 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.section-title {
  font-size: 38rpx;
  font-weight: 700;
  color: #111827;
}

.section-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #6b7280;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.section-link {
  font-size: 26rpx;
  color: #2563eb;
}

.quick-grid {
  padding: 0 32rpx;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20rpx;
}

.quick-item {
  padding: 24rpx 12rpx;
  background: #ffffff;
  border-radius: 24rpx;
  text-align: center;
  box-shadow: 0 6rpx 16rpx rgba(31, 41, 55, 0.05);
}

.quick-icon {
  width: 80rpx;
  height: 80rpx;
  margin: 0 auto 14rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.theme-blue { background: #dbeafe; }
.theme-orange { background: #ffedd5; }
.theme-amber { background: #fef3c7; }
.theme-emerald { background: #d1fae5; }
.theme-slate { background: #e5e7eb; }

.quick-title {
  display: block;
  font-size: 26rpx;
  font-weight: 600;
  color: #111827;
}

.quick-note {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: #6b7280;
}

.recommend-list {
  padding: 0 32rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.recommend-card {
  background: #ffffff;
  border-radius: 28rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 20rpx rgba(31, 41, 55, 0.06);
}

.rec-img {
  width: 100%;
  height: 280rpx;
}

.rec-content {
  padding: 24rpx;
}

.rec-header,
.rec-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.rec-header {
  margin-bottom: 12rpx;
}

.rec-name {
  flex: 1;
  font-size: 32rpx;
  font-weight: 600;
  color: #111827;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.rec-rating {
  font-size: 24rpx;
  color: #f59e0b;
}

.rec-desc {
  display: -webkit-box;
  margin-bottom: 18rpx;
  font-size: 26rpx;
  line-height: 1.5;
  color: #6b7280;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.rec-tag {
  font-size: 22rpx;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.1);
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
}

.rec-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #ef4444;
}

.hot-nearby-grid {
  padding: 0 32rpx;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
}

.feature-card {
  min-height: 360rpx;
  border-radius: 28rpx;
  overflow: hidden;
  position: relative;
  background: #ffffff;
  box-shadow: 0 8rpx 20rpx rgba(31, 41, 55, 0.08);
}

.feature-image {
  width: 100%;
  height: 100%;
}

.feature-overlay {
  position: absolute;
  inset: 0;
  padding: 24rpx 22rpx;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.08) 8%, rgba(15, 23, 42, 0.82) 100%);
}

.feature-eyebrow {
  display: inline-flex;
  align-self: flex-start;
  margin-bottom: 12rpx;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
  font-size: 20rpx;
}

.feature-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #ffffff;
}

.feature-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.88);
}

.feature-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 18rpx;
}

.feature-pill {
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.18);
  color: #ffffff;
  font-size: 20rpx;
}

.feature-pill.subtle {
  background: rgba(15, 23, 42, 0.28);
}

.feature-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24rpx;
  color: #6b7280;
  font-size: 26rpx;
}

.nearby-card {
  padding: 22rpx;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}

.nearby-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.nearby-eyebrow {
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
}

.nearby-status {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #111827;
}

.nearby-link {
  font-size: 22rpx;
  color: #2563eb;
}

.nearby-map-shell {
  margin-top: 18rpx;
  border-radius: 24rpx;
  overflow: hidden;
  background: #dbeafe;
}

.nearby-map {
  width: 100%;
  height: 170rpx;
}

.nearby-placeholder {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  padding: 18rpx;
  background:
    radial-gradient(circle at 20% 20%, rgba(37, 99, 235, 0.18), transparent 28%),
    linear-gradient(135deg, #e0f2fe 0%, #dbeafe 55%, #f8fafc 100%);
}

.placeholder-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.08) 1px, transparent 1px);
  background-size: 28rpx 28rpx;
}

.placeholder-pin {
  position: absolute;
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #2563eb;
  box-shadow: 0 0 0 8rpx rgba(37, 99, 235, 0.12);
}

.pin-a { top: 38rpx; left: 56rpx; }
.pin-b { top: 88rpx; left: 156rpx; }
.pin-c { top: 58rpx; right: 52rpx; }

.placeholder-copy {
  position: relative;
  z-index: 1;
  font-size: 22rpx;
  color: #1d4ed8;
}

.nearby-copy {
  margin-top: 18rpx;
}

.nearby-summary {
  display: block;
  font-size: 26rpx;
  line-height: 1.5;
  color: #111827;
}

.nearby-caption {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #6b7280;
}

.nearby-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 16rpx;
}

.nearby-tag {
  padding: 6rpx 12rpx;
  border-radius: 999rpx;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 20rpx;
}

.hot-scroll {
  margin-top: 20rpx;
  padding-left: 32rpx;
  white-space: nowrap;
}

.hot-card {
  display: inline-block;
  width: 280rpx;
  height: 360rpx;
  margin-right: 20rpx;
  border-radius: 28rpx;
  overflow: hidden;
  position: relative;
  box-shadow: 0 8rpx 20rpx rgba(31, 41, 55, 0.08);
}

.hot-card.compact {
  width: 220rpx;
  height: 260rpx;
}

.hot-img {
  width: 100%;
  height: 100%;
}

.hot-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 24rpx 20rpx;
  background: linear-gradient(180deg, rgba(17, 24, 39, 0) 0%, rgba(17, 24, 39, 0.82) 100%);
}

.hot-name {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #ffffff;
}

.hot-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  margin-top: 10rpx;
}

.hot-badge,
.hot-price {
  font-size: 22rpx;
  color: #ffffff;
}

.empty-tip {
  padding: 48rpx 32rpx;
  text-align: center;
  font-size: 26rpx;
  color: #6b7280;
}

.preference-popup {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.preference-content {
  width: 620rpx;
  background: #ffffff;
  border-radius: 28rpx;
  padding: 40rpx;
}

</style>
