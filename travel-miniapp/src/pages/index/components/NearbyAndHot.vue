<template>
  <view class="section">
    <view class="section-header">
      <text class="section-title">热门景点</text>
      <text class="section-link" @click="$emit('moreHot')">查看全部</text>
    </view>
    
    <scroll-view class="hot-scroll" scroll-x :show-scrollbar="false" v-if="hotSpots.length">
      <view class="hot-card" v-for="spot in hotSpots" :key="spot.id" @click="$emit('clickHot', spot)">
        <image class="hot-img" :src="getContentImageUrl(spot.coverImage)" mode="aspectFill" />
        <view class="hot-overlay">
          <text class="hot-name">{{ spot.name }}</text>
          <view class="hot-meta">
            <text class="hot-price">￥{{ spot.price }}</text>
            <view class="hot-rating"><uni-icons type="star-filled" size="12" color="#fcd34d"/> {{ spot.avgRating || '4.5' }}</view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- Main Nearby Map Card (Kept based on User Feedback) -->
    <view class="nearby-card" @click="$emit('clickNearby')">
      <view class="nearby-top">
        <text class="nearby-status">{{ nearbyHeadline || '附近探索' }}</text>
        <view class="nearby-badge" v-if="nearbySpots.length">
          <uni-icons type="location-filled" size="14" color="#2563eb" />
          <text class="badge-text">{{ nearbySpots.length }} 个附近景点</text>
        </view>
      </view>

      <view class="nearby-map-shell">
        <map
          v-if="canShowMap"
          class="nearby-map"
          :latitude="center.latitude"
          :longitude="center.longitude"
          :scale="12"
          :markers="markers"
          :show-location="true"
          :enable-scroll="false"
          :enable-zoom="false"
          @markertap="(e) => $emit('markerTap', e)"
        />
        <view v-else class="nearby-placeholder">
          <uni-icons type="location-filled" size="32" color="#93c5fd" />
          <text class="placeholder-text">{{ placeholderText }}</text>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup>
import { getContentImageUrl } from '@/utils/request'

defineProps({
  nearbyHeadline: { type: String, default: '附近探索' },
  canShowMap: { type: Boolean, default: false },
  center: { type: Object, default: () => ({ latitude: 39.9042, longitude: 116.4074 }) },
  markers: { type: Array, default: () => [] },
  nearbySpots: { type: Array, default: () => [] },
  placeholderText: { type: String, default: '开启定位，发现周边好去处' },
  hotSpots: { type: Array, default: () => [] }
})
defineEmits(['clickNearby', 'markerTap', 'moreHot', 'clickHot'])
</script>

<style scoped>
.section { padding: 0 0 48rpx; }
.section-header { padding: 0 32rpx; margin-bottom: 0; display: flex; align-items: center; justify-content: space-between;}
.section-title { font-size: 38rpx; font-weight: 700; color: #0f172a; }
.section-link { font-size: 26rpx; color: #6b7280; font-weight: 500; }

/* Hot Spots */
.hot-scroll { padding-left: 32rpx; white-space: nowrap; padding-bottom: 32rpx;}
.hot-card {
  display: inline-block; width: 280rpx; height: 360rpx; margin-right: 24rpx;
  border-radius: 36rpx; overflow: hidden; position: relative;
  box-shadow: 0 16rpx 36rpx rgba(15, 23, 42, 0.08);
}
.hot-img { width: 100%; height: 100%; }
.hot-overlay {
  position: absolute; left: 0; right: 0; bottom: 0; padding: 32rpx 24rpx 20rpx;
  background: linear-gradient(180deg, transparent 0%, rgba(17, 24, 39, 0.85) 100%);
  display: flex; flex-direction: column; justify-content: flex-end;
}
.hot-name { font-size: 30rpx; font-weight: 700; color: #ffffff; margin-bottom: 12rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hot-meta { display: flex; justify-content: space-between; align-items: center; }
.hot-price { font-size: 28rpx; font-weight: 800; color: #f87171; }
.hot-rating { display: flex; align-items: center; gap: 6rpx; font-size: 24rpx; color: #ffffff; font-weight: 600;}

/* Nearby Card */
.nearby-card {
  margin: 8rpx 32rpx 0; padding: 20rpx;
  background: rgba(255, 255, 255, 0.88); border-radius: 36rpx;
  box-shadow: 0 12rpx 36rpx rgba(15, 23, 42, 0.05);
  backdrop-filter: blur(18rpx);
}
.nearby-top {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx;
}
.nearby-status { font-size: 34rpx; font-weight: 700; color: #111827; }
.nearby-badge {
  display: flex; align-items: center; gap: 8rpx;
  padding: 10rpx 24rpx; background: rgba(255, 255, 255, 0.9); border-radius: 99rpx;
}
.badge-text { font-size: 24rpx; font-weight: 600; color: #334155; }

.nearby-map-shell { border-radius: 24rpx; overflow: hidden; background: #f8fafc; margin-top: 12rpx;}
.nearby-map { width: 100%; height: 260rpx; }
.nearby-placeholder {
  height: 260rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16rpx;
  background: linear-gradient(135deg, #f8fafc 0%, #e9eff7 100%);
}
.placeholder-text { font-size: 26rpx; color: #64748b; font-weight: 500; }

</style>
