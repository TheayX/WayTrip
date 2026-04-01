<!-- 登录页面 -->
<template>
  <div class="login-container">
    <!-- 背景装饰层 -->
    <div class="login-bg"></div>
    <div class="login-box">
      <!-- 左侧品牌介绍 -->
      <div class="login-left">
        <div class="left-content">
          <div class="brand">
            <el-icon class="logo-icon"><Position /></el-icon>
            <span class="logo-text">WayTrip·微旅</span>
          </div>
          <h2 class="slogan">探索世界<br/>记录美好瞬间</h2>
          <p class="desc">一个现代化的旅游服务管理平台</p>
        </div>
        <div class="circles">
          <div class="circle circle-1"></div>
          <div class="circle circle-2"></div>
        </div>
      </div>
      <!-- 右侧登录表单 -->
      <div class="login-right">
        <div class="form-wrapper">
          <h2 class="title">欢迎回来</h2>
          <p class="subtitle">请登录您的管理端账号</p>
          <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin" size="large" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/app/store/user.js'
import { ElMessage } from 'element-plus'

// 基础状态
const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

// 登录表单
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 提交登录
const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.loginAction(form.username, form.password)
    ElMessage.success({ message: '登录成功', center: true })
    router.push('/')
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  height: 100vh;
  width: 100vw;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f3f4f6;
  position: relative;
  overflow: hidden;

  .login-bg {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(135deg, #e0eafc 0%, #cfdef3 100%);
    z-index: 1;

    &::before, &::after {
      content: '';
      position: absolute;
      border-radius: 50%;
      filter: blur(80px);
      z-index: 2;
    }

    &::before {
      width: 600px;
      height: 600px;
      background: rgba(64, 158, 255, 0.3);
      top: -100px;
      left: -100px;
    }

    &::after {
      width: 500px;
      height: 500px;
      background: rgba(103, 194, 58, 0.2);
      bottom: -100px;
      right: -100px;
    }
  }
}

.login-box {
  width: 900px;
  height: 500px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  display: flex;
  overflow: hidden;
  z-index: 10;
  border: 1px solid rgba(255, 255, 255, 0.5);

  .login-left {
    width: 45%;
    background: linear-gradient(135deg, #409EFF 0%, #3a7bd5 100%);
    padding: 40px;
    color: white;
    display: flex;
    flex-direction: column;
    justify-content: center;
    position: relative;
    overflow: hidden;

    .left-content {
      position: relative;
      z-index: 2;
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 60px;
      font-size: 24px;
      font-weight: 600;

      .logo-icon {
        font-size: 28px;
        background: rgba(255, 255, 255, 0.2);
        padding: 8px;
        border-radius: 12px;
      }
    }

    .slogan {
      font-size: 32px;
      line-height: 1.4;
      margin-bottom: 20px;
      font-weight: 600;
      letter-spacing: 2px;
    }

    .desc {
      font-size: 15px;
      color: rgba(255, 255, 255, 0.8);
      letter-spacing: 1px;
    }

    .circles {
      .circle {
        position: absolute;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.1);
      }
      .circle-1 {
        width: 300px;
        height: 300px;
        top: -100px;
        right: -100px;
      }
      .circle-2 {
        width: 200px;
        height: 200px;
        bottom: -50px;
        left: -50px;
      }
    }
  }

  .login-right {
    width: 55%;
    padding: 50px 60px;
    display: flex;
    align-items: center;
    justify-content: center;

    .form-wrapper {
      width: 100%;
      max-width: 320px;

      .title {
        font-size: 26px;
        font-weight: 600;
        color: #1f2937;
        margin-bottom: 8px;
      }

      .subtitle {
        color: #6b7280;
        font-size: 14px;
        margin-bottom: 40px;
      }

      .login-form {
        :deep(.el-input__wrapper) {
          border-radius: 8px;
          box-shadow: 0 0 0 1px #e5e7eb inset;
          background-color: #f9fafb;
          transition: all 0.3s ease;

          &.is-focus, &:hover {
            box-shadow: 0 0 0 1px #409EFF inset;
            background-color: #fff;
          }
        }

        :deep(.el-input__inner) {
          height: 44px;
        }

        .login-btn {
          width: 100%;
          height: 44px;
          border-radius: 8px;
          font-size: 16px;
          margin-top: 10px;
          background: linear-gradient(135deg, #409EFF 0%, #3a7bd5 100%);
          border: none;
          transition: transform 0.2s, box-shadow 0.2s;

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
          }
          
          &:active {
            transform: translateY(1px);
          }
        }
      }
    }
  }
}
</style>
