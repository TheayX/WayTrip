import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

// 小程序入口在这里统一挂载 Pinia，确保所有页面共享同一份全局状态。
export function createApp() {
  const app = createSSRApp(App)
  const pinia = createPinia()
  
  app.use(pinia)
  
  return {
    app,
    pinia
  }
}
