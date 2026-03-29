<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-header">
        <span class="logo-icon">✈</span>
        <h1>注册 WayTrip</h1>
        <p>{{ step === 1 ? '创建账号，探索世界' : '完善个人资料（可跳过）' }}</p>
      </div>

      <!-- 步骤指示器 -->
      <div class="step-indicator">
        <div class="step" :class="{ active: step >= 1 }">1</div>
        <div class="step-line" :class="{ active: step >= 2 }"></div>
        <div class="step" :class="{ active: step >= 2 }">2</div>
      </div>

      <!-- 第一步：手机号 + 密码 -->
      <el-form v-if="step === 1" ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleStep1">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" prefix-icon="Phone" maxlength="11" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码 (至少6位)" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请确认密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="register-btn" @click="handleStep1">下一步</el-button>
        </el-form-item>
      </el-form>

      <!-- 第二步：头像 + 昵称（可跳过） -->
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
              <el-avatar :size="100" :src="avatarPreview || defaultRegisterAvatar" class="upload-avatar" />
              <div class="avatar-tip">点击上传头像</div>
            </div>
          </el-upload>
        </div>
        <el-input v-model="registerProfileForm.nickname" placeholder="请输入昵称" prefix-icon="User" maxlength="30" size="large" class="nickname-input" />
        <div class="step2-actions">
          <el-button size="large" class="skip-btn" :loading="loading" @click="handleSkip">跳过</el-button>
          <el-button type="primary" size="large" :loading="loading" class="confirm-btn" @click="handleStep2">完成注册</el-button>
        </div>
      </div>

      <div class="register-footer" v-if="step === 1">
        已有账号？<router-link to="/login" class="link">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { prepareRegister, register } from '@/api/auth'
import { uploadAvatar, updateUserInfo } from '@/api/user'
import { getAvatarUrl } from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const step = ref(1)
const defaultRegisterNickname = 'web用户'
const defaultRegisterAvatar = getAvatarUrl('/uploads/images/avatar.jpg')

// 第一步表单
const form = reactive({
  phone: '',
  password: '',
  confirmPassword: ''
})

// 第二步表单
const registerProfileForm = reactive({
  nickname: defaultRegisterNickname
})
const avatarPreview = ref('')
const avatarFile = ref(null)

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

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

// 第一步：仅前端验证，通过后进入第二步
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

// 头像选择
const handleAvatarChange = (file) => {
  avatarFile.value = file.raw
  avatarPreview.value = URL.createObjectURL(file.raw)
}

// 执行注册（跳过和完成注册都调用这里）
const doRegister = async (options = {}) => {
  const { nickname, avatar } = options
  loading.value = true
  try {
    // 1. 调用注册接口
    const registerData = {
      phone: form.phone,
      password: form.password
    }
    if (nickname) {
      registerData.nickname = nickname
    }
    const res = await register(registerData)
    userStore.login(res.data)

    // 2. 如果有头像，注册后再上传
    if (avatar) {
      try {
        const uploadRes = await uploadAvatar(avatar)
        await updateUserInfo({ avatar: uploadRes.data.url })
      } catch (e) {
        console.warn('头像上传失败，可稍后在个人中心设置')
      }
    }

    ElMessage.success('注册成功，欢迎来到 WayTrip！')
    router.push('/')
  } catch (e) {
    // 注册失败（如手机号已注册），回到第一步修改
    step.value = 1
  } finally {
    loading.value = false
  }
}

// 跳过第二步：使用默认头像和昵称直接注册
const handleSkip = () => {
  doRegister()
}

// 完成注册：带上昵称和头像
const handleStep2 = () => {
  doRegister({
      nickname: registerProfileForm.nickname.trim() || defaultRegisterNickname,
      avatar: avatarFile.value || null
    })
  }
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.register-header {
  text-align: center;
  margin-bottom: 24px;

  .logo-icon {
    font-size: 48px;
    display: block;
    margin-bottom: 12px;
  }

  h1 {
    font-size: 24px;
    color: #303133;
    margin-bottom: 8px;
  }

  p {
    color: #909399;
    font-size: 14px;
  }
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 28px;

  .step {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: #e0e0e0;
    color: #999;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    transition: all 0.3s;

    &.active {
      background: #409eff;
      color: #fff;
    }
  }

  .step-line {
    width: 60px;
    height: 3px;
    background: #e0e0e0;
    margin: 0 12px;
    transition: all 0.3s;

    &.active {
      background: #409eff;
    }
  }
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
}

.register-footer {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-top: 8px;

  .link {
    color: #409eff;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

.step2-content {
  display: flex;
  flex-direction: column;
  align-items: center;

  .avatar-upload {
    margin-bottom: 24px;
    text-align: center;

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
      border: 2px dashed #dcdfe6;
      transition: border-color 0.3s;

      &:hover {
        border-color: #409eff;
      }
    }

    .avatar-tip {
      margin-top: 10px;
      font-size: 13px;
      color: #909399;
    }
  }

  .nickname-input {
    width: 100%;
    margin-bottom: 24px;
  }

  .step2-actions {
    width: 100%;
    display: flex;
    gap: 12px;

    .skip-btn {
      flex: 1;
      height: 44px;
      border-radius: 8px;
    }

    .confirm-btn {
      flex: 1;
      height: 44px;
      border-radius: 8px;
    }
  }
}
</style>

