import { useUserStore } from '@/stores/user'

// 基础 URL 配置
const BASE_URL = 'http://localhost:8080/api/v1'
const SERVER_URL = 'http://localhost:8080'

/**
 * 获取完整图片URL
 */
export const getImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return SERVER_URL + url
}

/**
 * 封装请求方法 (Uni-app 版本)
 */
const request = (options) => {
  return new Promise((resolve, reject) => {
    const { url, method = 'GET', data = {}, showLoading = true } = options
    const userStore = useUserStore()

    if (showLoading) {
      uni.showLoading({ title: '加载中...', mask: true })
    }

    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': userStore.token ? `Bearer ${userStore.token}` : ''
      },
      success: (res) => {
        if (showLoading) uni.hideLoading()

        if (res.statusCode === 200) {
          const result = res.data
          if (result.code === 0) {
            resolve(result)
          } else if (result.code === 10002) {
            // Token 失效
            userStore.logout()
            uni.showToast({ title: '请重新登录', icon: 'none' })
            reject(result)
          } else {
            uni.showToast({ title: result.message || '请求失败', icon: 'none' })
            reject(result)
          }
        } else {
          uni.showToast({ title: '网络错误', icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        if (showLoading) uni.hideLoading()
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

// GET 请求
export const get = (url, data, options = {}) => {
  return request({ url, method: 'GET', data, ...options })
}

// POST 请求
export const post = (url, data, options = {}) => {
  return request({ url, method: 'POST', data, ...options })
}

// PUT 请求
export const put = (url, data, options = {}) => {
  return request({ url, method: 'PUT', data, ...options })
}

// DELETE 请求
export const del = (url, data, options = {}) => {
  return request({ url, method: 'DELETE', data, ...options })
}

export default {
  request,
  get,
  post,
  put,
  del,
  getImageUrl
}
