<template>
  <view class="ios-mine">
    <!-- 个人信息头 -->
    <view class="profile-header" @click="isLoggedIn ? null : doLogin()">
      <view class="avatar-container">
        <image class="avatar-lg" :src="userInfo?.avatar ? getImageUrl(userInfo.avatar) : '/static/默认头像.png'" />
      </view>
      <view class="profile-info">
        <text class="user-name">{{ isLoggedIn ? (userInfo?.nickname || '旅行家') : '点击登录' }}</text>
        <text class="user-desc">{{ isLoggedIn ? '开启你的探索之旅' : '登录同步数据' }}</text>
      </view>
      <view class="arrow-right" v-if="!isLoggedIn">›</view>
    </view>

    <view class="profile-extra" v-if="isLoggedIn">
      <text class="profile-line">手机号：{{ userInfo?.phone || '未绑定' }}</text>
      <text class="profile-edit" @click="openEditPopup">编辑资料</text>
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
      <view class="ios-cell" @click="goDeactivate">
        <view class="cell-icon">
          <image class="cell-icon-img" src="/static/icons/注销.png" />
        </view>
        <text class="cell-title">注销账户</text>
        <text class="cell-arrow">›</text>
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
        <text class="auth-tip">如果手机号已在Web端注册，输入正确密码即可直接绑定</text>
      </view>
    </view>

    <!-- ========== 第二步：可选设置头像和昵称 ========== -->
    <view class="auth-mask" v-if="authStep === 2">
      <view class="auth-panel">
        <text class="auth-title">完善个人资料 ✨</text>
        <text class="auth-subtitle">设置你的头像和昵称，开启旅程</text>

        <!-- 头像选择 -->
        <view class="auth-avatar-wrap">
          <!-- #ifdef MP-WEIXIN -->
          <button class="auth-avatar-btn" open-type="chooseAvatar" @chooseavatar="onAuthChooseAvatar">
            <image class="auth-avatar-img" :src="authForm.avatarPreview || '/static/默认头像.png'" />
            <view class="auth-avatar-edit">
              <text class="auth-avatar-edit-text">点击选择头像</text>
            </view>
          </button>
          <!-- #endif -->
          <!-- #ifndef MP-WEIXIN -->
          <view class="auth-avatar-btn" @click="chooseAvatarFromAlbum">
            <image class="auth-avatar-img" :src="authForm.avatarPreview || '/static/默认头像.png'" />
            <view class="auth-avatar-edit">
              <text class="auth-avatar-edit-text">点击选择头像</text>
            </view>
          </view>
          <!-- #endif -->
        </view>

        <!-- 昵称输入 -->
        <!-- #ifdef MP-WEIXIN -->
        <input
          class="auth-input"
          type="nickname"
          v-model="authForm.nickname"
          placeholder="点击填入微信昵称"
          @blur="onNicknameBlur"
        />
        <!-- #endif -->
        <!-- #ifndef MP-WEIXIN -->
        <input
          class="auth-input"
          v-model="authForm.nickname"
          placeholder="请输入昵称"
          maxlength="30"
        />
        <!-- #endif -->

        <view class="auth-actions">
          <button class="auth-btn skip" @click="skipStep2">跳过</button>
          <button class="auth-btn confirm" @click="submitStep2">完成设置</button>
        </view>
      </view>
    </view>

    <!-- ========== 编辑资料弹窗 ========== -->
    <view class="edit-mask" v-if="editVisible" @click="editVisible = false">
      <view class="edit-panel" @click.stop>
        <text class="edit-title">编辑资料</text>
        <!-- 头像选择 -->
        <view class="edit-avatar-row">
          <text class="edit-avatar-label">头像</text>
          <!-- #ifdef MP-WEIXIN -->
          <button class="edit-avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
            <image class="edit-avatar-img" :src="editForm.avatarPreview || editForm.avatar || '/static/默认头像.png'" />
            <text class="edit-avatar-tip">点击更换</text>
          </button>
          <!-- #endif -->
          <!-- #ifndef MP-WEIXIN -->
          <view class="edit-avatar-btn" @click="chooseAvatarFromAlbum">
            <image class="edit-avatar-img" :src="editForm.avatarPreview || editForm.avatar || '/static/默认头像.png'" />
            <text class="edit-avatar-tip">点击更换</text>
          </view>
          <!-- #endif -->
        </view>
        <!-- #ifdef MP-WEIXIN -->
        <input class="edit-input" type="nickname" v-model="editForm.nickname" placeholder="点击填入微信昵称" maxlength="30" />
        <!-- #endif -->
        <!-- #ifndef MP-WEIXIN -->
        <input class="edit-input" v-model="editForm.nickname" placeholder="请输入昵称" maxlength="30" />
        <!-- #endif -->
        <input class="edit-input" v-model="editForm.phone" placeholder="请输入手机号（可选）" maxlength="20" />
        <view class="edit-actions">
          <button class="edit-btn cancel" @click="editVisible = false">取消</button>
          <button class="edit-btn confirm" @click="submitProfile">保存</button>
        </view>
        <view class="edit-password" @click="goPassword">
          <text class="edit-password-text">修改密码</text>
          <text class="cell-arrow">›</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { wxLogin, wxBindPhone, getUserInfo, updateUserInfo, uploadAvatar, changePassword, deactivateAccount } from '@/api/auth'
import { getImageUrl } from '@/utils/request'

const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)

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

const authForm = reactive({
  nickname: '',
  avatarPreview: '',
  avatarTempFile: ''
})


// 第二步：选择头像
const onAuthChooseAvatar = (e) => {
  const url = e.detail.avatarUrl
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

  // 验证手机号
  if (!phone) {
    uni.showToast({ title: '请输入手机号', icon: 'none' })
    return
  }
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    uni.showToast({ title: '请输入有效的手机号', icon: 'none' })
    return
  }

  // 验证密码
  if (!password) {
    uni.showToast({ title: '请设置密码', icon: 'none' })
    return
  }
  if (password.length < 6) {
    uni.showToast({ title: '密码长度至少6个字符', icon: 'none' })
    return
  }
  if (password !== confirmPassword) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return
  }

  try {
    uni.showLoading({ title: '设置中...', mask: true })

    // 调用绑定接口：传入openid + 手机号 + 密码，后端创建用户或合并已有账户
    const res = await wxBindPhone({ openid: pendingOpenid.value, phone, password })

    // 绑定/注册成功，现在才真正登录
    userStore.login(res.data)
    await syncUserInfo()
    pendingOpenid.value = ''

    uni.hideLoading()

    if (res.data.user?.isMerged) {
      // 已有账户合并成功，无需设置头像昵称，直接完成
      authStep.value = 0
      uni.showToast({ title: '账户绑定成功，欢迎回来！', icon: 'success' })
    } else {
      // 全新用户，进入第二步设置头像昵称（可跳过）
      authStep.value = 2
      uni.showToast({ title: '设置成功，可以完善资料', icon: 'none' })
    }
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e?.data?.message || '设置失败', icon: 'none' })
  }
}

// 第二步：跳过头像昵称设置
const skipStep2 = () => {
  authStep.value = 0
  uni.showToast({ title: '欢迎使用微旅！', icon: 'success' })
}

// 第二步：提交头像昵称
const submitStep2 = async () => {
  const hasAvatar = !!authForm.avatarTempFile
  const hasNickname = !!authForm.nickname.trim()

  if (!hasAvatar && !hasNickname) {
    uni.showToast({ title: '请选择头像或填入昵称', icon: 'none' })
    return
  }

  try {
    uni.showLoading({ title: '保存中...', mask: true })

    let avatarUrl = ''
    if (hasAvatar) {
      const uploadRes = await uploadAvatar(authForm.avatarTempFile)
      avatarUrl = uploadRes.data.url
    }

    const updateData = {}
    if (hasNickname) updateData.nickname = authForm.nickname.trim()
    if (avatarUrl) updateData.avatar = avatarUrl

    await updateUserInfo(updateData)
    await syncUserInfo()

    uni.hideLoading()
    authStep.value = 0
    uni.showToast({ title: '设置成功，欢迎使用微旅！', icon: 'success' })
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

// ========== 编辑资料弹窗 ==========
const editVisible = ref(false)
const editForm = reactive({
  nickname: '',
  phone: '',
  avatar: '',
  avatarPreview: '',
  avatarTempFile: ''
})

const syncUserInfo = async () => {
  try {
    const res = await getUserInfo()
    userStore.setUserInfo(res.data)
  } catch (e) {
    console.error('同步用户信息失败', e)
  }
}

// 登录
const doLogin = async () => {
  try {
    // #ifdef MP-WEIXIN
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
      authForm.nickname = ''
      authForm.avatarPreview = ''
      authForm.avatarTempFile = ''
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
    // #endif

    // #ifdef H5
    uni.showToast({ title: 'H5端暂不支持微信登录', icon: 'none' })
    // #endif
  } catch (e) {
    console.error('登录失败', e)
    uni.showToast({ title: '登录失败', icon: 'none' })
  }
}

const openEditPopup = () => {
  editForm.nickname = userInfo.value?.nickname || ''
  editForm.phone = userInfo.value?.phone || ''
  editForm.avatar = userInfo.value?.avatar ? getImageUrl(userInfo.value.avatar) : ''
  editForm.avatarPreview = ''
  editForm.avatarTempFile = ''
  editVisible.value = true
}

const submitProfile = async () => {
  try {
    let avatarUrl = ''
    if (editForm.avatarTempFile) {
      const uploadRes = await uploadAvatar(editForm.avatarTempFile)
      avatarUrl = uploadRes.data.url
    }
    const updateData = {
      nickname: editForm.nickname.trim(),
      phone: editForm.phone.trim()
    }
    if (avatarUrl) {
      updateData.avatar = avatarUrl
    }
    await updateUserInfo(updateData)
    await syncUserInfo()
    editVisible.value = false
    uni.showToast({ title: '保存成功', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

// 编辑资料 - 选择头像
const onChooseAvatar = (e) => {
  const url = e.detail.avatarUrl
  if (url) {
    editForm.avatarPreview = url
    editForm.avatarTempFile = url
  }
}

// 非微信平台从相册选择头像
const chooseAvatarFromAlbum = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      const tempFilePath = res.tempFilePaths[0]
      // 判断当前哪个弹窗打开
      if (authStep.value === 2) {
        authForm.avatarPreview = tempFilePath
        authForm.avatarTempFile = tempFilePath
      } else {
        editForm.avatarPreview = tempFilePath
        editForm.avatarTempFile = tempFilePath
      }
    }
  })
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

// 跳转修改密码
const goPassword = () => {
  editVisible.value = false
  uni.navigateTo({ url: '/pages/mine/password' })
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

// 跳转注销账户
const goDeactivate = () => {
  uni.navigateTo({ url: '/pages/mine/deactivate' })
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
  margin-bottom: 16rpx;
}

.profile-extra {
  margin-bottom: 36rpx;
  padding-left: 172rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.profile-line {
  font-size: 26rpx;
  color: #8E8E93;
}

.profile-edit {
  font-size: 26rpx;
  color: #007AFF;
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

/* ========== 编辑资料弹窗 ========== */
.edit-mask {
  position: fixed;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}

.edit-panel {
  width: 100%;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 36rpx 32rpx calc(36rpx + env(safe-area-inset-bottom));
}

.edit-title {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  margin-bottom: 24rpx;
}

.edit-avatar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
  padding: 0 8rpx;
}

.edit-avatar-label {
  font-size: 30rpx;
  color: #333;
}

.edit-avatar-btn {
  display: flex;
  align-items: center;
  background: transparent;
  border: none;
  padding: 0;
  margin: 0;
  line-height: normal;
  font-size: inherit;
}

.edit-avatar-btn::after {
  border: none;
}

.edit-avatar-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 2rpx solid #E5E5EA;
}

.edit-avatar-tip {
  font-size: 24rpx;
  color: #8E8E93;
  margin-left: 16rpx;
}

.edit-input {
  height: 84rpx;
  border-radius: 16rpx;
  background: #F2F2F7;
  padding: 0 24rpx;
  margin-bottom: 20rpx;
  font-size: 30rpx;
}

.edit-actions {
  display: flex;
  gap: 16rpx;
}

.edit-btn {
  flex: 1;
  border-radius: 16rpx;
  font-size: 30rpx;
}

.edit-btn.cancel {
  color: #666;
  background: #f0f0f0;
}

.edit-btn.confirm {
  color: #fff;
  background: #007AFF;
}

.edit-password {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 1px solid #E5E5EA;
}

.edit-password-text {
  font-size: 30rpx;
  color: #007AFF;
}
</style>
