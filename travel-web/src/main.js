// Vue 应用入口文件（Web 端）
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import {
  ArrowLeft,
  ArrowRight,
  Camera,
  ChatDotRound,
  Collection,
  Delete,
  Document,
  Guide,
  Lock,
  MapLocation,
  Phone,
  Search,
  Setting,
  Star,
  SwitchButton,
  Tickets,
  User
} from '@element-plus/icons-vue'

import App from './App.vue'
import router from './app/router'
import './shared/styles/index.scss'

const app = createApp(App)

// 创建 Pinia 状态管理并启用持久化插件
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
app.use(pinia)

// 注册路由
app.use(router)

// 注册 Element Plus UI 组件库（中文语言包）
app.use(ElementPlus, { locale: zhCn })

// 仅注册当前 Web 端实际用到的图标，避免把整包图标打进主产物。
const globalIcons = {
  ArrowLeft,
  ArrowRight,
  Camera,
  ChatDotRound,
  Collection,
  Delete,
  Document,
  Guide,
  Lock,
  MapLocation,
  Phone,
  Search,
  Setting,
  Star,
  SwitchButton,
  Tickets,
  User
}

for (const [key, component] of Object.entries(globalIcons)) {
  app.component(key, component)
}

// 挂载应用到 #app 节点
app.mount('#app')
