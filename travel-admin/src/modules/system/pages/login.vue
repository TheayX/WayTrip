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
            <div class="brand-icon">✨</div>
            <span class="brand-text">WayTrip</span>
          </div>
          <h2 class="slogan">探索世界<br/>记录美好瞬间</h2>
          <p class="desc">现代化的旅游服务运营管理平台</p>
          <div class="feature-list">
            <div class="feature-item">
              <span class="feature-dot"></span>
              <span>智能推荐引擎</span>
            </div>
            <div class="feature-item">
              <span class="feature-dot"></span>
              <span>订单中心协同处理</span>
            </div>
            <div class="feature-item">
              <span class="feature-dot"></span>
              <span>用户行为分析</span>
            </div>
          </div>
        </div>
        <div class="circles">
          <div class="circle circle-1"></div>
          <div class="circle circle-2"></div>
          <div class="circle circle-3"></div>
        </div>
      </div>
      <!-- 右侧登录表单 -->
      <div class="login-right">
        <div class="form-wrapper">
          <h2 class="title">欢迎回来</h2>
          <p class="subtitle">请登录您的管理端账号以继续</p>
          <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin" size="large" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>
          <div class="footer-text">WayTrip Admin Console © 2026</div>
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

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

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
  background-color: #0f172a;
  position: relative;
  overflow: hidden;

  .login-bg {
    position: absolute;
    top: 0; left: 0;
    width: 100%; height: 100%;
    z-index: 1;

    &::before, &::after {
      content: '';
      position: absolute;
      border-radius: 50%;
      filter: blur(120px);
      z-index: 2;
    }
    &::before {
      width: 700px; height: 700px;
      background: rgba(59, 130, 246, 0.15);
      top: -200px; left: -200px;
    }
    &::after {
      width: 600px; height: 600px;
      background: rgba(139, 92, 246, 0.12);
      bottom: -200px; right: -200px;
    }
  }
}

.login-box {
  width: 960px;
  height: 560px;
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border-radius: 24px;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  overflow: hidden;
  z-index: 10;
  border: 1px solid rgba(255, 255, 255, 0.08);

  .login-left {
    width: 45%;
    background: linear-gradient(135deg, #3b82f6 0%, #6366f1 50%, #8b5cf6 100%);
    padding: 48px 40px;
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
      gap: 12px;
      margin-bottom: 48px;

      .brand-icon {
        font-size: 28px;
        background: rgba(255, 255, 255, 0.15);
        width: 48px; height: 48px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 14px;
      }
      .brand-text {
        font-size: 22px;
        font-weight: 700;
        letter-spacing: 1px;
      }
    }

    .slogan {
      font-size: 30px;
      line-height: 1.4;
      margin-bottom: 16px;
      font-weight: 700;
      letter-spacing: 2px;
    }

    .desc {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.7);
      margin-bottom: 32px;
    }

    .feature-list {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .feature-item {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 14px;
        color: rgba(255, 255, 255, 0.85);
      }

      .feature-dot {
        width: 6px; height: 6px;
        border-radius: 50%;
        background: #a5f3fc;
        box-shadow: 0 0 0 3px rgba(165, 243, 252, 0.3);
      }
    }

    .circles {
      .circle {
        position: absolute;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.06);
      }
      .circle-1 { width: 300px; height: 300px; top: -100px; right: -100px; }
      .circle-2 { width: 200px; height: 200px; bottom: -60px; left: -60px; }
      .circle-3 { width: 100px; height: 100px; top: 60%; right: 20%; background: rgba(255,255,255,0.04); }
    }
  }

  .login-right {
    width: 55%;
    background: #ffffff;
    padding: 50px 60px;
    display: flex;
    align-items: center;
    justify-content: center;

    .form-wrapper {
      width: 100%;
      max-width: 340px;

      .title {
        font-size: 28px;
        font-weight: 700;
        color: #0f172a;
        margin-bottom: 8px;
      }

      .subtitle {
        color: #94a3b8;
        font-size: 14px;
        margin-bottom: 40px;
      }

      .login-form {
        :deep(.el-input__wrapper) {
          border-radius: 10px;
          box-shadow: 0 0 0 1px #e2e8f0 inset;
          background-color: #f8fafc;
          transition: all 0.3s ease;

          &.is-focus, &:hover {
            box-shadow: 0 0 0 1px #3b82f6 inset;
            background-color: #fff;
          }
        }

        :deep(.el-input__inner) {
          height: 46px;
        }

        .login-btn {
          width: 100%;
          height: 46px;
          border-radius: 10px;
          font-size: 16px;
          font-weight: 600;
          margin-top: 12px;
          background: linear-gradient(135deg, #3b82f6, #6366f1);
          border: none;
          letter-spacing: 2px;
          transition: all 0.3s;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 20px rgba(59, 130, 246, 0.35);
          }
          &:active {
            transform: translateY(0);
          }
        }
      }

      .footer-text {
        text-align: center;
        margin-top: 40px;
        font-size: 12px;
        color: #cbd5e1;
      }
    }
  }
}
</style>
