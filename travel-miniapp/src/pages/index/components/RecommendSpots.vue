<template>
  <view class="section">
    <!-- Unlogged State Clean Banner -->
    <view v-if="!isLoggedIn" class="login-prompt-bar" @click="$emit('goLogin')">
      <view class="prompt-content">
        <uni-icons type="person-filled" size="18" color="#2563eb" />
        <text class="prompt-text">一键登录，获取精准的个性化推荐</text>
      </view>
      <uni-icons type="right" size="16" color="#3b82f6" />
    </view>

    <view class="section-header">
      <text class="section-title">{{ title }}</text>
      <view v-if="isLoggedIn" class="section-actions">
        <text class="section-link" @click="$emit('refresh')">换一批</text>
        <text class="section-link" @click="$emit('more')">查看更多</text>
      </view>
    </view>

    <!-- Recommend List -->
    <view class="recommend-list" v-if="isLoggedIn && spots.length">
      <view class="recommend-card" v-for="spot in spots" :key="spot.id" @click="$emit('click', spot)">
        <image class="rec-img" :src="getContentImageUrl(spot.coverImage)" mode="aspectFill" />
        <view class="rec-content">
          <view class="rec-header">
            <text class="rec-name">{{ spot.name }}</text>
            <view class="rec-rating"><uni-icons type="star-filled" size="14" color="#f59e0b"/> {{ spot.avgRating || '4.5' }}</view>
          </view>
          <text v-if="spot.intro" class="rec-desc">{{ spot.intro }}</text>
          <view class="rec-footer">
            <text class="rec-tag">{{ spot.categoryName || '景点' }}</text>
            <text class="rec-price">￥{{ spot.price }}</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- Empty State -->
    <view class="empty-tip" v-else-if="isLoggedIn && !spots.length">
      <uni-icons type="info" size="32" color="#cbd5e1" />
      <text class="empty-text">暂时没有推荐结果</text>
    </view>
  </view>
</template>

<script setup>
import { getContentImageUrl } from '@/utils/client'

defineProps({
  isLoggedIn: { type: Boolean, default: false },
  title: { type: String, default: '个性推荐' },
  spots: { type: Array, default: () => [] }
})
defineEmits(['goLogin', 'refresh', 'more', 'click'])
</script>

<style scoped>
.section { padding: 12rpx 32rpx; margin-bottom: 16rpx;}
.login-prompt-bar {
  display: flex; align-items: center; justify-content: space-between;
  background: linear-gradient(90deg, #eff6ff 0%, #e0f2fe 100%);
  padding: 24rpx 32rpx; border-radius: 99rpx; margin-bottom: 32rpx;
  box-shadow: 0 4rpx 12rpx rgba(59, 130, 246, 0.08);
}
.prompt-content { display: flex; align-items: center; gap: 14rpx; }
.prompt-text { font-size: 26rpx; color: #1d4ed8; font-weight: 600; }

.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx; }
.section-title { font-size: 38rpx; font-weight: 700; color: #111827; }
.section-actions { display: flex; gap: 32rpx; }
.section-link { font-size: 26rpx; color: #6b7280; font-weight: 500; }

.recommend-list { display: flex; flex-direction: column; gap: 24rpx; }
.recommend-card {
  background: #ffffff; border-radius: 36rpx; overflow: hidden;
  box-shadow: 0 12rpx 32rpx rgba(17, 24, 39, 0.04);
}
.rec-img { width: 100%; height: 280rpx; }
.rec-content { padding: 24rpx 28rpx; }
.rec-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; gap: 16rpx; }
.rec-name { font-size: 34rpx; font-weight: 700; color: #111827; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rec-rating { display: flex; align-items: center; gap: 6rpx; font-size: 26rpx; font-weight: 700; color: #f59e0b; }
.rec-desc { font-size: 26rpx; color: #6b7280; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; margin-bottom: 20rpx; line-height: 1.5; }
.rec-footer { display: flex; justify-content: space-between; align-items: center; }
.rec-tag { padding: 8rpx 20rpx; background: #f3f4f6; color: #4b5563; border-radius: 12rpx; font-size: 22rpx; font-weight: 500; }
.rec-price { font-size: 34rpx; font-weight: 800; color: #ef4444; }

.empty-tip { display: flex; flex-direction: column; align-items: center; padding: 64rpx 0; gap: 16rpx; }
.empty-text { font-size: 28rpx; color: #94a3b8; }
</style>
