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
        <view class="favorite-btn" @click="toggleFavorite">
          <text class="fav-icon">{{ spot.isFavorite ? '❤️' : '🤍' }}</text>
        </view>
      </view>
      <view class="info-meta">
        <text class="rating">★ {{ spot.avgRating }}</text>
        <text class="rating-count">({{ spot.ratingCount }}条评价)</text>
        <text class="divider">·</text>
        <text class="category">{{ spot.regionName }} · {{ spot.categoryName }}</text>
      </view>
      <view class="price-row">
        <text class="price">¥{{ spot.price }}</text>
        <text class="price-label">/人</text>
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
          <text class="detail-value address-text">{{ spot.address }}</text>
          <text class="nav-link">导航 ›</text>
        </view>
      </view>
    </view>

    <!-- 景点简介 -->
    <view class="desc-card">
      <text class="card-title">景点简介</text>
      <text class="desc-content">{{ spot.description || '暂无简介' }}</text>
    </view>

    <!-- 最新评论 -->
    <view class="comment-card">
      <view class="card-header">
        <text class="card-title">最新评论</text>
        <text class="more-link" @click="goComments">查看全部 ›</text>
      </view>
      <view class="comment-list" v-if="spot.latestComments?.length">
        <view class="comment-item" v-for="comment in spot.latestComments" :key="comment.id">
          <image class="comment-avatar" :src="comment.avatar || '/static/默认头像.png'" />
          <view class="comment-content">
            <view class="comment-header">
              <text class="comment-name">{{ comment.nickname }}</text>
              <text class="comment-score">★ {{ comment.score }}</text>
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
        <text class="action-icon">✍️</text>
        <text class="action-text">评价</text>
      </view>
      <view class="action-btn" @click="toggleFavorite">
        <text class="action-icon">{{ spot.isFavorite ? '❤️' : '🤍' }}</text>
        <text class="action-text">收藏</text>
      </view>
      <button class="buy-btn" @click="goBuy">立即购票</button>
    </view>

    <!-- 评分弹窗 -->
    <view class="rating-popup" v-if="ratingVisible" @click.self="ratingVisible = false">
      <view class="rating-content">
        <text class="rating-title">评价景点</text>
        <view class="star-row">
          <text 
            v-for="i in 5" 
            :key="i" 
            class="star" 
            :class="{ active: i <= ratingForm.score }"
            @click="ratingForm.score = i"
          >★</text>
        </view>
        <textarea 
          class="rating-textarea" 
          v-model="ratingForm.comment" 
          placeholder="分享你的游玩体验..."
          maxlength="500"
        />
        <view class="rating-actions">
          <button class="cancel-btn" @click="ratingVisible = false">取消</button>
          <button class="submit-btn" @click="submitRatingHandler">提交</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getSpotDetail } from '@/api/spot'
import { addFavorite, removeFavorite } from '@/api/favorite'
import { submitRating } from '@/api/rating'
import { getImageUrl } from '@/utils/request'

const spot = ref(null)
const spotId = ref(null)

const spotImages = computed(() => {
  if (!spot.value) return []

  const source = []
  if (Array.isArray(spot.value.images)) {
    source.push(...spot.value.images)
  }
  if (spot.value.coverImage) {
    source.unshift(spot.value.coverImage)
  }

  return Array.from(new Set(source.filter(Boolean))).map(img => getImageUrl(img))
})

const ratingVisible = ref(false)
const ratingForm = reactive({ score: 5, comment: '' })

const fetchSpotDetail = async () => {
  try {
    const res = await getSpotDetail(spotId.value)
    spot.value = res.data
    if (spot.value.userRating) {
      ratingForm.score = spot.value.userRating
    }
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

const previewImage = (index) => {
  if (!spotImages.value.length) return
  uni.previewImage({ current: index, urls: spotImages.value })
}

const toggleFavorite = async () => {
  try {
    if (spot.value.isFavorite) {
      await removeFavorite(spotId.value)
      spot.value.isFavorite = false
      uni.showToast({ title: '已取消收藏', icon: 'none' })
    } else {
      await addFavorite(spotId.value)
      spot.value.isFavorite = true
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

const goComments = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}

const showRatingPopup = () => {
  ratingVisible.value = true
}

const submitRatingHandler = async () => {
  if (ratingForm.score < 1) {
    uni.showToast({ title: '请选择评分', icon: 'none' })
    return
  }
  try {
    await submitRating({
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

const goBuy = () => {
  uni.navigateTo({ url: `/pages/order/create?spotId=${spotId.value}` })
}

onLoad((options) => {
  spotId.value = options.id
  fetchSpotDetail()
})
</script>

<style scoped>
.ios-page {
  background: #F2F2F7;
  min-height: 100vh;
  padding-bottom: 160rpx;
}

/* 图片轮播 */
.image-swiper {
  width: 100%;
  height: 500rpx;
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
  background: #fff;
  border-radius: 28rpx;
  position: relative;
  z-index: 1;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);
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
  padding: 10rpx;
}

.fav-icon {
  font-size: 48rpx;
}

.info-meta {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  font-size: 26rpx;
}

.rating {
  color: #FF9500;
  font-weight: 600;
}

.rating-count {
  color: #8E8E93;
  margin-left: 8rpx;
}

.divider {
  color: #C7C7CC;
  margin: 0 12rpx;
}

.category {
  color: #8E8E93;
}

.price-row {
  margin-top: 20rpx;
}

.price {
  font-size: 48rpx;
  color: #FF3B30;
  font-weight: 700;
}

.price-label {
  font-size: 26rpx;
  color: #8E8E93;
}

/* 详细信息卡片 */
.detail-card {
  margin: 0 32rpx 24rpx;
  padding: 0 28rpx;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
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
  color: #8E8E93;
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
}

.address-text {
  flex: 1;
  font-size: 28rpx;
  color: #1C1C1E;
}

.nav-link {
  color: #007AFF;
  font-size: 28rpx;
  margin-left: 16rpx;
}

/* 简介卡片 */
.desc-card {
  margin: 0 32rpx 24rpx;
  padding: 28rpx;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
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
  color: #8E8E93;
  line-height: 1.6;
}

/* 评论卡片 */
.comment-card {
  margin: 0 32rpx 24rpx;
  padding: 28rpx;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.more-link {
  font-size: 28rpx;
  color: #007AFF;
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

.comment-name {
  font-size: 28rpx;
  color: #1C1C1E;
  font-weight: 500;
}

.comment-score {
  font-size: 24rpx;
  color: #FF9500;
  font-weight: 600;
}

.comment-text {
  font-size: 28rpx;
  color: #8E8E93;
  margin-top: 12rpx;
  display: block;
  line-height: 1.5;
}

.comment-time {
  font-size: 24rpx;
  color: #C7C7CC;
  margin-top: 12rpx;
  display: block;
}

.empty-comment {
  text-align: center;
  padding: 40rpx;
  color: #8E8E93;
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
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  box-shadow: 0 -1rpx 0 rgba(0, 0, 0, 0.05);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 28rpx;
}

.action-icon {
  font-size: 40rpx;
}

.action-text {
  font-size: 22rpx;
  color: #8E8E93;
  margin-top: 4rpx;
}

.buy-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  background: #007AFF;
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
  border-radius: 28rpx;
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
  font-size: 60rpx;
  color: #E5E5EA;
}

.star.active {
  color: #FF9500;
}

.rating-textarea {
  width: 100%;
  height: 200rpx;
  border: 1px solid #E5E5EA;
  border-radius: 16rpx;
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
  background: #F2F2F7;
  color: #8E8E93;
}

.submit-btn {
  background: #007AFF;
  color: #fff;
}
</style>
