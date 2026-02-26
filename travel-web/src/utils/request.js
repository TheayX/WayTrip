import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'


const request = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

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
      ElMessage.error(res.message || '请求失败')

      if (res.code === 10002) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
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
  if (!url) return '/空.jpg'
  if (/^https?:\/\//i.test(url)) return url
  // 确保以 / 开头
  return url.startsWith('/') ? url : `/${url}`
}

export default request

