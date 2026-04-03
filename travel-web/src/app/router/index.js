// Web 端路由实例
import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { setupRouterGuards } from './guards'

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 路由切换时滚动到页面顶部
  scrollBehavior() {
    return { top: 0 }
  }
})

setupRouterGuards(router)

export default router
