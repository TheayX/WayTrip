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
    <FeatureEntryGrid
      :entries="homeEntryItems"
      @click="handleFeatureEntryClick"
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
import FeatureEntryGrid from '@/components/feature-entry/FeatureEntryGrid.vue'
import RecommendSpots from './components/RecommendSpots.vue'
import NearbyAndHot from './components/NearbyAndHot.vue'
import { useRecommendationFeed } from '@/composables/useRecommendationFeed'
import { getFeatureEntryById, getHomeEntryItems } from '@/constants/feature-entry-registry'
import { switchTabSafely } from '@/utils/navigation'
import { getAvatarUrl, getContentImageUrl } from '@/utils/request'
import { buildSpotDetailUrl, SPOT_DETAIL_SOURCE } from '@/utils/spot-detail'
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
  rotateRecommendationList,
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
const lastHomeRefreshAt = ref(0)
const lastObservedToken = ref(userStore.token || '')

// 常量配置
const markerIcon = '/static/marker/spot.png'
const HOME_BASE_CACHE_KEY = 'waytrip:miniapp:home:base'
const HOME_BASE_CACHE_TTL_MS = 2 * 60 * 1000
const HOME_REFRESH_INTERVAL_MS = 30 * 1000
const NEARBY_PREVIEW_LIMIT = 6
const NEARBY_FALLBACK_LIMIT = 2

const homeEntryItems = getHomeEntryItems()

// 计算属性
const recommendationSectionTitle = computed(() => (isLoggedIn.value ? recommendType.value : '个性推荐'))
const recommendPreview = computed(() => recommendations.value.slice(0, 4))
const primaryPopularSpot = computed(() => popularSpots.value[0] || null)
const remainingPopularSpots = computed(() => popularSpots.value.slice(1))
const MAX_NEARBY_DISTANCE_KM = 100

// 将输入值安全转换为有限数字。
const toFiniteNumber = (value) => {
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

// 判断纬度是否落在合法范围内。
const isValidLatitude = (value) => value !== null && value >= -90 && value <= 90
// 判断经度是否落在合法范围内。
const isValidLongitude = (value) => value !== null && value >= -180 && value <= 180
// 同时校验经纬度是否可用于地图展示。
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
  if (!normalizedNearbySpots.value.length) return []
  if (hasReasonableNearbySpots.value) {
    return normalizedNearbySpots.value.filter((spot) => {
      return spot.distanceKm === null || spot.distanceKm <= MAX_NEARBY_DISTANCE_KM
    })
  }
  return normalizedNearbySpots.value.slice(0, NEARBY_FALLBACK_LIMIT)
})

const isUsingDistantNearbyFallback = computed(() => {
  return normalizedNearbySpots.value.length > 0 && !hasReasonableNearbySpots.value
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
  return displayNearbySpots.value.slice(0, 3).map((spot, index) => ({
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
})

const nearbyHeadline = computed(() => {
  if (nearbyLoading.value) return '定位中'
  if (isUsingDistantNearbyFallback.value) return '较近推荐'
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
  if (isUsingDistantNearbyFallback.value && displayNearbySpots.value.length) {
    return `当前位置周边较远，先展示最近的 ${displayNearbySpots.value.length} 个景点`
  }
  if (locationStatus.value === 'ready' && displayNearbySpots.value.length) {
    const nearest = displayNearbySpots.value[0]
    return `你附近有 ${displayNearbySpots.value.length} 个景点，最近约 ${formatDistance(nearest.distanceKm)}`
  }
  if (locationStatus.value === 'empty') return '附近暂时没有可展示的景点'
  if (!isLoggedIn.value) return '登录后可按距离查看你附近的景点'
  return '点击卡片后先授权定位，再加载附近景点'
})

const nearbyCaption = computed(() => {
  if (isUsingDistantNearbyFallback.value && displayNearbySpots.value.length) {
    return `最近约 ${formatDistance(displayNearbySpots.value[0].distanceKm)} · 点击进入附近景点页`
  }
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
  if (locationStatus.value === 'empty') return '暂无可用景点'
  return '点击开启定位'
})

// 格式化附近景点距离文案。
const formatDistance = (value) => {
  const distance = Number(value)
  if (!Number.isFinite(distance)) return '-- km'
  return distance < 1 ? `${Math.max(100, Math.round(distance * 1000))} m` : `${distance.toFixed(1)} km`
}

// 清空附近景点相关状态。
const resetNearbyState = () => {
  nearbySpots.value = []
  nearbyLocation.value = null
  nearbyLoading.value = false
  locationStatus.value = 'idle'
  nearbySessionToken.value = ''
}

/**
 * 首页基础区块（轮播 + 热门）优先读本地缓存，减少重复进入首页时的首屏等待。
 */
const restoreHomeBaseFromCache = () => {
  try {
    const cached = uni.getStorageSync(HOME_BASE_CACHE_KEY)
    if (!cached || !cached.timestamp) {
      return false
    }
    if (Date.now() - cached.timestamp > HOME_BASE_CACHE_TTL_MS) {
      return false
    }
    banners.value = cached.banners || []
    popularSpots.value = cached.popularSpots || []
    return true
  } catch (_error) {
    return false
  }
}

const persistHomeBaseCache = () => {
  uni.setStorageSync(HOME_BASE_CACHE_KEY, {
    timestamp: Date.now(),
    banners: banners.value,
    popularSpots: popularSpots.value
  })
}

// 加载首页轮播图。
const fetchBanners = async () => {
  try {
    const res = await getBanners()
    banners.value = res.data?.list || []
    persistHomeBaseCache()
  } catch (error) {
    console.error('获取轮播图失败', error)
  }
}

// 加载首页热门景点。
const fetchHotSpots = async () => {
  try {
    const res = await getHotSpots(6)
    popularSpots.value = res.data?.list || []
    persistHomeBaseCache()
  } catch (error) {
    console.error('获取热门景点失败', error)
  }
}

// 加载推荐结果，并在需要时触发冷启动引导检查。
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

// 根据定位坐标加载附近景点列表。
const fetchNearbyByLocation = async (latitude, longitude, limit = 3) => {
  nearbyLoading.value = true
  try {
    nearbyLocation.value = { latitude, longitude }
    const res = await getNearbySpots(latitude, longitude, limit)
    nearbySpots.value = res.data?.list || []
    locationStatus.value = normalizedNearbySpots.value.length ? 'ready' : 'empty'
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

// 确保已登录且具备定位权限后再加载附近景点。
const ensureNearbyAccess = async () => {
  if (!promptLogin('登录后可查看附近景点，是否现在去登录？')) {
    return null
  }

  try {
    const position = await getAuthorizedLocation()
    await fetchNearbyByLocation(position.latitude, position.longitude, NEARBY_PREVIEW_LIMIT)
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

// 已有定位快照时自动加载附近景点。
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
    await fetchNearbyByLocation(position.latitude, position.longitude, NEARBY_PREVIEW_LIMIT)
  } catch (error) {
    if (error?.code === 10002) {
      return
    }
    console.error('自动加载附近景点失败', error)
  }
}

// 手动触发推荐换一批。
const handleRefresh = async () => {
  if (!promptLogin('登录后可换一批推荐，是否现在去登录？')) {
    return
  }

  uni.showLoading({ title: '加载中...' })
  try {
    await rotateRecommendationList()
    uni.showToast({ title: '换了一批', icon: 'none' })
  } catch (error) {
    console.error('换一批推荐失败', error)
    uni.showToast({ title: '换一批失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

// 打开偏好设置弹层。
const showPreferencePopup = async () => {
  if (!promptLogin('登录后可设置推荐偏好，是否现在去登录？')) {
    return
  }
  await openPreferenceDialog()
}

// 在命中冷启动条件时展示偏好引导。
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

// 偏好选择超过上限时提示用户。
const handleLimitExceed = () => {
  uni.showToast({ title: '最多选择 5 个', icon: 'none' })
}

// 保存用户偏好并刷新推荐结果。
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

// 跳过本次冷启动引导并回退到热门推荐。
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

// 点击轮播图时跳转到对应景点详情。
const handleBannerClick = (banner) => {
  if (banner.spotId) {
    goSpotDetail(banner.spotId)
  }
}

// 根据首页功能入口执行对应跳转逻辑。
const handleFeatureEntryClick = (entry) => {
  switch (entry.id) {
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
    case 'random-pick':
      navigateFeatureEntry('random-pick')
      break
    case 'budget':
      navigateFeatureEntry('budget')
      break
    case 'reviews':
      if (!promptLogin('登录后可查看游客口碑，是否现在去登录？')) {
        return
      }
      navigateFeatureEntry('reviews')
      break
    case 'more':
      navigateFeatureEntry('more')
      break
    default:
      break
  }
}

// 点击附近卡片时优先进入附近列表，缺少定位则先申请权限。
const handleNearbyCardClick = async () => {
  if (nearbyLocation.value) {
    goNearbyList()
    return
  }

  await ensureNearbyAccess()
}

// 点击地图标记时打开对应景点详情。
const handleNearbyMarkerTap = (event) => {
  const spot = displayNearbySpots.value.find(item => Number(item.id) === Number(event.detail.markerId))
  if (spot) {
    goSpotDetail(spot.id)
  }
}

// 从首页进入景点详情页。
const goSpotDetail = (spotId) => {
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: buildSpotDetailUrl(spotId, SPOT_DETAIL_SOURCE.HOME) })
}

// 进入景点列表页并默认按热度排序。
const goSpotList = () => {
  uni.navigateTo({ url: '/pages/spot/list?sortBy=heat' })
}

// 进入攻略列表页并默认按浏览量排序。
const goGuideList = () => {
  uni.navigateTo({ url: '/pages/guide/list?sortBy=view_count' })
}

// 进入完整推荐列表页。
const goRecommendationSpots = () => {
  if (!promptLogin('登录后可查看个性推荐，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: '/pages/recommendation/index' })
}

// 按功能入口配置跳转到目标页面。
const navigateFeatureEntry = (id) => {
  const featureEntry = getFeatureEntryById(id)
  if (!featureEntry?.url) return
  uni.navigateTo({ url: featureEntry.url })
}

// 进入附近景点列表页，并尽量带上当前位置。
const goNearbyList = (position = nearbyLocation.value) => {
  if (position) {
    uni.navigateTo({
      url: `/pages/spot/nearby?latitude=${position.latitude}&longitude=${position.longitude}`
    })
    return
  }
  uni.navigateTo({ url: '/pages/spot/nearby' })
}

// 打开景点搜索页。
const goSearch = () => {
  uni.navigateTo({ url: '/pages/spot/search' })
}

// 切换到个人中心页。
const goMine = () => {
  void switchTabSafely('/pages/mine/index')
}

// 在节流窗口外刷新首页核心内容。
const refreshHome = async ({ force = false } = {}) => {
  const now = Date.now()
  if (!force && now - lastHomeRefreshAt.value < HOME_REFRESH_INTERVAL_MS) {
    return
  }
  lastHomeRefreshAt.value = now
  await Promise.all([fetchBanners(), fetchHotSpots(), fetchRecommendations()])
}

// 登录态变化会影响推荐、附近景点和冷启动弹层，必须绕过普通首页刷新节流。
const syncHomeAuthState = () => {
  const currentToken = userStore.token || ''
  if (currentToken === lastObservedToken.value) {
    return false
  }

  lastObservedToken.value = currentToken
  resetNearbyState()
  resetRecommendationState()
  preferencePopupTriggered.value = false
  return true
}

// 下拉刷新时强制重载首页数据。
onPullDownRefresh(async () => {
  await refreshHome({ force: true })
  uni.stopPullDownRefresh()
})

// 页面显示时同步登录态并按需刷新首页与附近景点。
onShow(() => {
  const authChanged = syncHomeAuthState()

  if (!userStore.token || nearbySessionToken.value !== userStore.token) {
    resetNearbyState()
    preferencePopupTriggered.value = false
    if (!userStore.token) {
      resetRecommendationState()
    }
  }

  if (!banners.value.length || !popularSpots.value.length) {
    restoreHomeBaseFromCache()
  }
  void refreshHome({ force: authChanged })
  void tryLoadNearbyAutomatically()
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
