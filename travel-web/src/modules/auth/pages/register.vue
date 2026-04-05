<!-- 注册页 -->
<template>
  <div class="register-page premium-card">
    <div class="register-header">
      <p class="header-kicker">Create Account</p>
      <h1>注册 {{ APP_NAME }}</h1>
      <p class="header-desc">{{ step === 1 ? '先完成账号创建。' : '补充昵称和头像，可跳过。' }}</p>
    </div>

    <div class="step-indicator">
      <div class="step" :class="{ active: step >= 1 }">1</div>
      <div class="step-line" :class="{ active: step >= 2 }"></div>
      <div class="step" :class="{ active: step >= 2 }">2</div>
    </div>

    <el-form v-if="step === 1" ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleStep1">
      <el-form-item prop="phone">
        <el-input v-model="form.phone" placeholder="手机号" prefix-icon="Phone" maxlength="11" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" type="password" placeholder="密码（至少 6 位）" prefix-icon="Lock" show-password />
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" class="register-btn" :loading="loading" @click="handleStep1">下一步</el-button>
      </el-form-item>
    </el-form>

    <div v-if="step === 2" class="step2-content">
      <div class="avatar-upload">
        <el-upload
          class="avatar-uploader"
          :show-file-list="false"
          :auto-upload="false"
          accept="image/*"
          :on-change="handleAvatarChange"
        >
          <div class="avatar-wrapper">
            <el-avatar :size="96" :src="avatarPreview || defaultRegisterAvatar" class="upload-avatar" />
            <div class="avatar-tip">上传头像</div>
          </div>
        </el-upload>
      </div>
      <el-input v-model="registerProfileForm.nickname" placeholder="昵称" prefix-icon="User" maxlength="30" size="large" class="nickname-input" />
      <div class="step2-actions">
        <el-button size="large" class="skip-btn" :loading="loading" @click="handleSkip">跳过</el-button>
        <el-button type="primary" size="large" :loading="loading" class="confirm-btn" @click="handleStep2">完成注册</el-button>
      </div>
    </div>

    <div class="register-footer" v-if="step === 1">
      <span>已有账号？</span>
      <router-link to="/login" class="link">去登录</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/modules/account/store/user.js'
import { prepareRegister, register } from '@/modules/auth/api.js'
import { uploadAvatar, updateUserInfo } from '@/modules/account/api.js'
import { APP_NAME } from '@/shared/constants/app.js'
import { getAvatarUrl } from '@/shared/api/client.js'
import { ElMessage } from 'element-plus'

// 基础依赖与路由状态
const router = useRouter()
const userStore = useUserStore()

// 页面数据状态
const formRef = ref(null)
const loading = ref(false)
const step = ref(1)
const defaultRegisterNickname = 'web用户'
const defaultRegisterAvatar = getAvatarUrl('/uploads/avatar/avatar.jpg')

// 注册表单状态
const form = reactive({
  phone: '',
  password: '',
  confirmPassword: ''
})

const registerProfileForm = reactive({
  nickname: defaultRegisterNickname
})
const avatarPreview = ref('')
const avatarFile = ref(null)

// 工具方法
const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单校验规则
const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

// 交互处理方法
const handleStep1 = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await prepareRegister({
      phone: form.phone,
      password: form.password,
      confirmPassword: form.confirmPassword
    })
    registerProfileForm.nickname = defaultRegisterNickname
    avatarPreview.value = ''
    avatarFile.value = null
    step.value = 2
    ElMessage.success('校验通过')
  } finally {
    loading.value = false
  }
}

const handleAvatarChange = (file) => {
  avatarFile.value = file.raw
  avatarPreview.value = URL.createObjectURL(file.raw)
}

const doRegister = async (options = {}) => {
  const { nickname, avatar } = options
  loading.value = true
  try {
    const registerData = {
      phone: form.phone,
      password: form.password
    }
    if (nickname) {
      registerData.nickname = nickname
    }
    const res = await register(registerData)
    userStore.login(res.data)
    let finalAvatar = res.data.user?.avatar || defaultRegisterAvatar
    const finalNickname = nickname || res.data.user?.nickname || defaultRegisterNickname

    if (avatar) {
      try {
        const uploadRes = await uploadAvatar(avatar)
        finalAvatar = uploadRes.data.url
      } catch (e) {
        ElMessage.warning('头像上传失败，可稍后在个人中心设置')
      }
    }

    try {
      await updateUserInfo({
        nickname: finalNickname,
        phone: form.phone,
        avatar: finalAvatar
      })
      userStore.setUserInfo({
        ...res.data.user,
        nickname: finalNickname,
        phone: form.phone,
        avatar: finalAvatar
      })
    } catch (e) {
      // 注册已成功，此处仅补充提示资料未完全落库，避免用户误以为账号创建失败。
      ElMessage.warning('账号已注册成功，但头像或昵称未完全保存，可稍后在个人中心补充')
      userStore.setUserInfo({
        ...res.data.user,
        nickname: finalNickname,
        phone: form.phone,
        avatar: res.data.user?.avatar || defaultRegisterAvatar
      })
      router.push('/')
      return
    }

    if (avatar && finalAvatar === (res.data.user?.avatar || defaultRegisterAvatar)) {
      ElMessage.warning('账号已注册成功，头像未更新，可稍后在个人中心设置')
    } else {
      ElMessage.success(`注册成功，欢迎来到 ${APP_NAME}`)
    }

    avatarPreview.value = ''
    avatarFile.value = null
    router.push('/')
  } catch (e) {
    step.value = 1
  } finally {
    loading.value = false
  }
}

const handleSkip = () => {
  doRegister()
}

const handleStep2 = () => {
  doRegister({
    nickname: registerProfileForm.nickname.trim() || defaultRegisterNickname,
    avatar: avatarFile.value || null
  })
}
</script>

<style lang="scss" scoped>
.register-page {
  padding: 28px;
}

.register-header {
  margin-bottom: 20px;
}

.header-kicker {
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.register-header h1 {
  font-size: 32px;
  line-height: 1.12;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.header-desc {
  margin-top: 10px;
  color: #64748b;
  line-height: 1.75;
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.step {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
}

.step.active {
  background: #2563eb;
  color: #fff;
}

.step-line {
  width: 60px;
  height: 3px;
  background: #e2e8f0;
  margin: 0 12px;
  transition: all 0.3s;
}

.step-line.active {
  background: #2563eb;
}

.register-btn {
  width: 100%;
}

.register-footer {
  margin-top: 8px;
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  color: #64748b;
}

.link {
  color: #1d4ed8;
  font-weight: 700;
}

.step2-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-upload {
  margin-bottom: 22px;
  text-align: center;
}

.avatar-uploader {
  display: inline-block;
  cursor: pointer;
}

.avatar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-avatar {
  border: 2px dashed #d7e2ee;
  transition: border-color 0.3s;
}

.upload-avatar:hover {
  border-color: #2563eb;
}

.avatar-tip {
  margin-top: 10px;
  font-size: 13px;
  color: #64748b;
}

.nickname-input {
  width: 100%;
  margin-bottom: 22px;
}

.step2-actions {
  width: 100%;
  display: flex;
  gap: 12px;
}

.skip-btn,
.confirm-btn {
  flex: 1;
}
</style>
