<!-- 用户中心布局 -->
<template>
  <div class="account-layout">
    <aside class="account-sidebar card">
      <div class="account-user">
        <el-avatar :size="56" :src="getAvatarUrl(userStore.userInfo?.avatar)" icon="User" />
        <div class="account-user-meta">
          <strong>{{ userStore.userInfo?.nickname || '旅行家' }}</strong>
          <span>{{ userStore.userInfo?.phone || '完善资料，获得更准推荐' }}</span>
        </div>
      </div>

      <nav class="account-nav">
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          class="account-nav-item"
          :class="{ active: isActive(item) }"
          @click="router.push(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </nav>
    </aside>

    <section class="account-content">
      <router-view />
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Collection, Document, Setting, Tickets, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/modules/account/store/user.js'
import { getAvatarUrl } from '@/shared/api/client.js'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const navItems = computed(() => [
  { label: '个人中心', path: '/profile', icon: User },
  { label: '我的互动', path: '/profile/activity', icon: ChatDotRound },
  { label: '我的订单', path: '/orders', icon: Tickets },
  { label: '我的收藏', path: '/favorites', icon: Collection },
  { label: '我的评价', path: '/reviews', icon: Document },
  { label: '设置', path: '/settings', icon: Setting }
])

const isActive = (item) => {
  if (item.path === '/profile') {
    return route.path === '/profile'
  }

  return route.path === item.path || route.path.startsWith(`${item.path}/`)
}
</script>

<style lang="scss" scoped>
.account-layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
}

.account-sidebar {
  position: sticky;
  top: 120px;
  padding: 24px 20px;
  height: fit-content;
}

.account-user {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.account-user-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;

  strong {
    font-size: 18px;
    color: #111827;
  }

  span {
    color: #6b7280;
    font-size: 13px;
    line-height: 1.5;
  }
}

.account-nav {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.account-nav-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #f5f7fa;
    color: #409eff;
  }

  &.active {
    background: #ecf5ff;
    color: #409eff;
    font-weight: 600;
  }
}

.account-content {
  min-width: 0;
}

@media (max-width: 992px) {
  .account-layout {
    grid-template-columns: 1fr;
  }

  .account-sidebar {
    position: static;
  }

  .account-nav {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .account-layout {
    padding: 16px;
  }

  .account-nav {
    grid-template-columns: 1fr;
  }
}
</style>
