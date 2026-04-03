// Web 端路由守卫
import { useUserStore } from '@/modules/account/store/user.js'

export function setupRouterGuards(router) {
  /**
   * 全局前置守卫
   * 功能：
   * 1. 设置页面标题
   * 2. 检查需要登录的页面，未登录则跳转到登录页
   */
  router.beforeEach((to, from, next) => {
    document.title = to.meta.title ? `${to.meta.title} - WayTrip` : 'WayTrip'

    const userStore = useUserStore()

    // 需要登录但未登录时，保留原始目标地址，避免登录后丢失跳转意图
    if (to.meta.requiresAuth && !userStore.isLoggedIn) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }

    next()
  })
}
