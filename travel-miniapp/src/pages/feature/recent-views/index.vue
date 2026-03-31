<template>
  <view class="recent-page">
    <view class="hero-card">
      <text class="hero-title">近期热看</text>
      <text class="hero-desc">看看最近一段时间里，大家浏览更多的景点。</text>
      <view class="hero-stats">
        <view class="hero-stat">
          <text class="hero-stat-value">{{ recentViewedSpots.length }}</text>
          <text class="hero-stat-label">热门景点</text>
        </view>
        <view class="hero-stat">
          <text class="hero-stat-value">{{ recentViewDays }}</text>
          <text class="hero-stat-label">统计天数</text>
        </view>
      </view>
    </view>

    <view class="spot-card" v-for="item in recentViewedSpots" :key="item.id" @click="goSpotDetail(item.id)">
      <image class="spot-image" :src="getImageUrl(item.coverImage)" mode="aspectFill" />
      <view class="spot-content">
        <view class="spot-header">
          <text class="spot-title">{{ item.name }}</text>
          <text class="spot-price">{{ formatRecentViewPrice(item.price) }}</text>
        </view>
        <view class="spot-meta">
          <text class="meta-tag">{{ item.categoryName || '景点' }}</text>
          <text class="meta-tag">{{ item.viewCount || 0 }} 次浏览</text>
          <text class="meta-rating">{{ formatRecentViewRating(item.avgRating) }}</text>
        </view>
      </view>
    </view>

    <view class="empty-card" v-if="!loading && !recentViewedSpots.length">
      <text class="empty-title">最近还没有可展示的浏览热点</text>
      <text class="empty-desc">等有更多浏览记录后，这里会自动更新。</text>
    </view>

    <view class="loading-row" v-if="loading">
      <text>正在整理近期热看的景点...</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchRecentViewedSpots } from '@/services/feature'
import { promptLogin } from '@/utils/auth'
import { formatFeaturePrice, formatFeatureRating } from '@/utils/feature-display'
import { getImageUrl } from '@/utils/request'

const recentViewedSpots = ref([])
const recentViewDays = ref(14)
const loading = ref(false)

const formatRecentViewPrice = (value) => formatFeaturePrice(value, { freeText: '¥0 免费' })
const formatRecentViewRating = (value) => formatFeatureRating(value)

const loadRecentViewedSpots = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const result = await fetchRecentViewedSpots()
    recentViewedSpots.value = result.list
    recentViewDays.value = result.days
  } catch (error) {
    console.error('加载最近都在看失败', error)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const goSpotDetail = (id) => {
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/spot/detail?id=${id}&source=recent-views` })
}

onLoad(() => {
  loadRecentViewedSpots()
})
</script>

<style scoped>
.recent-page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f4f6fb;
}

.hero-card,
.spot-card,
.empty-card {
  background: #fff;
  border-radius: 32rpx;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.05);
}

.hero-card {
  padding: 32rpx 28rpx;
  background: linear-gradient(135deg, #fffbeb 0%, #ffffff 56%, #fff7ed 100%);
}

.hero-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #111827;
}

.hero-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #64748b;
}

.hero-stats {
  display: flex;
  gap: 18rpx;
  margin-top: 22rpx;
}

.hero-stat {
  flex: 1;
  padding: 18rpx 20rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.72);
}

.hero-stat-value {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #111827;
}

.hero-stat-label {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #64748b;
}

.spot-card {
  overflow: hidden;
  margin-top: 22rpx;
}

.spot-image {
  width: 100%;
  height: 280rpx;
}

.spot-content {
  padding: 24rpx;
}

.spot-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.spot-title {
  flex: 1;
  font-size: 32rpx;
  font-weight: 700;
  color: #111827;
}

.spot-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #d97706;
}

.spot-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 18rpx;
}

.meta-tag,
.meta-rating {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #f8fafc;
  font-size: 22rpx;
  color: #475569;
}

.empty-card {
  margin-top: 22rpx;
  padding: 36rpx 28rpx;
}

.empty-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #1f2937;
}

.empty-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 25rpx;
  line-height: 1.7;
  color: #64748b;
}

.loading-row {
  text-align: center;
  padding: 22rpx 0;
  font-size: 24rpx;
  color: #94a3b8;
}
</style>
