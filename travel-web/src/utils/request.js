// Axios 请求封装（统一管理请求/响应拦截、错误处理）
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

/**
 * 创建 Axios 实例
 * 配置：基础路径、超时时间等
 */
const request = axios.create({
  baseURL: '/api/v1', // Web 端接口前缀
  timeout: 10000 // 请求超时时间（毫秒）
})

// 防止重复跳转登录页的标志位
let authRedirectInProgress = false

/**
 * 跳转到登录页（带防抖处理）
 * @param {string} message - 提示信息
 */
const redirectToLogin = async (message) => {
  const userStore = useUserStore()
  const hadToken = Boolean(userStore.token) // 记录之前是否有 Token
  const currentRoute = router.currentRoute.value
  // 如果当前不在登录页，设置重定向参数
  const redirect = currentRoute?.path && currentRoute.path !== '/login'
    ? currentRoute.fullPath
    : undefined

  userStore.logout()

  // 防止重复跳转
  if (authRedirectInProgress) {
    return
  }

  authRedirectInProgress = true

  // 只有在之前有 Token 的情况下才显示提示
  if (message && hadToken) {
    ElMessage.warning(message)
  }

  try {
    await router.replace(
      redirect
        ? { path: '/login', query: { redirect } } // 带重定向参数
        : { path: '/login' }
    )
  } finally {
    authRedirectInProgress = false
  }
}

/**
 * 请求拦截器
 * 功能：自动添加 Token 到请求头
 */
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/**
 * 响应拦截器
 * 功能：
 * 1. 统一处理业务错误（code !== 0）
 * 2. Token 失效时自动清除并跳转登录页（code === 10002 或 401 状态码）
 * 3. 网络错误提示
 */
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 业务错误处理
    if (res.code !== 0) {
      // Token 失效，跳转登录页
      if (res.code === 10002) {
        redirectToLogin(res.message || '登录状态已失效，请重新登录')
      } else {
        ElMessage.error(res.message || '请求失败')
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    // HTTP 401 未授权，跳转登录页
    if (error?.response?.status === 401) {
      redirectToLogin('登录状态已失效，请重新登录')
      return Promise.reject(error)
    }

    // 其他网络错误或服务器异常
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

/**
 * 获取完整图片 URL
 * @param {string} url - 图片相对路径或完整 URL
 * @returns {string} 完整的图片 URL
 *
 * 说明：
 * - 开发环境：使用 Vite proxy（/uploads -> localhost:8080）
 * - 生产环境：可配置为实际服务器地址
 * - 空值处理：返回默认占位图
 */
export const getImageUrl = (url) => {
  if (!url) return '/empty-image.png'
  if (/^https?:\/\//i.test(url)) return url // 已经是完整 URL
  // 确保以 / 开头
  return url.startsWith('/') ? url : `/${url}`
}

/**
 * 获取头像 URL
 * @param {string} url - 头像相对路径
 * @returns {string} 完整的头像 URL
 */
export const getAvatarUrl = (url) => {
  if (!url) return '/default-avatar.png'
  return getImageUrl(url)
}

export default request
