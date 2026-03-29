<template>
  <view class="ios-header">
    <view class="header-top">
      <view class="greeting-box">
        <text class="large-title">WayTrip 伴你出行</text>
      </view>
      <image class="avatar-sm" :src="avatarUrl" @click="$emit('goMine')" />
    </view>
    <view class="search-bar" @click="$emit('goSearch')">
      <!-- keeping styling clean -->
      <uni-search-bar
        :modelValue="''"
        placeholder="探索你的下一次完美旅行..."
        :clearButton="'none'"
        :cancelButton="'none'"
        :radius="100"
        :readonly="true"
        bgColor="rgba(255, 255, 255, 0.6)"
      />
    </view>
  </view>
</template>

<script setup>
defineProps({
  avatarUrl: { type: String, default: '' }
})
defineEmits(['goSearch', 'goMine'])
</script>

<style scoped>
/* Glassmorphism/clean styles for header */
.ios-header {
  padding: 88rpx 32rpx 24rpx;
  background: linear-gradient(180deg, #f4f6fb 0%, rgba(244, 246, 251, 0.9) 100%);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
}
.header-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx; }
.large-title { font-size: 48rpx; font-weight: 800; color: #111827; letter-spacing: 1rpx; }
.avatar-sm { width: 72rpx; height: 72rpx; border-radius: 50%; background: #dbe4f0; }
.search-bar :deep(.uni-searchbar) { padding: 0; background: transparent; }
.search-bar :deep(.uni-searchbar__box) { border: 2rpx solid #e5e7eb; height: 80rpx; border-radius: 40rpx; backdrop-filter: blur(10px); box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.02); }
</style>
