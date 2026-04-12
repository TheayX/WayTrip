<!-- 景点详情页 -->
<template>
  <view class="ios-page" v-if="spot">
    <!-- 图片轮播 -->
    <swiper class="image-swiper" indicator-dots indicator-active-color="#fff" circular v-if="spotImages.length">
      <swiper-item v-for="(img, index) in spotImages" :key="index">
        <image class="swiper-image" :src="img" mode="aspectFill" @click="previewImage(index)" />
      </swiper-item>
    </swiper>
    <view class="image-swiper empty-swiper" v-else>
      <text class="empty-swiper-text">暂无景点图片</text>
    </view>

    <!-- 基本信息卡片 -->
    <view class="info-card">
      <view class="info-header">
        <text class="spot-name">{{ spot.name }}</text>
        <view class="favorite-btn" :class="{ active: spot.isFavorite }" @click="toggleFavorite">
          <uni-icons :type="spot.isFavorite ? 'heart-filled' : 'heart'" size="22" :color="spot.isFavorite ? '#be123c' : '#64748b'" />
        </view>
      </view>
      <view class="info-meta">
        <view class="rating-chip">
          <uni-icons type="star-filled" size="14" color="#d97706" />
          <text class="rating">{{ spot.avgRating }}</text>
        </view>
        <text class="rating-count">({{ spot.ratingCount }}条评价)</text>
        <text class="divider">·</text>
        <text class="category">{{ spot.regionName }} · {{ spot.categoryName }}</text>
      </view>
      <view class="price-row">
        <view>
          <text class="price">¥{{ spot.price }}</text>
          <text class="price-label">/人</text>
        </view>
        <view class="price-stats">
          <text class="price-stat">{{ spot.favoriteCount ?? 0 }}收藏</text>
          <text class="price-stat">{{ spot.viewCount ?? 0 }}浏览</text>
        </view>
      </view>
    </view>

    <!-- 详细信息 -->
    <view class="detail-card">
      <view class="detail-item">
        <text class="detail-label">开放时间</text>
        <text class="detail-value">{{ spot.openTime || '暂无信息' }}</text>
      </view>
      <view class="detail-item" @click="openNavigation">
        <text class="detail-label">景点地址</text>
        <view class="detail-value-row">
          <view class="address-meta">
            <text class="detail-value address-text">{{ spot.address }}</text>
            <text v-if="distanceText" class="distance-text">· 距你 {{ distanceText }}</text>
          </view>
          <text class="nav-link">导航 ›</text>
        </view>
      </view>
      <view class="detail-item">
        <text class="detail-label">创建时间</text>
        <text class="detail-value">{{ formatDateTime(spot.createdAt) }}</text>
      </view>
      <view class="detail-item">
        <text class="detail-label">更新时间</text>
        <text class="detail-value">{{ formatDateTime(spot.updatedAt) }}</text>
      </view>
    </view>

    <!-- 景点简介 -->
    <view class="desc-card">
      <text class="card-title">景点简介</text>
      <text class="desc-content">{{ spot.description || '暂无简介' }}</text>
    </view>

    <!-- 相似景点区域 -->
    <view class="related-card" v-if="similarSpots.length">
      <view class="card-header">
        <text class="card-title">你可能还想去</text>
        <text class="more-link">{{ similarUpdateTimeText }}</text>
      </view>
      <scroll-view class="related-scroll" scroll-x :show-scrollbar="false">
        <view
          class="related-item"
          v-for="item in similarSpots"
          :key="item.spotId"
          @click="goSimilarSpot(item.spotId)"
        >
          <image class="related-image" :src="getContentImageUrl(item.coverImage)" mode="aspectFill" />
          <view class="related-info">
            <text class="related-name">{{ item.spotName }}</text>
            <text class="related-meta">{{ item.regionName || '周边景点' }} · {{ item.categoryName || '推荐' }}</text>
            <view class="related-footer">
              <text class="related-price">¥{{ item.price || 0 }}</text>
              <text class="related-score">匹配度 {{ formatSimilarity(item.similarity) }}</text>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 最新评论 -->
    <view class="comment-card">
      <view class="card-header">
        <text class="card-title">最新评论</text>
        <text class="more-link" @click="showRatingPopup">去评价 ›</text>
      </view>
      <view class="comment-list" v-if="spot.latestComments?.length">
        <view class="comment-item" v-for="comment in spot.latestComments" :key="comment.id">
          <image class="comment-avatar" :src="getAvatarUrl(comment.avatar)" />
          <view class="comment-content">
            <view class="comment-header">
              <text class="comment-name">{{ comment.nickname || UNKNOWN_USER_DISPLAY }}</text>
              <view class="comment-meta">
                <view class="comment-score">
                  <uni-icons type="star-filled" size="13" color="#d97706" />
                  <text class="comment-score-text">{{ comment.score }}</text>
                </view>
                <text
                  v-if="canDeleteComment(comment)"
                  class="comment-delete"
                  @tap.stop="handleDeleteComment(comment)"
                >删除</text>
              </view>
            </view>
            <text class="comment-text">{{ comment.comment }}</text>
            <text class="comment-time">{{ comment.createdAt }}</text>
          </view>
        </view>
      </view>
      <view class="empty-comment" v-else>
        <text>暂无评论，快来抢沙发吧~</text>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="action-btn" @click="showRatingPopup">
        <uni-icons type="compose" size="22" color="#64748b" />
        <text class="action-text">评价</text>
      </view>
      <view class="action-btn" :class="{ active: spot.isFavorite }" @click="toggleFavorite">
        <uni-icons :type="spot.isFavorite ? 'heart-filled' : 'heart'" size="22" :color="spot.isFavorite ? '#be123c' : '#64748b'" />
        <text class="action-text">收藏</text>
      </view>
      <button class="buy-btn" @click="goBuy">立即购票</button>
    </view>

    <!-- 评分弹窗 -->
    <view class="rating-popup" v-if="ratingVisible" @tap.self="ratingVisible = false" @touchmove.stop.prevent>
      <view class="rating-content" @tap.stop>
        <text class="rating-title">评价景点</text>
        <view class="star-row">
          <view
            v-for="i in 5" 
            :key="i" 
            class="star" 
            :class="{ active: i <= ratingForm.score }"
            @tap.stop="ratingForm.score = i"
          >
            <uni-icons type="star-filled" size="22" :color="i <= ratingForm.score ? '#d97706' : '#e2e8f0'" />
          </view>
        </view>
        <textarea 
          class="rating-textarea" 
          v-model="ratingForm.comment" 
          placeholder="分享你的游玩体验..."
          maxlength="500"
          @tap.stop
        />
        <view class="rating-actions">
          <button class="cancel-btn" @tap.stop="ratingVisible = false">取消</button>
          <button class="submit-btn" @tap.stop="submitRatingHandler">提交</button>
        </view>
      </view>
    </view>
  </view>

  <view class="ios-page loading-page" v-else-if="loading">
    <text class="state-text">正在加载景点详情...</text>
  </view>

  <view class="ios-page loading-page" v-else>
    <text class="state-text">{{ loadErrorMessage || '景点信息不存在或已下架' }}</text>
    <button class="state-btn" @click="goSpotList">返回景点列表</button>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onLoad, onShow, onHide, onUnload } from '@dcloudio/uni-app'
import { getSpotDetail, getSimilarSpots, recordSpotView } from '@/api/spot'
import { addFavorite, removeFavorite } from '@/api/favorite'
import { deleteReview, submitReview } from '@/api/review'
import { guardLoginPage, promptLogin } from '@/utils/auth'
import { getLocationSnapshot } from '@/utils/location'
import { getAvatarUrl, getContentImageUrl } from '@/utils/request'
import { buildSpotDetailUrl, SPOT_DETAIL_SOURCE } from '@/utils/spot-detail'
import { useUserStore } from '@/stores/user'

// 基础依赖与用户状态
const UNKNOWN_USER_DISPLAY = '未知用户'
const spot = ref(null)
const spotId = ref(null)
const currentLocation = ref(null)
const loading = ref(true)
const loadErrorMessage = ref('')
const userStore = useUserStore()
let enterTime = 0
let viewSource = SPOT_DETAIL_SOURCE.DETAIL

// 计算属性
const spotImages = computed(() => {
  if (!spot.value) return []

  const source = []
  if (Array.isArray(spot.value.images)) {
    source.push(...spot.value.images)
  }
  if (spot.value.coverImage) {
    source.unshift(spot.value.coverImage)
  }

  return Array.from(new Set(source.filter(Boolean))).map(img => getContentImageUrl(img))
})

const ratingVisible = ref(false)
const ratingForm = reactive({ score: 5, comment: '' })
const openReviewByQuery = ref(false)
const reviewPopupOpened = ref(false)
const similarSpots = ref([])
const similarUpdateTime = ref('')

const distanceText = computed(() => {
  if (!spot.value || !currentLocation.value) return ''

  const spotLatitude = Number(spot.value.latitude)
  const spotLongitude = Number(spot.value.longitude)
  const userLatitude = Number(currentLocation.value.latitude)
  const userLongitude = Number(currentLocation.value.longitude)

  if (![spotLatitude, spotLongitude, userLatitude, userLongitude].every(Number.isFinite)) {
    return ''
  }

  const distanceKm = calculateDistanceKm(userLatitude, userLongitude, spotLatitude, spotLongitude)
  return formatDistance(distanceKm)
})

const similarUpdateTimeText = computed(() => {
  return similarUpdateTime.value ? `更新于 ${similarUpdateTime.value}` : '为你精选'
})

// 工具方法
const syncSpotPreview = (data) => {
  if (!data?.id) return
  uni.setStorageSync('spot_detail_updated', {
    id: data.id,
    name: data.name,
    coverImage: data.coverImage,
    intro: data.intro || data.description || '',
    regionName: data.regionName,
    categoryName: data.categoryName,
    price: data.price,
    avgRating: data.avgRating,
    isFavorite: data.isFavorite
  })
}

const saveSpotFootprint = (data) => {
  if (!data?.id) return
  const history = uni.getStorageSync('spot_footprints')
  const footprints = Array.isArray(history) ? history : []
  const nextItem = {
    id: data.id,
    name: data.name,
    coverImage: data.coverImage,
    regionName: data.regionName,
    viewedAt: Date.now()
  }
  const nextList = [nextItem, ...footprints.filter(item => item.id !== data.id)].slice(0, 20)
  uni.setStorageSync('spot_footprints', nextList)
}

// 数据加载方法
const fetchSpotDetail = async () => {
  loading.value = true
  loadErrorMessage.value = ''
  try {
    const res = await getSpotDetail(spotId.value)
    spot.value = res.data
    saveSpotFootprint(spot.value)
    syncSpotPreview(spot.value)
    if (spot.value.userRating) {
      ratingForm.score = spot.value.userRating
    }
    if (openReviewByQuery.value && !reviewPopupOpened.value) {
      if (promptLogin('登录后可评价景点，是否现在去登录？')) {
        ratingVisible.value = true
        reviewPopupOpened.value = true
      }
    }
  } catch (e) {
    spot.value = null
    loadErrorMessage.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const fetchSimilarSpots = async () => {
  if (!spotId.value) return
  try {
    const res = await getSimilarSpots(spotId.value, 6)
    similarSpots.value = res.data?.neighbors || []
    similarUpdateTime.value = res.data?.lastUpdateTime || ''
  } catch (e) {
    console.error('获取相似景点失败', e)
    similarSpots.value = []
  }
}

const calculateDistanceKm = (fromLatitude, fromLongitude, toLatitude, toLongitude) => {
  const toRadians = (degree) => degree * Math.PI / 180
  const earthRadiusKm = 6371
  const deltaLatitude = toRadians(toLatitude - fromLatitude)
  const deltaLongitude = toRadians(toLongitude - fromLongitude)
  const a = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
    + Math.cos(toRadians(fromLatitude)) * Math.cos(toRadians(toLatitude))
    * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2)

  return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

const formatDistance = (value) => {
  const distance = Number(value)
  if (!Number.isFinite(distance)) return ''
  return distance < 1 ? `${Math.max(100, Math.round(distance * 1000))} m` : `${distance.toFixed(1)} km`
}

const formatDateTime = (value) => {
  if (!value) return '暂无信息'
  const normalized = String(value).replace('T', ' ')
  return normalized.slice(0, 19)
}

const syncCurrentLocation = async () => {
  const snapshot = await getLocationSnapshot()
  currentLocation.value = snapshot.current
}

// 交互处理方法
const previewImage = (index) => {
  if (!spotImages.value.length) return
  uni.previewImage({ current: index, urls: spotImages.value })
}

const toggleFavorite = async () => {
  if (!promptLogin('登录后可收藏景点，是否现在去登录？')) {
    return
  }

  try {
    if (spot.value.isFavorite) {
      await removeFavorite(spotId.value)
      spot.value.isFavorite = false
      syncSpotPreview(spot.value)
      uni.showToast({ title: '已取消收藏', icon: 'none' })
    } else {
      await addFavorite(spotId.value)
      spot.value.isFavorite = true
      syncSpotPreview(spot.value)
      uni.showToast({ title: '收藏成功', icon: 'none' })
    }
  } catch (e) {
    console.error('收藏操作失败', e)
  }
}

const openNavigation = () => {
  if (!spot.value.latitude || !spot.value.longitude) {
    uni.showToast({ title: '暂无位置信息', icon: 'none' })
    return
  }
  uni.openLocation({
    latitude: Number(spot.value.latitude),
    longitude: Number(spot.value.longitude),
    name: spot.value.name,
    address: spot.value.address
  })
}

const canDeleteComment = (comment) => {
  return userStore.isLoggedIn && comment.userId === userStore.userInfo?.id
}

const showRatingPopup = () => {
  if (!promptLogin('登录后可评价景点，是否现在去登录？')) {
    return
  }
  ratingVisible.value = true
}

const submitRatingHandler = async () => {
  if (!promptLogin('登录后可评价景点，是否现在去登录？')) {
    return
  }

  if (ratingForm.score < 1) {
    uni.showToast({ title: '请选择评分', icon: 'none' })
    return
  }
  try {
    await submitReview({
      spotId: spotId.value,
      score: ratingForm.score,
      comment: ratingForm.comment
    })
    uni.showToast({ title: '评价成功', icon: 'success' })
    ratingVisible.value = false
    fetchSpotDetail()
  } catch (e) {
    uni.showToast({ title: '评价失败', icon: 'none' })
  }
}

const handleDeleteComment = (comment) => {
  if (!promptLogin('登录后可删除评价，是否现在去登录？')) {
    return
  }

  uni.showModal({
    title: '删除评价',
    content: '删除后评分会一并撤销，确认删除吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await deleteReview(comment.id)
        uni.showToast({ title: '评价已删除', icon: 'success' })
        fetchSpotDetail()
      } catch (e) {
        uni.showToast({ title: '删除失败', icon: 'none' })
      }
    }
  })
}

// 页面跳转方法
const goBuy = () => {
  if (!promptLogin('登录后可创建订单，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/order/create?spotId=${spotId.value}` })
}

const goSimilarSpot = (id) => {
  uni.navigateTo({ url: buildSpotDetailUrl(id, SPOT_DETAIL_SOURCE.SIMILAR) })
}

const goSpotList = () => {
  uni.navigateTo({ url: '/pages/spot/list' })
}

const formatSimilarity = (value) => {
  if (typeof value !== 'number') return '0.00'
  return value.toFixed(2)
}

// 生命周期
onLoad((options) => {
  if (!guardLoginPage('登录后可查看景点详情，是否现在去登录？')) {
    loading.value = false
    return
  }

  spotId.value = options.id
  if (!spotId.value) {
    loading.value = false
    loadErrorMessage.value = '景点参数无效'
    return
  }

  viewSource = options.source || SPOT_DETAIL_SOURCE.DETAIL
  openReviewByQuery.value = options.openReview === '1'
  reviewPopupOpened.value = false
  void syncCurrentLocation()
  fetchSpotDetail()
  void fetchSimilarSpots()
})

onShow(() => {
  enterTime = Date.now()
})

const reportView = () => {
  if (spotId.value && userStore.isLoggedIn && enterTime > 0) {
    const duration = Math.floor((Date.now() - enterTime) / 1000)
    recordSpotView(spotId.value, viewSource, duration).catch(() => {})
  }
}

onHide(() => {
  reportView()
  enterTime = 0
})

onUnload(() => {
  reportView()
})
</script>

<style scoped>
.ios-page {
  background: transparent;
  min-height: 100vh;
  padding-bottom: 160rpx;
}

.loading-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24rpx;
  padding: 120rpx 40rpx;
}

.state-text {
  font-size: 28rpx;
  color: #64748b;
  text-align: center;
  line-height: 1.6;
}

.state-btn {
  min-width: 280rpx;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  background: #18181b;
  color: #fff;
  font-size: 28rpx;
  border: none;
}

/* 图片轮播 */
.image-swiper {
  width: 100%;
  height: 540rpx;
}

.swiper-image {
  width: 100%;
  height: 100%;
}

.empty-swiper {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e5e7eb;
}

.empty-swiper-text {
  color: #6b7280;
  font-size: 28rpx;
}

/* 信息卡片 */
.info-card {
  margin: -60rpx 32rpx 24rpx;
  padding: 28rpx;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 40rpx;
  position: relative;
  z-index: 1;
  box-shadow: 0 16rpx 40rpx rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18rpx);
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.spot-name {
  font-size: 40rpx;
  font-weight: 700;
  color: #1C1C1E;
  flex: 1;
}

.favorite-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 24rpx;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
}

.favorite-btn.active {
  background: #fff1f2;
}

.info-meta {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  font-size: 26rpx;
}
.rating-chip {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(255, 247, 237, 0.9);
}

.rating {
  color: #b45309;
  font-weight: 700;
}

.rating-count {
  color: #6b7280;
  margin-left: 8rpx;
}

.divider {
  color: #9ca3af;
  margin: 0 12rpx;
}

.category {
  color: #6b7280;
}

.price-row {
  margin-top: 20rpx;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.price {
  font-size: 48rpx;
  color: #FF3B30;
  font-weight: 700;
}

.price-label {
  font-size: 26rpx;
  color: #6b7280;
}

.price-stats {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20rpx;
}

.price-stat {
  font-size: 24rpx;
  color: #6b7280;
}

/* 详细信息卡片 */
.detail-card {
  margin: 0 32rpx 24rpx;
  padding: 0 28rpx;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 36rpx;
  box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, 0.05);
}

.detail-item {
  display: flex;
  padding: 28rpx 0;
  border-bottom: 1px solid #F2F2F7;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  width: 160rpx;
  font-size: 28rpx;
  color: #6b7280;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  font-size: 28rpx;
  color: #1C1C1E;
}

.detail-value-row {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  min-width: 0;
}

.address-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.address-text {
  flex: 1;
  font-size: 28rpx;
  color: #1C1C1E;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-link {
  color: #2563eb;
  font-size: 28rpx;
  margin-left: 16rpx;
}

.distance-text {
  color: #8a6a2f;
  font-weight: 600;
  flex-shrink: 0;
}

/* 简介卡片 */
.desc-card {
  margin: 0 32rpx 24rpx;
  padding: 28rpx;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 36rpx;
  box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, 0.05);
}

.card-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1C1C1E;
  display: block;
  margin-bottom: 16rpx;
}

.desc-content {
  font-size: 28rpx;
  color: #6b7280;
  line-height: 1.6;
}

.related-card {
  margin: 0 32rpx 24rpx;
  padding: 28rpx;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 36rpx;
  box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, 0.05);
}

.related-scroll {
  white-space: nowrap;
}

.related-item {
  display: inline-block;
  width: 300rpx;
  margin-right: 20rpx;
  background: #f8fafc;
  border-radius: 32rpx;
  overflow: hidden;
}

.related-item:last-child {
  margin-right: 0;
}

.related-image {
  width: 300rpx;
  height: 190rpx;
}

.related-info {
  padding: 16rpx;
}

.related-name {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1C1C1E;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.related-meta {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.related-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12rpx;
  margin-top: 14rpx;
}

.related-price {
  font-size: 28rpx;
  font-weight: 600;
  color: #FF3B30;
}

.related-score {
  font-size: 20rpx;
  color: #8a6a2f;
}

/* 评论卡片 */
.comment-card {
  margin: 0 32rpx 24rpx;
  padding: 28rpx;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 36rpx;
  box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.more-link {
  font-size: 28rpx;
  color: #334155;
}

.comment-item {
  display: flex;
  padding: 20rpx 0;
  border-bottom: 1px solid #F2F2F7;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.comment-name {
  font-size: 28rpx;
  color: #1C1C1E;
  font-weight: 500;
}

.comment-score { display: inline-flex; align-items: center; gap: 6rpx; }
.comment-score-text { font-size: 24rpx; color: #b45309; font-weight: 700; }

.comment-delete {
  font-size: 24rpx;
  color: #FF3B30;
}

.comment-text {
  font-size: 28rpx;
  color: #6b7280;
  margin-top: 12rpx;
  display: block;
  line-height: 1.5;
}

.comment-time {
  font-size: 24rpx;
  color: #9ca3af;
  margin-top: 12rpx;
  display: block;
}

.empty-comment {
  text-align: center;
  padding: 40rpx;
  color: #6b7280;
  font-size: 28rpx;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 20rpx 32rpx;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  box-shadow: 0 -8rpx 24rpx rgba(15, 23, 42, 0.04);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 28rpx;
}

.action-text {
  font-size: 22rpx;
  color: #6b7280;
  margin-top: 8rpx;
}

.action-btn.active .action-text {
  color: #be123c;
}

.buy-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  background: #2563eb;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: 44rpx;
  margin-left: 24rpx;
  border: none;
}

/* 评分弹窗 */
.rating-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.rating-content {
  width: 600rpx;
  background: #fff;
  border-radius: 40rpx;
  padding: 40rpx;
}

.rating-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #1C1C1E;
  text-align: center;
  display: block;
  margin-bottom: 30rpx;
}

.star-row {
  display: flex;
  justify-content: center;
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.star {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20rpx;
  background: rgba(248, 250, 252, 0.92);
}

.star.active {
  background: rgba(255, 247, 237, 0.94);
}

.rating-textarea {
  width: 100%;
  height: 200rpx;
  border: 1px solid #E5E5EA;
  border-radius: 36rpx;
  padding: 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.rating-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 30rpx;
}

.cancel-btn,
.submit-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  border: none;
}

.cancel-btn {
  background: #f4f6fb;
  color: #6b7280;
}

.submit-btn {
  background: #2563eb;
  color: #fff;
}
</style>
