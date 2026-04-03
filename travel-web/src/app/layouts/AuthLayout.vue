<!-- 认证布局 -->
<template>
  <div class="auth-layout">
    <header class="auth-header">
      <router-link to="/" class="brand-link" aria-label="WayTrip 首页">
        <span class="brand-mark" aria-hidden="true">
          <span class="brand-mark-core"></span>
        </span>
        <span class="brand-copy">
          <strong>{{ APP_NAME }}</strong>
          <span>Travel Curated</span>
        </span>
      </router-link>
    </header>

    <main class="auth-main">
      <section class="auth-hero">
        <p class="auth-eyebrow">Account Access</p>
        <h1>进入你的旅行空间</h1>
        <p>登录后继续管理偏好、订单、收藏与旅行记录，整个账号体验会与 Web 主站保持同一套高级感视觉语言。</p>
      </section>

      <div class="auth-card premium-card">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { APP_NAME } from '@/shared/constants/app.js'
</script>

<style lang="scss" scoped>
.auth-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.12), transparent 24%),
    radial-gradient(circle at right center, rgba(200, 169, 91, 0.12), transparent 18%),
    linear-gradient(180deg, #f9fbfe 0%, #f2f6fb 100%);
}

.auth-header {
  padding: 26px 32px 0;
}

.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 15px;
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 70%, #c8a95b 100%);
  box-shadow: 0 20px 28px -18px rgba(37, 99, 235, 0.72);
}

.brand-mark::before {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.24), rgba(255, 255, 255, 0));
}

.brand-mark-core {
  width: 18px;
  height: 18px;
  border-radius: 7px;
  border: 2px solid rgba(255, 255, 255, 0.92);
  transform: rotate(45deg);
}

.brand-copy {
  display: flex;
  flex-direction: column;
}

.brand-copy strong {
  font-size: 18px;
  color: #0f172a;
  line-height: 1.1;
}

.brand-copy span {
  margin-top: 4px;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #64748b;
}

.auth-main {
  flex: 1;
  width: min(100%, 1180px);
  margin: 0 auto;
  padding: 48px 24px 56px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(380px, 460px);
  gap: 48px;
  align-items: center;
}

.auth-hero {
  max-width: 520px;
}

.auth-eyebrow {
  margin-bottom: 14px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #64748b;
}

.auth-hero h1 {
  font-size: 48px;
  line-height: 1.08;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.auth-hero p {
  margin-top: 18px;
  color: #64748b;
  line-height: 1.9;
  font-size: 15px;
}

.auth-card {
  width: 100%;
  padding: 28px;
}

@media (max-width: 992px) {
  .auth-main {
    grid-template-columns: 1fr;
    gap: 28px;
    padding-top: 28px;
  }

  .auth-hero {
    max-width: none;
  }

  .auth-hero h1 {
    font-size: 38px;
  }
}

@media (max-width: 768px) {
  .auth-header {
    padding: 18px 20px 0;
  }

  .auth-main {
    padding: 20px 16px 32px;
  }

  .auth-card {
    padding: 20px;
    border-radius: 22px;
  }

  .auth-hero h1 {
    font-size: 32px;
  }
}
</style>
