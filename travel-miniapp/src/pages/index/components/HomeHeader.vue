<!-- 首页顶部搜索栏 -->
<template>
  <view class="ios-header">
    <image class="hero-brand-bg" src="/static/brand/waytrip-standard.svg" mode="widthFix" />
    <view class="header-top">
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
      <image class="avatar-sm" :src="avatarUrl" @click="$emit('goMine')" />
    </view>
  </view>
</template>

<script setup>
// 头部只透传搜索和个人中心入口，避免把首页导航逻辑拆散到样式组件里。
defineProps({
  avatarUrl: { type: String, default: '' }
})
// 事件保持轻量，页面层可按登录态自行决定跳转还是弹出引导。
defineEmits(['goSearch', 'goMine'])
</script>

<style scoped>
.ios-header {
  padding: 42rpx 32rpx 28rpx;
  background: linear-gradient(180deg, rgba(248, 250, 255, 0.96) 0%, rgba(244, 246, 251, 0.9) 100%);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(18rpx);
  overflow: hidden;
}
.hero-brand-bg {
  position: absolute;
  left: 28rpx;
  top: 18rpx;
  width: 236rpx;
  display: block;
  opacity: 0.16;
  pointer-events: none;
}
.header-top {
  display: flex;
  align-items: center;
  gap: 16rpx;
  position: relative;
  z-index: 1;
  padding-top: 18rpx;
}
.search-bar { flex: 1; }
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
