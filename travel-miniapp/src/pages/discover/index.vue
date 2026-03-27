<template>
  <view class="discover-page">
    <view class="hero-card">
      <text class="hero-title">发现页</text>
      <text class="hero-desc">景点和攻略的统一入口先落地，后续再补联动筛选和聚合展示。</text>
      <view class="hero-actions">
        <view class="hero-btn primary" @click="goSpotList">景点列表</view>
        <view class="hero-btn" @click="goGuideList">攻略列表</view>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">核心入口</text>
      </view>
      <view class="entry-grid">
        <view class="entry-card" @click="goSpotList">
          <text class="entry-title">景点列表</text>
          <text class="entry-desc">按地区、分类、排序浏览景点</text>
        </view>
        <view class="entry-card" @click="goGuideList">
          <text class="entry-title">攻略列表</text>
          <text class="entry-desc">按分类浏览攻略内容</text>
        </view>
        <view class="entry-card" @click="goSearch">
          <text class="entry-title">全局搜索</text>
          <text class="entry-desc">搜索景点和攻略关键词</text>
        </view>
        <view class="entry-card" @click="goRecommendList">
          <text class="entry-title">推荐列表</text>
          <text class="entry-desc">查看更多推荐结果</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">后续补强</text>
      </view>
      <view class="todo-list">
        <view class="todo-item">分类与地区的统一筛选联动</view>
        <view class="todo-item">景点与攻略的同屏聚合</view>
        <view class="todo-item">条件组合筛选与排序收拢</view>
      </view>
    </view>
  </view>
</template>

<script setup>
const goSpotList = () => {
  uni.navigateTo({ url: '/pages/spot/list' })
}

const goGuideList = () => {
  uni.navigateTo({ url: '/pages/guide/list' })
}

const goSearch = () => {
  uni.navigateTo({ url: '/pages/spot/search' })
}

const goRecommendList = () => {
  uni.navigateTo({ url: '/pages/recommend/list' })
}
</script>

<style scoped>
.discover-page {
  min-height: 100vh;
  padding: 32rpx;
  background: #f4f6fb;
}

.hero-card,
.entry-card,
.todo-list {
  background: #ffffff;
  border-radius: 28rpx;
  box-shadow: 0 8rpx 20rpx rgba(31, 41, 55, 0.05);
}

.hero-card {
  padding: 36rpx 32rpx;
}

.hero-title {
  display: block;
  font-size: 42rpx;
  font-weight: 700;
  color: #111827;
}

.hero-desc {
  display: block;
  margin-top: 16rpx;
  font-size: 28rpx;
  line-height: 1.6;
  color: #6b7280;
}

.hero-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 28rpx;
}

.hero-btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 20rpx;
  text-align: center;
  background: #eef2f7;
  color: #374151;
  font-size: 28rpx;
  font-weight: 600;
}

.hero-btn.primary {
  background: #2563eb;
  color: #ffffff;
}

.section {
  margin-top: 28rpx;
}

.section-header {
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #111827;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20rpx;
}

.entry-card {
  padding: 28rpx;
}

.entry-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.entry-desc {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.5;
  color: #6b7280;
}

.todo-list {
  padding: 16rpx 28rpx;
}

.todo-item {
  padding: 22rpx 0;
  font-size: 26rpx;
  color: #374151;
}

.todo-item:not(:last-child) {
  border-bottom: 1rpx solid #edf2f7;
}
</style>
