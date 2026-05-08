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
                  <span class="top-search-result-main">
                    <span class="top-search-result-title">{{ item.title }}</span>
                    <span class="top-search-result-group">{{ item.groupTitle }}</span>
                  </span>
                  <span class="top-search-result-desc">{{ item.description }}</span>
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
          <!-- 消息通知 -->
          <el-popover
            v-model:visible="notificationPopoverVisible"
            trigger="click"
            placement="bottom-end"
            :width="420"
            :teleported="false"
            popper-class="admin-notification-popover"
            @show="handleNotificationPopoverShow"
          >
            <template #reference>
              <button type="button" class="action-icon notification-trigger" aria-label="消息通知">
                <el-badge :value="notificationCount" :hidden="notificationCount === 0" :max="99">
                  <el-icon><Bell /></el-icon>
                </el-badge>
              </button>
            </template>

            <div class="notification-panel">
              <div class="notification-panel__header">
                <div>
                  <div class="notification-panel__title">消息通知</div>
                  <div class="notification-panel__sub-title">{{ lastLoadedLabel || '最近注册用户和新订单会显示在这里' }}</div>
                </div>
                <div class="notification-panel__actions">
                  <el-button
                    text
                    size="small"
                    :disabled="!hasUnreadNotifications"
                    @click="markAllNotificationsAsRead"
                  >
                    全部已读
                  </el-button>
                  <el-button text size="small" :loading="notificationLoading" @click="refreshNotifications">刷新</el-button>
                </div>
              </div>

              <el-alert
                v-if="notificationErrorMessage"
                :title="notificationErrorMessage"
                type="warning"
                show-icon
                :closable="false"
                class="notification-panel__alert"
              />

              <div v-if="notificationSections.some(section => section.items.length)" class="notification-panel__content">
                <section v-for="section in notificationSections" :key="section.key" class="notification-section">
                  <div class="notification-section__header">
                    <span>{{ section.title }}</span>
                    <el-tag effect="plain" size="small">{{ section.count }}</el-tag>
                  </div>
                   <div v-if="section.items.length" class="notification-list">
                     <button
                       v-for="item in section.items"
                       :key="item.id"
                       type="button"
                       class="notification-item"
                       :class="{ 'is-read': isNotificationRead(item.id) }"
                       @click="openNotification(item)"
                     >
                       <span v-if="!isNotificationRead(item.id)" class="notification-item__badge"></span>
                       <span class="notification-item__main">
                         <span class="notification-item__title-row">
                           <span class="notification-item__type">{{ item.typeLabel }}</span>
                           <span class="notification-item__title">{{ item.title }}</span>
                         </span>
                         <span class="notification-item__desc">{{ item.description || '点击查看详情' }}</span>
                       </span>
                       <span class="notification-item__time">{{ item.timeLabel }}</span>
                     </button>
                   </div>
                </section>
              </div>

              <el-empty v-else description="最近没有新的用户注册或订单" :image-size="72" />
            </div>
          </el-popover>
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
import { useAdminNotifications } from '@/shared/composables/useAdminNotifications.js'
import { ElMessage } from 'element-plus'
import { Fold, Expand, Search, Bell, ArrowDown, Moon, Sunny } from '@element-plus/icons-vue'
import brandMarkUrl from '@/shared/assets/brand/waytrip-standard-mark.svg'
import brandLogoUrl from '@/shared/assets/brand/waytrip-standard.svg'

// 顶层布局状态：控制导航折叠、主题切换以及头部快捷交互。
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isCollapse = ref(false)
const { currentTheme, isSystemMode, setThemeMode } = useTheme()
const {
  loading: notificationLoading,
  errorMessage: notificationErrorMessage,
  lastLoadedAt,
  lastLoadedLabel,
  notificationSections,
  notificationCount,
  hasUnreadNotifications,
  loadNotifications,
  markNotificationAsRead,
  markNotificationsAsRead,
  isNotificationRead
} = useAdminNotifications()
const globalSearchKeyword = ref('')
const searchPanelVisible = ref(false)
const activeSearchResultIndex = ref(0)
const notificationPopoverVisible = ref(false)

// 全局搜索直接映射到后台核心页面，并按页面支持的查询参数生成跳转条件。
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

// 菜单数据从路由表派生，保证导航结构与实际可访问页面保持单一来源。
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

// 通知弹层第一次展开时再加载，减少后台首屏并发请求。
const handleNotificationPopoverShow = () => {
  if (!lastLoadedAt.value) {
    void refreshNotifications()
  }
}

const refreshNotifications = async () => {
  await loadNotifications()
  if (notificationErrorMessage.value) {
    ElMessage.warning(notificationErrorMessage.value)
  }
}

const markAllNotificationsAsRead = () => {
  const unreadNotificationIds = notificationSections.value
    .flatMap(section => section.items)
    .filter(item => !isNotificationRead(item.id))
    .map(item => item.id)

  if (!unreadNotificationIds.length) {
    return
  }

  // 批量已读只更新当前面板中的未读项，和现有本地存储策略保持一致。
  markNotificationsAsRead(unreadNotificationIds)
}

const openNotification = (item) => {
  notificationPopoverVisible.value = false
  // 已读状态先本地落下，保证跳转后再次打开面板时反馈及时。
  markNotificationAsRead(item.id)
  if (!item?.route?.path) return
  router.push({ path: item.route.path, query: item.route.query || {} })
}

// 当前分组标题同时服务于面包屑和侧边栏默认展开逻辑。
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
  // 失焦延迟关闭，给结果项点击留出时间，避免 mousedown 还没处理面板就消失。
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
  // 搜索只负责跳转和透传筛选词，具体筛选行为由目标页面自行解释。
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

// 输入变化后重置高亮项，避免旧索引越界或落在错误结果上。
watch(globalSearchKeyword, () => {
  activeSearchResultIndex.value = 0
})

// 页面跳转后主动收起面板，避免布局切换时残留旧搜索结果。
watch(() => route.fullPath, () => {
  closeSearchPanel()
})

onMounted(async () => {
  // 刷新后若本地仍有 token，需要补拉管理员资料，保证头部信息完整。
  if (userStore.token && !userStore.adminInfo) {
    try {
      await userStore.getInfo()
    } catch (e) {
      userStore.logout()
      router.push('/login')
      return
    }
  }

  if (userStore.token) {
    void loadNotifications()
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

      &.is-active {
        background-color: var(--el-color-primary-light-9);
        color: var(--el-color-primary);
        font-weight: 600;
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

  .notification-trigger {
    padding: 0;
    border: none;
    background: transparent;
    font: inherit;
    appearance: none;
  }

  :deep(.admin-notification-popover) {
    padding: 0;
  }

  :deep(.admin-notification-popover .el-popover__title) {
    display: none;
  }

  :deep(.admin-notification-popover .el-popover__content) {
    padding: 0;
  }

  .notification-panel {
    padding: 16px;
    border-radius: 16px;
    background: var(--wt-surface-elevated);
    border: 1px solid var(--wt-border-default);
    box-shadow: var(--wt-shadow-float);
  }

  .notification-panel__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
  }

  .notification-panel__actions {
    display: flex;
    align-items: center;
    gap: 4px;
    flex-shrink: 0;
  }

  .notification-panel__title {
    font-size: 15px;
    font-weight: 700;
    color: var(--wt-text-primary);
  }

  .notification-panel__sub-title {
    margin-top: 4px;
    font-size: 12px;
    color: var(--wt-text-secondary);
  }

  .notification-panel__alert {
    margin-bottom: 12px;
  }

  .notification-panel__content {
    display: flex;
    flex-direction: column;
    gap: 12px;
    max-height: 420px;
    overflow-y: auto;
    padding-right: 4px;
  }

  .notification-section {
    padding: 12px;
    border: 1px solid var(--wt-border-default);
    border-radius: 12px;
    background: linear-gradient(180deg, var(--wt-surface-panel) 0%, var(--wt-surface-muted) 100%);
  }

  .notification-section__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
    font-size: 13px;
    font-weight: 600;
    color: var(--wt-text-primary);
  }

  .notification-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

   .notification-item {
     width: 100%;
     padding: 10px 12px;
     border: 1px solid transparent;
     border-radius: 10px;
     background: color-mix(in srgb, var(--wt-surface-elevated) 82%, var(--wt-surface-panel));
     text-align: left;
     cursor: pointer;
     display: flex;
     align-items: flex-start;
     justify-content: space-between;
     gap: 12px;
     transition: all 0.2s ease;
     position: relative;

     &:hover {
       border-color: var(--el-color-primary-light-5);
       background: var(--el-color-primary-light-9);
       transform: translateY(-1px);
     }

     /* 未读状态 */
     &:not(.is-read) {
       background: var(--el-color-primary-light-9);
     }
   }

   .notification-item__badge {
     position: absolute;
     top: 8px;
     left: 8px;
     width: 8px;
     height: 8px;
     border-radius: 50%;
     background: var(--wt-accent-rose-text);
     flex-shrink: 0;
   }

   .notification-item__main {
     min-width: 0;
     flex: 1;
     margin-left: 12px;
   }

  .notification-item__title-row {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .notification-item__type {
    flex-shrink: 0;
    padding: 2px 8px;
    border-radius: 999px;
    font-size: 12px;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }

  .notification-item__title {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px;
    font-weight: 600;
    color: var(--wt-text-primary);
  }

  .notification-item__desc {
    margin-top: 4px;
    font-size: 12px;
    line-height: 1.5;
    color: var(--wt-text-secondary);
  }

  .notification-item__time {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--wt-text-secondary);
    white-space: nowrap;
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
    background: var(--wt-surface-muted);
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
      box-shadow: 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 72%, transparent);
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
      box-shadow: var(--wt-shadow-float);
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
      color: var(--wt-text-regular);
      cursor: pointer;
      transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;

      &:hover,
      &.is-active {
        background: var(--el-color-primary-light-9);
        color: var(--wt-text-primary);
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
      box-shadow: 0 4px 12px color-mix(in srgb, var(--el-color-primary) 12%, transparent);
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
