<!-- 攻略详情页 -->
<template>
  <view class="guide-detail-page" v-if="guide">
    <!-- 顶部封面区域 -->
    <image class="guide-cover" :src="getImageUrl(guide.coverImage)" mode="aspectFill" />

    <!-- 攻略信息区域 -->
    <view class="guide-header card">
      <text class="guide-kicker">旅行攻略</text>
      <text class="guide-title">{{ resolveGuideText(guide.title) }}</text>
      <view class="guide-meta">
        <text class="guide-category">{{ resolveGuideCategory(guide.category) }}</text>
        <text class="guide-info">浏览 {{ guide.viewCount || 0 }} · {{ guide.createdAt || '--' }}</text>
      </view>
      <text class="guide-intro">把路线、玩法和景点信息整理成一篇更适合随时翻看的出行笔记。</text>
    </view>

    <!-- 攻略内容区域 -->
    <view class="guide-content card">
      <rich-text v-if="hasGuideHtmlContent" :nodes="guide.content"></rich-text>
      <text v-else class="guide-content-text">{{ resolveGuideText(guide.content) }}</text>
    </view>

    <!-- 关联景点区域 -->
    <view class="related-spots card">
      <view class="section-header">
        <view>
          <text class="section-title">攻略关联景点</text>
          <text class="section-subtitle">共 {{ guide.relatedSpots?.length || 0 }} 个可直接查看的景点</text>
        </view>
        <text class="section-link" @click="goSpotList">更多景点</text>
      </view>

      <scroll-view class="spots-scroll" scroll-x v-if="guide.relatedSpots?.length" :show-scrollbar="false">
        <view
          class="spot-card"
          v-for="spot in guide.relatedSpots"
          :key="spot.id"
          @click="goSpotDetail(spot.id)"
        >
          <image class="spot-image" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
          <view class="spot-info">
            <text class="spot-name">{{ resolveGuideText(spot.name) }}</text>
            <text class="spot-price">{{ spot.price || '--' }}</text>
            <text class="spot-link">查看详情</text>
          </view>
        </view>
      </scroll-view>

      <view class="empty-related" v-else>
        <text class="empty-text">这篇攻略暂时没有配置关联景点</text>
        <view class="empty-btn" @click="goSpotList">去景点列表看看</view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getGuideDetail } from '@/api/guide'
import { guardLoginPage } from '@/utils/auth'
import { getImageUrl } from '@/utils/request'
import { buildSpotDetailUrl, SPOT_DETAIL_SOURCE } from '@/utils/spot-detail'

// 页面数据状态
const guide = ref(null)
const guideId = ref(null)
const resolveGuideText = (value) => value || '--'
const resolveGuideCategory = (value) => value || '攻略'
const hasGuideHtmlContent = computed(() => /<[^>]+>/.test(guide.value?.content || ''))

// 工具方法
const syncGuidePreview = (data) => {
  if (!data?.id) return
  uni.setStorageSync('guide_detail_updated', {
    id: data.id,
    title: data.title,
    coverImage: data.coverImage,
    summary: data.summary || '',
    category: data.category,
    viewCount: data.viewCount,
    createdAt: data.createdAt
  })
}

// 数据加载方法
const fetchGuideDetail = async () => {
  try {
    const res = await getGuideDetail(guideId.value)
    guide.value = res.data
    syncGuidePreview(guide.value)
    if (guide.value?.id && typeof guide.value.viewCount === 'number') {
      uni.setStorageSync('guide_view_updated', {
        id: guide.value.id,
        viewCount: guide.value.viewCount
      })
    }
  } catch (e) {
    console.error('获取攻略详情失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

// 页面跳转方法
const goSpotDetail = (id) => {
  uni.navigateTo({
    url: buildSpotDetailUrl(id, SPOT_DETAIL_SOURCE.GUIDE)
  })
}

const goSpotList = () => {
  uni.navigateTo({ url: '/pages/spot/list?sortBy=heat' })
}

// 生命周期
onLoad((options) => {
  if (!guardLoginPage('登录后可查看攻略详情，是否现在去登录？')) {
    return
  }

  guideId.value = options.id
  fetchGuideDetail()
})
</script>

<style scoped>
.guide-detail-page {
  min-height: 100vh;
  padding-bottom: 40rpx;
  background: transparent;
}

.guide-cover {
  width: 100%;
  height: 460rpx;
}

.guide-header {
  margin: -72rpx 24rpx 20rpx;
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16rpx 40rpx rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18rpx);
}

.guide-kicker {
  display: block;
  margin-bottom: 12rpx;
  font-size: 22rpx;
  font-weight: 700;
  letter-spacing: 6rpx;
  color: #64748b;
  text-transform: uppercase;
}

.guide-title {
  display: block;
  margin-bottom: 16rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.guide-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.guide-category {
  font-size: 24rpx;
  color: #8a6a2f;
  background: #fffdf7;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
}

.guide-info {
  font-size: 24rpx;
  color: #94a3b8;
}

.guide-intro {
  display: block;
  margin-top: 18rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: #64748b;
}

.guide-content {
  margin: 0 24rpx 20rpx;
  font-size: 28rpx;
  line-height: 1.8;
  color: #334155;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, 0.05);
}

.guide-content-text {
  display: block;
  white-space: pre-line;
}

.related-spots {
  margin: 0 24rpx;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, 0.05);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.section-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.section-link {
  font-size: 24rpx;
  color: #334155;
}

.spots-scroll {
  white-space: nowrap;
}

.spot-card {
  display: inline-block;
  width: 280rpx;
  margin-right: 20rpx;
  background: #f8fafc;
  border-radius: 28rpx;
  overflow: hidden;
}

.spot-card:last-child {
  margin-right: 0;
}

.spot-image {
  width: 280rpx;
  height: 190rpx;
}

.spot-info {
  padding: 18rpx;
}

.spot-name {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1f2937;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.spot-price {
  display: block;
  margin-top: 10rpx;
  font-size: 28rpx;
  color: #ef4444;
}

.spot-link {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #334155;
}

.empty-related {
  padding: 32rpx 0 12rpx;
  text-align: center;
}

.empty-text {
  display: block;
  font-size: 26rpx;
  color: #94a3b8;
}

.empty-btn {
  display: inline-block;
  margin-top: 20rpx;
  padding: 14rpx 28rpx;
  border-radius: 999rpx;
  background: #f8fafc;
  color: #334155;
  font-size: 24rpx;
}
</style>
