import { useUserStore } from '@/stores/user'

// 常量配置
const SERVER_URL = 'http://localhost:8080'
const BASE_URL = `${SERVER_URL.replace(/\/$/, '')}/api/v1`

// 图片地址处理方法
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

export const getAvatarUrl = (url) => {
  if (!url) return '/static/default-avatar.png'
  return getImageUrl(url)
}

export const getContentImageUrl = (url) => {
  if (!url) return '/static/empty-image.png'
  return getImageUrl(url)
}

const appendQueryParams = (url, params) => {
  if (!params || typeof params !== 'object') {
    return url
  }

  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&')

  if (!query) {
    return url
  }

  return `${url}${url.includes('?') ? '&' : '?'}${query}`
}

// 基础请求方法
const request = (options) => {
  return new Promise((resolve, reject) => {
    const { url, method = 'GET', data = {}, params = null, showLoading = true, rejectOnAuthExpired = false } = options
    const userStore = useUserStore()
    const hadToken = Boolean(userStore.token)
    const requestUrl = appendQueryParams(BASE_URL + url, params)

    if (showLoading) {
      uni.showLoading({ title: '加载中...', mask: true })
    }

    uni.request({
      url: requestUrl,
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
            userStore.logout()

            // 只有原本确实存在登录态时，才提示“登录失效”。
            if (hadToken) {
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
            }

            const authExpiredResult = { code: 10002, data: null, message: result.message || 'Token invalid' }
            if (rejectOnAuthExpired) {
              reject(authExpiredResult)
              return
            }

            resolve(authExpiredResult)
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

// 对外暴露方法
export const get = (url, data, options = {}) => {
  return request({ url, method: 'GET', data, ...options })
}

export const post = (url, data, options = {}) => {
  return request({ url, method: 'POST', data, ...options })
}

export const put = (url, data, options = {}) => {
  return request({ url, method: 'PUT', data, ...options })
}

export const del = (url, data, options = {}) => {
  return request({ url, method: 'DELETE', data, ...options })
}

export const uploadFile = (url, filePath, name = 'file', formData = {}) => {
  return new Promise((resolve, reject) => {
    const userStore = useUserStore()
    uni.showLoading({ title: '上传中...', mask: true })
    uni.uploadFile({
      url: BASE_URL + url,
      filePath,
      name,
      formData,
      header: {
        'Authorization': userStore.token ? `Bearer ${userStore.token}` : ''
      },
      success: (res) => {
        uni.hideLoading()
        if (res.statusCode === 200) {
          const result = JSON.parse(res.data)
          if (result.code === 0) {
            resolve(result)
          } else {
            uni.showToast({ title: result.message || '上传失败', icon: 'none' })
            reject(result)
          }
        } else {
          uni.showToast({ title: '上传失败', icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        uni.hideLoading()
        uni.showToast({ title: '上传失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

export default {
  request,
  get,
  post,
  put,
  del,
  uploadFile,
  getImageUrl,
  getAvatarUrl,
  getContentImageUrl
}
