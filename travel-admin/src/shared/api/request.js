// Axios 请求封装（统一管理请求/响应拦截）
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/app/store/user.js'
import router from '@/app/router/index.js'

const AUTH_EXPIRED_CODE = 10002
const SUCCESS_CODE = 0
const ACCESS_DENIED_CODE = 10003
const AUTH_EXPIRED_MESSAGE = '登录状态已失效，请重新登录'
const NETWORK_ERROR_MESSAGE = '网络异常，请稍后重试'
const REQUEST_FAILED_MESSAGE = '请求失败'
const NO_PERMISSION_MESSAGE = '暂无权限访问该功能'

let authRedirectInProgress = false

const redirectToLogin = async (message) => {
  const userStore = useUserStore()
  const hadToken = Boolean(userStore.token)
  userStore.logout()

  if (authRedirectInProgress) {
    return
  }

  authRedirectInProgress = true
  if (message && hadToken) {
    ElMessage.warning(message)
  }

  try {
    await router.replace('/login')
  } finally {
    authRedirectInProgress = false
  }
}

/**
 * 创建 Axios 实例
 * 配置：基础路径、超时时间等
 */
const request = axios.create({
  baseURL: '/api/admin/v1', // 后端接口前缀
  timeout: 10000 // 请求超时时间（毫秒）
})

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
  (error) => {
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 * 功能：
 * 1. 统一处理业务错误（code !== 0）
 * 2. Token 失效时自动清除并跳转登录页（code === 10002）
 * 3. 网络错误提示
 */
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 业务错误处理
    if (res.code !== SUCCESS_CODE) {
      if (res.code === AUTH_EXPIRED_CODE) {
        redirectToLogin(res.message || AUTH_EXPIRED_MESSAGE)
      } else if (res.code === ACCESS_DENIED_CODE) {
        ElMessage.warning(res.message || NO_PERMISSION_MESSAGE)
      } else {
        ElMessage.error(res.message || REQUEST_FAILED_MESSAGE)
      }

      return Promise.reject(new Error(res.message || REQUEST_FAILED_MESSAGE))
    }
    return res
  },
  (error) => {
    if (error?.response?.status === 401) {
      redirectToLogin(AUTH_EXPIRED_MESSAGE)
      return Promise.reject(error)
    }

    if (error?.response?.status === 403) {
      ElMessage.warning(NO_PERMISSION_MESSAGE)
      return Promise.reject(error)
    }

    // 网络错误或服务器异常
    ElMessage.error(error.message || NETWORK_ERROR_MESSAGE)
    return Promise.reject(error)
  }
)

export default request
