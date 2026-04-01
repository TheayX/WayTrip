// Axios 请求封装（统一管理请求/响应拦截）
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/app/store/user.js'
import router from '@/app/router/index.js'

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
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')

      // Token 失效，强制登出
      if (res.code === 10002) {
        const userStore = useUserStore()
        userStore.clearToken()
        router.push('/login')
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    // 网络错误或服务器异常
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
