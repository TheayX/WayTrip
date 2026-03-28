<template>
  <view class="ios-page">
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

    <view class="banner-container" v-if="banners.length">
      <swiper class="banner" indicator-dots indicator-active-color="#fff" autoplay circular>
        <swiper-item v-for="banner in banners" :key="banner.id" @click="handleBannerClick(banner)">
          <image class="banner-image" :src="getContentImageUrl(banner.imageUrl)" mode="aspectFill" />
        </swiper-item>
      </swiper>
    </view>

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
        <text class="guest-title">登录后可保存偏好，拿到更稳定的推荐结果</text>
        <text class="guest-subtitle">同步收藏、订单和个性化推荐结果</text>
      </view>
      <text class="guest-action">去登录</text>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">{{ recommendType }}</text>
        <view v-if="isLoggedIn" class="section-actions">
          <text class="section-link" @click="handleRefresh">换一批</text>
          <text class="section-link" @click="goRecommendList">查看更多</text>
        </view>
      </view>

      <view class="preference-tip" v-if="isLoggedIn && needPreference" @click="showPreferencePopup">
        <view>
          <text class="tip-text">选择你感兴趣的景点分类，帮助冷启动推荐更准确</text>
          <text class="tip-subtext">当前偏好标签统一使用景点分类</text>
        </view>
        <text class="tip-arrow">></text>
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
        <text>{{ isLoggedIn ? '当前暂无推荐内容' : '登录后查看推荐内容' }}</text>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">热门目的地</text>
        <text class="section-link" @click="goSpotList">查看全部</text>
      </view>

      <view class="hot-nearby-grid">
        <view v-if="featuredHotSpot" class="feature-card hot-feature-card" @click="goSpotDetail(featuredHotSpot.id)">
          <image class="feature-image" :src="getContentImageUrl(featuredHotSpot.coverImage)" mode="aspectFill" />
          <view class="feature-overlay">
            <text class="feature-eyebrow">热门景点</text>
            <text class="feature-title">{{ featuredHotSpot.name }}</text>
            <text class="feature-subtitle">
              {{ featuredHotSpot.categoryName || '精选景点' }} · ￥{{ featuredHotSpot.price }}
            </text>
            <view class="feature-foot">
              <text class="feature-pill">评分 {{ featuredHotSpot.avgRating || '4.6' }}</text>
              <text class="feature-pill subtle">热度 {{ featuredHotSpot.heatScore || '--' }}</text>
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

          <view v-if="nearbySpots.length" class="nearby-tags">
            <text v-for="spot in nearbySpots.slice(0, 2)" :key="spot.id" class="nearby-tag">
              {{ spot.name }}
            </text>
          </view>
        </view>
      </view>

      <scroll-view class="hot-scroll" scroll-x :show-scrollbar="false" v-if="secondaryHotSpots.length">
        <view class="hot-card compact" v-for="spot in secondaryHotSpots" :key="spot.id" @click="goSpotDetail(spot.id)">
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

    <view class="preference-popup" v-if="preferenceVisible" @click.self="preferenceVisible = false">
      <view class="preference-content">
        <text class="preference-title">选择你感兴趣的景点分类</text>
        <view class="preference-tags">
          <view
            v-for="cat in categories"
            :key="cat.id"
            class="preference-tag-item"
            :class="{ active: selectedCategories.includes(cat.id) }"
            @click="toggleCategory(cat.id)"
          >
            {{ cat.name }}
          </view>
        </view>
        <view class="preference-actions">
          <button class="cancel-btn" @click="preferenceVisible = false">取消</button>
          <button class="confirm-btn" @click="savePreferences">确定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getBanners, getHotSpots, getNearbySpots, getRecommendations, refreshRecommendations } from '@/api/home'
import { updatePreferences } from '@/api/auth'
import { getFilters } from '@/api/spot'
import { promptLogin } from '@/utils/auth'
import { getAuthorizedLocation, getLocationIfAuthorized } from '@/utils/location'
import { getAvatarUrl, getContentImageUrl } from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isLoggedIn = computed(() => userStore.isLoggedIn)

const banners = ref([])
const hotSpots = ref([])
const recommendations = ref([])
const recommendationType = ref('hot')
const needPreference = ref(false)

const preferenceVisible = ref(false)
const categories = ref([])
const selectedCategories = ref([])

const nearbySpots = ref([])
const nearbyLocation = ref(null)
const nearbyLoading = ref(false)
const locationStatus = ref('idle')
const nearbySessionToken = ref('')

const markerIcon = '/static/tabbar/spot-active.png'

const quickActions = [
  { id: 'spots', title: '景点列表', note: '全部景点', icon: 'location-filled', theme: 'blue', action: 'spot-list' },
  { id: 'guides', title: '攻略列表', note: '游玩攻略', icon: 'paperplane-filled', theme: 'orange', action: 'guide-list' },
  { id: 'recommend', title: '推荐列表', note: '个性推荐', icon: 'star-filled', theme: 'amber', action: 'recommend-list' },
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

const recommendType = computed(() => {
  const types = {
    personalized: '为你推荐',
    preference: '猜你喜欢',
    hot: '热门推荐'
  }
  return types[recommendationType.value] || '为你推荐'
})

const recommendPreview = computed(() => recommendations.value.slice(0, 4))
const featuredHotSpot = computed(() => hotSpots.value[0] || null)
const secondaryHotSpots = computed(() => hotSpots.value.slice(1))
const canShowNearbyMap = computed(() => locationStatus.value === 'ready' && nearbySpots.value.length > 0)

const nearbyMapCenter = computed(() => {
  const base = nearbyLocation.value || nearbySpots.value[0]
  return {
    latitude: Number(base?.latitude || 39.9042),
    longitude: Number(base?.longitude || 116.4074)
  }
})

const nearbyMarkers = computed(() => {
  const markers = nearbySpots.value.slice(0, 3).map((spot, index) => ({
    id: Number(spot.id),
    latitude: Number(spot.latitude),
    longitude: Number(spot.longitude),
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

  if (nearbyLocation.value) {
    markers.push({
      id: -1,
      latitude: Number(nearbyLocation.value.latitude),
      longitude: Number(nearbyLocation.value.longitude),
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
  if (locationStatus.value === 'ready' && nearbySpots.value.length) return '查看景点'
  if (!isLoggedIn.value) return '去登录'
  return '开启定位'
})

const nearbySummary = computed(() => {
  if (nearbyLoading.value) return '正在获取你周边的景点'
  if (locationStatus.value === 'ready' && nearbySpots.value.length) {
    const nearest = nearbySpots.value[0]
    return `你附近有 ${nearbySpots.value.length} 个景点，最近约 ${formatDistance(nearest.distanceKm)}`
  }
  if (locationStatus.value === 'empty') return '附近暂时没有可展示的景点'
  if (!isLoggedIn.value) return '登录后可按距离查看你附近的景点'
  return '点击卡片后先授权定位，再加载附近景点'
})

const nearbyCaption = computed(() => {
  if (locationStatus.value === 'ready' && nearbySpots.value.length) {
    return `${nearbySpots.value[0].regionName || '周边区域'} · 点击进入附近景点页`
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
    hotSpots.value = res.data?.list || []
  } catch (error) {
    console.error('获取热门景点失败', error)
  }
}

const fetchRecommendations = async () => {
  if (!userStore.token) {
    recommendations.value = []
    recommendationType.value = 'hot'
    needPreference.value = false
    return
  }

  try {
    const res = await getRecommendations(12)
    recommendations.value = res.data?.list || []
    recommendationType.value = res.data?.type || 'hot'
    needPreference.value = res.data?.needPreference || false
  } catch (error) {
    console.error('获取推荐失败', error)
  }
}

const fetchCategories = async () => {
  try {
    const res = await getFilters()
    categories.value = res.data?.categories || []
  } catch (error) {
    console.error('获取分类失败', error)
  }
}

const fetchNearbyByLocation = async (latitude, longitude, limit = 3) => {
  nearbyLoading.value = true
  try {
    nearbyLocation.value = { latitude, longitude }
    const res = await getNearbySpots(latitude, longitude, limit)
    nearbySpots.value = res.data?.list || []
    locationStatus.value = nearbySpots.value.length ? 'ready' : 'empty'
    nearbySessionToken.value = userStore.token || ''
    return nearbySpots.value
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
    const position = await getLocationIfAuthorized()
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

const handleRefresh = async () => {
  if (!promptLogin('登录后可刷新推荐，是否现在去登录？')) {
    return
  }

  uni.showLoading({ title: '加载中...' })
  try {
    const res = await refreshRecommendations(12)
    recommendations.value = res.data?.list || []
    recommendationType.value = res.data?.type || 'hot'
    needPreference.value = res.data?.needPreference || false
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

  if (!categories.value.length) {
    await fetchCategories()
  }
  preferenceVisible.value = true
}

const toggleCategory = (id) => {
  const index = selectedCategories.value.indexOf(id)
  if (index > -1) {
    selectedCategories.value.splice(index, 1)
    return
  }
  if (selectedCategories.value.length >= 5) {
    uni.showToast({ title: '最多选择 5 个', icon: 'none' })
    return
  }
  selectedCategories.value.push(id)
}

const savePreferences = async () => {
  if (!selectedCategories.value.length) {
    uni.showToast({ title: '请至少选择一个', icon: 'none' })
    return
  }

  try {
    const categoryNames = selectedCategories.value
      .map(id => categories.value.find(cat => cat.id === id)?.name)
      .filter(Boolean)
    await updatePreferences({ categoryIds: selectedCategories.value })
    userStore.updatePreferences({
      preferences: categoryNames,
      preferenceCategoryIds: [...selectedCategories.value],
      preferenceCategoryNames: categoryNames
    })
    preferenceVisible.value = false
    uni.showToast({ title: '设置成功', icon: 'success' })
    await handleRefresh()
  } catch (error) {
    console.error('保存偏好失败', error)
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
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
    case 'recommend-list':
      goRecommendList()
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
  if (locationStatus.value === 'ready' && nearbySpots.value.length) {
    goNearbyList()
    return
  }

  await ensureNearbyAccess()
}

const handleNearbyMarkerTap = (event) => {
  const spot = nearbySpots.value.find(item => Number(item.id) === Number(event.detail.markerId))
  if (spot) {
    goSpotDetail(spot.id)
  }
}

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

const goRecommendList = () => {
  if (!promptLogin('登录后可查看推荐列表，是否现在去登录？')) {
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

onPullDownRefresh(async () => {
  await refreshHome()
  uni.stopPullDownRefresh()
})

onShow(() => {
  if (!userStore.token || nearbySessionToken.value !== userStore.token) {
    resetNearbyState()
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

.preference-tip {
  margin: 0 32rpx 24rpx;
  padding: 24rpx 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 6rpx 16rpx rgba(31, 41, 55, 0.05);
}

.tip-text {
  display: block;
  font-size: 28rpx;
  color: #2563eb;
}

.tip-subtext {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #6b7280;
}

.tip-arrow {
  font-size: 34rpx;
  color: #9ca3af;
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

.preference-title {
  display: block;
  margin-bottom: 28rpx;
  font-size: 34rpx;
  font-weight: 700;
  text-align: center;
  color: #111827;
}

.preference-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 28rpx;
}

.preference-tag-item {
  padding: 16rpx 24rpx;
  border-radius: 999rpx;
  background: #eef2f7;
  font-size: 24rpx;
  color: #4b5563;
}

.preference-tag-item.active {
  background: #2563eb;
  color: #ffffff;
}

.preference-actions {
  display: flex;
  gap: 20rpx;
}

.cancel-btn,
.confirm-btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 42rpx;
  font-size: 28rpx;
  border: none;
}

.cancel-btn {
  background: #eef2f7;
  color: #4b5563;
}

.confirm-btn {
  background: #2563eb;
  color: #ffffff;
}
</style>
