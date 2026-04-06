import { useUserStore } from '@/stores/user'

// 常量配置
const SERVER_URL = 'http://localhost:8080'
const BASE_URL = `${SERVER_URL.replace(/\/$/, '')}/api/v1`
const SUCCESS_CODE = 0
const AUTH_EXPIRED_CODE = 10002
const HTTP_UNAUTHORIZED_STATUS = 401
const ACCESS_DENIED_CODE = 10003
const AUTH_EXPIRED_MESSAGE = '登录状态已失效，请重新登录'
const NETWORK_ERROR_MESSAGE = '网络异常，请稍后重试'
const REQUEST_FAILED_MESSAGE = '请求失败'
const NO_PERMISSION_MESSAGE = '暂无权限访问该功能'

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

// 全局 Loading 引用计数，避免并发请求造成 show/hide 不配对。
let loadingRefCount = 0
let authRedirectInProgress = false

const showGlobalLoading = (title = '加载中...') => {
  loadingRefCount += 1
  if (loadingRefCount === 1) {
    uni.showLoading({ title, mask: true })
  }
}

const hideGlobalLoading = () => {
  if (loadingRefCount <= 0) {
    return
  }

  loadingRefCount -= 1
  if (loadingRefCount === 0) {
    uni.hideLoading()
  }
}

const redirectToLogin = () => {
  if (authRedirectInProgress) {
    return
  }

  authRedirectInProgress = true
  uni.reLaunch({
    url: '/pages/mine/index',
    complete: () => {
      globalThis.setTimeout(() => {
        authRedirectInProgress = false
      }, 300)
    }
  })
}

const resolveOrRejectAuthExpired = ({ rejectOnAuthExpired, resolve, reject, message }) => {
  const authExpiredResult = {
    code: AUTH_EXPIRED_CODE,
    data: null,
    message: message || AUTH_EXPIRED_MESSAGE
  }

  if (rejectOnAuthExpired) {
    reject(authExpiredResult)
    return
  }

  resolve(authExpiredResult)
}

const handleAuthExpired = ({ hadToken, userStore, rejectOnAuthExpired, resolve, reject, message }) => {
  userStore.logout()

  if (hadToken) {
    uni.showToast({ title: message || AUTH_EXPIRED_MESSAGE, icon: 'none' })
  }

  redirectToLogin()
  resolveOrRejectAuthExpired({ rejectOnAuthExpired, resolve, reject, message })
}

// 基础请求方法
const request = (options) => {
  return new Promise((resolve, reject) => {
    // 默认不自动弹全局 loading，避免与页面内手动 loading 管理冲突。
    const { url, method = 'GET', data = {}, params = null, showLoading = false, rejectOnAuthExpired = false } = options
    const userStore = useUserStore()
    const hadToken = Boolean(userStore.token)
    const requestUrl = appendQueryParams(BASE_URL + url, params)

    if (showLoading) {
      showGlobalLoading('加载中...')
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
        if (showLoading) hideGlobalLoading()

        if (res.statusCode === 200) {
          const result = res.data
          if (result.code === SUCCESS_CODE) {
            resolve(result)
          } else if (result.code === AUTH_EXPIRED_CODE) {
            handleAuthExpired({
              hadToken,
              userStore,
              rejectOnAuthExpired,
              resolve,
              reject,
              message: result.message
            })
          } else if (result.code === ACCESS_DENIED_CODE) {
            uni.showToast({ title: result.message || NO_PERMISSION_MESSAGE, icon: 'none' })
            reject(result)
          } else {
            uni.showToast({ title: result.message || REQUEST_FAILED_MESSAGE, icon: 'none' })
            reject(result)
          }
        } else if (res.statusCode === HTTP_UNAUTHORIZED_STATUS) {
          handleAuthExpired({ hadToken, userStore, rejectOnAuthExpired, resolve, reject })
        } else if (res.statusCode === 403) {
          uni.showToast({ title: NO_PERMISSION_MESSAGE, icon: 'none' })
          reject(res)
        } else {
          uni.showToast({ title: REQUEST_FAILED_MESSAGE, icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        if (showLoading) hideGlobalLoading()
        uni.showToast({ title: NETWORK_ERROR_MESSAGE, icon: 'none' })
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
    const hadToken = Boolean(userStore.token)
    showGlobalLoading('上传中...')
    uni.uploadFile({
      url: BASE_URL + url,
      filePath,
      name,
      formData,
      header: {
        'Authorization': userStore.token ? `Bearer ${userStore.token}` : ''
      },
      success: (res) => {
        hideGlobalLoading()
        if (res.statusCode === 200) {
          const result = JSON.parse(res.data)
          if (result.code === SUCCESS_CODE) {
            resolve(result)
          } else if (result.code === AUTH_EXPIRED_CODE) {
            handleAuthExpired({ hadToken, userStore, rejectOnAuthExpired: true, resolve, reject, message: result.message })
          } else {
            uni.showToast({ title: result.message || '上传失败', icon: 'none' })
            reject(result)
          }
        } else if (res.statusCode === HTTP_UNAUTHORIZED_STATUS) {
          handleAuthExpired({ hadToken, userStore, rejectOnAuthExpired: true, resolve, reject })
        } else {
          uni.showToast({ title: REQUEST_FAILED_MESSAGE, icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        hideGlobalLoading()
        uni.showToast({ title: NETWORK_ERROR_MESSAGE, icon: 'none' })
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
