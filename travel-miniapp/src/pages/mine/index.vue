<!-- 我的页面 -->
<template>
  <view class="ios-mine">
    <!-- 用户信息区域 (晨光与冰蓝渐变) -->
    <view class="profile-hero" :class="{ guest: !isLoggedIn }" @click="!isLoggedIn ? doLogin() : null">
      <view class="profile-header">
        <view class="avatar-container">
          <image class="avatar-lg" :src="getAvatarUrl(userInfo?.avatar)" />
        </view>
        <view class="profile-info">
          <text class="user-name">{{ isLoggedIn ? (userInfo?.nickname || '旅行家') : '旅程从登录开始' }}</text>
          <text class="user-desc">{{ isLoggedIn ? '开启你的极简探索之旅' : '点此登录，发掘专属旅行足迹' }}</text>
        </view>
        <view class="hero-action" v-if="!isLoggedIn">
          <text class="hero-action-text">登录</text>
          <uni-icons type="right" size="14" color="#ea580c" />
        </view>
      </view>

      <view class="profile-extra" v-if="isLoggedIn">
        <text class="profile-line">手机号：{{ formatPhone(userInfo?.phone) }}</text>
      </view>
    </view>

    <!-- 统计概览 -->
    <view class="stats-board" v-if="isLoggedIn">
      <view class="stats-item" @click="goActivity('browse')">
        <text class="stats-value">{{ dashboardStats.viewed }}</text>
        <text class="stats-label">浏览历史</text>
      </view>
      <view class="stats-item" @click="goActivity('favorite')">
        <text class="stats-value">{{ dashboardStats.favorites }}</text>
        <text class="stats-label">我的收藏</text>
      </view>
      <view class="stats-item" @click="goActivity('review')">
        <text class="stats-value">{{ dashboardStats.reviews }}</text>
        <text class="stats-label">我的评价</text>
      </view>
    </view>

    <!-- 订单概览 -->
    <view class="order-overview" v-if="isLoggedIn">
      <view class="overview-header">
        <text class="overview-title">旅行订单</text>
        <text class="overview-link" @click="goOrders">查看全部</text>
      </view>
      <view class="overview-grid">
        <view class="overview-card" @click="goOrdersByStatus('pending')">
          <text class="overview-value">{{ orderStats.pending }}</text>
          <text class="overview-label">待支付</text>
        </view>
        <view class="overview-card" @click="goOrdersByStatus('paid')">
          <text class="overview-value">{{ orderStats.paid }}</text>
          <text class="overview-label">已支付</text>
        </view>
        <view class="overview-card" @click="goOrdersByStatus('completed')">
          <text class="overview-value">{{ orderStats.completed }}</text>
          <text class="overview-label">已完成</text>
        </view>
      </view>
    </view>

    <view class="ios-group">
      <view class="ios-cell" @click="goSettings">
        <view class="cell-icon"><uni-icons type="gear-filled" size="20" color="#64748b" /></view>
        <text class="cell-title">系统设置</text>
        <uni-icons type="right" size="16" color="#d1d5db" />
      </view>
    </view>

    <view class="locked-group-shell">
      <view class="ios-group" :class="{ 'group-locked': !isLoggedIn }">
        <view class="ios-cell" @click="goOrders">
          <view class="cell-icon bg-blue"><uni-icons type="wallet-filled" size="20" color="#3b82f6" /></view>
          <text class="cell-title">我的订单</text>
          <uni-icons type="right" size="16" color="#d1d5db" />
        </view>
        <view class="ios-cell" @click="goActivity()">
          <view class="cell-icon bg-orange"><uni-icons type="chatbubble-filled" size="20" color="#ea580c" /></view>
          <text class="cell-title">我的互动</text>
          <uni-icons type="right" size="16" color="#d1d5db" />
        </view>
        <view class="ios-cell" @click="goPreference">
          <view class="cell-icon bg-amber"><uni-icons type="star-filled" size="20" color="#d97706" /></view>
          <text class="cell-title">偏好设置</text>
          <uni-icons type="right" size="16" color="#d1d5db" />
        </view>
        <view class="ios-cell" @click="goProfile">
          <view class="cell-icon bg-emerald"><uni-icons type="person-filled" size="20" color="#10b981" /></view>
          <text class="cell-title">账号资料</text>
          <uni-icons type="right" size="16" color="#d1d5db" />
        </view>
      </view>

      <view class="group-lock-mask" v-if="!isLoggedIn" @click="doLogin">
        <view class="group-lock-content">
          <text class="group-lock-title">专属旅行资产</text>
          <text class="group-lock-desc">一键登录，随时管理你的行程与档案</text>
          <view class="group-lock-button">微信快捷登录</view>
        </view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="ios-group logout-group" v-if="isLoggedIn">
      <view class="ios-cell center-align" @click="doLogout">
        <text class="logout-text">退出登录</text>
      </view>
    </view>

    <!-- 弹层组件 -->
    <AuthPopup 
      v-model:visibleStep="authStep"
      :openid="pendingOpenid"
      @success="onAuthSuccess"
    />
  </view>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getFavoriteList } from '@/api/favorite'
import { getOrderList } from '@/api/order'
import { getMyReviews } from '@/api/review'
import { wxLogin } from '@/api/auth'
import { getUserInfo } from '@/api/user'
import AuthPopup from './components/AuthPopup.vue'
import { getAvatarUrl } from '@/utils/request'

// 基础依赖与用户状态
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)

// 页面数据状态
const dashboardStats = reactive({ viewed: 0, favorites: 0, reviews: 0 })
const orderStats = reactive({ pending: 0, paid: 0, completed: 0 })

// Auth popup state
const authStep = ref(0)
const pendingOpenid = ref('')

// 工具方法
const formatPhone = (phone) => {
  if (!phone || !phone.trim()) return '未绑定'
  const normalized = phone.trim()
  if (/^1\d{10}$/.test(normalized)) return `${normalized.slice(0, 3)}****${normalized.slice(7)}`
  if (/^1\d{2}\*{4}\d{4}$/.test(normalized)) return normalized
  return '已隐藏'
}

const syncUserInfo = async () => {
  try {
    const res = await getUserInfo()
    userStore.setUserInfo(res.data)
  } catch (e) { console.error('同步用户信息失败', e) }
}

const loadRecentFootprints = () => {
  const history = uni.getStorageSync('spot_footprints')
  const footprints = Array.isArray(history) ? history : []
  dashboardStats.viewed = footprints.length
}

const loadMineOverview = async () => {
  if (!isLoggedIn.value) {
    dashboardStats.viewed = 0; dashboardStats.favorites = 0; dashboardStats.reviews = 0
    orderStats.pending = 0; orderStats.paid = 0; orderStats.completed = 0
    return
  }

  loadRecentFootprints()

  try {
    const [favoriteRes, reviewRes, pendingRes, paidRes, completedRes] = await Promise.all([
      getFavoriteList(1, 1), getMyReviews(1, 1),
      getOrderList({ status: 'pending', page: 1, pageSize: 1 }),
      getOrderList({ status: 'paid', page: 1, pageSize: 1 }),
      getOrderList({ status: 'completed', page: 1, pageSize: 1 })
    ])

    dashboardStats.favorites = favoriteRes.data?.total || favoriteRes.data?.list?.length || 0
    dashboardStats.reviews = reviewRes.data?.total || reviewRes.data?.list?.length || 0
    orderStats.pending = pendingRes.data?.total || pendingRes.data?.list?.length || 0
    orderStats.paid = paidRes.data?.total || paidRes.data?.list?.length || 0
    orderStats.completed = completedRes.data?.total || completedRes.data?.list?.length || 0
  } catch (e) {
    console.error('加载我的页概览失败', e)
  }
}

const doLogin = async () => {
  try {
    const loginRes = await uni.login({ provider: 'weixin' })
    const res = await wxLogin(loginRes.code)

    if (res.data.isNewUser) {
      pendingOpenid.value = res.data.openid
      authStep.value = 1
    } else {
      userStore.login(res.data)
      await syncUserInfo()
      await loadMineOverview()

      if (res.data.isReactivated) {
        uni.showModal({
          title: '账户已恢复',
          content: '欢迎回来！你的账户已恢复，可以继续使用微旅了。',
          showCancel: false, confirmText: '确认'
        })
      } else {
        uni.showToast({ title: '登录成功', icon: 'success' })
      }
    }
  } catch (e) {
    uni.showToast({ title: '登录失败', icon: 'none' })
  }
}

const onAuthSuccess = async () => {
  await syncUserInfo()
  await loadMineOverview()
}

const doLogout = () => {
  uni.showModal({
    title: '提示', content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        dashboardStats.viewed = 0; dashboardStats.favorites = 0; dashboardStats.reviews = 0
        orderStats.pending = 0; orderStats.paid = 0; orderStats.completed = 0
        uni.showToast({ title: '已退出登录', icon: 'none' })
      }
    }
  })
}

// Routes
const goOrders = () => uni.navigateTo({ url: '/pages/order/list' })
const goOrdersByStatus = (status) => uni.navigateTo({ url: `/pages/order/list?status=${status}` })
const goActivity = (tab = 'browse') => uni.navigateTo({ url: `/pages/mine/activity?tab=${tab}` })
const goPreference = () => uni.navigateTo({ url: '/pages/mine/preference' })
const goProfile = () => uni.navigateTo({ url: '/pages/mine/profile' })
const goSettings = () => uni.navigateTo({ url: '/pages/mine/settings/index' })

onShow(async () => {
  if (isLoggedIn.value) await syncUserInfo()
  await loadMineOverview()
})

watch(isLoggedIn, async (loggedIn, prevLoggedIn) => {
  if (!loggedIn || loggedIn === prevLoggedIn) return
  await syncUserInfo()
  await loadMineOverview()
})
</script>

<style scoped>
.ios-mine {
  background-color: #f4f6fb;
  min-height: 100vh;
  padding: 20rpx 32rpx;
  padding-bottom: 48rpx;
}

.profile-hero {
  margin-bottom: 32rpx;
  padding: 40rpx 32rpx;
  border-radius: 40rpx;
  background: linear-gradient(135deg, #e0f2fe 0%, #dbeafe 100%);
  box-shadow: 0 12rpx 32rpx rgba(59, 130, 246, 0.08);
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.profile-hero.guest {
  background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
  box-shadow: 0 12rpx 32rpx rgba(249, 115, 22, 0.06);
}

.profile-header { display: flex; align-items: center; z-index: 1; position: relative; }

.avatar-container { margin-right: 32rpx; }
.avatar-lg { width: 140rpx; height: 140rpx; border-radius: 50%; border: 6rpx solid #ffffff; box-shadow: 0 8rpx 20rpx rgba(0,0,0,0.06); }

.profile-info { flex: 1; }
.user-name { font-size: 42rpx; font-weight: 800; color: #1e293b; display: block; margin-bottom: 12rpx; letter-spacing: 1rpx; }
.guest .user-name { color: #9a3412; }
.user-desc { font-size: 26rpx; color: #64748b; font-weight: 500; }
.guest .user-desc { color: #c2410c; }

.hero-action { display: flex; align-items: center; gap: 8rpx; padding: 14rpx 28rpx; border-radius: 99rpx; background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(8px); }
.hero-action-text { font-size: 24rpx; color: #ea580c; font-weight: 700; }

.profile-extra { margin-top: 24rpx; padding-left: 172rpx; display: flex; flex-direction: column; z-index: 1; position: relative;}
.profile-line { font-size: 24rpx; color: #475569; font-weight: 500; }

/* Stats */
.stats-board { display: flex; background: #ffffff; border-radius: 36rpx; padding: 28rpx 16rpx; margin-bottom: 32rpx; box-shadow: 0 8rpx 24rpx rgba(17, 24, 39, 0.03); }
.stats-item { flex: 1; text-align: center; }
.stats-value { display: block; font-size: 40rpx; font-weight: 800; color: #111827; }
.stats-label { display: block; margin-top: 10rpx; font-size: 24rpx; color: #6b7280; font-weight: 500; }

/* Orders */
.order-overview { background: #ffffff; border-radius: 36rpx; padding: 32rpx; margin-bottom: 32rpx; box-shadow: 0 8rpx 24rpx rgba(17, 24, 39, 0.03); }
.overview-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx; }
.overview-title { font-size: 32rpx; font-weight: 700; color: #111827; }
.overview-link { font-size: 24rpx; color: #6b7280; font-weight: 500;}
.overview-grid { display: flex; gap: 20rpx; }
.overview-card { flex: 1; background: #f8fafc; border-radius: 24rpx; padding: 28rpx 12rpx; text-align: center; transition: all 0.2s ease; }
.overview-card:active { background: #f1f5f9; }
.overview-value { display: block; font-size: 36rpx; font-weight: 800; color: #2563eb; }
.overview-label { display: block; margin-top: 10rpx; font-size: 24rpx; color: #64748b; font-weight: 500;}

/* Group list */
.ios-group { background: #ffffff; border-radius: 36rpx; overflow: hidden; margin-bottom: 32rpx; box-shadow: 0 8rpx 24rpx rgba(17, 24, 39, 0.03); }
.locked-group-shell { position: relative; margin-bottom: 32rpx; }
.locked-group-shell .ios-group { margin-bottom: 0; }
.group-locked { filter: grayscale(0.5) opacity(0.8); }

.ios-cell { display: flex; align-items: center; padding: 32rpx; background: #ffffff; position: relative; }
.ios-cell:not(:last-child)::after { content: ''; position: absolute; bottom: 0; left: 108rpx; right: 32rpx; height: 1px; background-color: #f1f5f9; }
.ios-cell:active { background-color: #f8fafc; }
.group-locked .ios-cell { pointer-events: none; }

.cell-icon { width: 64rpx; height: 64rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; margin-right: 24rpx; background: #f1f5f9;}
.bg-blue { background: #eff6ff; }
.bg-orange { background: #fff7ed; }
.bg-amber { background: #fffbeb; }
.bg-emerald { background: #ecfdf5; }

.cell-title { font-size: 30rpx; font-weight: 600; color: #1f2937; flex: 1; }

.group-lock-mask { position: absolute; left: 0; top: 0; right: 0; bottom: 0; border-radius: 36rpx; background: rgba(248, 250, 252, 0.5); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; padding: 40rpx; z-index: 10;}
.group-lock-content { width: 100%; border-radius: 32rpx; padding: 48rpx 40rpx; background: rgba(255, 255, 255, 0.95); box-shadow: 0 16rpx 40rpx rgba(17, 24, 39, 0.06); text-align: center; }
.group-lock-title { display: block; font-size: 34rpx; font-weight: 800; color: #111827; }
.group-lock-desc { display: block; margin-top: 12rpx; font-size: 26rpx; line-height: 1.6; color: #64748b; }
.group-lock-button { margin-top: 32rpx; height: 88rpx; line-height: 88rpx; border-radius: 99rpx; background: linear-gradient(135deg, #3b82f6, #2563eb); color: #fff; font-size: 30rpx; font-weight: 700; box-shadow: 0 4rpx 16rpx rgba(37, 99, 235, 0.3);}

/* Logout */
.center-align { justify-content: center; }
.logout-text { color: #ef4444; font-size: 32rpx; font-weight: 700; }
</style>
