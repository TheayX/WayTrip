<template>
  <view class="mine-page">
    <!-- 用户信息 -->
    <view class="user-card">
      <image class="user-avatar" :src="userInfo?.avatar || '/static/default-avatar.png'" />
      <view class="user-info" v-if="isLoggedIn">
        <text class="user-name">{{ userInfo?.nickname || '旅行者' }}</text>
        <view class="user-tags" v-if="userInfo?.preferences?.length">
          <text class="tag" v-for="tag in userInfo.preferences" :key="tag">{{ tag }}</text>
        </view>
      </view>
      <view class="user-info" v-else @click="doLogin">
        <text class="login-tip">点击登录</text>
      </view>
    </view>

    <!-- 功能菜单 -->
    <view class="menu-card card">
      <view class="menu-item" @click="goOrders">
        <text class="menu-icon">📋</text>
        <text class="menu-text">我的订单</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goFavorites">
        <text class="menu-icon">❤️</text>
        <text class="menu-text">我的收藏</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goPreference">
        <text class="menu-icon">🏷️</text>
        <text class="menu-text">偏好设置</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 其他菜单 -->
    <view class="menu-card card">
      <view class="menu-item" @click="contactService">
        <text class="menu-icon">📞</text>
        <text class="menu-text">联系客服</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="showAbout">
        <text class="menu-icon">ℹ️</text>
        <text class="menu-text">关于我们</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-btn" v-if="isLoggedIn" @click="doLogout">
      退出登录
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { wxLogin } from '@/api/auth'

const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)

// 登录
const doLogin = async () => {
  try {
    // #ifdef MP-WEIXIN
    const loginRes = await uni.login({ provider: 'weixin' })
    const res = await wxLogin(loginRes.code)
    userStore.login(res.data)
    uni.showToast({ title: '登录成功', icon: 'success' })
    // #endif
    
    // #ifdef H5
    uni.showToast({ title: 'H5端暂不支持微信登录', icon: 'none' })
    // #endif
  } catch (e) {
    console.error('登录失败', e)
    uni.showToast({ title: '登录失败', icon: 'none' })
  }
}

// 退出登录
const doLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        uni.showToast({ title: '已退出登录', icon: 'none' })
      }
    }
  })
}

// 跳转订单
const goOrders = () => {
  uni.navigateTo({ url: '/pages/order/list' })
}

// 跳转收藏
const goFavorites = () => {
  uni.navigateTo({ url: '/pages/favorite/index' })
}

// 跳转偏好设置
const goPreference = () => {
  uni.navigateTo({ url: '/pages/mine/preference' })
}

// 联系客服
const contactService = () => {
  uni.showModal({
    title: '联系客服',
    content: '客服电话：400-123-4567',
    showCancel: false
  })
}

// 关于我们
const showAbout = () => {
  uni.showModal({
    title: '关于我们',
    content: 'WayTrip·微旅 v1.0.0 - 基于协同过滤的个性化推荐',
    showCancel: false
  })
}
</script>

<style scoped>
.mine-page {
  padding: 20rpx;
  min-height: 100vh;
  background: #f5f5f5;
}

/* 用户卡片 */
.user-card {
  display: flex;
  align-items: center;
  padding: 40rpx;
  background: linear-gradient(135deg, #409EFF, #67C23A);
  border-radius: 16rpx;
  margin-bottom: 20rpx;
}

.user-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
  margin-right: 30rpx;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 36rpx;
  font-weight: bold;
  color: #fff;
  display: block;
}

.user-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.tag {
  font-size: 22rpx;
  color: #fff;
  background: rgba(255, 255, 255, 0.3);
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}

.login-tip {
  font-size: 32rpx;
  color: #fff;
}

/* 菜单卡片 */
.menu-card {
  margin-bottom: 20rpx;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
}

.menu-text {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}

.menu-arrow {
  font-size: 32rpx;
  color: #ccc;
}

/* 退出按钮 */
.logout-btn {
  margin-top: 60rpx;
  text-align: center;
  padding: 30rpx;
  background: #fff;
  border-radius: 16rpx;
  color: #ff6b6b;
  font-size: 30rpx;
}
</style>
