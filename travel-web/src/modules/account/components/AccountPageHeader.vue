<!-- 用户中心页面头部 -->
<template>
  <div class="account-page-header">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: ACCOUNT_ROUTE_PATHS.profile }">用户中心</el-breadcrumb-item>
      <el-breadcrumb-item>{{ title }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="header-main">
      <div>
        <h2 class="page-title">{{ title }}</h2>
        <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
      </div>
      <div v-if="$slots.actions" class="header-actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ACCOUNT_ROUTE_PATHS } from '@/modules/account/constants/routes.js'

// 账户页统一复用该头部，保证面包屑和标题结构在各子页保持一致。
defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  }
})

// 头部自身不管理动作逻辑，只通过具名插槽让各页面按需补充右侧操作区。
</script>

<style lang="scss" scoped>
.account-page-header {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.header-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #6b7280;
  line-height: 1.6;
}

.header-actions {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .header-main {
    flex-direction: column;
  }
}
</style>
