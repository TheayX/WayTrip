<!-- 管理后台主布局组件 -->
<template>
  <el-container class="layout-container">
    <!-- 侧边栏: 通栏贯穿设计 -->
    <el-aside :width="isCollapse ? '80px' : '260px'" class="aside">
      <div class="logo">
        <img v-if="isCollapse" :src="brandMarkUrl" alt="" aria-hidden="true" class="logo-icon" />
        <template v-else>
          <img :src="brandLogoUrl" alt="WayTrip" class="logo-full" />
          <span class="logo-text">ADMIN</span>
        </template>
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

    <el-container class="main-container">
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-left">
          <!-- 折叠按钮 -->
          <div class="action-icon" @click="isCollapse = !isCollapse">
            <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </div>
          <!-- 全局快捷搜索 -->
          <div
            v-if="!isCollapse"
            class="top-search"
            @focusin="handleSearchFocus"
            @focusout="handleSearchBlur"
          >
            <el-icon class="top-search-icon"><Search /></el-icon>
            <input
              v-model.trim="globalSearchKeyword"
              type="text"
              class="top-search-input"
              placeholder="搜索页面、功能或数据"
              @keydown.down.prevent="moveActiveResult(1)"
              @keydown.up.prevent="moveActiveResult(-1)"
              @keydown.enter.prevent="handleSearchEnter"
              @keydown.esc.prevent="closeSearchPanel"
            />
            <button
              v-if="globalSearchKeyword"
              type="button"
              class="top-search-clear"
              aria-label="清空搜索"
              @mousedown.prevent
              @click="clearSearchKeyword"
            >
              清空
            </button>

            <div v-if="showSearchPanel" class="top-search-panel">
              <div v-if="filteredSearchResults.length" class="top-search-result-list">
                <button
                  v-for="(item, index) in filteredSearchResults"
                  :key="item.key"
                  type="button"
                  class="top-search-result"
                  :class="{ 'is-active': index === activeSearchResultIndex }"
                  @mousedown.prevent="handleSearchSelect(item)"
                  @mouseenter="activeSearchResultIndex = index"
                >
                  <div class="top-search-result-main">
                    <span class="top-search-result-title">{{ item.title }}</span>
                    <span class="top-search-result-group">{{ item.groupTitle }}</span>
                  </div>
                  <div class="top-search-result-desc">{{ item.description }}</div>
                </button>
              </div>
              <div v-else class="top-search-empty">
                没有匹配项，试试输入“景点”“订单”“用户”等关键词。
              </div>
            </div>
          </div>
        </div>
        <div class="header-right">
          <!-- 面包屑导航简化 -->
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item>{{ currentGroupTitle }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
          <img :src="brandMarkUrl" alt="" aria-hidden="true" class="header-brand-icon" />
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/app/store/user.js'
import { NAVIGATION_GROUPS, NAVIGATION_GROUP_MAP } from '@/shared/constants/navigation.js'
import { THEME_MODE_OPTIONS } from '@/shared/constants/theme.js'
import { useTheme } from '@/shared/composables/useTheme.js'
import { Fold, Expand, Search, Bell, ArrowDown, Moon, Sunny } from '@element-plus/icons-vue'
import brandMarkUrl from '@/shared/assets/brand/waytrip-mark.svg'
import brandLogoUrl from '@/shared/assets/brand/waytrip-logo.svg'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isCollapse = ref(false)
const { currentTheme, isSystemMode, setThemeMode } = useTheme()
const globalSearchKeyword = ref('')
const searchPanelVisible = ref(false)
const activeSearchResultIndex = ref(0)

const GLOBAL_SEARCH_ITEMS = [
  {
    key: 'dashboard',
    title: '运营概览',
    description: '快速进入仪表盘总览页',
    group: 'dashboard',
    path: '/dashboard',
    keywords: ['概览', '仪表盘', '首页', 'dashboard']
  },
  {
    key: 'spot',
    title: '景点管理',
    description: '跳转到景点管理，并按关键词筛选景点',
    group: 'content',
    path: '/spot',
    keywords: ['景点', 'spot', '景区', '门票'],
    buildQuery: (keyword) => keyword ? { keyword } : {}
  },
  {
    key: 'guide',
    title: '攻略管理',
    description: '跳转到攻略管理，并按标题关键词筛选攻略',
    group: 'content',
    path: '/guide',
    keywords: ['攻略', 'guide', '游记', '内容'],
    buildQuery: (keyword) => keyword ? { keyword } : {}
  },
  {
    key: 'banner',
    title: '轮播图管理',
    description: '进入轮播图配置页',
    group: 'content',
    path: '/banner',
    keywords: ['轮播图', 'banner', '海报']
  },
  {
    key: 'category',
    title: '分类管理',
    description: '进入景点分类维护页',
    group: 'content',
    path: '/category',
    keywords: ['分类', 'category', '标签']
  },
  {
    key: 'region',
    title: '地区管理',
    description: '进入地区树维护页',
    group: 'content',
    path: '/region',
    keywords: ['地区', '区域', '城市', 'region']
  },
  {
    key: 'order-order-no',
    title: '订单中心',
    description: '跳转到订单中心，并按订单号筛选',
    group: 'transaction',
    path: '/order',
    keywords: ['订单', 'order', '订单号', '交易'],
    buildQuery: (keyword) => keyword ? { orderNo: keyword } : {}
  },
  {
    key: 'order-spot-name',
    title: '订单中心 / 景点名',
    description: '跳转到订单中心，并按景点名称筛选订单',
    group: 'transaction',
    path: '/order',
    keywords: ['订单', '景点订单', 'spot order', '交易'],
    buildQuery: (keyword) => keyword ? { spotName: keyword } : {}
  },
  {
    key: 'user',
    title: '用户管理',
    description: '跳转到用户管理，并按昵称筛选用户',
    group: 'user-ops',
    path: '/user',
    keywords: ['用户', 'user', '昵称', '会员'],
    buildQuery: (keyword) => keyword ? { nickname: keyword } : {}
  },
  {
    key: 'review-nickname',
    title: '评价管理 / 用户',
    description: '跳转到评价管理，并按用户昵称筛选评价',
    group: 'user-ops',
    path: '/review',
    keywords: ['评价', '评论', 'review', '用户评价'],
    buildQuery: (keyword) => keyword ? { nickname: keyword } : {}
  },
  {
    key: 'review-spot-name',
    title: '评价管理 / 景点',
    description: '跳转到评价管理，并按景点名称筛选评价',
    group: 'user-ops',
    path: '/review',
    keywords: ['评价', '评论', '景点评价', 'review'],
    buildQuery: (keyword) => keyword ? { spotName: keyword } : {}
  },
  {
    key: 'favorite-nickname',
    title: '用户收藏 / 用户',
    description: '跳转到用户收藏页，并按昵称筛选',
    group: 'user-ops',
    path: '/favorite',
    keywords: ['收藏', 'favorite', '用户收藏'],
    buildQuery: (keyword) => keyword ? { nickname: keyword } : {}
  },
  {
    key: 'favorite-spot-name',
    title: '用户收藏 / 景点',
    description: '跳转到用户收藏页，并按景点名称筛选',
    group: 'user-ops',
    path: '/favorite',
    keywords: ['收藏', '景点收藏', 'favorite'],
    buildQuery: (keyword) => keyword ? { spotName: keyword } : {}
  },
  {
    key: 'preference',
    title: '用户偏好',
    description: '跳转到用户偏好页，并按昵称筛选',
    group: 'user-ops',
    path: '/preference',
    keywords: ['偏好', 'preference', '兴趣', '画像'],
    buildQuery: (keyword) => keyword ? { nickname: keyword } : {}
  },
  {
    key: 'view-log-nickname',
    title: '浏览行为 / 用户',
    description: '跳转到浏览行为页，并按昵称筛选',
    group: 'user-ops',
    path: '/view-log',
    keywords: ['浏览', '行为', '日志', 'view log'],
    buildQuery: (keyword) => keyword ? { nickname: keyword } : {}
  },
  {
    key: 'view-log-spot-name',
    title: '浏览行为 / 景点',
    description: '跳转到浏览行为页，并按景点名称筛选',
    group: 'user-ops',
    path: '/view-log',
    keywords: ['浏览', '景点浏览', 'view log', '行为'],
    buildQuery: (keyword) => keyword ? { spotName: keyword } : {}
  },
  {
    key: 'recommendation-overview',
    title: '推荐总览',
    description: '进入推荐系统运行总览页',
    group: 'recommendation',
    path: '/recommendation',
    keywords: ['推荐', 'recommendation', '算法']
  },
  {
    key: 'recommendation-config',
    title: '推荐配置',
    description: '进入推荐配置页调整参数',
    group: 'recommendation',
    path: '/recommendation/config',
    keywords: ['推荐配置', '算法配置', 'recommendation config']
  },
  {
    key: 'admin',
    title: '管理员管理',
    description: '跳转到管理员管理，并按姓名或用户名筛选',
    group: 'system',
    path: '/admin',
    keywords: ['管理员', 'admin', '账号', '系统管理'],
    buildQuery: (keyword) => keyword ? { keyword } : {}
  }
]

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

// 搜索结果同时匹配标题、说明、分组和别名，保证常见中文口语也能命中。
const filteredSearchResults = computed(() => {
  const keyword = globalSearchKeyword.value.trim().toLowerCase()

  return GLOBAL_SEARCH_ITEMS
    .map((item) => ({
      ...item,
      groupTitle: NAVIGATION_GROUP_MAP[item.group]?.title || '快捷入口'
    }))
    .filter((item) => {
      if (!keyword) {
        return true
      }
      const haystacks = [
        item.title,
        item.description,
        item.groupTitle,
        ...(item.keywords || [])
      ]
      return haystacks.some((value) => String(value).toLowerCase().includes(keyword))
    })
})

const showSearchPanel = computed(() => {
  return searchPanelVisible.value && !isCollapse.value
})

const closeSearchPanel = () => {
  searchPanelVisible.value = false
}

const handleSearchFocus = () => {
  searchPanelVisible.value = true
}

const handleSearchBlur = () => {
  window.setTimeout(() => {
    searchPanelVisible.value = false
  }, 120)
}

const moveActiveResult = (direction) => {
  if (!filteredSearchResults.value.length) {
    return
  }
  searchPanelVisible.value = true
  const maxIndex = filteredSearchResults.value.length - 1
  activeSearchResultIndex.value = direction > 0
    ? (activeSearchResultIndex.value >= maxIndex ? 0 : activeSearchResultIndex.value + 1)
    : (activeSearchResultIndex.value <= 0 ? maxIndex : activeSearchResultIndex.value - 1)
}

const handleSearchSelect = (item) => {
  const keyword = globalSearchKeyword.value.trim()
  closeSearchPanel()
  router.push({
    path: item.path,
    query: item.buildQuery ? item.buildQuery(keyword) : {}
  })
}

const handleSearchEnter = () => {
  const target = filteredSearchResults.value[activeSearchResultIndex.value] || filteredSearchResults.value[0]
  if (!target) {
    return
  }
  handleSearchSelect(target)
}

const clearSearchKeyword = () => {
  globalSearchKeyword.value = ''
  activeSearchResultIndex.value = 0
  searchPanelVisible.value = true
}

watch(globalSearchKeyword, () => {
  activeSearchResultIndex.value = 0
})

watch(() => route.fullPath, () => {
  closeSearchPanel()
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

.aside {
  background: var(--wt-surface-panel);
  border-right: 1px solid var(--wt-border-default);
  height: 100%;
  border-radius: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.2s cubic-bezier(0.2, 0, 0, 1) 0s;
  flex-shrink: 0;
  z-index: 10;

  .logo {
    min-height: 84px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px 20px;
    cursor: pointer;
    flex-shrink: 0;
    border-bottom: 1px solid var(--wt-border-default);
    flex-direction: column;
    gap: 6px;

    .logo-icon {
      width: 28px;
      height: 28px;
      flex-shrink: 0;
      display: block;
    }

    .logo-full {
      width: 132px;
      height: auto;
      display: block;
      flex-shrink: 0;
    }

    .logo-text {
      font-size: 14px;
      font-weight: 700;
      letter-spacing: 0.25em;
      color: var(--wt-text-regular);
      line-height: 1;
    }
  }

  .aside-menu {
    border-right: none;
    flex: 1;
    overflow-y: auto;
    background-color: transparent;
    padding: 16px 16px;
    
    &::-webkit-scrollbar {
      width: 0; /* 隐藏侧边栏滚动条 */
    }

    :deep(.el-menu-item), :deep(.el-sub-menu__title) {
      height: 40px;
      line-height: 40px;
      border-radius: 8px;
      margin-bottom: 4px;
      color: var(--wt-text-regular);
      font-size: 14px;

      &:hover {
        background-color: var(--wt-surface-hover);
        color: var(--wt-text-primary);
      }
    }

    :deep(.el-menu-item.is-active) {
      background-color: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
      font-weight: 600;
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
  padding: 0 32px;
  background-color: var(--wt-surface-panel);
  border-bottom: 1px solid var(--wt-border-default);

  .header-left, .header-right {
    display: flex;
    align-items: center;
    gap: 20px;
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

  .header-brand-icon {
    width: 26px;
    height: 26px;
    display: block;
    flex-shrink: 0;
    opacity: 0.96;
    transition: opacity 0.2s ease, transform 0.2s ease;

    &:hover {
      opacity: 1;
      transform: translateY(-1px);
    }
  }

  .top-search {
    position: relative;
    display: flex;
    align-items: center;
    background: var(--wt-fill-striped);
    padding: 0 14px;
    height: 38px;
    border-radius: 8px;
    color: var(--el-text-color-secondary);
    font-size: 14px;
    border: 1px solid var(--wt-border-default);
    min-width: 320px;
    transition: all 0.2s ease;

    &:focus-within {
      border-color: var(--el-color-primary);
      box-shadow: 0 0 0 1px var(--el-color-primary);
      background: var(--wt-surface-elevated);
    }

    .top-search-icon {
      flex-shrink: 0;
    }

    .top-search-input {
      flex: 1;
      height: 100%;
      margin-left: 8px;
      border: none;
      outline: none;
      background: transparent;
      color: var(--el-text-color-primary);
      font-size: 14px;

      &::placeholder {
        color: var(--el-text-color-secondary);
      }
    }

    .top-search-clear {
      border: none;
      background: transparent;
      color: var(--el-color-primary);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      padding: 0;
      margin-left: 8px;
      flex-shrink: 0;
    }

    .top-search-panel {
      position: absolute;
      top: calc(100% + 12px);
      left: 0;
      width: 100%;
      padding: 10px;
      border-radius: 18px;
      border: 1px solid var(--wt-border-default);
      background: var(--wt-surface-elevated);
      box-shadow: 0 18px 36px rgba(15, 23, 42, 0.14);
      z-index: 30;
    }

    .top-search-result-list {
      display: flex;
      flex-direction: column;
      gap: 6px;
      max-height: 360px;
      overflow-y: auto;
    }

    .top-search-result {
      width: 100%;
      border: none;
      text-align: left;
      padding: 12px 14px;
      border-radius: 14px;
      background: transparent;
      cursor: pointer;
      transition: background-color 0.2s ease, transform 0.2s ease;

      &:hover,
      &.is-active {
        background: var(--el-color-primary-light-9);
        transform: translateY(-1px);
      }
    }

    .top-search-result-main {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-bottom: 4px;
    }

    .top-search-result-title {
      color: var(--el-text-color-primary);
      font-weight: 600;
    }

    .top-search-result-group {
      color: var(--el-color-primary);
      font-size: 12px;
      flex-shrink: 0;
    }

    .top-search-result-desc {
      color: var(--el-text-color-secondary);
      font-size: 12px;
      line-height: 1.5;
    }

    .top-search-empty {
      padding: 14px;
      color: var(--el-text-color-secondary);
      font-size: 13px;
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

@media (max-width: 1280px) {
  .header {
    .top-search {
      min-width: 260px;
    }
  }
}

@media (max-width: 960px) {
  .header {
    .breadcrumb {
      display: none;
    }

    .top-search {
      min-width: 220px;
    }
  }
}
</style>
