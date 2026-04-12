<!-- 游客口碑页面 -->
<template>
  <view class="reviews-page">
    <view class="hero-card">
      <text class="hero-title">游客口碑</text>
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

    <view class="review-card" v-for="item in activeReviews" :key="item.id" @click="goSpotDetail(item.spotId)">
      <view class="review-header">
        <view class="user-box">
          <image class="avatar" :src="getAvatarUrl(item.avatar)" mode="aspectFill" />
          <view class="user-meta">
            <text class="nickname">{{ resolveMiniappUserDisplayName(item.nickname) }}</text>
            <text class="spot-name">{{ resolveMiniappSpotDisplayName(item.spotName, '景点待补充') }}</text>
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
      <text>正在整理游客评价...</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { fetchTravelerReviewFeed } from '@/services/traveler-reviews'
import { promptLogin } from '@/utils/auth'
import { getAvatarUrl } from '@/utils/request'
import { resolveMiniappSpotDisplayName, resolveMiniappUserDisplayName } from '@/utils/resource-display'
import { buildSpotDetailUrl, SPOT_DETAIL_SOURCE } from '@/utils/spot-detail'

// 好评与避雷分栏在页面层拆开，方便切换时直接复用同一套展示结构。
const tabs = [
  { label: '高分种草', value: 'positive' },
  { label: '真实避雷', value: 'negative' }
]
const activeTab = ref('positive')
const positiveReviews = ref([])
const negativeReviews = ref([])
const loading = ref(false)

const activeReviews = computed(() => (activeTab.value === 'positive' ? positiveReviews.value : negativeReviews.value))
const emptyStateTitle = computed(() => (activeTab.value === 'positive' ? '暂时没有高分种草内容' : '暂时没有真实避雷内容'))
const emptyStateDesc = computed(() => activeTab.value === 'positive' ? '当前没有高分评论内容。' : '当前没有低分评论内容。')
// 口碑流会在进入页和返回页时都刷新，尽量减少长时间停留后的内容陈旧感。
const loadTravelerReviewFeed = async () => {
  if (loading.value) return
  loading.value = true

  try {
    const reviewFeed = await fetchTravelerReviewFeed()
    positiveReviews.value = reviewFeed.positive
    negativeReviews.value = reviewFeed.negative
  } catch (error) {
    console.error('加载游客口碑失败', error)
    uni.showToast({ title: '加载口碑失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

// 详情页需要登录后查看，这里统一收口校验可减少卡片层重复逻辑。
const goSpotDetail = (spotId) => {
  if (!spotId) return
  if (!promptLogin('登录后可查看景点详情，是否现在去登录？')) {
    return
  }
  uni.navigateTo({ url: buildSpotDetailUrl(spotId, SPOT_DETAIL_SOURCE.TRAVELER_REVIEWS) })
}

onLoad(() => {
  loadTravelerReviewFeed()
})

onShow(() => {
  loadTravelerReviewFeed()
})
</script>

<style scoped>
.reviews-page { min-height: 100vh; padding: 24rpx; background: radial-gradient(circle at top, rgba(255, 255, 255, 0.94), rgba(245, 247, 250, 0.9) 48%, rgba(238, 242, 247, 1) 100%), linear-gradient(180deg, #fafafa 0%, #eef2f7 100%); }
.hero-card, .tab-bar, .review-card, .empty-card { background: rgba(255, 255, 255, 0.78); border: 1rpx solid rgba(255, 255, 255, 0.84); border-radius: 32rpx; box-shadow: 0 18rpx 48rpx rgba(15, 23, 42, 0.08), inset 0 1rpx 0 rgba(255, 255, 255, 0.82); }
.hero-card { padding: 32rpx 28rpx; background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.58) 100%); }
.hero-stats { display: flex; gap: 18rpx; margin-top: 22rpx; }
.hero-stat { flex: 1; padding: 18rpx 20rpx; border-radius: 24rpx; background: rgba(255, 255, 255, 0.72); }
.hero-stat-value { display: block; font-size: 34rpx; font-weight: 700; color: #111827; }
.hero-stat-label { display: block; margin-top: 8rpx; font-size: 22rpx; color: #64748b; }
.hero-title { display: block; font-size: 40rpx; font-weight: 700; color: #111827; }
.hero-desc { display: block; margin-top: 14rpx; font-size: 26rpx; line-height: 1.7; color: #64748b; }
.tab-bar { display: flex; gap: 12rpx; margin-top: 24rpx; padding: 10rpx; }
.tab-item { flex: 1; height: 76rpx; line-height: 76rpx; text-align: center; border-radius: 24rpx; font-size: 28rpx; font-weight: 600; color: #6b7280; }
.tab-item.active { background: #18181b; color: #fff; }
.review-card { margin-top: 22rpx; padding: 26rpx; }
.review-header { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.user-box { display: flex; align-items: center; gap: 16rpx; min-width: 0; }
.avatar { width: 72rpx; height: 72rpx; border-radius: 50%; background: #e5e7eb; }
.user-meta { min-width: 0; }
.nickname, .spot-name { display: block; }
.nickname { font-size: 28rpx; font-weight: 600; color: #111827; }
.spot-name { margin-top: 8rpx; font-size: 23rpx; color: #64748b; }
.score-badge { flex-shrink: 0; padding: 10rpx 18rpx; border-radius: 999rpx; font-size: 22rpx; font-weight: 700; }
.score-badge.positive { background: rgba(34, 197, 94, 0.12); color: #15803d; }
.score-badge.negative { background: rgba(239, 68, 68, 0.12); color: #b91c1c; }
.comment { display: block; margin-top: 20rpx; font-size: 27rpx; line-height: 1.8; color: #334155; display: -webkit-box; overflow: hidden; -webkit-line-clamp: 4; -webkit-box-orient: vertical; }
.review-footer { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; margin-top: 22rpx; }
.footer-text { font-size: 22rpx; color: #94a3b8; }
.footer-link { font-size: 24rpx; color: #18181b; }
.empty-card { margin-top: 22rpx; padding: 36rpx 28rpx; }
.empty-title { display: block; font-size: 28rpx; font-weight: 700; color: #1f2937; }
.empty-desc { display: block; margin-top: 14rpx; font-size: 25rpx; line-height: 1.7; color: #64748b; }
.loading-row { text-align: center; padding: 22rpx 0; font-size: 24rpx; color: #94a3b8; }
</style>
