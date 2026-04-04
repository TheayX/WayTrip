<!-- 登录页面 -->
<template>
  <div class="login-container">
    <div class="login-bg"></div>
    <div class="login-box">
      <div class="login-left">
        <div class="left-content">
          <div class="brand">
            <div class="brand-icon">
              <span class="brand-mark brand-mark-primary"></span>
              <span class="brand-mark brand-mark-secondary"></span>
            </div>
            <span class="brand-text">WayTrip</span>
          </div>
          <div class="eyebrow">WayTrip Admin</div>
          <h2 class="slogan">统一管理内容、交易与用户行为</h2>
          <p class="desc">面向景点、订单与推荐链路的运营后台。</p>
          <div class="feature-list">
            <div class="feature-item">
              <span class="feature-index">01</span>
              <span>内容管理</span>
            </div>
            <div class="feature-item">
              <span class="feature-index">02</span>
              <span>订单处理</span>
            </div>
            <div class="feature-item">
              <span class="feature-index">03</span>
              <span>行为回看</span>
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="form-wrapper">
          <div class="form-kicker">账号登录</div>
          <h2 class="title">欢迎回来</h2>
          <p class="subtitle">登录管理端账号，继续处理今日运营任务。</p>
          <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin" size="large" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
                {{ loading ? '登录中...' : '进入后台' }}
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
  padding: 32px;
  background:
    radial-gradient(circle at top left, rgba(148, 163, 184, 0.2) 0%, rgba(148, 163, 184, 0) 30%),
    radial-gradient(circle at bottom right, rgba(30, 64, 175, 0.16) 0%, rgba(30, 64, 175, 0) 34%),
    linear-gradient(135deg, #f8fafc 0%, #eef2f7 48%, #e2e8f0 100%);
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
      filter: blur(100px);
      z-index: 2;
    }
    &::before {
      width: 540px;
      height: 540px;
      background: rgba(71, 85, 105, 0.12);
      top: -160px;
      left: -140px;
    }
    &::after {
      width: 480px;
      height: 480px;
      background: rgba(37, 99, 235, 0.12);
      bottom: -160px;
      right: -120px;
    }
  }
}

.login-box {
  width: min(1080px, 100%);
  min-height: 620px;
  background: rgba(255, 255, 255, 0.56);
  backdrop-filter: blur(28px);
  -webkit-backdrop-filter: blur(28px);
  border-radius: 32px;
  box-shadow: 0 30px 90px rgba(15, 23, 42, 0.16);
  display: flex;
  overflow: hidden;
  z-index: 10;
  border: 1px solid rgba(255, 255, 255, 0.72);

  .login-left {
    width: 46%;
    background:
      linear-gradient(180deg, rgba(15, 23, 42, 0.92) 0%, rgba(30, 41, 59, 0.88) 100%),
      linear-gradient(135deg, rgba(202, 138, 4, 0.18) 0%, rgba(202, 138, 4, 0) 42%);
    padding: 56px 48px;
    color: #f8fafc;
    display: flex;
    flex-direction: column;
    justify-content: center;
    position: relative;
    overflow: hidden;

    &::after {
      content: '';
      position: absolute;
      inset: auto -80px -120px auto;
      width: 320px;
      height: 320px;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(202, 138, 4, 0.24) 0%, rgba(202, 138, 4, 0) 70%);
    }

    .left-content {
      position: relative;
      z-index: 2;
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 34px;

      .brand-icon {
        position: relative;
        width: 48px;
        height: 48px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 14px;
        background: rgba(255, 255, 255, 0.08);
        border: 1px solid rgba(255, 255, 255, 0.14);
      }

      .brand-mark {
        position: absolute;
        display: block;
        border-radius: 999px;
      }

      .brand-mark-primary {
        width: 24px;
        height: 24px;
        background: linear-gradient(135deg, #f8fafc 0%, #dbeafe 100%);
        transform: translate(-6px, -6px);
      }

      .brand-mark-secondary {
        width: 20px;
        height: 20px;
        background: linear-gradient(135deg, #ca8a04 0%, #fde68a 100%);
        transform: translate(7px, 7px);
      }

      .brand-text {
        font-size: 22px;
        font-weight: 700;
        letter-spacing: 0.06em;
      }
    }

    .eyebrow {
      font-size: 12px;
      letter-spacing: 0.16em;
      text-transform: uppercase;
      color: rgba(226, 232, 240, 0.72);
      margin-bottom: 16px;
    }

    .slogan {
      font-size: 34px;
      line-height: 1.24;
      margin-bottom: 14px;
      font-weight: 700;
      letter-spacing: 0.02em;
      max-width: 320px;
    }

    .desc {
      font-size: 15px;
      line-height: 1.8;
      color: rgba(226, 232, 240, 0.78);
      margin-bottom: 28px;
      max-width: 300px;
    }

    .feature-list {
      display: flex;
      flex-direction: column;
      gap: 14px;

      .feature-item {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 14px;
        color: rgba(248, 250, 252, 0.9);
      }

      .feature-index {
        width: 28px;
        flex-shrink: 0;
        color: rgba(250, 204, 21, 0.86);
        font-size: 12px;
        font-weight: 700;
        letter-spacing: 0.08em;
      }
    }
  }

  .login-right {
    width: 54%;
    background: rgba(255, 255, 255, 0.72);
    padding: 56px 64px;
    display: flex;
    align-items: center;
    justify-content: center;

    .form-wrapper {
      width: 100%;
      max-width: 360px;

      .form-kicker {
        margin-bottom: 14px;
        font-size: 12px;
        color: #78716c;
        letter-spacing: 0.14em;
        text-transform: uppercase;
        font-weight: 700;
      }

      .title {
        font-size: 34px;
        font-weight: 700;
        color: #0c0a09;
        margin-bottom: 10px;
      }

      .subtitle {
        color: #78716c;
        font-size: 14px;
        line-height: 1.7;
        margin-bottom: 36px;
      }

      .login-form {
        :deep(.el-form-item) {
          margin-bottom: 18px;
        }

        :deep(.el-input__wrapper) {
          min-height: 50px;
          border-radius: 14px;
          box-shadow: 0 0 0 1px rgba(214, 211, 209, 0.92) inset;
          background-color: rgba(250, 250, 249, 0.92);
          transition: all 0.3s ease;

          &.is-focus, &:hover {
            box-shadow: 0 0 0 1px #ca8a04 inset;
            background-color: #fff;
          }
        }

        :deep(.el-input__inner) {
          height: 50px;
        }

        .login-btn {
          width: 100%;
          height: 50px;
          border-radius: 14px;
          font-size: 16px;
          font-weight: 600;
          margin-top: 8px;
          background: linear-gradient(135deg, #1c1917, #44403c);
          border: none;
          letter-spacing: 0.08em;
          transition: all 0.3s;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 14px 30px rgba(28, 25, 23, 0.22);
          }
          &:active {
            transform: translateY(0);
          }
        }
      }

      .footer-text {
        text-align: center;
        margin-top: 32px;
        font-size: 12px;
        color: #a8a29e;
      }
    }
  }
}

@media (max-width: 960px) {
  .login-container {
    padding: 20px;
  }

  .login-box {
    flex-direction: column;
    min-height: auto;
  }

  .login-box .login-left,
  .login-box .login-right {
    width: 100%;
  }

  .login-box .login-left {
    padding: 36px 28px 28px;
    gap: 28px;
  }

  .login-box .login-right {
    padding: 36px 28px 40px;
  }
}

@media (max-width: 640px) {
  .login-container {
    padding: 16px;
  }

  .login-box {
    border-radius: 24px;
  }

  .login-box .login-left {
    padding: 30px 22px 24px;
  }

  .login-box .login-right {
    padding: 30px 22px 32px;
  }

  .login-box .login-left .slogan,
  .login-box .login-right .form-wrapper .title {
    font-size: 28px;
  }
}
</style>
