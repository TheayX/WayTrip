<!-- 管理后台主布局组件 -->
<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <!-- Logo 区域 -->
      <div class="logo">
        <span v-if="!isCollapse">WayTrip 运营中心</span>
        <span v-else>旅</span>
      </div>
      <!-- 导航菜单 -->
      <el-menu
        :default-active="$route.path"
        :default-openeds="defaultOpenGroups"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        class="aside-menu"
        background-color="#001529"
        text-color="#a6adb4"
        active-text-color="#ffffff"
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

    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-left">
          <!-- 折叠按钮 -->
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <!-- 面包屑导航 -->
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentGroupTitle">{{ currentGroupTitle }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 用户下拉菜单 -->
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="28" class="user-avatar">{{ userStore.adminInfo?.realName?.charAt(0) || '管' }}</el-avatar>
              <span class="user-name">{{ userStore.adminInfo?.realName || userStore.adminInfo?.username || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main">
        <div class="main-content-wrapper">
          <!-- 路由视图：页面内容渲染区域 -->
          <router-view />
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

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
// 侧边栏折叠状态
const isCollapse = ref(false)

// 计算属性：按业务域分组生成菜单结构，避免一级菜单继续横向膨胀
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

// 处理用户下拉菜单命令
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

// 组件挂载时检查用户信息
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
  overflow: hidden;
}

.aside {
  background-color: #001529;
  transition: width 0.3s cubic-bezier(0.2, 0, 0, 1) 0s;
  box-shadow: 2px 0 8px 0 rgba(29, 35, 41, 0.05);
  z-index: 10;
  display: flex;
  flex-direction: column;

  .logo {
    height: 60px;
    line-height: 60px;
    text-align: center;
    color: #fff;
    font-size: 18px;
    font-weight: 600;
    background-color: #002140;
    letter-spacing: 1px;
    flex-shrink: 0;
  }

  .aside-menu {
    border-right: none;
    flex: 1;
    overflow-y: auto;
    
    &::-webkit-scrollbar {
      width: 4px;
    }
    &::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.2);
      border-radius: 4px;
    }

    :deep(.el-menu-item) {
      margin: 4px 8px;
      border-radius: 4px;
      height: 40px;
      line-height: 40px;

      &.is-active {
        background-color: #1890ff;
        color: #fff;
      }

      &:hover:not(.is-active) {
        color: #fff;
      }
    }

    :deep(.el-sub-menu__title) {
      margin: 4px 8px;
      border-radius: 4px;
      height: 40px;
      line-height: 40px;

      &:hover {
        color: #fff;
      }
    }
  }
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 9;
  padding: 0 24px;
  height: 60px;

  .header-left {
    display: flex;
    align-items: center;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      margin-right: 20px;
      color: #5c6b77;
      transition: color 0.3s;
      
      &:hover {
        color: #1890ff;
      }
    }
    
    .breadcrumb {
      :deep(.el-breadcrumb__inner) {
        font-weight: 500;
        color: #666;
      }
      :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
        color: #333;
        font-weight: 600;
      }
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;
      padding: 0 12px;
      height: 40px;
      border-radius: 4px;
      transition: background 0.3s;
      
      &:hover {
        background: #f6f6f6;
      }

      .user-avatar {
        background: #1890ff;
        color: #fff;
        margin-right: 8px;
      }
      
      .user-name {
        color: #333;
        font-weight: 500;
        margin-right: 4px;
      }
      
      .el-icon {
        color: #999;
      }
    }
  }
}

.main {
  background-color: #f0f2f5;
  padding: 24px;
  box-sizing: border-box;
  overflow-y: auto;
  position: relative;

  .main-content-wrapper {
    min-height: calc(100vh - 108px);
    border-radius: 8px;
  }
}
</style>
