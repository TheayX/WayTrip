<!-- 登录页面 -->
<template>
  <div class="login-container" :class="{ 'login-container--dark': currentTheme === 'dark' }">
    <div class="login-bg"></div>
    <div class="theme-switcher">
      <span class="theme-switcher__label">主题</span>
      <div class="theme-switcher__group">
        <button
          v-for="option in THEME_MODE_OPTIONS"
          :key="option.value"
          type="button"
          class="theme-switcher__button"
          :class="{ 'theme-switcher__button--active': themeMode === option.value }"
          @click="setThemeMode(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </div>
    <div class="login-box">
      <div class="login-left">
          <div class="left-content">
            <div class="brand">
            <img :src="brandLogoUrl" alt="WayTrip Admin" class="brand-logo" />
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
import brandLogoUrl from '@/shared/assets/brand/waytrip-standard.svg'
import { useTheme } from '@/shared/composables/useTheme.js'
import { THEME_MODE_OPTIONS } from '@/shared/constants/theme.js'

// 登录页同时承接账号认证和主题切换入口，避免未登录状态下还要跳去别处改主题。
const router = useRouter()
const userStore = useUserStore()
const { themeMode, currentTheme, setThemeMode } = useTheme()
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
  // 先走表单校验，再发起登录请求，避免无效请求频繁打到认证接口。
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
  color: var(--wt-text-primary);
  --login-shell-bg:
    radial-gradient(circle at top left, rgba(148, 163, 184, 0.2) 0%, rgba(148, 163, 184, 0) 30%),
    radial-gradient(circle at bottom right, rgba(30, 64, 175, 0.16) 0%, rgba(30, 64, 175, 0) 34%),
    linear-gradient(135deg, #f8fafc 0%, #eef2f7 48%, #e2e8f0 100%);
  --login-panel-bg: rgba(255, 255, 255, 0.56);
  --login-panel-border: rgba(255, 255, 255, 0.72);
  --login-panel-shadow: 0 30px 90px rgba(15, 23, 42, 0.16);
  --login-left-text: #f8fafc;
  --login-right-bg: rgba(255, 255, 255, 0.72);
  --login-kicker: #78716c;
  --login-title: #0c0a09;
  --login-subtitle: #78716c;
  --login-input-bg: rgba(250, 250, 249, 0.92);
  --login-input-border: rgba(214, 211, 209, 0.92);
  --login-input-hover-bg: #fff;
  --login-input-text: #0f172a;
  --login-input-placeholder: #94a3b8;
  --login-input-icon: #64748b;
  --login-footer: #a8a29e;
  background:
    var(--login-shell-bg);
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
      background: var(--wt-accent-blue-bg);
      bottom: -160px;
      right: -120px;
    }
  }
}

.theme-switcher {
  position: absolute;
  top: 24px;
  right: 24px;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--login-panel-bg) 88%, transparent);
  border: 1px solid var(--login-panel-border);
  box-shadow: var(--wt-shadow-soft);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.theme-switcher__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--wt-text-secondary);
}

.theme-switcher__group {
  display: inline-flex;
  align-items: center;
  padding: 4px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--login-right-bg) 72%, transparent);
}

.theme-switcher__button {
  border: none;
  background: transparent;
  color: var(--wt-text-regular);
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;

  &:hover {
    color: var(--wt-text-primary);
  }
}

.theme-switcher__button--active {
  background: var(--wt-surface-elevated);
  color: var(--wt-text-primary);
  box-shadow: var(--wt-shadow-soft);
}

.login-box {
  width: min(1080px, 100%);
  min-height: 620px;
  background: var(--login-panel-bg);
  backdrop-filter: blur(28px);
  -webkit-backdrop-filter: blur(28px);
  border-radius: 32px;
  box-shadow: var(--login-panel-shadow);
  display: flex;
  overflow: hidden;
  z-index: 10;
  border: 1px solid var(--login-panel-border);

  .login-left {
    width: 46%;
    background:
      linear-gradient(180deg, rgba(15, 23, 42, 0.92) 0%, rgba(30, 41, 59, 0.88) 100%),
      linear-gradient(135deg, rgba(202, 138, 4, 0.18) 0%, rgba(202, 138, 4, 0) 42%);
    padding: 56px 48px;
    color: var(--login-left-text);
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
      max-width: 360px;
    }

    .brand {
      display: flex;
      align-items: center;
      margin-bottom: 38px;
      margin-left: -18px;

      .brand-logo {
        width: 333px;
        max-width: none;
        height: auto;
        display: block;
        flex-shrink: 0;
        filter: drop-shadow(0 18px 24px color-mix(in srgb, var(--el-color-primary) 20%, transparent));
      }
    }

    .eyebrow {
      font-size: 15px;
      font-weight: 700;
      letter-spacing: 0.16em;
      text-transform: uppercase;
      color: rgba(226, 232, 240, 0.72);
      margin-bottom: 18px;
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
    background: var(--login-right-bg);
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
        color: var(--login-kicker);
        letter-spacing: 0.14em;
        text-transform: uppercase;
        font-weight: 700;
      }

      .title {
        font-size: 34px;
        font-weight: 700;
        color: var(--login-title);
        margin-bottom: 10px;
      }

      .subtitle {
        color: var(--login-subtitle);
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
          box-shadow: 0 0 0 1px var(--login-input-border) inset;
          background-color: var(--login-input-bg);
          transition: all 0.3s ease;

          &.is-focus, &:hover {
            box-shadow: 0 0 0 1px var(--el-color-primary) inset;
            background-color: var(--login-input-hover-bg);
          }
        }

        :deep(.el-input__inner) {
          height: 50px;
          color: var(--login-input-text);
        }

        :deep(.el-input__inner::placeholder) {
          color: var(--login-input-placeholder);
        }

        :deep(.el-input__prefix-inner),
        :deep(.el-input__suffix-inner),
        :deep(.el-input__icon) {
          color: var(--login-input-icon);
        }

        .login-btn {
          width: 100%;
          height: 50px;
          border-radius: 14px;
          font-size: 16px;
          font-weight: 600;
          margin-top: 8px;
          background: var(--wt-accent-gradient-blue);
          border: none;
          letter-spacing: 0.08em;
          transition: all 0.3s;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 14px 30px color-mix(in srgb, var(--el-color-primary) 24%, transparent);
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
        color: var(--login-footer);
      }
    }
  }
}

:global(html.dark) .login-container {
  --login-shell-bg:
    radial-gradient(circle at top left, var(--wt-accent-blue-bg) 0%, transparent 32%),
    radial-gradient(circle at bottom right, var(--wt-accent-cyan-bg) 0%, transparent 34%),
    linear-gradient(135deg, var(--wt-surface-page) 0%, var(--wt-surface-panel) 52%, var(--wt-surface-muted) 100%);
  --login-panel-bg: color-mix(in srgb, var(--wt-surface-page) 72%, transparent);
  --login-panel-border: var(--wt-border-default);
  --login-panel-shadow: var(--wt-shadow-float);
  --login-left-text: var(--wt-text-primary);
  --login-right-bg: color-mix(in srgb, var(--wt-surface-panel) 86%, transparent);
  --login-kicker: var(--wt-text-secondary);
  --login-title: var(--wt-text-primary);
  --login-subtitle: var(--wt-text-regular);
  --login-input-bg: color-mix(in srgb, var(--wt-surface-muted) 88%, transparent);
  --login-input-border: var(--wt-border-default);
  --login-input-hover-bg: var(--wt-surface-elevated);
  --login-input-text: var(--wt-text-primary);
  --login-input-placeholder: var(--wt-text-secondary);
  --login-input-icon: var(--wt-text-regular);
  --login-footer: var(--wt-text-secondary);
}

.login-container--dark {
  --login-shell-bg:
    radial-gradient(circle at top left, var(--wt-accent-blue-bg) 0%, transparent 28%),
    radial-gradient(circle at 80% 18%, var(--wt-accent-cyan-bg) 0%, transparent 24%),
    linear-gradient(135deg, var(--wt-surface-page) 0%, var(--wt-surface-panel) 48%, var(--wt-surface-muted) 100%);
  --login-panel-bg: color-mix(in srgb, var(--wt-surface-page) 78%, transparent);
  --login-panel-border: var(--wt-border-default);
  --login-panel-shadow: var(--wt-shadow-float);
  --login-left-text: var(--wt-text-primary);
  --login-right-bg: color-mix(in srgb, var(--wt-surface-panel) 92%, transparent);
  --login-kicker: var(--el-color-primary);
  --login-title: var(--wt-text-primary);
  --login-subtitle: var(--wt-text-regular);
  --login-input-bg: color-mix(in srgb, var(--wt-surface-muted) 92%, transparent);
  --login-input-border: var(--wt-border-default);
  --login-input-hover-bg: var(--wt-surface-elevated);
  --login-input-text: var(--wt-text-primary);
  --login-input-placeholder: var(--wt-text-secondary);
  --login-input-icon: var(--el-color-primary);
  --login-footer: var(--wt-text-secondary);

  .login-bg {
    &::before {
      background: var(--wt-accent-cyan-bg);
    }

    &::after {
      background: var(--wt-accent-blue-bg);
    }
  }

  .theme-switcher {
    background: color-mix(in srgb, var(--wt-surface-page) 76%, transparent);
    border-color: var(--wt-border-default);
  }

  .theme-switcher__group {
    background: var(--wt-surface-panel);
  }

  .theme-switcher__button--active {
    background: var(--wt-surface-elevated);
    color: var(--wt-text-primary);
  }

  .login-box {
    border-color: var(--wt-border-default);
  }

  .login-left {
    background:
      linear-gradient(180deg, var(--wt-surface-page) 0%, var(--wt-surface-panel) 100%),
      linear-gradient(135deg, var(--wt-accent-blue-bg) 0%, transparent 42%);
  }

  .login-right {
    box-shadow: inset 1px 0 0 var(--wt-divider-soft);
  }

  .login-form {
    :deep(.el-input__wrapper) {
      box-shadow:
        0 0 0 1px color-mix(in srgb, var(--el-color-primary) 22%, var(--wt-border-default)) inset,
        var(--wt-shadow-soft);
    }

    :deep(.el-input__wrapper.is-focus),
    :deep(.el-input__wrapper:hover) {
      box-shadow:
        0 0 0 1px var(--el-color-primary) inset,
        var(--wt-shadow-float);
    }

    .login-btn {
      background: var(--wt-accent-gradient-blue);

      &:hover {
        box-shadow: 0 18px 36px color-mix(in srgb, var(--el-color-primary) 32%, transparent);
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

    .left-content {
      max-width: 320px;
    }

    .brand .brand-logo {
      width: 220px;
      margin-left: -12px;
    }
  }

  .login-box .login-right {
    padding: 36px 28px 40px;
  }

  .theme-switcher {
    top: 16px;
    right: 16px;
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

    .left-content {
      max-width: 280px;
    }

    .brand .brand-logo {
      width: 196px;
      margin-left: -10px;
    }
  }

  .login-box .login-right {
    padding: 30px 22px 32px;
  }

  .theme-switcher {
    left: 16px;
    right: 16px;
    justify-content: space-between;
    gap: 8px;
  }

  .theme-switcher__group {
    flex: 1;
    min-width: 0;
  }

  .theme-switcher__button {
    flex: 1;
    padding: 8px 10px;
  }

  .login-box .login-left .slogan,
  .login-box .login-right .form-wrapper .title {
    font-size: 28px;
  }
}
</style>
