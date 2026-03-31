<template>
  <view class="reviews-page">
    <view class="hero-card">
      <text class="hero-title">真实口碑</text>
      <text class="hero-desc">先看真实游客怎么说，再决定要不要去。</text>
      <view class="hero-stats">
        <view class="hero-stat">
          <text class="hero-stat-value">{{ positiveReviews.length }}</text>
          <text class="hero-stat-label">高分种草</text>
        </view>
        <view class="hero-stat">
          <text class="hero-stat-value">{{ negativeReviews.length }}</text>
          <text class="hero-stat-label">真实避雷</text>
        </view>
      </view>
    </view>

    <view class="tab-bar">
      <view
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: activeTab === tab.value }"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </view>
    </view>

    <view class="feed-note">
      <text class="feed-note-title">当前口碑流说明</text>
      <text class="feed-note-text">当前版本先聚合热门景点的有效评论，页面结构保持信息流形态，后续可直接替换为全站评价接口。</text>
    </view>

    <view class="action-row">
      <view class="action-link" :class="{ disabled: loading }" @click="refreshReviewFeed">{{ loading ? '刷新中...' : '刷新口碑流' }}</view>
    </view>

    <view class="review-card" v-for="item in activeReviews" :key="item.id" @click="goSpotDetail(item.spotId)">
      <view class="review-header">
        <view class="user-box">
          <image class="avatar" :src="getAvatarUrl(item.avatar)" mode="aspectFill" />
          <view class="user-meta">
            <text class="nickname">{{ item.nickname || '匿名用户' }}</text>
            <text class="spot-name">{{ item.spotName || '景点待补充' }}</text>
          </view>
        </view>
        <view class="score-badge" :class="item.score >= 4 ? 'positive' : 'negative'">
          {{ item.score }} 分
        </view>
      </view>

      <text class="comment">{{ item.comment }}</text>

      <view class="review-footer">
        <text class="footer-text">{{ item.createdAt || '最近发布' }}</text>
        <text class="footer-link">查看景点详情</text>
      </view>
    </view>

    <view class="empty-card" v-if="!loading && !activeReviews.length">
      <text class="empty-title">{{ emptyStateTitle }}</text>
      <text class="empty-desc">{{ emptyStateDesc }}</text>
    </view>

    <view class="loading-row" v-if="loading">
      <text>正在整理真实评价...</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchReviewFeedPreview } from '@/services/feature'
import { promptLogin } from '@/utils/auth'
import { getAvatarUrl } from '@/utils/request'

// 页面数据状态
const tabs = [
  { label: '高分种草', value: 'positive' },
  { label: '真实避雷', value: 'negative' }
]
const activeTab = ref('positive')
const positiveReviews = ref([])
const negativeReviews = ref([])
const loading = ref(false)

// 计算属性
const activeReviews = computed(() => (activeTab.value === 'positive' ? positiveReviews.value : negativeReviews.value))
const emptyStateTitle = computed(() => (activeTab.value === 'positive' ? '暂时没有高分种草内容' : '暂时没有真实避雷内容'))
const emptyStateDesc = computed(() => {
  if (activeTab.value === 'positive') {
    return '当前聚合范围内暂时没有高分评论，后续接全站评价流后会更完整。'
  }
  return '当前聚合范围内暂时没有低分评论，后续接全站评价流后会更完整。'
})

// 数据加载方法
const loadReviewFeed = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const reviewFeed = await fetchReviewFeedPreview()
    positiveReviews.value = reviewFeed.positive
    negativeReviews.value = reviewFeed.negative
  } catch (error) {
    console.error('加载真实口碑失败', error)
    uni.showToast({ title: '加载口碑失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const refreshReviewFeed = () => {
  if (loading.value) return
  loadReviewFeed()
}

// 页面跳转方法
const goSpotDetail = (spotId) => {
  if (!spotId) return
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: `/pages/spot/detail?id=${spotId}&source=reviews` })
}

onLoad(() => {
  loadReviewFeed()
})
</script>

<style scoped>
.reviews-page {
  min-height: 100vh;
  padding: 24rpx;
  background: #f4f6fb;
}

.hero-card,
.tab-bar,
.review-card,
.empty-card {
  background: #fff;
  border-radius: 32rpx;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.05);
}

.hero-card {
  padding: 32rpx 28rpx;
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 56%, #eef2ff 100%);
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

.tab-bar {
  display: flex;
  gap: 12rpx;
  margin-top: 24rpx;
  padding: 10rpx;
}

.tab-item {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  text-align: center;
  border-radius: 24rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #6b7280;
}

.tab-item.active {
  background: #2563eb;
  color: #fff;
}

.feed-note {
  margin-top: 22rpx;
  padding: 22rpx 24rpx;
  border-radius: 26rpx;
  background: #fff;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.05);
}

.feed-note-title {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: #1f2937;
}

.feed-note-text {
  display: block;
  margin-top: 10rpx;
  font-size: 23rpx;
  line-height: 1.7;
  color: #64748b;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 18rpx;
}

.action-link {
  padding: 12rpx 20rpx;
  border-radius: 999rpx;
  background: #ffffff;
  color: #2563eb;
  font-size: 24rpx;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.05);
}

.action-link.disabled {
  opacity: 0.6;
}

.review-card {
  margin-top: 22rpx;
  padding: 26rpx;
}

.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-width: 0;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #e5e7eb;
}

.user-meta {
  min-width: 0;
}

.nickname,
.spot-name {
  display: block;
}

.nickname {
  font-size: 28rpx;
  font-weight: 600;
  color: #111827;
}

.spot-name {
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #64748b;
}

.score-badge {
  flex-shrink: 0;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.score-badge.positive {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.score-badge.negative {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.comment {
  display: block;
  margin-top: 20rpx;
  font-size: 27rpx;
  line-height: 1.8;
  color: #334155;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}

.review-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 22rpx;
}

.footer-text {
  font-size: 22rpx;
  color: #94a3b8;
}

.footer-link {
  font-size: 24rpx;
  color: #2563eb;
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
