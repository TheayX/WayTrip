<!-- 管理后台主布局组件 -->
<template>
  <el-container class="layout-container">
    <!-- 侧边栏: 外层用于制造浮动边距 -->
    <div class="aside-wrapper" :style="{ width: isCollapse ? '80px' : '240px' }">
      <el-aside width="100%" class="aside">
        <!-- Logo 区域 -->
        <div class="logo">
          <div class="logo-icon">✨</div>
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
            <span class="search-placeholder">Search... (⌘K)</span>
          </div>
        </div>
        <div class="header-right">
          <!-- 面包屑导航简化 -->
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item>{{ currentGroupTitle }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
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
import { Fold, Expand, Search, Bell, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isCollapse = ref(false)

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
  background-color: var(--el-bg-color-page, #f3f4f6);
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
  background-color: #ffffff;
  height: 100%;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
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
      font-size: 24px;
      margin-right: 8px;
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
      background: #e5e7eb;
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
      background-color: #e5e7eb;
      color: var(--el-text-color-primary);
    }
  }

  .top-search {
    display: flex;
    align-items: center;
    background-color: #ffffff;
    padding: 0 16px;
    height: 36px;
    border-radius: 18px;
    color: var(--el-text-color-secondary);
    font-size: 14px;
    cursor: text;
    border: 1px solid var(--el-border-color-light);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

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
    padding: 4px 12px;
    border-radius: 20px;
    background: #ffffff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
    border: 1px solid var(--el-border-color-light);
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
    border-radius: 16px;
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
