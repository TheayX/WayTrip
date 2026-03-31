<!-- 首页 -->
<template>
  <view class="ios-page">
    <HomeHeader 
      :avatar-url="getAvatarUrl(userInfo?.avatar)"
      @goSearch="goSearch"
      @goMine="goMine"
    />
    <HomeBanner 
      :banners="banners"
      @click="handleBannerClick"
    />
    <QuickNav
      :actions="homeQuickActions"
      @click="handleQuickAction"
    />
    <NearbyAndHot 
      :hot-spots="popularSpots"
      :nearby-headline="nearbyHeadline"
      :can-show-map="canShowNearbyMap"
      :center="nearbyMapCenter"
      :markers="nearbyMarkers"
      :nearby-spots="displayNearbySpots"
      :placeholder-text="nearbyPlaceholderText"
      @clickNearby="handleNearbyCardClick"
      @markerTap="handleNearbyMarkerTap"
      @moreHot="goSpotList"
      @clickHot="(spot) => goSpotDetail(spot.id)"
    />
    <RecommendSpots 
      :is-logged-in="isLoggedIn"
      :title="recommendationSectionTitle"
      :spots="recommendPreview"
      @goLogin="goMine"
      @refresh="handleRefresh"
      @more="goRecommendationSpots"
      @click="(spot) => goSpotDetail(spot.id)"
    />

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
import HomeHeader from './components/HomeHeader.vue'
import HomeBanner from './components/HomeBanner.vue'
import QuickNav from './components/QuickNav.vue'
import RecommendSpots from './components/RecommendSpots.vue'
import NearbyAndHot from './components/NearbyAndHot.vue'
import { useRecommendationFeed } from '@/composables/useRecommendationFeed'
import { getFeatureEntryById, getHomeQuickActions } from '@/constants/feature-navigation'
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

const homeQuickActions = getHomeQuickActions()

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
  switch (action.id) {
    case 'spots':
      goSpotList()
      break
    case 'guides':
      goGuideList()
      break
    case 'recommend':
      goRecommendationSpots()
      break
    case 'nearby':
      handleNearbyCardClick()
      break
    case 'blindbox':
      navigateConfiguredFeature('blindbox')
      break
    case 'budget':
      navigateConfiguredFeature('budget')
      break
    case 'reviews':
      if (!promptLogin('登录后可查看真实口碑，是否现在去登录？')) {
        return
      }
      navigateConfiguredFeature('reviews')
      break
    case 'recent-views':
      navigateConfiguredFeature('recent-views')
      break
    case 'more':
      navigateConfiguredFeature('more')
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

const navigateConfiguredFeature = (id) => {
  const featureEntry = getFeatureEntryById(id)
  if (!featureEntry?.url) return
  uni.navigateTo({ url: featureEntry.url })
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
  border-radius: 36rpx;
  padding: 40rpx;
}
</style>
