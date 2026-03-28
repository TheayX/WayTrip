<template>
  <view class="ios-mine">
    <view class="profile-hero" :class="{ guest: !isLoggedIn }" @click="!isLoggedIn ? doLogin() : null">
      <view class="profile-header">
        <view class="avatar-container">
          <image class="avatar-lg" :src="getAvatarUrl(userInfo?.avatar)" />
        </view>
        <view class="profile-info">
          <text class="user-name">{{ isLoggedIn ? (userInfo?.nickname || '旅行家') : '旅程从登录开始' }}</text>
          <text class="user-desc">{{ isLoggedIn ? '开启你的探索之旅' : '登录后同步订单、收藏、评价和推荐偏好' }}</text>
        </view>
        <view class="hero-action" v-if="!isLoggedIn">
          <text class="hero-action-text">立即登录</text>
          <text class="arrow-right">›</text>
        </view>
      </view>

      <view class="profile-extra" v-if="isLoggedIn">
        <text class="profile-line">手机号：{{ formatPhone(userInfo?.phone) }}</text>
        <text class="profile-line subtle">账号资料与安全设置已整理到下方账户区域</text>
      </view>

    </view>

    <view class="stats-board" v-if="isLoggedIn">
      <view class="stats-item" @click="goActivity('browse')">
        <text class="stats-value">{{ dashboardStats.viewed }}</text>
        <text class="stats-label">最近浏览</text>
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

    <view class="order-overview" v-if="isLoggedIn">
      <view class="overview-header">
        <text class="overview-title">订单状态</text>
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

    <view class="footprint-section" v-if="isLoggedIn && recentFootprints.length">
      <view class="overview-header">
        <text class="overview-title">最近浏览</text>
        <text class="overview-link" @click="goActivity('browse')">查看全部</text>
      </view>
      <scroll-view class="footprint-scroll" scroll-x :show-scrollbar="false">
        <view
          v-for="item in recentFootprints"
          :key="item.id"
          class="footprint-card"
          @click="goSpotById(item.id)"
        >
          <image class="footprint-image" :src="getImageUrl(item.coverImage)" mode="aspectFill" />
          <text class="footprint-name">{{ item.name }}</text>
          <text class="footprint-meta">{{ item.regionName || '景点' }}</text>
        </view>
      </scroll-view>
    </view>

    <view class="section-label">无需登录</view>
    <view class="ios-group">
      <view class="ios-cell" @click="goSettings">
        <view class="cell-icon">
          <image class="cell-icon-img" src="/static/icons/service.png" />
        </view>
        <text class="cell-title">设置</text>
        <text class="cell-arrow">›</text>
      </view>
    </view>

    <view class="section-label">登录后使用</view>
    <view class="locked-group-shell">
      <view class="ios-group" :class="{ 'group-locked': !isLoggedIn }">
        <view class="ios-cell" @click="goOrders">
          <view class="cell-icon">
            <image class="cell-icon-img" src="/static/icons/order.png" />
          </view>
          <text class="cell-title">我的订单</text>
          <text class="cell-arrow">›</text>
        </view>
        <view class="ios-cell" @click="goActivity()">
          <view class="cell-icon">
            <image class="cell-icon-img" src="/static/icons/review.png" />
          </view>
          <text class="cell-title">我的互动</text>
          <text class="cell-arrow">›</text>
        </view>
        <view class="ios-cell" @click="goPreference">
          <view class="cell-icon">
            <image class="cell-icon-img" src="/static/icons/preference.png" />
          </view>
          <text class="cell-title">偏好设置</text>
          <text class="cell-arrow">›</text>
        </view>
        <view class="ios-cell" @click="goProfile">
          <view class="cell-icon">
            <image class="cell-icon-img" src="/static/icons/service.png" />
          </view>
          <text class="cell-title">账号资料</text>
          <text class="cell-arrow">›</text>
        </view>
      </view>

      <view class="group-lock-mask" v-if="!isLoggedIn" @click="doLogin">
        <view class="group-lock-content">
          <text class="group-lock-title">登录后管理你的旅行资产</text>
          <text class="group-lock-desc">订单、收藏、评价、偏好和资料编辑都会集中在这里</text>
          <view class="group-lock-button">微信一键登录</view>
        </view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="ios-group logout-group" v-if="isLoggedIn">
      <view class="ios-cell center-align" @click="doLogout">
        <text class="logout-text">退出登录</text>
      </view>
    </view>

    <!-- ========== 第一步：强制设置手机号和密码 ========== -->
    <view class="auth-mask" v-if="authStep === 1">
      <view class="auth-panel">
        <text class="auth-title">欢迎来到微旅 🎉</text>
        <text class="auth-subtitle">设置手机号和密码保护账户</text>

        <!-- 手机号 -->
        <input
          class="auth-input"
          type="tel"
          v-model="step1Form.phone"
          placeholder="请输入手机号"
          maxlength="11"
        />

        <!-- 密码 -->
        <view class="auth-input-wrap">
          <input
            class="auth-input-field"
            :type="step1PwdVisible ? 'text' : 'password'"
            :password="!step1PwdVisible"
            v-model="step1Form.password"
            placeholder="设置密码（至少6位）"
            maxlength="50"
          />
          <text class="pwd-eye" @click="step1PwdVisible = !step1PwdVisible">{{ step1PwdVisible ? '🙈' : '👁' }}</text>
        </view>

        <!-- 确认密码 -->
        <view class="auth-input-wrap">
          <input
            class="auth-input-field"
            :type="step1ConfirmPwdVisible ? 'text' : 'password'"
            :password="!step1ConfirmPwdVisible"
            v-model="step1Form.confirmPassword"
            placeholder="确认密码"
            maxlength="50"
          />
          <text class="pwd-eye" @click="step1ConfirmPwdVisible = !step1ConfirmPwdVisible">{{ step1ConfirmPwdVisible ? '🙈' : '👁' }}</text>
        </view>

        <view class="auth-actions">
          <button class="auth-btn confirm full" @click="submitStep1">下一步</button>
        </view>
        <text v-if="step1Error" class="auth-error">{{ step1Error }}</text>
        <text class="auth-tip">如果手机号已在Web端注册，输入正确密码即可直接绑定</text>
      </view>
    </view>

    <!-- ========== 第二步：可选设置头像和昵称 ========== -->
    <view class="auth-mask" v-if="authStep === 2">
      <view class="auth-panel">
        <text class="auth-title">完成注册 ✨</text>
        <text class="auth-subtitle">设置头像和昵称，或直接跳过完成注册</text>

        <!-- 头像选择 -->
        <view class="auth-avatar-wrap">
          <button class="auth-avatar-btn" open-type="chooseAvatar" @chooseavatar="onAuthChooseAvatar">
            <image class="auth-avatar-img" :src="authForm.avatarPreview || defaultRegisterAvatar" />
            <view class="auth-avatar-edit">
              <text class="auth-avatar-edit-text">点击选择头像</text>
            </view>
          </button>
        </view>

        <!-- 昵称输入 -->
        <input
          class="auth-input"
          type="nickname"
          v-model="authForm.nickname"
          placeholder="点击填入微信昵称"
          @blur="onNicknameBlur"
        />

        <view class="auth-actions">
          <button class="auth-btn skip" @click="skipStep2">跳过</button>
          <button class="auth-btn confirm" @click="submitStep2">完成注册</button>
        </view>
      </view>
    </view>

    <view class="auth-mask" v-if="preferenceGuideVisible">
      <view class="preference-guide-panel">
        <PreferenceCategorySelector
          v-model="preferenceGuideSelection"
          :categories="preferenceGuideCategories"
          eyebrow="冷启动推荐"
          title="选择你感兴趣的景点分类"
          subtitle="现在选好偏好，推荐会立刻从热门兜底切到偏好冷启动；也可以先跳过。"
          primary-text="立即开启"
          secondary-text="跳过"
          @submit="saveRegisterPreferences"
          @secondary="skipRegisterPreferences"
          @limit-exceed="handlePreferenceLimitExceed"
        />
      </view>
    </view>

  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getFavoriteList } from '@/api/favorite'
import { getOrderList } from '@/api/order'
import { getMyReviews } from '@/api/review'
import { wxLogin, wxBindPhone, prepareWxBindPhone, getUserInfo, updateUserInfo, uploadAvatar, updatePreferences } from '@/api/auth'
import { getFilters } from '@/api/spot'
import {
  markColdStartGuideCompleted,
  markColdStartGuidePending,
  markColdStartGuideSkipped
} from '@/utils/cold-start-guide'
import PreferenceCategorySelector from '@/components/PreferenceCategorySelector.vue'
import { getAvatarUrl, getImageUrl } from '@/utils/request'

const userStore = useUserStore()
const defaultRegisterAvatar = getAvatarUrl('/uploads/images/avatar.jpg')
const defaultRegisterNickname = '微信用户'

const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)
const dashboardStats = reactive({
  viewed: 0,
  favorites: 0,
  reviews: 0
})
const orderStats = reactive({
  pending: 0,
  paid: 0,
  completed: 0
})
const recentFootprints = ref([])

const formatPhone = (phone) => {
  if (!phone || !phone.trim()) return '未绑定'
  const normalized = phone.trim()
  if (/^1\d{10}$/.test(normalized)) {
    return `${normalized.slice(0, 3)}****${normalized.slice(7)}`
  }
  if (/^1\d{2}\*{4}\d{4}$/.test(normalized)) {
    return normalized
  }
  return '已隐藏'
}

// ========== 新用户两步授权流程 ==========
const authStep = ref(0) // 0: 未开始, 1: 设置手机号密码, 2: 设置头像昵称（可跳过）
const pendingOpenid = ref('') // 临时存储新用户的openid（尚未创建用户）
const step1Form = reactive({
  phone: '',
  password: '',
  confirmPassword: ''
})
const step1PwdVisible = ref(false)
const step1ConfirmPwdVisible = ref(false)
const step1Error = ref('')

const authForm = reactive({
  nickname: defaultRegisterNickname,
  avatarPreview: '',
  avatarTempFile: ''
})

const pendingRegister = reactive({
  phone: '',
  password: ''
})
const preferenceGuideVisible = ref(false)
const preferenceGuideCategories = ref([])
const preferenceGuideSelection = ref([])

const onAuthChooseAvatar = (e) => {
  const url = e?.detail?.avatarUrl || ''
  if (url) {
    authForm.avatarPreview = url
    authForm.avatarTempFile = url
  }
}

// 昵称输入
const onNicknameBlur = (e) => {
  if (e.detail?.value) {
    authForm.nickname = e.detail.value
  }
}

// 第一步：提交手机号和密码（核心绑定/注册逻辑）
const submitStep1 = async () => {
  const phone = step1Form.phone.trim()
  const password = step1Form.password.trim()
  const confirmPassword = step1Form.confirmPassword.trim()
  step1Error.value = ''

  // 验证手机号
  if (!phone) {
    step1Error.value = '请输入手机号'
    return
  }
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    step1Error.value = '请输入有效的手机号'
    return
  }

  // 验证密码
  if (!password) {
    step1Error.value = '请设置密码'
    return
  }
  if (password.length < 6) {
    step1Error.value = '密码长度至少6个字符'
    return
  }
  if (password !== confirmPassword) {
    step1Error.value = '两次输入的密码不一致'
    return
  }

  try {
    uni.showLoading({ title: '设置中...', mask: true })

    // 第一步只做校验：
    // 已有账户直接合并登录；新用户仅通过校验，不立即创建账户。
    const res = await prepareWxBindPhone({ openid: pendingOpenid.value, phone, password })

    uni.hideLoading()

    if (res.data?.completed && res.data?.login) {
      userStore.login(res.data.login)
      await syncUserInfo()
      pendingOpenid.value = ''
      pendingRegister.phone = ''
      pendingRegister.password = ''
      authStep.value = 0
      uni.showToast({ title: '账户绑定成功，欢迎回来！', icon: 'success' })
    } else {
      pendingRegister.phone = phone
      pendingRegister.password = password
      authForm.nickname = defaultRegisterNickname
      authForm.avatarPreview = ''
      authForm.avatarTempFile = ''
      authStep.value = 2
      uni.showToast({ title: '校验通过', icon: 'success' })
    }
  } catch (e) {
    uni.hideLoading()
    step1Error.value = e?.data?.message || '校验失败，请检查手机号或密码'
  }
}

const finalizeRegister = async () => {
  const res = await wxBindPhone({
    openid: pendingOpenid.value,
    phone: pendingRegister.phone,
    password: pendingRegister.password
  })
  userStore.login(res.data)
  markColdStartGuidePending(res.data?.user?.id)
  await syncUserInfo()
  pendingOpenid.value = ''
  pendingRegister.phone = ''
  pendingRegister.password = ''
}

const fetchPreferenceGuideCategories = async () => {
  if (preferenceGuideCategories.value.length) return
  try {
    const res = await getFilters()
    preferenceGuideCategories.value = res.data?.categories || []
  } catch (e) {
    console.error('获取偏好分类失败', e)
  }
}

const openPreferenceGuide = async () => {
  await fetchPreferenceGuideCategories()
  preferenceGuideSelection.value = [...(userStore.userInfo?.preferenceCategoryIds || [])]
  preferenceGuideVisible.value = true
}

const handlePreferenceLimitExceed = () => {
  uni.showToast({ title: '最多选择5个', icon: 'none' })
}

const saveRegisterPreferences = async () => {
  try {
    await updatePreferences({ categoryIds: preferenceGuideSelection.value })
    const categoryNames = preferenceGuideSelection.value
      .map(id => preferenceGuideCategories.value.find(cat => cat.id === id)?.name)
      .filter(Boolean)
    userStore.updatePreferences({
      preferences: categoryNames,
      preferenceCategoryIds: [...preferenceGuideSelection.value],
      preferenceCategoryNames: categoryNames
    })
    markColdStartGuideCompleted(userStore.userInfo?.id)
    preferenceGuideVisible.value = false
    uni.showToast({ title: '偏好已生效，开始为你推荐', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '保存偏好失败', icon: 'none' })
  }
}

const skipRegisterPreferences = () => {
  markColdStartGuideSkipped(userStore.userInfo?.id)
  preferenceGuideVisible.value = false
  uni.showToast({ title: '已跳过，后续可在我的-偏好设置里设置', icon: 'none' })
}

// 第二步：跳过头像昵称设置
const skipStep2 = async () => {
  try {
    uni.showLoading({ title: '注册中...', mask: true })
    await finalizeRegister()
    uni.hideLoading()
    authStep.value = 0
    await openPreferenceGuide()
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e?.data?.message || '注册失败', icon: 'none' })
  }
}

// 第二步：提交头像昵称
const submitStep2 = async () => {
  try {
    uni.showLoading({ title: '保存中...', mask: true })

    await finalizeRegister()

    const hasAvatar = !!authForm.avatarTempFile
    const nickname = authForm.nickname.trim() || defaultRegisterNickname
    let avatarUrl = ''
    if (hasAvatar) {
      const uploadRes = await uploadAvatar(authForm.avatarTempFile)
      avatarUrl = uploadRes.data.url
    }

    const updateData = {
      nickname
    }
    if (avatarUrl) updateData.avatar = avatarUrl

    await updateUserInfo(updateData)
    await syncUserInfo()

    uni.hideLoading()
    authStep.value = 0
    await openPreferenceGuide()
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

const syncUserInfo = async () => {
  try {
    const res = await getUserInfo()
    userStore.setUserInfo(res.data)
  } catch (e) {
    console.error('同步用户信息失败', e)
  }
}

const loadRecentFootprints = () => {
  const history = uni.getStorageSync('spot_footprints')
  const footprints = Array.isArray(history) ? history : []
  recentFootprints.value = footprints.slice(0, 6)
  dashboardStats.viewed = footprints.length
}

const loadMineOverview = async () => {
  if (!isLoggedIn.value) {
    dashboardStats.viewed = 0
    dashboardStats.favorites = 0
    dashboardStats.reviews = 0
    orderStats.pending = 0
    orderStats.paid = 0
    orderStats.completed = 0
    recentFootprints.value = []
    return
  }

  loadRecentFootprints()

  try {
    const [favoriteRes, reviewRes, pendingRes, paidRes, completedRes] = await Promise.all([
      getFavoriteList(1, 1),
      getMyReviews(1, 1),
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

// 登录
const doLogin = async () => {
  try {
    const loginRes = await uni.login({ provider: 'weixin' })
    const res = await wxLogin(loginRes.code)

    if (res.data.isNewUser) {
      // 新用户：后端没有创建用户，只返回了openid
      // 临时存储openid，弹出手机号密码表单
      pendingOpenid.value = res.data.openid
      step1Form.phone = ''
      step1Form.password = ''
      step1Form.confirmPassword = ''
      step1PwdVisible.value = false
      step1ConfirmPwdVisible.value = false
      step1Error.value = ''
      authForm.nickname = defaultRegisterNickname
      authForm.avatarPreview = ''
      authForm.avatarTempFile = ''
      pendingRegister.phone = ''
      pendingRegister.password = ''
      authStep.value = 1 // 弹出第一步：手机号密码
    } else {
      // 老用户：直接登录
      userStore.login(res.data)
      await syncUserInfo()

      if (res.data.isReactivated) {
        uni.showModal({
          title: '账户已恢复',
          content: '欢迎回来！你的账户已恢复，可以继续使用微旅了。',
          showCancel: false,
          confirmText: '确认'
        })
      } else {
        uni.showToast({ title: '登录成功', icon: 'success' })
      }
    }

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
        dashboardStats.viewed = 0
        dashboardStats.favorites = 0
        dashboardStats.reviews = 0
        orderStats.pending = 0
        orderStats.paid = 0
        orderStats.completed = 0
        recentFootprints.value = []
        uni.showToast({ title: '已退出登录', icon: 'none' })
      }
    }
  })
}

// 跳转订单
const goOrders = () => {
  uni.navigateTo({ url: '/pages/order/list' })
}

const goOrdersByStatus = (status) => {
  uni.navigateTo({ url: `/pages/order/list?status=${status}` })
}

// 跳转收藏
const goActivity = (tab = 'browse') => {
  uni.navigateTo({ url: `/pages/mine/activity?tab=${tab}` })
}

// 跳转偏好设置
const goPreference = () => {
  uni.navigateTo({ url: '/pages/mine/preference' })
}

const goProfile = () => {
  uni.navigateTo({ url: '/pages/mine/profile' })
}

const goSettings = () => {
  uni.navigateTo({ url: '/pages/mine/settings/index' })
}

const goSpotById = (id) => {
  uni.navigateTo({ url: `/pages/spot/detail?id=${id}&source=footprint` })
}

onShow(async () => {
  if (isLoggedIn.value) {
    await syncUserInfo()
  }
  await loadMineOverview()
})
</script>

<style scoped>
.ios-mine {
  background-color: #F2F2F7;
  min-height: 100vh;
  padding: 20rpx 32rpx;
  padding-top: 120rpx;
}

.profile-hero {
  margin-bottom: 24rpx;
  padding: 32rpx;
  border-radius: 32rpx;
  background:
    linear-gradient(135deg, rgba(16, 46, 70, 0.95), rgba(39, 86, 120, 0.9)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0));
  box-shadow: 0 16rpx 44rpx rgba(15, 23, 42, 0.12);
}

.profile-hero.guest {
  background:
    linear-gradient(135deg, rgba(195, 117, 63, 0.95), rgba(230, 170, 106, 0.92)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0));
}

.profile-header {
  display: flex;
  align-items: center;
}

.profile-extra {
  margin-top: 24rpx;
  padding-left: 172rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.profile-line {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

.profile-line.subtle {
  color: rgba(255, 255, 255, 0.62);
}

.stats-board {
  display: flex;
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx 16rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.02);
}

.stats-item {
  flex: 1;
  text-align: center;
}

.stats-value {
  display: block;
  font-size: 38rpx;
  font-weight: 700;
  color: #111827;
}

.stats-label {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8E8E93;
}

.order-overview,
.footprint-section {
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.02);
}

.overview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.overview-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.overview-link {
  font-size: 24rpx;
  color: #007AFF;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
}

.overview-card {
  background: #F2F6FC;
  border-radius: 20rpx;
  padding: 24rpx 12rpx;
  text-align: center;
}

.overview-value {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #007AFF;
}

.overview-label {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #4B5563;
}

.footprint-scroll {
  white-space: nowrap;
}

.footprint-card {
  display: inline-block;
  width: 220rpx;
  margin-right: 16rpx;
}

.footprint-image {
  width: 220rpx;
  height: 160rpx;
  border-radius: 18rpx;
  background: #E5E7EB;
}

.footprint-name {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.footprint-meta {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #8E8E93;
}

.avatar-lg {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 8rpx 20rpx rgba(0,0,0,0.12);
}

.profile-info {
  margin-left: 32rpx;
  flex: 1;
}

.user-name {
  font-size: 44rpx;
  font-weight: 700;
  color: #fff;
  display: block;
  margin-bottom: 8rpx;
}

.user-desc {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.76);
}

.hero-action {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 18rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
  flex-shrink: 0;
}

.hero-action-text {
  font-size: 24rpx;
  color: #fff;
  font-weight: 600;
}

.arrow-right {
  font-size: 40rpx;
  color: rgba(255, 255, 255, 0.72);
}

.section-label {
  margin: 28rpx 6rpx 18rpx;
  font-size: 24rpx;
  font-weight: 600;
  letter-spacing: 2rpx;
  color: #8E8E93;
}

/* 菜单组 - iOS Inset Grouped 风格 */
.ios-group {
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 32rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.02);
}

.locked-group-shell {
  position: relative;
  margin-bottom: 32rpx;
}

.locked-group-shell .ios-group {
  margin-bottom: 0;
}

.group-locked {
  filter: saturate(0.7);
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

.group-locked .ios-cell {
  pointer-events: none;
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

.group-lock-mask {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  border-radius: 24rpx;
  background: linear-gradient(180deg, rgba(242, 242, 247, 0.08), rgba(242, 242, 247, 0.74));
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28rpx;
}

.group-lock-content {
  width: 100%;
  border-radius: 28rpx;
  padding: 32rpx 28rpx;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.08);
  text-align: center;
}

.group-lock-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: #111827;
}

.group-lock-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #6B7280;
}

.group-lock-button {
  margin-top: 24rpx;
  height: 78rpx;
  line-height: 78rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #D9822B, #E8A255);
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
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

/* ========== 新用户授权弹窗 ========== */
.auth-mask {
  position: fixed;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.auth-panel {
  width: 600rpx;
  background: #fff;
  border-radius: 32rpx;
  padding: 48rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.preference-guide-panel {
  width: 640rpx;
  background: #fff;
  border-radius: 32rpx;
  padding: 48rpx 36rpx;
}

.auth-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #000;
  margin-bottom: 12rpx;
}

.auth-subtitle {
  font-size: 26rpx;
  color: #8E8E93;
  margin-bottom: 40rpx;
}

.auth-avatar-wrap {
  margin-bottom: 32rpx;
}

.auth-avatar-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: transparent;
  border: none;
  padding: 0;
  margin: 0;
  line-height: normal;
}

.auth-avatar-btn::after {
  border: none;
}

.auth-avatar-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  border: 4rpx solid #E5E5EA;
}

.auth-avatar-edit {
  margin-top: 12rpx;
}

.auth-avatar-edit-text {
  font-size: 24rpx;
  color: #007AFF;
}

.auth-input {
  width: 100%;
  height: 84rpx;
  border-radius: 16rpx;
  background: #F2F2F7;
  padding: 0 24rpx;
  margin-bottom: 16rpx;
  font-size: 30rpx;
  text-align: center;
}

.auth-input-wrap {
  width: 100%;
  height: 84rpx;
  border-radius: 16rpx;
  background: #F2F2F7;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  margin-bottom: 16rpx;
}

.auth-input-field {
  flex: 1;
  height: 84rpx;
  font-size: 30rpx;
  text-align: center;
}

.pwd-eye {
  font-size: 40rpx;
  padding: 0 8rpx;
  flex-shrink: 0;
}

.auth-actions {
  display: flex;
  gap: 16rpx;
  width: 100%;
}

.auth-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 16rpx;
  font-size: 30rpx;
  text-align: center;
}

.auth-btn.skip {
  color: #666;
  background: #F2F2F7;
}

.auth-btn.confirm {
  color: #fff;
  background: #007AFF;
}

.auth-btn.confirm.full {
  flex: 1;
  width: 100%;
}

.auth-tip {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #8E8E93;
  text-align: center;
}

.auth-error {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #FF3B30;
  text-align: center;
}

</style>
