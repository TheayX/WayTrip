<template>
  <view class="guide-detail-page" v-if="guide">
    <!-- 封面图 -->
    <image class="guide-cover" :src="getImageUrl(guide.coverImage)" mode="aspectFill" />

    <!-- 标题信息 -->
    <view class="guide-header card">
      <text class="guide-title">{{ guide.title }}</text>
      <view class="guide-meta">
        <text class="guide-category">{{ guide.category }}</text>
        <text class="guide-info">👁 {{ guide.viewCount }} · {{ guide.createdAt }}</text>
      </view>
    </view>

    <!-- 攻略内容 -->
    <view class="guide-content card">
      <rich-text :nodes="guide.content"></rich-text>
    </view>

    <!-- 关联景点 -->
    <view class="related-spots card" v-if="guide.relatedSpots?.length">
      <text class="section-title">相关景点</text>
      <scroll-view class="spots-scroll" scroll-x>
        <view 
          class="spot-card" 
          v-for="spot in guide.relatedSpots" 
          :key="spot.id"
          @click="goSpotDetail(spot.id)"
        >
          <image class="spot-image" :src="getImageUrl(spot.coverImage)" mode="aspectFill" />
          <view class="spot-info">
            <text class="spot-name">{{ spot.name }}</text>
            <text class="spot-price price">{{ spot.price }}</text>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getGuideDetail } from '@/api/guide'
import { getImageUrl } from '@/utils/request'

// 攻略数据
const guide = ref(null)
const guideId = ref(null)

// 获取攻略详情
const fetchGuideDetail = async () => {
  try {
    const res = await getGuideDetail(guideId.value)
    guide.value = res.data
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


// 跳转景点详情
const goSpotDetail = (id) => {
  uni.navigateTo({
    url: `/pages/spot/detail?id=${id}&source=guide`
  })
}

// 页面加载
onLoad((options) => {
  guideId.value = options.id
  fetchGuideDetail()
})
</script>

<style scoped>
.guide-detail-page {
  padding-bottom: 40rpx;
}

/* 封面图 */
.guide-cover {
  width: 100%;
  height: 400rpx;
}

/* 标题信息 */
.guide-header {
  margin: -60rpx 20rpx 20rpx;
  position: relative;
  z-index: 1;
}

.guide-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 16rpx;
}

.guide-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.guide-category {
  font-size: 24rpx;
  color: #409EFF;
  background: rgba(64, 158, 255, 0.1);
  padding: 6rpx 16rpx;
  border-radius: 4rpx;
}

.guide-info {
  font-size: 24rpx;
  color: #999;
}

/* 攻略内容 */
.guide-content {
  margin: 0 20rpx 20rpx;
  font-size: 28rpx;
  line-height: 1.8;
  color: #333;
}

/* 关联景点 */
.related-spots {
  margin: 0 20rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 20rpx;
}

.spots-scroll {
  white-space: nowrap;
}

.spot-card {
  display: inline-block;
  width: 280rpx;
  margin-right: 20rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
  overflow: hidden;
}

.spot-card:last-child {
  margin-right: 0;
}

.spot-image {
  width: 280rpx;
  height: 180rpx;
}

.spot-info {
  padding: 16rpx;
}

.spot-name {
  font-size: 26rpx;
  color: #333;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spot-price {
  font-size: 28rpx;
  margin-top: 8rpx;
  display: block;
}
</style>
