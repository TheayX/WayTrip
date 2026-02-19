import { useUserStore } from '@/stores/user'

// 基础 URL 配置
// Base URLs
const SERVER_URL = 'http://localhost:8080'
const BASE_URL = `${SERVER_URL.replace(/\/$/, '')}/api/v1`

/**
 * 获取完整图片URL
 */
const isHttpUrl = (value) => /^http:\/\//i.test(value)
const isHttpsUrl = (value) => /^https:\/\//i.test(value)
const isAbsoluteUrl = (value) => isHttpUrl(value) || isHttpsUrl(value)
const isLocalHostUrl = (value) => /\/\/(localhost|127\.0\.0\.1|\[::1\])(:\d+)?(\/|$)/i.test(value)
const toHttps = (value) => value.replace(/^http:\/\//i, 'https://')

export const getImageUrl = (url) => {
  if (!url) return ''
  if (isAbsoluteUrl(url)) {
    // WeChat mini program disallows HTTP images; upgrade when not localhost.
    return isHttpUrl(url) && !isLocalHostUrl(url) ? toHttps(url) : url
  }
  const base = SERVER_URL.replace(/\/$/, '')
  const path = url.startsWith('/') ? url : `/${url}`
  const fullUrl = `${base}${path}`
  return isHttpUrl(fullUrl) && !isLocalHostUrl(fullUrl) ? toHttps(fullUrl) : fullUrl
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
            // 弹窗提示，不显式 reject 避免报错提示
            uni.showModal({
              title: '提示',
              content: '登录状态已失效，请重新登录',
              confirmText: '去登录',
              success: (modalRes) => {
                if (modalRes.confirm) {
                  uni.reLaunch({ url: '/pages/mine/index' })
                }
              }
            })
            // 这里 resolve(null) 或者 reject(result) 取决于业务需要，
            // 为了防止页面爆红，可以 resolve(null) 并让调用方自行处理空数据
            resolve({ code: 10002, data: null, message: result.message || 'Token invalid' })
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
