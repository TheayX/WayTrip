// Web 端路由表定义
import { ROUTE_NAMES } from '@/shared/constants/route-names.js'
import { AUTH_ROUTE_PATHS } from '@/shared/constants/route-paths.js'

const publicRoutes = [
  { path: '', name: ROUTE_NAMES.home, component: () => import('@/modules/home/index.vue'), meta: { title: '首页' } },
  { path: 'discover', name: ROUTE_NAMES.discover, component: () => import('@/modules/discover/index.vue'), meta: { title: '发现' } },
  { path: 'recommendations', name: ROUTE_NAMES.recommendations, component: () => import('@/modules/recommendation/index.vue'), meta: { title: '推荐景点', requiresAuth: true } },
  { path: 'nearby', name: ROUTE_NAMES.nearby, component: () => import('@/modules/nearby/index.vue'), meta: { title: '附近景点', requiresAuth: true } },
  { path: 'random-pick', name: ROUTE_NAMES.randomPick, component: () => import('@/modules/random-pick/index.vue'), meta: { title: '随心一选' } },
  { path: 'budget-travel', name: ROUTE_NAMES.budgetTravel, component: () => import('@/modules/budget-travel/index.vue'), meta: { title: '穷游玩法' } },
  { path: 'traveler-reviews', name: ROUTE_NAMES.travelerReviews, component: () => import('@/modules/traveler-reviews/index.vue'), meta: { title: '游客口碑' } },
  { path: 'trending-views', name: ROUTE_NAMES.trendingViews, component: () => import('@/modules/trending-views/index.vue'), meta: { title: '近期热看' } },
  { path: 'more', name: ROUTE_NAMES.more, component: () => import('@/modules/more/index.vue'), meta: { title: '更多功能' } },
  { path: 'spots', name: ROUTE_NAMES.spotList, component: () => import('@/modules/spot/pages/list.vue'), meta: { title: '景点列表' } },
  { path: 'spots/:id', name: ROUTE_NAMES.spotDetail, component: () => import('@/modules/spot/pages/detail.vue'), meta: { title: '景点详情' } },
  { path: 'guides', name: ROUTE_NAMES.guideList, component: () => import('@/modules/guide/pages/list.vue'), meta: { title: '攻略列表' } },
  { path: 'guides/:id', name: ROUTE_NAMES.guideDetail, component: () => import('@/modules/guide/pages/detail.vue'), meta: { title: '攻略详情' } },
  { path: 'order/create/:spotId', name: ROUTE_NAMES.orderCreate, component: () => import('@/modules/order/pages/create.vue'), meta: { title: '创建订单', requiresAuth: true } },
  { path: 'search', name: ROUTE_NAMES.search, component: () => import('@/modules/search/index.vue'), meta: { title: '搜索' } }
]

const accountRoutes = [
  { path: 'profile', name: ROUTE_NAMES.profile, component: () => import('@/modules/account/pages/profile.vue'), meta: { title: '个人中心', requiresAuth: true } },
  { path: 'activity', name: ROUTE_NAMES.activity, component: () => import('@/modules/account/pages/activity.vue'), meta: { title: '我的互动', requiresAuth: true } },
  { path: 'settings', name: ROUTE_NAMES.settings, component: () => import('@/modules/account/pages/settings.vue'), meta: { title: '设置', requiresAuth: true } },
  { path: 'orders', name: ROUTE_NAMES.orderList, component: () => import('@/modules/order/pages/list.vue'), meta: { title: '我的订单', requiresAuth: true } },
  { path: 'orders/:id', name: ROUTE_NAMES.orderDetail, component: () => import('@/modules/order/pages/detail.vue'), meta: { title: '订单详情', requiresAuth: true } },
  { path: 'favorites', name: ROUTE_NAMES.favorites, component: () => import('@/modules/favorite/index.vue'), meta: { title: '我的收藏', requiresAuth: true } },
  { path: 'reviews', name: ROUTE_NAMES.reviewList, component: () => import('@/modules/review/index.vue'), meta: { title: '我的评价', requiresAuth: true } }
]

export const appShellRoutes = [
  {
    path: '/',
    component: () => import('@/app/layouts/AppShellLayout.vue'),
    children: [
      ...publicRoutes,
      {
        path: 'account',
        component: () => import('@/app/layouts/AccountLayout.vue'),
        children: accountRoutes
      }
    ]
  }
]

// 认证路由先统一挂到认证布局下，后续登录注册页可独立演进视觉结构
export const authRoutes = [
  {
    path: '/',
    component: () => import('@/app/layouts/AuthLayout.vue'),
    children: [
      { path: AUTH_ROUTE_PATHS.login.slice(1), name: ROUTE_NAMES.login, component: () => import('@/modules/auth/pages/login.vue'), meta: { title: '登录' } },
      { path: AUTH_ROUTE_PATHS.register.slice(1), name: ROUTE_NAMES.register, component: () => import('@/modules/auth/pages/register.vue'), meta: { title: '注册' } }
    ]
  }
]

export const routes = [
  ...appShellRoutes,
  ...authRoutes
]
