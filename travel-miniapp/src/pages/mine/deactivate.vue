<!-- 账号注销页 -->
<template>
  <view class="deactivate-page">
    <view class="hero-card">
      <text class="hero-title">注销账户</text>
      <text class="hero-subtitle">这是高风险操作，确认前请先了解数据与恢复方式。</text>
    </view>

    <view class="warning-card">
      <view class="warning-icon">风险提示</view>
      <text class="warning-title">注销账户</text>
      <text class="warning-desc">注销后账号会进入停用状态，所有数据将被保留。</text>
    </view>

    <!-- 注销说明区域 -->
    <view class="info-section">
      <text class="info-title">注销说明</text>
      <view class="info-item">
        <text class="info-dot">•</text>
        <text class="info-text">注销后账号会先进入停用状态</text>
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

    <!-- 操作区域 -->
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

// 基础依赖与用户状态
const userStore = useUserStore()
const loading = ref(false)

// 页面跳转方法
const goBack = () => {
  uni.navigateBack()
}

// 交互处理方法
const confirmDeactivate = () => {
  uni.showModal({
    title: '确认注销',
    content: '注销后账号会进入停用状态，后续重新登录可恢复，确定要继续吗？',
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
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.94), rgba(245, 247, 250, 0.9) 48%, rgba(238, 242, 247, 1) 100%),
    linear-gradient(180deg, #fafafa 0%, #eef2f7 100%);
  min-height: 100vh;
  padding: 20rpx 32rpx;
  padding-top: 40rpx;
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

.warning-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 36rpx;
  padding: 48rpx 32rpx;
  text-align: center;
  margin-bottom: 40rpx;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.warning-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 132rpx;
  height: 56rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: rgba(190, 24, 93, 0.1);
  color: #9f1239;
  font-size: 24rpx;
  font-weight: 600;
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
  color: #6b7280;
  line-height: 1.6;
}

.info-section {
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 36rpx;
  padding: 32rpx;
  margin-bottom: 40rpx;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
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
  background: rgba(17, 24, 39, 0.06);
  color: #666;
  border: none;
  border-radius: 36rpx;
  font-size: 32rpx;
  font-weight: 600;
}

.btn-deactivate {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  background: #9f1239;
  color: #fff;
  border: none;
  border-radius: 36rpx;
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

