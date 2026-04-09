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

// 守卫逻辑统一在独立文件注册，路由实例职责只保留基础配置。
setupRouterGuards(router)

export default router
