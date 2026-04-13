<!-- 账号资料页 -->
<template>
  <view class="profile-page">
    <view class="hero-card">
      <text class="hero-title">账号资料</text>
      <text class="hero-subtitle">更新头像、昵称与联系方式，保持你的旅行身份信息完整。</text>
    </view>

    <view class="profile-card">
      <text class="card-title">基本资料</text>

      <view class="avatar-section">
        <text class="field-label">头像</text>
        <button class="avatar-button" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
          <image class="avatar-image" :src="getAvatarUrl(form.avatarPreview || form.avatar)" />
          <text class="avatar-tip">点击更换头像</text>
        </button>
      </view>

      <view class="field-block">
        <text class="field-label">昵称</text>
        <input
          class="field-input"
          type="nickname"
          v-model="form.nickname"
          placeholder="请输入昵称"
          maxlength="30"
        />
      </view>

      <view class="field-block">
        <text class="field-label">手机号</text>
        <input
          class="field-input"
          v-model="form.phone"
          placeholder="请输入手机号（可选）"
          maxlength="20"
        />
      </view>

      <button class="save-button" @click="submitProfile">保存资料</button>
    </view>

    <!-- 账户安全区域 -->
    <view class="profile-card">
      <text class="card-title">账户安全</text>
      <view class="link-cell" @click="goPassword">
        <text class="link-title">修改密码</text>
        <text class="link-desc">更新登录密码，保护账户安全</text>
        <text class="link-arrow">›</text>
      </view>
      <view class="link-cell danger-cell" @click="goDeactivate">
        <text class="link-title danger-text">注销账户</text>
        <text class="link-desc">注销后账号会停用，后续可通过同一微信重新登录恢复</text>
        <text class="link-arrow danger-text">›</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getUserInfo, updateUserInfo, uploadAvatar } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { getAvatarUrl } from '@/utils/request'

// 基础依赖与用户状态
const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)

// 页面数据状态
const form = reactive({
  nickname: '',
  phone: '',
  avatar: '',
  avatarPreview: '',
  avatarTempFile: ''
})

// 数据加载方法
const syncUserInfo = async () => {
  const res = await getUserInfo()
  userStore.setUserInfo(res.data)
}

const hydrateForm = () => {
  form.nickname = userInfo.value?.nickname || ''
  form.phone = userInfo.value?.phone || ''
  form.avatar = getAvatarUrl(userInfo.value?.avatar)
  form.avatarPreview = ''
  form.avatarTempFile = ''
}

const ensureLogin = () => {
  if (isLoggedIn.value) return true
  uni.showToast({ title: '请先登录', icon: 'none' })
  setTimeout(() => {
    uni.navigateBack({ delta: 1 })
  }, 300)
  return false
}

// 交互处理方法
const onChooseAvatar = (e) => {
  const url = e?.detail?.avatarUrl || ''
  if (!url) return
  form.avatarPreview = url
  form.avatarTempFile = url
}

const submitProfile = async () => {
  try {
    uni.showLoading({ title: '保存中...', mask: true })
    let avatarUrl = ''
    if (form.avatarTempFile) {
      const uploadRes = await uploadAvatar(form.avatarTempFile)
      avatarUrl = uploadRes.data.url
    }
    const payload = {
      nickname: form.nickname.trim(),
      phone: form.phone.trim()
    }
    if (avatarUrl) {
      payload.avatar = avatarUrl
    }
    await updateUserInfo(payload)
    await syncUserInfo()
    hydrateForm()
    uni.hideLoading()
    uni.showToast({ title: '保存成功', icon: 'success' })
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

// 页面跳转方法
const goPassword = () => {
  uni.navigateTo({ url: '/pages/mine/password' })
}

const goDeactivate = () => {
  uni.navigateTo({ url: '/pages/mine/deactivate' })
}

// 生命周期
onShow(async () => {
  if (!ensureLogin()) return
  try {
    await syncUserInfo()
    hydrateForm()
  } catch (e) {
    uni.showToast({ title: '加载资料失败', icon: 'none' })
  }
})
</script>

<style scoped>
.profile-page {
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

.profile-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1rpx solid rgba(255, 255, 255, 0.84);
  border-radius: 40rpx;
  padding: 32rpx 28rpx;
  box-shadow:
    0 18rpx 48rpx rgba(15, 23, 42, 0.08),
    inset 0 1rpx 0 rgba(255, 255, 255, 0.82);
}

.profile-card + .profile-card {
  margin-top: 24rpx;
}

.card-title {
  display: block;
  margin-bottom: 24rpx;
  font-size: 32rpx;
  font-weight: 700;
  color: #111827;
}

.avatar-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.avatar-button {
  display: flex;
  align-items: center;
  background: transparent;
  border: none;
  padding: 0;
  margin: 0;
  line-height: normal;
}

.avatar-button::after {
  border: none;
}

.avatar-image {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 2rpx solid #E5E7EB;
}

.avatar-tip {
  margin-left: 16rpx;
  font-size: 24rpx;
  color: #6B7280;
}

.field-block {
  margin-bottom: 24rpx;
}

.field-label {
  display: block;
  margin-bottom: 14rpx;
  font-size: 26rpx;
  color: #6B7280;
}

.field-input {
  height: 88rpx;
  padding: 0 24rpx;
  border-radius: 18rpx;
  background: #F3F4F6;
  font-size: 30rpx;
  color: #111827;
}

.save-button {
  margin-top: 8rpx;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 18rpx;
  background: #18181b;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
}

.link-cell {
  position: relative;
  padding-right: 36rpx;
}

.link-cell + .link-cell {
  margin-top: 28rpx;
  padding-top: 28rpx;
  border-top: 1rpx solid #EDF2F7;
}

.link-title {
  display: block;
  font-size: 30rpx;
  color: #111827;
  font-weight: 600;
}

.link-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #6B7280;
}

.link-arrow {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  font-size: 32rpx;
  color: #9CA3AF;
}

.danger-cell .link-desc,
.danger-text {
  color: #DC2626;
}
</style>
