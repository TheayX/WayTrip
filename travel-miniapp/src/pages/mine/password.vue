<!-- 密码设置页 -->
<template>
  <view class="password-page">
    <!-- 密码表单区域 -->
    <view class="form-group">
      <view class="form-item">
        <text class="form-label">旧密码</text>
        <input
          class="form-input"
          v-model="form.oldPassword"
          :type="oldPwdVisible ? 'text' : 'password'"
          :password="!oldPwdVisible"
          placeholder="请输入旧密码（首次设置可留空）"
        />
        <text class="pwd-eye" @click="oldPwdVisible = !oldPwdVisible">{{ oldPwdVisible ? '🙈' : '👁' }}</text>
      </view>
      <view class="form-item">
        <text class="form-label">新密码</text>
        <input
          class="form-input"
          v-model="form.newPassword"
          :type="newPwdVisible ? 'text' : 'password'"
          :password="!newPwdVisible"
          placeholder="请输入新密码（至少6位）"
        />
        <text class="pwd-eye" @click="newPwdVisible = !newPwdVisible">{{ newPwdVisible ? '🙈' : '👁' }}</text>
      </view>
      <view class="form-item">
        <text class="form-label">确认密码</text>
        <input
          class="form-input"
          v-model="form.confirmPassword"
          :type="confirmPwdVisible ? 'text' : 'password'"
          :password="!confirmPwdVisible"
          placeholder="请再次输入新密码"
        />
        <text class="pwd-eye" @click="confirmPwdVisible = !confirmPwdVisible">{{ confirmPwdVisible ? '🙈' : '👁' }}</text>
      </view>
    </view>
    <button class="submit-btn" :loading="loading" @click="handleSubmit">确认修改</button>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { changePassword } from '@/api/user'

// 页面数据状态
const loading = ref(false)
const oldPwdVisible = ref(false)
const newPwdVisible = ref(false)
const confirmPwdVisible = ref(false)
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 交互处理方法
const handleSubmit = async () => {
  if (!form.newPassword) {
    return uni.showToast({ title: '请输入新密码', icon: 'none' })
  }
  if (form.newPassword.length < 6) {
    return uni.showToast({ title: '密码长度至少6个字符', icon: 'none' })
  }
  if (form.newPassword !== form.confirmPassword) {
    return uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
  }

  loading.value = true
  try {
    await changePassword({
      oldPassword: form.oldPassword || undefined,
      newPassword: form.newPassword
    })
    uni.showToast({ title: '密码修改成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (e) {
    console.error('修改密码失败', e)
  }
  loading.value = false
}
</script>

<style scoped>
.password-page {
  background-color: #f4f6fb;
  min-height: 100vh;
  padding: 32rpx;
}

.form-group {
  background: #fff;
  border-radius: 36rpx;
  overflow: hidden;
  margin-bottom: 40rpx;
}

.form-item {
  padding: 28rpx 32rpx;
  display: flex;
  align-items: center;
  position: relative;
}

.form-item:not(:last-child)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 32rpx;
  right: 0;
  height: 1px;
  background-color: #E5E5EA;
}

.form-label {
  font-size: 30rpx;
  color: #000;
  width: 160rpx;
  flex-shrink: 0;
}

.form-input {
  flex: 1;
  font-size: 30rpx;
  height: 60rpx;
}

.pwd-eye {
  font-size: 36rpx;
  padding: 0 8rpx;
  flex-shrink: 0;
}

.submit-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 36rpx;
  font-size: 32rpx;
  font-weight: 600;
}

.submit-btn:active {
  opacity: 0.85;
}
</style>

