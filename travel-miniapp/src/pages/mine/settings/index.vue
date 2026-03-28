<template>
  <view class="settings-page">
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
      <view class="settings-cell danger" @click="goDeactivate">
        <text class="cell-title">注销账户</text>
        <text class="cell-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup>
const goDeactivate = () => {
  uni.navigateTo({ url: '/pages/mine/deactivate' })
}

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
  background: #f4f6fb;
}

.settings-group {
  background: #ffffff;
  border-radius: 28rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 20rpx rgba(31, 41, 55, 0.05);
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

.settings-cell.danger,
.settings-cell.danger .cell-title {
  color: #dc2626;
}

.cell-title {
  font-size: 30rpx;
}

.cell-arrow {
  font-size: 32rpx;
  color: #9ca3af;
}
</style>
