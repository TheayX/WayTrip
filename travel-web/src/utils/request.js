import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'


const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

let authRedirectInProgress = false

const redirectToLogin = async (message) => {
  const userStore = useUserStore()
  const hadToken = Boolean(userStore.token)
  const currentRoute = router.currentRoute.value
  const redirect = currentRoute?.path && currentRoute.path !== '/login'
    ? currentRoute.fullPath
    : undefined

  userStore.logout()

  if (authRedirectInProgress) {
    return
  }

  authRedirectInProgress = true

  if (message && hadToken) {
    ElMessage.warning(message)
  }

  try {
    await router.replace(
      redirect
        ? { path: '/login', query: { redirect } }
        : { path: '/login' }
    )
  } finally {
    authRedirectInProgress = false
  }
}

// 请求拦截器
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

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 0) {
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
    if (error?.response?.status === 401) {
      redirectToLogin('登录状态已失效，请重新登录')
      return Promise.reject(error)
    }

    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

/**
 * 获取完整图片URL
 * 开发环境走 vite proxy（/uploads -> localhost:8080）
 * 生产环境可配置为实际服务器地址
 */
export const getImageUrl = (url) => {
  if (!url) return '/empty-image.png'
  if (/^https?:\/\//i.test(url)) return url
  // 确保以 / 开头
  return url.startsWith('/') ? url : `/${url}`
}

export const getAvatarUrl = (url) => {
  if (!url) return '/default-avatar.png'
  return getImageUrl(url)
}

export default request
