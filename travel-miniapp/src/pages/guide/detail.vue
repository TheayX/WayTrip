<!-- 攻略详情页 -->
<template>
  <view class="guide-detail-page" v-if="guide">
    <!-- 顶部封面区域 -->
    <image class="guide-cover" :src="getImageUrl(guide.coverImage)" mode="aspectFill" />

    <!-- 攻略信息区域 -->
    <view class="guide-header card">
      <text class="guide-title">{{ guide.title }}</text>
      <view class="guide-meta">
        <text class="guide-category">{{ guide.category }}</text>
        <text class="guide-info">👁 {{ guide.viewCount }} · {{ guide.createdAt }}</text>
      </view>
      <text class="guide-intro">这篇攻略提到的景点会集中展示在下方，方便直接跳转查看。</text>
    </view>

    <!-- 攻略内容区域 -->
    <view class="guide-content card">
      <rich-text :nodes="guide.content"></rich-text>
    </view>

    <!-- 关联景点区域 -->
    <view class="related-spots card">
      <view class="section-header">
        <view>
          <text class="section-title">攻略关联景点</text>
          <text class="section-subtitle">共 {{ guide.relatedSpots?.length || 0 }} 个可直达景点入口</text>
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
            <text class="spot-name">{{ spot.name }}</text>
            <text class="spot-price">{{ spot.price }}</text>
            <text class="spot-link">查看景点详情 ›</text>
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
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getGuideDetail } from '@/api/guide'
import { guardLoginPage } from '@/utils/auth'
import { getImageUrl } from '@/utils/request'

// 页面数据状态
const guide = ref(null)
const guideId = ref(null)

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
    url: `/pages/spot/detail?id=${id}&source=guide`
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
  background: linear-gradient(180deg, #f5f7fa 0%, #eef3f8 100%);
}

.guide-cover {
  width: 100%;
  height: 420rpx;
}

.guide-header {
  margin: -72rpx 24rpx 20rpx;
  position: relative;
  z-index: 1;
}

.guide-title {
  display: block;
  margin-bottom: 16rpx;
  font-size: 40rpx;
  font-weight: 700;
  color: #1f2937;
}

.guide-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.guide-category {
  font-size: 24rpx;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.1);
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
}

.related-spots {
  margin: 0 24rpx;
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
  color: #1f2937;
}

.section-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.section-link {
  font-size: 24rpx;
  color: #2563eb;
}

.spots-scroll {
  white-space: nowrap;
}

.spot-card {
  display: inline-block;
  width: 280rpx;
  margin-right: 20rpx;
  background: #f8fafc;
  border-radius: 18rpx;
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
  color: #2563eb;
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
  background: #eff6ff;
  color: #2563eb;
  font-size: 24rpx;
}
</style>
