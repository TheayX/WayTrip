// Web 端路由配置
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/modules/account/store/user.js'

// 路由表配置
const routes = [
  // 主布局（包含所有子页面）
  {
    path: '/',
    component: () => import('@/app/layouts/AppShellLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/modules/home/index.vue'), meta: { title: '首页' } },
      { path: 'discover', name: 'Discover', component: () => import('@/modules/discover/index.vue'), meta: { title: '发现' } },
      { path: 'recommendations', name: 'Recommendations', component: () => import('@/modules/recommendation/index.vue'), meta: { title: '推荐景点', requiresAuth: true } },
      { path: 'nearby', name: 'Nearby', component: () => import('@/modules/nearby/index.vue'), meta: { title: '附近景点', requiresAuth: true } },
      { path: 'spots', name: 'SpotList', component: () => import('@/modules/spot/pages/list.vue'), meta: { title: '景点列表' } },
      { path: 'spots/:id', name: 'SpotDetail', component: () => import('@/modules/spot/pages/detail.vue'), meta: { title: '景点详情' } },
      { path: 'guides', name: 'GuideList', component: () => import('@/modules/guide/pages/list.vue'), meta: { title: '攻略列表' } },
      { path: 'guides/:id', name: 'GuideDetail', component: () => import('@/modules/guide/pages/detail.vue'), meta: { title: '攻略详情' } },
      { path: 'orders', name: 'OrderList', component: () => import('@/modules/order/pages/list.vue'), meta: { title: '我的订单', requiresAuth: true } },
      { path: 'orders/:id', name: 'OrderDetail', component: () => import('@/modules/order/pages/detail.vue'), meta: { title: '订单详情', requiresAuth: true } },
      { path: 'order/create/:spotId', name: 'OrderCreate', component: () => import('@/modules/order/pages/create.vue'), meta: { title: '创建订单', requiresAuth: true } },
      { path: 'favorites', name: 'Favorites', component: () => import('@/modules/favorite/index.vue'), meta: { title: '我的收藏', requiresAuth: true } },
      { path: 'reviews', name: 'ReviewList', component: () => import('@/modules/review/index.vue'), meta: { title: '我的评价', requiresAuth: true } },
      { path: 'profile', name: 'Profile', component: () => import('@/modules/account/pages/profile.vue'), meta: { title: '个人中心', requiresAuth: true } },
      { path: 'profile/activity', name: 'Activity', component: () => import('@/modules/account/pages/activity.vue'), meta: { title: '我的互动', requiresAuth: true } },
      { path: 'settings', name: 'Settings', component: () => import('@/modules/account/pages/settings.vue'), meta: { title: '设置', requiresAuth: true } },
      { path: 'search', name: 'Search', component: () => import('@/modules/search/index.vue'), meta: { title: '搜索' } }
    ]
  },
  // 登录页
  { path: '/login', name: 'Login', component: () => import('@/modules/auth/pages/login.vue'), meta: { title: '登录' } },
  // 注册页
  { path: '/register', name: 'Register', component: () => import('@/modules/auth/pages/register.vue'), meta: { title: '注册' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 路由切换时滚动到页面顶部
  scrollBehavior() {
    return { top: 0 }
  }
})

/**
 * 全局前置守卫
 * 功能：
 * 1. 设置页面标题
 * 2. 检查需要登录的页面，未登录则跳转到登录页
 */
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - WayTrip` : 'WayTrip'

  const userStore = useUserStore()
  // 需要登录但未登录，跳转到登录页并带上重定向参数
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
