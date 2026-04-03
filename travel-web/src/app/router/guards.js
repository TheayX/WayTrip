// Web 端路由守卫
import { useUserStore } from '@/modules/account/store/user.js'
import { APP_NAME } from '@/shared/constants/app.js'
import { AUTH_ROUTE_PATHS } from '@/shared/constants/route-paths.js'

export function setupRouterGuards(router) {
  /**
   * 全局前置守卫
   * 功能：
   * 1. 设置页面标题
   * 2. 检查需要登录的页面，未登录则跳转到登录页
   */
  router.beforeEach((to, from, next) => {
    document.title = to.meta.title ? `${to.meta.title} - ${APP_NAME}` : APP_NAME

    const userStore = useUserStore()

    // 需要登录但未登录时，保留原始目标地址，避免登录后丢失跳转意图
    if (to.meta.requiresAuth && !userStore.isLoggedIn) {
      next({ path: AUTH_ROUTE_PATHS.login, query: { redirect: to.fullPath } })
      return
    }

    next()
  })
}
