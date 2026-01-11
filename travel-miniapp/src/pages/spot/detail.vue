<template>
  <view class="spot-detail-page" v-if="spot">
    <!-- 图片轮播 -->
    <swiper class="image-swiper" indicator-dots circular>
      <swiper-item v-for="(img, index) in spotImages" :key="index">
        <image class="swiper-image" :src="img" mode="aspectFill" @click="previewImage(index)" />
      </swiper-item>
    </swiper>

    <!-- 基本信息 -->
    <view class="info-card card">
      <view class="info-header">
        <text class="spot-name">{{ spot.name }}</text>
        <view class="favorite-btn" @click="toggleFavorite">
          <text>{{ spot.isFavorite ? '❤️' : '🤍' }}</text>
        </view>
      </view>
      <view class="info-meta">
        <text class="rating">⭐ {{ spot.avgRating }} ({{ spot.ratingCount }}条评价)</text>
        <text class="category">{{ spot.regionName }} · {{ spot.categoryName }}</text>
      </view>
      <view class="price-row">
        <text class="price">¥{{ spot.price }}</text>
        <text class="price-label">/人</text>
      </view>
    </view>

    <!-- 详细信息 -->
    <view class="detail-card card">
      <view class="detail-item">
        <text class="detail-label">开放时间</text>
        <text class="detail-value">{{ spot.openTime || '暂无信息' }}</text>
      </view>
      <view class="detail-item" @click="openNavigation">
        <text class="detail-label">景点地址</text>
        <view class="detail-value address">
          <text>{{ spot.address }}</text>
          <text class="nav-icon">📍导航</text>
        </view>
      </view>
    </view>

    <!-- 景点简介 -->
    <view class="desc-card card">
      <text class="card-title">景点简介</text>
      <text class="desc-content">{{ spot.description || '暂无简介' }}</text>
    </view>

    <!-- 最新评论 -->
    <view class="comment-card card">
      <view class="card-header">
        <text class="card-title">最新评论</text>
        <text class="more-link" @click="goComments">查看全部</text>
      </view>
      <view class="comment-list" v-if="spot.latestComments?.length">
        <view class="comment-item" v-for="comment in spot.latestComments" :key="comment.id">
          <image class="comment-avatar" :src="comment.avatar || '/static/default-avatar.png'" />
          <view class="comment-content">
            <view class="comment-header">
              <text class="comment-name">{{ comment.nickname }}</text>
              <text class="comment-score">⭐ {{ comment.score }}</text>
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

// 景点数据
const spot = ref(null)
const spotId = ref(null)

// 处理后的图片列表
const spotImages = computed(() => {
  if (!spot.value?.images) return []
  return spot.value.images.map(img => getImageUrl(img))
})

// 评分弹窗
const ratingVisible = ref(false)
const ratingForm = reactive({
  score: 5,
  comment: ''
})

// 获取景点详情
const fetchSpotDetail = async () => {
  try {
    const res = await getSpotDetail(spotId.value)
    spot.value = res.data
    // 如果用户已评分，设置默认值
    if (spot.value.userRating) {
      ratingForm.score = spot.value.userRating
    }
  } catch (e) {
    console.error('获取景点详情失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

// 预览图片
const previewImage = (index) => {
  uni.previewImage({
    current: index,
    urls: spotImages.value
  })
}

// 切换收藏
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

// 打开导航
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

// 跳转评论列表
const goComments = () => {
  uni.showToast({ title: '功能开发中', icon: 'none' })
}

// 显示评分弹窗
const showRatingPopup = () => {
  ratingVisible.value = true
}

// 提交评分
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
    // 刷新详情
    fetchSpotDetail()
  } catch (e) {
    console.error('评价失败', e)
    uni.showToast({ title: '评价失败', icon: 'none' })
  }
}

// 跳转购票
const goBuy = () => {
  uni.navigateTo({
    url: `/pages/order/create?spotId=${spotId.value}`
  })
}

// 页面加载
onLoad((options) => {
  spotId.value = options.id
  fetchSpotDetail()
})
</script>

<style scoped>
.spot-detail-page {
  padding-bottom: 140rpx;
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

/* 信息卡片 */
.info-card {
  margin: -40rpx 20rpx 20rpx;
  position: relative;
  z-index: 1;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.spot-name {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  flex: 1;
}

.favorite-btn {
  font-size: 48rpx;
  padding: 10rpx;
}

.info-meta {
  display: flex;
  gap: 20rpx;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #666;
}

.rating {
  color: #ff9500;
}

.price-row {
  margin-top: 20rpx;
}

.price {
  font-size: 48rpx;
  color: #ff6b6b;
  font-weight: bold;
}

.price-label {
  font-size: 26rpx;
  color: #999;
}

/* 详细信息 */
.detail-card {
  margin: 0 20rpx 20rpx;
}

.detail-item {
  display: flex;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  width: 160rpx;
  font-size: 28rpx;
  color: #999;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.detail-value.address {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-icon {
  color: #409EFF;
  font-size: 26rpx;
}

/* 简介 */
.desc-card {
  margin: 0 20rpx 20rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 16rpx;
}

.desc-content {
  font-size: 28rpx;
  color: #666;
  line-height: 1.6;
}

/* 评论 */
.comment-card {
  margin: 0 20rpx 20rpx;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.more-link {
  font-size: 26rpx;
  color: #409EFF;
}

.comment-item {
  display: flex;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
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
  color: #333;
}

.comment-score {
  font-size: 24rpx;
  color: #ff9500;
}

.comment-text {
  font-size: 28rpx;
  color: #666;
  margin-top: 12rpx;
  display: block;
}

.comment-time {
  font-size: 24rpx;
  color: #999;
  margin-top: 12rpx;
  display: block;
}

.empty-comment {
  text-align: center;
  padding: 40rpx;
  color: #999;
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
  padding: 20rpx 30rpx;
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.05);
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 30rpx;
}

.action-icon {
  font-size: 40rpx;
}

.action-text {
  font-size: 22rpx;
  color: #666;
  margin-top: 4rpx;
}

.buy-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  background: #ff6b6b;
  color: #fff;
  font-size: 32rpx;
  border-radius: 40rpx;
  margin-left: 30rpx;
}

/* 评分弹窗 */
.rating-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.rating-content {
  width: 600rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
}

.rating-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
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
  color: #ddd;
}

.star.active {
  color: #ff9500;
}

.rating-textarea {
  width: 100%;
  height: 200rpx;
  border: 1rpx solid #eee;
  border-radius: 12rpx;
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
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.submit-btn {
  background: #409EFF;
  color: #fff;
}
</style>
