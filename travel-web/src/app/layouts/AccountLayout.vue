<!-- 用户中心布局 -->
<template>
  <div class="account-layout">
    <aside class="account-sidebar premium-card">
      <div class="account-user">
        <el-avatar :size="64" :src="getAvatarUrl(userStore.userInfo?.avatar)" icon="User" />
        <div class="account-user-meta">
          <p class="account-user-eyebrow">Personal Space</p>
          <strong>{{ userStore.userInfo?.nickname || '旅行家' }}</strong>
          <span>{{ userStore.userInfo?.phone || '完善资料后，可同步获得更稳定的个性推荐。' }}</span>
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
          <span class="account-nav-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <span class="account-nav-copy">
            <strong>{{ item.label }}</strong>
            <span>{{ item.desc }}</span>
          </span>
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
import { ACCOUNT_ROUTE_PATHS } from '@/modules/account/constants/routes.js'
import { useUserStore } from '@/modules/account/store/user.js'
import { getAvatarUrl } from '@/shared/api/client.js'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 导航项集中定义，保证文案、图标和跳转路径在账户中心保持一致。
const navItems = computed(() => [
  { label: '个人中心', desc: '查看基础信息与偏好摘要', path: ACCOUNT_ROUTE_PATHS.profile, icon: User },
  { label: '我的互动', desc: '回看评论、足迹和最近操作', path: ACCOUNT_ROUTE_PATHS.activity, icon: ChatDotRound },
  { label: '我的订单', desc: '继续支付或查看行程状态', path: ACCOUNT_ROUTE_PATHS.orders, icon: Tickets },
  { label: '我的收藏', desc: '集中管理想去和已收藏内容', path: ACCOUNT_ROUTE_PATHS.favorites, icon: Collection },
  { label: '我的评价', desc: '管理你发布过的评价记录', path: ACCOUNT_ROUTE_PATHS.reviews, icon: Document },
  { label: '设置', desc: '修改账号、安全与体验选项', path: ACCOUNT_ROUTE_PATHS.settings, icon: Setting }
])

const isActive = (item) => {
  // “个人中心”是账户根页，需要避免被其他 /account/* 子路径误判为激活。
  if (item.path === ACCOUNT_ROUTE_PATHS.profile) {
    return route.path === ACCOUNT_ROUTE_PATHS.profile
  }

  return route.path === item.path || route.path.startsWith(`${item.path}/`)
}
</script>

<style lang="scss" scoped>
.account-layout {
  width: min(100%, 1240px);
  margin: 0 auto;
  padding: 8px 24px 0;
  display: grid;
  grid-template-columns: 330px minmax(0, 1fr);
  gap: 24px;
}

.account-sidebar {
  position: sticky;
  top: 164px;
  padding: 24px 20px;
  height: fit-content;
}

.account-user {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 22px;
}

.account-user-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.account-user-eyebrow {
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #64748b;
}

.account-user-meta strong {
  font-size: 20px;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.account-user-meta span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.account-nav {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.account-nav-item {
  width: 100%;
  padding: 14px;
  border: 1px solid transparent;
  border-radius: 20px;
  background: transparent;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.account-nav-item:hover {
  background: rgba(248, 250, 252, 0.9);
  border-color: #dbeafe;
}

.account-nav-item.active {
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
  border-color: #bfdbfe;
  box-shadow: 0 22px 32px -28px rgba(37, 99, 235, 0.82);
}

.account-nav-icon {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  flex: none;
}

.account-nav-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
}

.account-nav-copy strong {
  font-size: 15px;
  color: #0f172a;
}

.account-nav-copy span {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.account-content {
  min-width: 0;
}

@media (max-width: 1100px) {
  .account-layout {
    grid-template-columns: 1fr;
  }

  .account-sidebar {
    position: static;
  }
}

@media (max-width: 768px) {
  .account-layout {
    padding: 0 16px;
  }

  .account-sidebar {
    padding: 20px 16px;
    border-radius: 22px;
  }
}
</style>
