<template>
  <view class="ios-mine">
    <!-- 个人信息头 -->
    <view class="profile-header" @click="isLoggedIn ? null : doLogin()">
      <view class="avatar-container">
        <image class="avatar-lg" :src="userInfo?.avatar || '/static/default-avatar.png'" />
      </view>
      <view class="profile-info">
        <text class="user-name">{{ isLoggedIn ? (userInfo?.nickname || '旅行家') : '点击登录' }}</text>
        <text class="user-desc">{{ isLoggedIn ? '开启你的探索之旅' : '登录同步数据' }}</text>
      </view>
      <view class="arrow-right" v-if="!isLoggedIn">›</view>
    </view>

    <!-- 菜单组1 -->
    <view class="ios-group">
      <view class="ios-cell" @click="goOrders">
        <view class="cell-icon">
          <image class="cell-icon-img" src="/static/icons/订单.png" />
        </view>
        <text class="cell-title">我的订单</text>
        <text class="cell-arrow">›</text>
      </view>
      <view class="ios-cell" @click="goFavorites">
        <view class="cell-icon">
          <image class="cell-icon-img" src="/static/icons/收藏.png" />
        </view>
        <text class="cell-title">我的收藏</text>
        <text class="cell-arrow">›</text>
      </view>
    </view>

    <!-- 菜单组2 -->
    <view class="ios-group">
      <view class="ios-cell" @click="goPreference">
        <view class="cell-icon">
          <image class="cell-icon-img" src="/static/icons/偏好.png" />
        </view>
        <text class="cell-title">偏好设置</text>
        <text class="cell-arrow">›</text>
      </view>
      <view class="ios-cell" @click="contactService">
        <view class="cell-icon">
          <image class="cell-icon-img" src="/static/icons/客服.png" />
        </view>
        <text class="cell-title">联系客服</text>
        <text class="cell-arrow">›</text>
      </view>
      <view class="ios-cell" @click="showAbout">
        <view class="cell-icon">
          <image class="cell-icon-img" src="/static/icons/关于.png" />
        </view>
        <text class="cell-title">关于我们</text>
        <text class="cell-arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="ios-group logout-group" v-if="isLoggedIn">
      <view class="ios-cell center-align" @click="doLogout">
        <text class="logout-text">退出登录</text>
      </view>
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
    content: 'WayTrip·微旅 v1.0.0\n基于协同过滤的个性化旅游推荐',
    showCancel: false
  })
}
</script>

<style scoped>
.ios-mine {
  background-color: #F2F2F7;
  min-height: 100vh;
  padding: 20rpx 32rpx;
  padding-top: 120rpx;
}

/* 个人信息头 */
.profile-header {
  display: flex;
  align-items: center;
  margin-bottom: 60rpx;
}

.avatar-lg {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  border: 4rpx solid #fff;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.1);
}

.profile-info {
  margin-left: 32rpx;
  flex: 1;
}

.user-name {
  font-size: 44rpx;
  font-weight: 700;
  color: #000;
  display: block;
  margin-bottom: 8rpx;
}

.user-desc {
  font-size: 28rpx;
  color: #8E8E93;
}

.arrow-right {
  font-size: 40rpx;
  color: #C7C7CC;
}

/* 菜单组 - iOS Inset Grouped 风格 */
.ios-group {
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 32rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.02);
}

.ios-cell {
  display: flex;
  align-items: center;
  padding: 32rpx;
  background: #fff;
  position: relative;
}

/* 分割线 */
.ios-cell:not(:last-child)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 112rpx;
  right: 0;
  height: 1px;
  background-color: #E5E5EA;
}

.ios-cell:active {
  background-color: #F2F2F7;
}

/* 图标容器 - 毛玻璃风格 */
.cell-icon {
  width: 60rpx;
  height: 60rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
  background: rgba(120, 120, 128, 0.08);
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.cell-icon-img {
  width: 32rpx;
  height: 32rpx;
  opacity: 0.85;
}

.cell-title {
  font-size: 34rpx;
  color: #000;
  flex: 1;
}

.cell-arrow {
  color: #C7C7CC;
  font-size: 34rpx;
  font-weight: 300;
}

/* 退出登录 */
.center-align {
  justify-content: center;
}

.logout-text {
  color: #FF3B30;
  font-size: 34rpx;
  font-weight: 600;
}
</style>
