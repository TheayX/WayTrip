<!-- 设置页 -->
<template>
  <view class="settings-page">
    <view class="hero-card">
      <text class="hero-title">通用设置</text>
      <text class="hero-subtitle">管理缓存、联系支持，并查看应用相关信息。</text>
    </view>

    <view class="settings-group">
      <view class="settings-cell" @click="contactService">
        <text class="cell-title">联系客服</text>
        <text class="cell-arrow">›</text>
      </view>
      <view class="settings-cell" @click="showAbout">
        <text class="cell-title">关于我们</text>
        <text class="cell-arrow">›</text>
      </view>
      <view class="settings-cell" @click="clearCache">
        <text class="cell-title">清除缓存</text>
        <text class="cell-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup>
// 交互处理方法
const contactService = () => {
  uni.showModal({
    title: '联系客服',
    content: '客服电话：400-123-4567',
    showCancel: false
  })
}

const showAbout = () => {
  uni.showModal({
    title: '关于我们',
    content: 'WayTrip·微旅 v1.0.0\n基于协同过滤的个性化旅游推荐',
    showCancel: false
  })
}

const clearCache = () => {
  uni.showModal({
    title: '清除缓存',
    content: '确认清除本地缓存吗？',
    success: (res) => {
      if (!res.confirm) return
      uni.clearStorageSync()
      uni.showToast({ title: '缓存已清除', icon: 'success' })
    }
  })
}
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
  padding: 24rpx 32rpx;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.94), rgba(245, 247, 250, 0.9) 48%, rgba(238, 242, 247, 1) 100%),
    linear-gradient(180deg, #fafafa 0%, #eef2f7 100%);
}

.hero-card {
  margin-bottom: 24rpx;
  padding: 28rpx;
  border-radius: 34rpx;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.58) 100%);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.hero-title {
  display: block;
  font-size: 38rpx;
  font-weight: 600;
  color: #18181b;
}

.hero-subtitle {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #52525b;
}

.settings-group {
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 40rpx;
  overflow: hidden;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.settings-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 28rpx;
  font-size: 30rpx;
  color: #111827;
}

.settings-cell:not(:last-child) {
  border-bottom: 1rpx solid #edf2f7;
}

.cell-title {
  font-size: 30rpx;
}

.cell-arrow {
  font-size: 32rpx;
  color: #9ca3af;
}
</style>
