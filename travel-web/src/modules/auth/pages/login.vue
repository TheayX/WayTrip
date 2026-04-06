<!-- 登录页 -->
<template>
  <div class="login-page premium-card">
    <div class="login-header">
       <p class="header-kicker">Sign In</p>
       <h1>欢迎回到 {{ APP_NAME }}</h1>
       <p class="header-desc">登录你的账号，继续未完成的行程与探索。</p>
     </div>

    <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent="handleLogin">
      <el-form-item prop="phone">
        <el-input v-model="form.phone" placeholder="手机号" prefix-icon="Phone" maxlength="11" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">登录</el-button>
      </el-form-item>
    </el-form>

    <div class="login-footer">
      <span>还没有账号？</span>
      <router-link to="/register" class="link">创建账号</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/modules/account/store/user.js'
import { login } from '@/modules/auth/api.js'
import { APP_NAME } from '@/shared/constants/app.js'
import { ElMessage } from 'element-plus'

// 基础依赖与路由状态
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 页面数据状态
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  phone: '',
  password: ''
})

// 表单校验规则
const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

// 交互处理方法
const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form)
    userStore.login(res.data)

    if (res.data.user?.isReactivated) {
      ElMessage.success('账户已恢复，欢迎回来')
    } else {
      ElMessage.success('登录成功')
    }

    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  padding: 28px;
}

.login-header {
  margin-bottom: 24px;
}

.header-kicker {
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.login-header h1 {
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

.login-btn {
  width: 100%;
}

.login-footer {
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
</style>
