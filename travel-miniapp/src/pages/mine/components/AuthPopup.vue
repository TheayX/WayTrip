<template>
  <view>
    <!-- ========== 第一步：强制设置手机号和密码 ========== -->
    <view class="auth-mask" v-if="step === 1">
      <view class="auth-panel">
        <image class="auth-brand-mark" src="/static/brand/standard-mark.svg" mode="aspectFit" />
        <view class="auth-head">
          <text class="auth-kicker">WayTrip Account</text>
          <text class="auth-title">完成账号设置</text>
          <text class="auth-subtitle">绑定手机号并设置密码，后续可直接使用同一账户登录。</text>
        </view>

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
          <text class="pwd-toggle" @click="step1PwdVisible = !step1PwdVisible">{{ step1PwdVisible ? '隐藏' : '显示' }}</text>
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
          <text class="pwd-toggle" @click="step1ConfirmPwdVisible = !step1ConfirmPwdVisible">{{ step1ConfirmPwdVisible ? '隐藏' : '显示' }}</text>
        </view>

        <view class="auth-actions">
          <button class="auth-btn confirm full" @click="submitStep1">下一步</button>
        </view>
        <text v-if="step1Error" class="auth-error">{{ step1Error }}</text>
        <text class="auth-tip">如果手机号已在 Web 端注册，输入正确密码后会直接完成绑定。</text>
      </view>
    </view>

    <!-- ========== 第二步：可选设置头像和昵称 ========== -->
    <view class="auth-mask" v-if="step === 2">
      <view class="auth-panel">
        <image class="auth-brand-mark" src="/static/brand/standard-mark.svg" mode="aspectFit" />
        <view class="auth-head">
          <text class="auth-kicker">WayTrip Profile</text>
          <text class="auth-title">完善个人资料</text>
          <text class="auth-subtitle">设置头像和昵称，或直接跳过，稍后也可以在账号资料里继续修改。</text>
        </view>

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

    <!-- 偏好设置弹层 -->
    <view class="auth-mask" v-if="preferenceGuideVisible">
      <view class="preference-guide-panel">
        <PreferenceCategorySelector
          v-model="preferenceGuideSelection"
          :categories="preferenceGuideCategories"
          eyebrow="偏好冷启动"
          title="选择你感兴趣的景点分类"
          subtitle="现在选好偏好，推荐会立刻从热门冷启动切到偏好冷启动；也可以先跳过。"
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
import { ref, reactive, watch } from 'vue'
import { wxBindPhone, prepareWxBindPhone } from '@/api/auth'
import { updateUserInfo, uploadAvatar, setPreferences } from '@/api/user'
import { getFilters } from '@/api/spot'
import { useUserStore } from '@/stores/user'
import { getAvatarUrl } from '@/utils/request'
import PreferenceCategorySelector from '@/components/PreferenceCategorySelector.vue'
import {
  markColdStartGuideCompleted,
  markColdStartGuidePending,
  markColdStartGuideSkipped
} from '@/utils/cold-start-guide'

const props = defineProps({
  visibleStep: { type: Number, default: 0 },
  openid: { type: String, default: '' }
})

const emit = defineEmits(['update:visibleStep', 'success'])

const step = ref(props.visibleStep)
watch(() => props.visibleStep, (val) => step.value = val)

const userStore = useUserStore()
const defaultRegisterAvatar = getAvatarUrl('/uploads/avatar/avatar.jpg')
const defaultRegisterNickname = '微信用户'

const step1Form = reactive({ phone: '', password: '', confirmPassword: '' })
const step1PwdVisible = ref(false)
const step1ConfirmPwdVisible = ref(false)
const step1Error = ref('')

const authForm = reactive({ nickname: defaultRegisterNickname, avatarPreview: '', avatarTempFile: '' })
const pendingRegister = reactive({ phone: '', password: '' })

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

const onNicknameBlur = (e) => {
  if (e.detail?.value) authForm.nickname = e.detail.value
}

const submitStep1 = async () => {
  const phone = step1Form.phone.trim()
  const password = step1Form.password.trim()
  const confirmPassword = step1Form.confirmPassword.trim()
  step1Error.value = ''

  if (!phone) { step1Error.value = '请输入手机号'; return }
  if (!/^1[3-9]\d{9}$/.test(phone)) { step1Error.value = '请输入有效的手机号'; return }
  if (!password) { step1Error.value = '请设置密码'; return }
  if (password.length < 6) { step1Error.value = '密码长度至少6个字符'; return }
  if (password !== confirmPassword) { step1Error.value = '两次输入的密码不一致'; return }

  try {
    uni.showLoading({ title: '设置中...', mask: true })
    const res = await prepareWxBindPhone({ openid: props.openid, phone, password })
    uni.hideLoading()

    if (res.data?.completed && res.data?.login) {
      userStore.login(res.data.login)
      emit('success')
      emit('update:visibleStep', 0)
      uni.showToast({ title: '账户绑定成功，欢迎回来！', icon: 'success' })
    } else {
      pendingRegister.phone = phone
      pendingRegister.password = password
      emit('update:visibleStep', 2)
      uni.showToast({ title: '校验通过', icon: 'success' })
    }
  } catch (e) {
    uni.hideLoading()
    step1Error.value = e?.data?.message || '校验失败，请检查手机号或密码'
  }
}

const finalizeRegister = async () => {
  const res = await wxBindPhone({
    openid: props.openid,
    phone: pendingRegister.phone,
    password: pendingRegister.password
  })
  userStore.login(res.data)
  markColdStartGuidePending(res.data?.user?.id)
  emit('success') // Sync user state in parent
}

const fetchPreferenceGuideCategories = async () => {
  if (preferenceGuideCategories.value.length) return
  try {
    const res = await getFilters()
    preferenceGuideCategories.value = res.data?.categories || []
  } catch (e) {}
}

const openPreferenceGuide = async () => {
  await fetchPreferenceGuideCategories()
  preferenceGuideSelection.value = [...(userStore.userInfo?.preferenceCategoryIds || [])]
  preferenceGuideVisible.value = true
}

const skipStep2 = async () => {
  try {
    uni.showLoading({ title: '注册中...', mask: true })
    await finalizeRegister()
    uni.hideLoading()
    emit('update:visibleStep', 0)
    await openPreferenceGuide()
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e?.data?.message || '注册失败', icon: 'none' })
  }
}

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
      nickname,
      phone: pendingRegister.phone
    }
    if (avatarUrl) updateData.avatar = avatarUrl

    await updateUserInfo(updateData)
    emit('success')
    uni.hideLoading()
    emit('update:visibleStep', 0)
    await openPreferenceGuide()
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

const handlePreferenceLimitExceed = () => {
  uni.showToast({ title: '最多选择5个', icon: 'none' })
}

const saveRegisterPreferences = async () => {
  try {
    await setPreferences({ categoryIds: preferenceGuideSelection.value })
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
    uni.showToast({ title: '偏好已生效，已切换到偏好推荐', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '保存偏好失败', icon: 'none' })
  }
}

const skipRegisterPreferences = () => {
  markColdStartGuideSkipped(userStore.userInfo?.id)
  preferenceGuideVisible.value = false
  uni.showToast({ title: '已跳过，后续可在我的-偏好设置里设置', icon: 'none' })
}
</script>

<style scoped>
/* ========== 新用户授权弹窗 ========== */
.auth-mask {
  position: fixed; left: 0; top: 0; width: 100%; height: 100%;
  background: rgba(15, 23, 42, 0.42); backdrop-filter: blur(14px);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
  padding: 32rpx;
  box-sizing: border-box;
}
.auth-panel {
  width: 620rpx;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(248, 250, 252, 0.92) 100%);
  border: 2rpx solid rgba(255, 255, 255, 0.72);
  border-radius: 44rpx;
  padding: 52rpx 40rpx 40rpx;
  display: flex; flex-direction: column; align-items: stretch;
  box-shadow: 0 20rpx 56rpx rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(18rpx);
  box-sizing: border-box;
  position: relative;
}
.preference-guide-panel {
  width: 640rpx;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(248, 250, 252, 0.92) 100%);
  border: 2rpx solid rgba(255, 255, 255, 0.72);
  border-radius: 44rpx;
  padding: 44rpx 36rpx;
  box-shadow: 0 20rpx 56rpx rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(18rpx);
  box-sizing: border-box;
}
.auth-brand-mark {
  width: 72rpx;
  height: 72rpx;
  display: block;
  position: absolute;
  left: 32rpx;
  top: 32rpx;
}
.auth-head {
  padding: 8rpx 12rpx 0;
  margin-bottom: 40rpx;
  text-align: center;
}
.auth-kicker { display: block; font-size: 22rpx; font-weight: 700; letter-spacing: 4rpx; color: #64748b; margin-bottom: 10rpx; }
.auth-title { display: block; font-size: 42rpx; font-weight: 800; color: #0f172a; margin-bottom: 14rpx; }
.auth-subtitle { display: block; font-size: 26rpx; color: #64748b; line-height: 1.7; }

.auth-avatar-wrap { margin-bottom: 32rpx; }
.auth-avatar-btn { display: flex; flex-direction: column; align-items: center; background: transparent; padding: 0; margin: 0; line-height: normal; }
.auth-avatar-btn::after { border: none; }
.auth-avatar-img { width: 160rpx; height: 160rpx; border-radius: 50%; box-shadow: 0 10rpx 28rpx rgba(17, 24, 39, 0.08); border: 6rpx solid rgba(255, 255, 255, 0.88); }
.auth-avatar-edit { margin-top: 16rpx; }
.auth-avatar-edit-text { font-size: 24rpx; color: #475569; font-weight: 600;}

.auth-input {
  width: 100%;
  height: 92rpx;
  border-radius: 30rpx;
  background: rgba(248, 250, 252, 0.92);
  border: 2rpx solid rgba(226, 232, 240, 0.9);
  padding: 0 28rpx;
  margin-bottom: 20rpx;
  font-size: 30rpx;
  text-align: center;
  box-sizing: border-box;
}
.auth-input-wrap {
  width: 100%;
  height: 92rpx;
  border-radius: 30rpx;
  background: rgba(248, 250, 252, 0.92);
  border: 2rpx solid rgba(226, 232, 240, 0.9);
  display: flex;
  align-items: center;
  padding: 0 28rpx;
  margin-bottom: 20rpx;
  box-sizing: border-box;
}
.auth-input-field { flex: 1; height: 92rpx; font-size: 30rpx; text-align: center; }
.pwd-toggle {
  font-size: 24rpx;
  color: #64748b;
  font-weight: 600;
  padding-left: 16rpx;
  flex-shrink: 0;
}

.auth-actions { display: flex; gap: 20rpx; width: 100%; margin-top: 16rpx; box-sizing: border-box; }
.auth-btn { flex: 1; height: 88rpx; line-height: 88rpx; border-radius: 32rpx; font-size: 30rpx; text-align: center; font-weight: 700; }
.auth-btn.skip { color: #475569; background: rgba(241, 245, 249, 0.95); }
.auth-btn.confirm { color: #ffffff; background: linear-gradient(135deg, #0f172a, #334155); box-shadow: 0 10rpx 24rpx rgba(15, 23, 42, 0.16); }
.auth-btn.confirm.full { flex: 1; width: 100%; }

.auth-tip { display: block; margin-top: 24rpx; font-size: 24rpx; color: #94a3b8; text-align: center; line-height: 1.7; }
.auth-error { display: block; margin-top: 16rpx; font-size: 24rpx; color: #dc2626; text-align: center; font-weight: 600;}
</style>
