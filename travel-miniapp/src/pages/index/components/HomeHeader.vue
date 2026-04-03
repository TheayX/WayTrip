<template>
  <view class="ios-header">
    <view class="header-top">
      <view class="greeting-box">
        <text class="header-kicker">WayTrip</text>
        <text class="large-title">下一次出发，先从这里开始。</text>
      </view>
      <image class="avatar-sm" :src="avatarUrl" @click="$emit('goMine')" />
    </view>
    <view class="search-bar" @click="$emit('goSearch')">
      <uni-search-bar
        :modelValue="''"
        placeholder="搜索景点、攻略或目的地"
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
.ios-header {
  padding: 88rpx 32rpx 28rpx;
  background: linear-gradient(180deg, rgba(248, 250, 255, 0.96) 0%, rgba(244, 246, 251, 0.9) 100%);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(18rpx);
}
.header-top { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 28rpx; }
.greeting-box { display: flex; flex-direction: column; gap: 10rpx; max-width: 520rpx; }
.header-kicker { font-size: 22rpx; font-weight: 700; letter-spacing: 6rpx; color: #64748b; text-transform: uppercase; }
.large-title { font-size: 50rpx; font-weight: 800; color: #0f172a; line-height: 1.18; }
.avatar-sm {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffffff 0%, #dbe4f0 100%);
  box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.08);
}
.search-bar :deep(.uni-searchbar) { padding: 0; background: transparent; }
.search-bar :deep(.uni-searchbar__box) {
  border: 2rpx solid rgba(255, 255, 255, 0.76);
  height: 84rpx;
  border-radius: 42rpx;
  backdrop-filter: blur(14rpx);
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.05);
  background: rgba(255, 255, 255, 0.82);
}
</style>
