<!-- 首页横幅轮播 -->
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

// 轮播组件保持纯展示，只接收父层整理好的横幅数据与点击事件。
// 数据为空时直接不渲染，避免首页出现仅占位不承载信息的轮播区域。
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
