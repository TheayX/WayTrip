<!-- 主布局 -->
<template>
  <div class="app-shell">
    <header class="shell-header">
      <div class="page-container">
        <div class="shell-header-bar glass-panel">
           <div class="shell-left">
             <router-link to="/" class="brand-link" :aria-label="`${APP_NAME} 首页`">
               <img :src="brandLogoUrl" :alt="APP_NAME" class="brand-logo">
             </router-link>

            <nav class="shell-nav">
              <router-link to="/" class="shell-nav-link" :class="{ active: isHomeActive }">首页</router-link>
              <router-link to="/discover" class="shell-nav-link" active-class="active">发现</router-link>
              <router-link to="/spots" class="shell-nav-link" active-class="active">景点</router-link>
              <router-link to="/guides" class="shell-nav-link" active-class="active">攻略</router-link>
              <el-dropdown trigger="hover" placement="bottom-start" @command="handleFeatureCommand">
                <button type="button" class="shell-nav-link shell-nav-button" :class="{ active: isFeatureMenuActive }">
                  <span>精选功能</span>
                  <el-icon class="nav-arrow"><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="item in featureMenuItems"
                      :key="item.path"
                      :command="item.path"
                    >
                      <el-icon><component :is="item.icon" /></el-icon>
                      <span>{{ item.label }}</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </nav>
          </div>

          <div class="shell-right">
            <button v-if="route.path !== APP_ROUTE_PATHS.search" type="button" class="search-entry" @click="$router.push(APP_ROUTE_PATHS.search)">
              <el-icon><Search /></el-icon>
              <span>搜索景点 / 攻略</span>
            </button>

            <template v-if="userStore.isLoggedIn">
              <el-dropdown trigger="click" @command="handleCommand">
                <button type="button" class="user-trigger">
                  <el-avatar :size="34" :src="getAvatarUrl(userStore.userInfo?.avatar)" icon="User" />
                  <span class="user-copy">
                    <strong>{{ userStore.userInfo?.nickname || '旅行家' }}</strong>
                    <span>个人空间</span>
                  </span>
                  <el-icon class="user-arrow"><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">
                      <el-icon><User /></el-icon>个人中心
                    </el-dropdown-item>
                    <el-dropdown-item command="activity">
                      <el-icon><ChatDotRound /></el-icon>我的互动
                    </el-dropdown-item>
                    <el-dropdown-item command="orders">
                      <el-icon><Tickets /></el-icon>我的订单
                    </el-dropdown-item>
                    <el-dropdown-item command="settings">
                      <el-icon><Setting /></el-icon>设置
                    </el-dropdown-item>
                    <el-dropdown-item divided command="logout">
                      <el-icon><SwitchButton /></el-icon>退出登录
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <el-button type="primary" @click="$router.push(AUTH_ROUTE_PATHS.login)">登录</el-button>
            </template>
          </div>
        </div>
      </div>
    </header>

    <main class="shell-main">
      <div v-if="showBackBar" class="context-bar">
        <div class="page-container">
          <div class="context-bar-inner glass-panel">
            <button type="button" class="context-back" @click="handleBack">
              <el-icon><ArrowLeft /></el-icon>
              <span>返回上一页</span>
            </button>
            <p class="context-text">{{ currentPageText }}</p>
          </div>
        </div>
      </div>
      <router-view />
    </main>

    <footer class="shell-footer">
      <div class="page-container">
        <div class="shell-footer-panel premium-card">
          <div class="footer-brand">
            <div class="footer-brand-head">
              <img :src="brandMarkUrl" alt="" aria-hidden="true" class="brand-mark brand-mark-footer">
              <div>
                <strong>{{ APP_NAME }}</strong>
                <p>把推荐、附近和攻略整理成更有秩序的旅行探索体验。</p>
              </div>
            </div>
          </div>

          <div class="footer-links">
            <router-link to="/discover">发现灵感</router-link>
            <router-link to="/spots">热门景点</router-link>
            <router-link to="/guides">旅行攻略</router-link>
            <router-link to="/random-pick">随心一选</router-link>
          </div>

          <div class="footer-meta">
            <span>Curated travel planning for modern explorers.</span>
            <span>© 2026 {{ APP_NAME }}</span>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/modules/account/store/user.js'
import { ACCOUNT_ROUTE_PATHS } from '@/modules/account/constants/routes.js'
import { APP_NAME } from '@/shared/constants/app.js'
import { ROUTE_NAMES } from '@/shared/constants/route-names.js'
import { APP_ROUTE_PATHS, AUTH_ROUTE_PATHS } from '@/shared/constants/route-paths.js'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAvatarUrl } from '@/shared/api/client.js'
import { ArrowDown, ChatDotRound, Discount, MagicStick, Search, Setting, Stopwatch, Tickets, User, SwitchButton, ArrowLeft } from '@element-plus/icons-vue'
import brandLogoUrl from '@/shared/assets/brand/waytrip-standard.svg'
import brandMarkUrl from '@/shared/assets/brand/waytrip-standard-mark.svg'

// 基础依赖与路由状态
const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
// 精选功能菜单与高亮态共用同一组路由定义，避免文案和激活规则分叉维护。
const featureRouteNames = [
  ROUTE_NAMES.randomPick,
  ROUTE_NAMES.budgetTravel,
  ROUTE_NAMES.travelerReviews,
  ROUTE_NAMES.trendingViews,
  ROUTE_NAMES.more
]
const featureMenuItems = [
  { label: '随心一选', path: APP_ROUTE_PATHS.randomPick, icon: MagicStick },
  { label: '穷游玩法', path: APP_ROUTE_PATHS.budgetTravel, icon: Discount },
  { label: '游客口碑', path: APP_ROUTE_PATHS.travelerReviews, icon: ChatDotRound },
  { label: '近期热看', path: APP_ROUTE_PATHS.trendingViews, icon: Stopwatch }
]

// 计算属性
const isHomeActive = computed(() => route.name === ROUTE_NAMES.home)
const isFeatureMenuActive = computed(() => featureRouteNames.includes(route.name))
const hiddenBackRoutes = [ROUTE_NAMES.home, ROUTE_NAMES.login, ROUTE_NAMES.register]
const showBackBar = computed(() => {
  if (typeof route.meta?.hideBackBar === 'boolean') {
    return !route.meta.hideBackBar
  }
  return !hiddenBackRoutes.includes(route.name)
})
const currentPageText = computed(() => route.meta?.title || '继续浏览当前内容')

// 交互处理方法
const handleBack = () => {
  // 优先回退浏览历史；若用户是直达当前页，则回到首页兜底。
  if (window.history.length > 1) {
    router.back()
    return
  }

  router.push('/')
}

const handleFeatureCommand = (path) => {
  if (!path) return
  router.push(path)
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push(ACCOUNT_ROUTE_PATHS.profile)
      break
    case 'orders':
      router.push(ACCOUNT_ROUTE_PATHS.orders)
      break
    case 'activity':
      router.push(ACCOUNT_ROUTE_PATHS.activity)
      break
    case 'settings':
      router.push(ACCOUNT_ROUTE_PATHS.settings)
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 退出后使用 replace，避免浏览器回退又回到需要登录的个人页。
        userStore.logout()
        ElMessage.success('已退出登录')
        router.replace(AUTH_ROUTE_PATHS.login)
      }).catch(() => {})
      break
  }
}
</script>

<style lang="scss" scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.shell-header {
  position: sticky;
  top: 0;
  z-index: 120;
  padding: 18px 0 0;
}

.shell-header-bar {
  min-height: 76px;
  padding: 12px 16px 12px 20px;
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.shell-left,
.shell-right,
.shell-nav {
  display: flex;
  align-items: center;
}

.shell-left {
  gap: 22px;
  min-width: 0;
}

.shell-right {
  gap: 12px;
  flex-shrink: 0;
}

.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.brand-logo {
  height: 44px;
  width: auto;
  display: block;
  flex-shrink: 0;
}

/* 使用几何图形替代 emoji，让品牌表达更稳定、更像正式产品。 */
.brand-mark {
  width: 42px;
  height: 42px;
  display: block;
  flex-shrink: 0;
  filter: drop-shadow(0 18px 24px rgba(37, 99, 235, 0.2));
}

.shell-nav {
  gap: 4px;
}

.shell-nav-link {
  min-height: 42px;
  padding: 0 16px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #475569;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease;
}

.shell-nav-link:hover {
  color: #1d4ed8;
  background: rgba(239, 246, 255, 0.92);
}

.shell-nav-link.active {
  color: #1d4ed8;
  background: rgba(239, 246, 255, 0.96);
}

.shell-nav-button {
  font-family: inherit;
}

.nav-arrow {
  font-size: 12px;
}

.search-entry {
  min-width: 220px;
  min-height: 46px;
  padding: 0 16px;
  border: 1px solid rgba(226, 232, 240, 0.96);
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.92);
  color: #64748b;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    color 0.2s ease;
}

.search-entry:hover {
  border-color: rgba(147, 197, 253, 0.96);
  background: #ffffff;
  color: #334155;
}

.user-trigger {
  min-height: 48px;
  padding: 6px 10px 6px 6px;
  border: 1px solid rgba(226, 232, 240, 0.96);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.user-trigger:hover {
  border-color: rgba(147, 197, 253, 0.96);
  box-shadow: 0 18px 26px -24px rgba(15, 23, 42, 0.45);
}

.user-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
}

.user-copy strong {
  max-width: 108px;
  font-size: 13px;
  line-height: 1.2;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-copy span,
.user-arrow {
  color: #64748b;
  font-size: 12px;
}

.shell-main {
  flex: 1;
  padding-top: 16px;
}

.context-bar {
  position: sticky;
  top: 94px;
  z-index: 110;
  padding-bottom: 8px;
}

.context-bar-inner {
  min-height: 58px;
  padding: 10px 16px;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.context-back {
  border: none;
  background: transparent;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #334155;
  font-weight: 600;
  cursor: pointer;
}

.context-text {
  color: #64748b;
  font-size: 13px;
}

.shell-footer {
  padding: 32px 0 28px;
}

.shell-footer-panel {
  padding: 28px;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) auto auto;
  gap: 24px;
  align-items: center;
}

.footer-brand-head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark-footer {
  width: 48px;
  height: 48px;
  border-radius: 16px;
}

.footer-brand strong {
  display: block;
  font-size: 18px;
  color: #0f172a;
}

.footer-brand p,
.footer-meta {
  color: #64748b;
  line-height: 1.7;
}

.footer-links {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 18px;
}

.footer-links a {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.footer-links a:hover {
  color: #1d4ed8;
}

.footer-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .shell-header-bar {
    flex-wrap: wrap;
  }

  .shell-left,
  .shell-right {
    width: 100%;
    justify-content: space-between;
  }

  .shell-footer-panel {
    grid-template-columns: 1fr;
  }

  .footer-meta {
    align-items: flex-start;
  }
}

@media (max-width: 992px) {
  .shell-nav {
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .search-entry {
    min-width: 0;
    flex: 1;
  }

  .context-bar {
    top: 144px;
  }
}

@media (max-width: 768px) {
  .shell-header {
    padding-top: 12px;
  }

  .shell-header-bar {
    padding: 14px;
    border-radius: 24px;
  }

  .context-text,
  .footer-meta span:first-child {
    display: none;
  }

  .shell-left,
  .shell-right {
    flex-direction: column;
    align-items: stretch;
  }

  .shell-nav {
    width: 100%;
  }

  .shell-nav-link {
    flex: none;
  }

  .user-trigger,
  .search-entry {
    width: 100%;
  }

  .context-bar {
    top: 188px;
  }

  .context-bar-inner {
    align-items: flex-start;
    flex-direction: column;
  }

  .shell-footer-panel {
    padding: 24px 22px;
  }

  .footer-links {
    gap: 14px;
  }
}
</style>
