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
import { getContentImageUrl } from '@/utils/client'

defineProps({
  banners: { type: Array, default: () => [] }
})
defineEmits(['click'])
</script>

<style scoped>
.banner-container { padding: 8rpx 32rpx 24rpx; }
.banner, .banner-image { width: 100%; height: 320rpx; border-radius: 32rpx; }
.banner { overflow: hidden; box-shadow: 0 16rpx 32rpx rgba(17, 24, 39, 0.08); transform: translateZ(0); }
</style>
