// 管理后台路由配置
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '运营概览', icon: 'Odometer', group: 'dashboard' }
      },
      {
        path: 'spot',
        name: 'Spot',
        component: () => import('@/views/spot/index.vue'),
        meta: { title: '景点管理', icon: 'Location', group: 'content' }
      },
      {
        path: 'category',
        name: 'Category',
        component: () => import('@/views/category/index.vue'),
        meta: { title: '分类管理', icon: 'Menu', group: 'content' }
      },
      {
        path: 'region',
        name: 'Region',
        component: () => import('@/views/region/index.vue'),
        meta: { title: '地区管理', icon: 'MapLocation', group: 'content' }
      },
      {
        path: 'guide',
        name: 'Guide',
        component: () => import('@/views/guide/index.vue'),
        meta: { title: '攻略管理', icon: 'Document', group: 'content' }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '订单管理', icon: 'List', group: 'transaction' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User', group: 'user-ops' }
      },
      {
        path: 'review',
        name: 'Review',
        component: () => import('@/views/review/index.vue'),
        meta: { title: '评价管理', icon: 'ChatDotRound', group: 'user-ops' }
      },
      {
        path: 'preference',
        name: 'Preference',
        component: () => import('@/views/preference/index.vue'),
        meta: { title: '用户偏好', icon: 'CollectionTag', group: 'user-ops' }
      },
      {
        path: 'favorite',
        name: 'Favorite',
        component: () => import('@/views/favorite/index.vue'),
        meta: { title: '用户收藏', icon: 'Star', group: 'user-ops' }
      },
      {
        path: 'view-log',
        name: 'ViewLog',
        component: () => import('@/views/view-log/index.vue'),
        meta: { title: '浏览行为', icon: 'View', group: 'user-ops' }
      },
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('@/views/admin/index.vue'),
        meta: { title: '管理员管理', icon: 'UserFilled', group: 'system' }
      },
      {
        path: 'banner',
        name: 'Banner',
        component: () => import('@/views/banner/index.vue'),
        meta: { title: '轮播图管理', icon: 'Picture', group: 'content' }
      },
      {
        path: 'recommendation',
        name: 'RecommendationOverview',
        component: () => import('@/views/recommendation/overview.vue'),
        meta: { title: '推荐总览', icon: 'MagicStick', group: 'recommendation' }
      },
      {
        path: 'recommendation/config',
        name: 'Recommendation',
        component: () => import('@/views/recommendation/index.vue'),
        meta: { title: '推荐配置', icon: 'MagicStick', group: 'recommendation' }
      }
    ]
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：权限控制 + 页面标题设置
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - WayTrip 管理端` : 'WayTrip 管理端'

  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !userStore.token) {
    next('/login')
  } else if (to.path === '/login' && userStore.token) {
    next('/')
  } else {
    next()
  }
})

export default router
