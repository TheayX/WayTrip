<template>
  <view class="nearby-page">
    <view class="hero-card">
      <view class="hero-copy">
        <text class="hero-title">附近景点</text>
        <text class="hero-subtitle">{{ heroSubtitle }}</text>
      </view>
      <view class="hero-action" @click="handleRetryLocation">
        <uni-icons type="location-filled" size="16" color="#2563eb" />
        <text class="hero-action-text">{{ actionText }}</text>
      </view>
    </view>

    <view class="map-card">
      <map
        v-if="canShowMap"
        class="nearby-map"
        :latitude="mapCenter.latitude"
        :longitude="mapCenter.longitude"
        :scale="12"
        :markers="markers"
        :show-location="true"
        @markertap="handleMarkerTap"
      />
      <view v-else class="map-placeholder">
        <view class="placeholder-grid"></view>
        <text class="placeholder-text">{{ placeholderText }}</text>
      </view>
    </view>

    <view v-if="nearbySpots.length" class="spot-list">
      <view class="spot-card" v-for="spot in nearbySpots" :key="spot.id" @click="goSpotDetail(spot.id)">
        <image class="spot-image" :src="getContentImageUrl(spot.coverImage)" mode="aspectFill" />
        <view class="spot-content">
          <view class="spot-header">
            <text class="spot-name">{{ spot.name }}</text>
            <text class="spot-distance">{{ formatDistance(spot.distanceKm) }}</text>
          </view>
          <view class="spot-meta">
            <text class="spot-tag">{{ spot.regionName || '附近区域' }}</text>
            <text class="spot-tag">{{ spot.categoryName || '景点' }}</text>
          </view>
          <view class="spot-footer">
            <text class="spot-price">￥{{ spot.price }}</text>
            <text class="spot-rating">评分 {{ spot.avgRating || '4.5' }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-else-if="!loading" class="empty-state">
      <image class="empty-image" src="/static/empty-image.png" mode="widthFix" />
      <text class="empty-title">{{ emptyTitle }}</text>
      <text class="empty-desc">{{ emptyDesc }}</text>
      <view class="primary-btn" @click="handleRetryLocation">{{ actionText }}</view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getNearbySpots } from '@/api/home'
import { promptLogin } from '@/utils/auth'
import { getAuthorizedLocation, getLocationSnapshot } from '@/utils/location'
import { getContentImageUrl } from '@/utils/request'
import UniIcons from '@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue'

const nearbySpots = ref([])
const locationInfo = ref(null)
const loading = ref(false)
const locationStatus = ref('idle')

const markerIcon = '/static/tabbar/spot-active.png'

const heroSubtitle = computed(() => {
  if (loading.value) return '正在根据当前位置获取周边景点'
  if (locationStatus.value === 'ready' && nearbySpots.value.length) {
    return `共找到 ${nearbySpots.value.length} 个景点，最近约 ${formatDistance(nearbySpots.value[0].distanceKm)}`
  }
  if (locationStatus.value === 'empty') return '当前位置附近暂时没有可展示的景点'
  return '登录并授权定位后，可按距离浏览附近景点'
})

const actionText = computed(() => (loading.value ? '定位中' : '重新定位'))

const placeholderText = computed(() => {
  if (loading.value) return '定位中...'
  if (locationStatus.value === 'empty') return '暂无附近景点'
  return '点击重新定位'
})

const canShowMap = computed(() => locationStatus.value === 'ready' && nearbySpots.value.length > 0)

const mapCenter = computed(() => {
  const base = locationInfo.value || nearbySpots.value[0]
  return {
    latitude: Number(base?.latitude || 39.9042),
    longitude: Number(base?.longitude || 116.4074)
  }
})

const markers = computed(() => {
  const spotMarkers = nearbySpots.value.map((spot, index) => ({
    id: Number(spot.id),
    latitude: Number(spot.latitude),
    longitude: Number(spot.longitude),
    iconPath: markerIcon,
    width: 26,
    height: 32,
    zIndex: 10 + index,
    callout: {
      content: `${spot.name} ${formatDistance(spot.distanceKm)}`,
      display: 'BYCLICK',
      padding: 6,
      borderRadius: 10
    }
  }))

  if (locationInfo.value) {
    spotMarkers.push({
      id: -1,
      latitude: Number(locationInfo.value.latitude),
      longitude: Number(locationInfo.value.longitude),
      iconPath: markerIcon,
      width: 18,
      height: 22,
      alpha: 0.7,
      zIndex: 9
    })
  }

  return spotMarkers
})

const emptyTitle = computed(() => {
  if (locationStatus.value === 'empty') return '附近暂时没有景点'
  return '还没有加载附近景点'
})

const emptyDesc = computed(() => {
  if (locationStatus.value === 'empty') return '你可以重新定位，或者先回首页看看热门景点'
  return '点击按钮后会重新申请定位并加载附近景点'
})

const formatDistance = (value) => {
  const distance = Number(value)
  if (!Number.isFinite(distance)) return '-- km'
  return distance < 1 ? `${Math.max(100, Math.round(distance * 1000))} m` : `${distance.toFixed(1)} km`
}

const fetchNearby = async (latitude, longitude, showErrorToast = false) => {
  loading.value = true
  try {
    locationInfo.value = { latitude, longitude }
    const res = await getNearbySpots(latitude, longitude, 10)
    nearbySpots.value = res.data?.list || []
    locationStatus.value = nearbySpots.value.length ? 'ready' : 'empty'
  } catch (error) {
    if (error?.code === 10002) {
      return
    }
    nearbySpots.value = []
    locationStatus.value = 'empty'
    console.error('获取附近景点失败', error)
    if (showErrorToast) {
      uni.showToast({ title: '附近景点加载失败', icon: 'none' })
    }
  } finally {
    loading.value = false
  }
}

const loadNearby = async () => {
  if (!promptLogin('登录后可查看附近景点，是否现在去登录？')) {
    return
  }

  try {
    const position = await getAuthorizedLocation()
    await fetchNearby(position.latitude, position.longitude, true)
  } catch (error) {
    if (error?.message !== 'LOCATION_PERMISSION_DENIED') {
      uni.showToast({ title: '定位失败，请稍后重试', icon: 'none' })
    }
  }
}

const handleRetryLocation = () => {
  loadNearby()
}

const handleMarkerTap = (event) => {
  const target = nearbySpots.value.find(item => Number(item.id) === Number(event.detail.markerId))
  if (target) {
    goSpotDetail(target.id)
  }
}

const goSpotDetail = (id) => {
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/spot/detail?id=${id}&source=nearby` })
}

onLoad(async (options) => {
  if (!promptLogin('登录后可查看附近景点，是否现在去登录？')) {
    return
  }

  const latitude = Number(options?.latitude)
  const longitude = Number(options?.longitude)
  if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
    await fetchNearby(latitude, longitude)
    return
  }

  const snapshot = await getLocationSnapshot()
  if (snapshot.current) {
    await fetchNearby(snapshot.current.latitude, snapshot.current.longitude)
  }
})
</script>

<style scoped>
.nearby-page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f4f6fb;
}

.hero-card,
.map-card,
.spot-card,
.empty-state {
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 20rpx rgba(31, 41, 55, 0.06);
}

.hero-card {
  padding: 28rpx;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.hero-title {
  display: block;
  font-size: 38rpx;
  font-weight: 700;
  color: #111827;
}

.hero-subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #6b7280;
}

.hero-action {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 14rpx 18rpx;
  border-radius: 999rpx;
  background: #eff6ff;
  color: #2563eb;
}

.hero-action-text {
  font-size: 22rpx;
  color: #2563eb;
}

.map-card {
  margin-top: 20rpx;
  overflow: hidden;
}

.nearby-map,
.map-placeholder {
  width: 100%;
  height: 360rpx;
}

.map-placeholder {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
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
  background-size: 32rpx 32rpx;
}

.placeholder-text {
  position: relative;
  z-index: 1;
  font-size: 26rpx;
  color: #1d4ed8;
}

.spot-list {
  margin-top: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.spot-card {
  overflow: hidden;
}

.spot-image {
  width: 100%;
  height: 260rpx;
}

.spot-content {
  padding: 22rpx 24rpx 24rpx;
}

.spot-header,
.spot-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.spot-name {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.spot-distance {
  font-size: 24rpx;
  color: #2563eb;
}

.spot-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.spot-tag {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #eef2ff;
  font-size: 22rpx;
  color: #4b5563;
}

.spot-footer {
  margin-top: 18rpx;
}

.spot-price {
  font-size: 32rpx;
  font-weight: 700;
  color: #ef4444;
}

.spot-rating {
  font-size: 22rpx;
  color: #6b7280;
}

.empty-state {
  margin-top: 20rpx;
  padding: 56rpx 36rpx;
  display: flex;
  align-items: center;
  flex-direction: column;
  text-align: center;
}

.empty-image {
  width: 280rpx;
}

.empty-title {
  margin-top: 24rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.empty-desc {
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #6b7280;
}

.primary-btn {
  margin-top: 28rpx;
  padding: 18rpx 40rpx;
  border-radius: 999rpx;
  background: #2563eb;
  color: #ffffff;
  font-size: 26rpx;
}
</style>
