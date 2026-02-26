import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/home/index.vue'), meta: { title: '首页' } },
      { path: 'spots', name: 'SpotList', component: () => import('@/views/spot/list.vue'), meta: { title: '景点列表' } },
      { path: 'spots/:id', name: 'SpotDetail', component: () => import('@/views/spot/detail.vue'), meta: { title: '景点详情' } },
      { path: 'guides', name: 'GuideList', component: () => import('@/views/guide/list.vue'), meta: { title: '攻略列表' } },
      { path: 'guides/:id', name: 'GuideDetail', component: () => import('@/views/guide/detail.vue'), meta: { title: '攻略详情' } },
      { path: 'orders', name: 'OrderList', component: () => import('@/views/order/list.vue'), meta: { title: '我的订单', requiresAuth: true } },
      { path: 'orders/:id', name: 'OrderDetail', component: () => import('@/views/order/detail.vue'), meta: { title: '订单详情', requiresAuth: true } },
      { path: 'order/create/:spotId', name: 'OrderCreate', component: () => import('@/views/order/create.vue'), meta: { title: '创建订单', requiresAuth: true } },
      { path: 'favorites', name: 'Favorites', component: () => import('@/views/favorite/index.vue'), meta: { title: '我的收藏', requiresAuth: true } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/profile/index.vue'), meta: { title: '个人中心', requiresAuth: true } },
      { path: 'search', name: 'Search', component: () => import('@/views/search/index.vue'), meta: { title: '搜索' } }
    ]
  },
  { path: '/login', name: 'Login', component: () => import('@/views/login/index.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'Register', component: () => import('@/views/register/index.vue'), meta: { title: '注册' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - WayTrip` : 'WayTrip'

  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router

