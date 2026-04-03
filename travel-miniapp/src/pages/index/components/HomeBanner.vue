<template>
  <view class="banner-container" v-if="banners.length">
    <swiper class="banner" indicator-dots indicator-active-color="#fff" autoplay circular>
      <swiper-item v-for="banner in banners" :key="banner.id" @click="$emit('click', banner)">
        <image class="banner-image" :src="getContentImageUrl(banner.imageUrl)" mode="aspectFill" />
      </swiper-item>
    </swiper>
  </view>
</template>

<script setup>
import { getContentImageUrl } from '@/utils/request'

defineProps({
  banners: { type: Array, default: () => [] }
})
defineEmits(['click'])
</script>

<style scoped>
.banner-container { padding: 8rpx 32rpx 28rpx; }
.banner, .banner-image { width: 100%; height: 340rpx; border-radius: 36rpx; }
.banner {
  overflow: hidden;
  box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.1);
  transform: translateZ(0);
}
</style>
