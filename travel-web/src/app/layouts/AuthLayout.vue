<!-- 认证布局 -->
<template>
  <div class="auth-layout">
    <header class="auth-header">
      <router-link to="/" class="brand">
        <span class="brand-icon">✈</span>
        <span class="brand-text">{{ APP_NAME }}</span>
      </router-link>
    </header>

    <main class="auth-main">
      <div class="auth-card">
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
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.14), transparent 28%),
    linear-gradient(180deg, #f7fbff 0%, #eef4fb 100%);
}

.auth-header {
  padding: 24px 32px 0;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
  text-decoration: none;
  font-size: 24px;
  font-weight: 700;
}

.brand-icon {
  font-size: 28px;
}

.auth-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px 56px;
}

.auth-card {
  width: 100%;
  max-width: 460px;
}
</style>
