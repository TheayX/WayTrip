<template>
  <view class="deactivate-page">
    <view class="warning-card">
      <view class="warning-icon">⚠️</view>
      <text class="warning-title">注销账户</text>
      <text class="warning-desc">注销后你将无法使用此账户登录，所有数据将被保留。</text>
    </view>

    <view class="info-section">
      <text class="info-title">注销说明</text>
      <view class="info-item">
        <text class="info-dot">•</text>
        <text class="info-text">注销后无法立即使用此账户</text>
      </view>
      <view class="info-item">
        <text class="info-dot">•</text>
        <text class="info-text">你的订单记录、收藏等数据将被保留</text>
      </view>
      <view class="info-item">
        <text class="info-dot">•</text>
        <text class="info-text">使用同一微信号重新登录可恢复账户</text>
      </view>
      <view class="info-item">
        <text class="info-dot">•</text>
        <text class="info-text">此操作无法撤销，请谨慎考虑</text>
      </view>
    </view>

    <view class="action-buttons">
      <button class="btn-cancel" @click="goBack">取消</button>
      <button class="btn-deactivate" @click="confirmDeactivate">确认注销</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { deactivateAccount } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)

const goBack = () => {
  uni.navigateBack()
}

const confirmDeactivate = () => {
  uni.showModal({
    title: '确认注销',
    content: '注销账户后将无法登录，确定要继续吗？',
    confirmText: '确认注销',
    cancelText: '取消',
    success: async (res) => {
      if (res.confirm) {
        await doDeactivate()
      }
    }
  })
}

const doDeactivate = async () => {
  loading.value = true
  try {
    await deactivateAccount()
    uni.showToast({ title: '账户已注销', icon: 'success' })

    // 等待 toast 显示后，登出并返回首页
    setTimeout(() => {
      userStore.logout()
      uni.reLaunch({ url: '/pages/index/index' })
    }, 1500)
  } catch (e) {
    uni.showToast({ title: '注销失败，请重试', icon: 'none' })
    console.error('注销失败', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.deactivate-page {
  background-color: #F2F2F7;
  min-height: 100vh;
  padding: 20rpx 32rpx;
  padding-top: 40rpx;
}

.warning-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 32rpx;
  text-align: center;
  margin-bottom: 40rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.02);
}

.warning-icon {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}

.warning-title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: #000;
  margin-bottom: 12rpx;
}

.warning-desc {
  display: block;
  font-size: 28rpx;
  color: #8E8E93;
  line-height: 1.6;
}

.info-section {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 40rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.02);
}

.info-title {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #000;
  margin-bottom: 20rpx;
}

.info-item {
  display: flex;
  gap: 12rpx;
  margin-bottom: 16rpx;
  align-items: flex-start;
}

.info-dot {
  font-size: 28rpx;
  color: #FF3B30;
  flex-shrink: 0;
  margin-top: 2rpx;
}

.info-text {
  font-size: 28rpx;
  color: #606266;
  line-height: 1.5;
}

.action-buttons {
  display: flex;
  gap: 16rpx;
  margin-bottom: 40rpx;
}

.btn-cancel {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  background: #F2F2F7;
  color: #666;
  border: none;
  border-radius: 24rpx;
  font-size: 32rpx;
  font-weight: 600;
}

.btn-deactivate {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  background: #FF3B30;
  color: #fff;
  border: none;
  border-radius: 24rpx;
  font-size: 32rpx;
  font-weight: 600;
}

.btn-cancel:active {
  opacity: 0.8;
}

.btn-deactivate:active {
  opacity: 0.9;
}
</style>

