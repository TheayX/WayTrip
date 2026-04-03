// Web 端路由表定义

/**
 * 主站布局路由
 * 说明：
 * 1. 当前仍沿用现有访问路径，先完成壳层收口
 * 2. 用户中心路径后续再统一迁到 account 语义
 */
export const appShellRoutes = [
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
  }
]

// 认证路由先统一挂到认证布局下，后续登录注册页可独立演进视觉结构
export const authRoutes = [
  {
    path: '/',
    component: () => import('@/app/layouts/AuthLayout.vue'),
    children: [
      { path: 'login', name: 'Login', component: () => import('@/modules/auth/pages/login.vue'), meta: { title: '登录' } },
      { path: 'register', name: 'Register', component: () => import('@/modules/auth/pages/register.vue'), meta: { title: '注册' } }
    ]
  }
]

export const routes = [
  ...appShellRoutes,
  ...authRoutes
]
