<!-- 管理后台主布局组件 -->
<template>
  <el-container class="layout-container">
    <!-- 侧边栏: 外层用于制造浮动边距 -->
    <div class="aside-wrapper" :style="{ width: isCollapse ? '80px' : '240px' }">
      <el-aside width="100%" class="aside">
        <!-- Logo 区域 -->
        <div class="logo">
          <img :src="brandMarkUrl" alt="" aria-hidden="true" class="logo-icon" />
          <span v-if="!isCollapse" class="logo-text">WayTrip Admin</span>
        </div>
        <!-- 导航菜单 -->
        <el-menu
          :default-active="$route.path"
          :default-openeds="defaultOpenGroups"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          class="aside-menu"
        >
          <template v-for="group in groupedMenuList" :key="group.key">
            <el-menu-item v-if="group.single && group.items.length === 1" :index="group.items[0].fullPath">
              <el-icon><component :is="group.icon" /></el-icon>
              <template #title>{{ group.items[0].meta.title }}</template>
            </el-menu-item>
            <el-sub-menu v-else :index="group.key">
              <template #title>
                <el-icon><component :is="group.icon" /></el-icon>
                <span>{{ group.title }}</span>
              </template>
              <el-menu-item v-for="item in group.items" :key="item.path" :index="item.fullPath">
                <el-icon><component :is="item.meta.icon" /></el-icon>
                <template #title>{{ item.meta.title }}</template>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-aside>
    </div>

    <el-container class="main-container">
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-left">
          <!-- 折叠按钮 -->
          <div class="action-icon" @click="isCollapse = !isCollapse">
            <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </div>
          <!-- 快捷搜索 (仅 UI 演示) -->
          <div class="top-search" v-if="!isCollapse">
            <el-icon><Search /></el-icon>
            <span class="search-placeholder">搜索页面、功能或数据</span>
          </div>
        </div>
        <div class="header-right">
          <!-- 面包屑导航简化 -->
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item>{{ currentGroupTitle }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
          <!-- 主题切换 -->
          <el-dropdown trigger="click" @command="setThemeMode">
            <button type="button" class="theme-switch">
              <el-icon><Sunny v-if="currentTheme === 'light'" /><Moon v-else /></el-icon>
              <span>{{ currentThemeLabel }}</span>
              <span v-if="isSystemMode" class="theme-switch-mode">系统</span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="item in THEME_MODE_OPTIONS"
                  :key="item.value"
                  :command="item.value"
                >
                  {{ item.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <!-- 消息通知角标 -->
          <div class="action-icon">
            <el-badge is-dot class="item">
              <el-icon><Bell /></el-icon>
            </el-badge>
          </div>
          <!-- 用户下拉菜单 -->
          <el-dropdown @command="handleCommand" trigger="click">
            <span class="user-info">
              <el-avatar :size="32" class="user-avatar" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
              <span class="user-name">{{ userStore.adminInfo?.realName || userStore.adminInfo?.username || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main">
        <div class="main-content-wrapper">
          <router-view v-slot="{ Component }">
            <transition name="fade-transform" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/app/store/user.js'
import { NAVIGATION_GROUPS, NAVIGATION_GROUP_MAP } from '@/shared/constants/navigation.js'
import { THEME_MODE_OPTIONS } from '@/shared/constants/theme.js'
import { useTheme } from '@/shared/composables/useTheme.js'
import { Fold, Expand, Search, Bell, ArrowDown, Moon, Sunny } from '@element-plus/icons-vue'
import brandMarkUrl from '@/shared/assets/brand/waytrip-mark.svg'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isCollapse = ref(false)
const { currentTheme, isSystemMode, setThemeMode } = useTheme()

const groupedMenuList = computed(() => {
  const mainRoute = router.options.routes.find(r => r.path === '/')
  const leafRoutes = (mainRoute?.children || []).map(item => ({
    ...item,
    fullPath: item.path.startsWith('/') ? item.path : `/${item.path}`
  }))

  return NAVIGATION_GROUPS.map(group => ({
    ...group,
    items: leafRoutes.filter(item => item.meta?.group === group.key)
  })).filter(group => group.items.length > 0)
})

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

const currentGroupTitle = computed(() => {
  return NAVIGATION_GROUP_MAP[route.meta?.group]?.title || ''
})

const defaultOpenGroups = computed(() => {
  return currentGroupTitle.value ? [route.meta?.group] : []
})

const currentThemeLabel = computed(() => {
  return currentTheme.value === 'dark' ? '暗色主题' : '浅色主题'
})

onMounted(async () => {
  if (userStore.token && !userStore.adminInfo) {
    try {
      await userStore.getInfo()
    } catch (e) {
      userStore.logout()
      router.push('/login')
    }
  }
})
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
  width: 100vw;
  background: var(--bg-page);
  overflow: hidden;
  display: flex;
}

/* 浮动侧边栏包裹器 */
.aside-wrapper {
  padding: 16px 0 16px 16px;
  transition: width 0.3s cubic-bezier(0.2, 0, 0, 1) 0s;
  flex-shrink: 0;
}

.aside {
  background: var(--wt-surface-panel);
  border: 1px solid var(--wt-border-default);
  height: 100%;
  border-radius: 24px;
  box-shadow: var(--wt-shadow-card);
  backdrop-filter: blur(20px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: none;

  .logo {
    height: 72px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;
    cursor: pointer;
    flex-shrink: 0;

    .logo-icon {
      width: 28px;
      height: 28px;
      margin-right: 8px;
      flex-shrink: 0;
      display: block;
    }

    .logo-text {
      font-size: 18px;
      font-weight: 700;
      color: var(--el-text-color-primary);
      background: linear-gradient(135deg, var(--el-color-primary), #6366f1);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      white-space: nowrap;
    }
  }

  .aside-menu {
    border-right: none;
    flex: 1;
    overflow-y: auto;
    background-color: transparent;
    padding: 0 12px 16px;
    
    &::-webkit-scrollbar {
      width: 4px;
    }
    &::-webkit-scrollbar-thumb {
      background: var(--wt-scrollbar-thumb);
      border-radius: 4px;
    }

    :deep(.el-menu-item), :deep(.el-sub-menu__title) {
      height: 44px;
      line-height: 44px;
      border-radius: 8px;
      margin-bottom: 4px;
      color: var(--el-text-color-regular);

      &:hover {
        background-color: var(--el-color-primary-light-9);
        color: var(--el-color-primary);
      }
    }

    :deep(.el-menu-item.is-active) {
      background-color: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
      font-weight: 600;
      
      &::before {
        content: '';
        position: absolute;
        left: -12px;
        top: 25%;
        height: 50%;
        width: 4px;
        background: var(--el-color-primary);
        border-radius: 0 4px 4px 0;
      }
    }
  }
}

.main-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.header {
  height: 72px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px 0 16px;
  background-color: transparent;

  .header-left, .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .action-icon {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    cursor: pointer;
    color: var(--el-text-color-regular);
    transition: all 0.2s;
    font-size: 18px;

    &:hover {
      background-color: var(--wt-surface-hover);
      color: var(--el-text-color-primary);
    }
  }

  .theme-switch {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    height: 38px;
    padding: 0 14px;
    border: 1px solid var(--wt-border-default);
    border-radius: 999px;
    background: var(--wt-surface-elevated);
    box-shadow: var(--wt-shadow-soft);
    color: var(--el-text-color-primary);
    cursor: pointer;
    transition: transform 0.2s ease, border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-1px);
      border-color: var(--el-color-primary-light-5);
      box-shadow: var(--wt-shadow-float);
    }

    span {
      font-size: 13px;
      font-weight: 600;
      line-height: 1;
    }

    .theme-switch-mode {
      padding: 2px 8px;
      font-size: 12px;
      color: var(--wt-text-secondary);
      background: var(--el-color-primary-light-9);
      border-radius: 999px;
    }
  }

  .top-search {
    display: flex;
    align-items: center;
    background: var(--wt-surface-panel);
    padding: 0 16px;
    height: 40px;
    border-radius: 999px;
    color: var(--el-text-color-secondary);
    font-size: 14px;
    cursor: text;
    border: 1px solid var(--wt-border-default);
    box-shadow: var(--wt-shadow-soft);

    .search-placeholder {
      margin-left: 8px;
    }
  }

  .breadcrumb {
    margin-right: 16px;
    :deep(.el-breadcrumb__inner) {
      color: var(--el-text-color-secondary);
      font-weight: 500;
    }
    :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
      color: var(--el-text-color-primary);
      font-weight: 600;
    }
  }

  .user-info {
    display: flex;
    align-items: center;
    cursor: pointer;
    padding: 5px 12px 5px 8px;
    border-radius: 999px;
    background: var(--wt-surface-elevated);
    box-shadow: var(--wt-shadow-soft);
    border: 1px solid var(--wt-border-default);
    transition: all 0.2s;
    
    &:hover {
      border-color: var(--el-color-primary-light-5);
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
    }

    .user-avatar {
      margin-right: 10px;
    }
    
    .user-name {
      color: var(--el-text-color-primary);
      font-weight: 600;
      margin-right: 8px;
      font-size: 14px;
    }
    
    .el-icon {
      color: var(--el-text-color-secondary);
    }
  }
}

.main {
  padding: 0 24px 24px 24px;
  
  .main-content-wrapper {
    height: 100%;
    border-radius: 24px;
    /* transition animation */
    .fade-transform-leave-active,
    .fade-transform-enter-active {
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .fade-transform-enter-from {
      opacity: 0;
      transform: translateX(10px);
    }
    .fade-transform-leave-to {
      opacity: 0;
      transform: translateX(-10px);
    }
  }
}
</style>
