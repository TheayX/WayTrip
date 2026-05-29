import { useUserStore } from '@/stores/user'
import { getCurrentPageRoute, traceRuntime } from '@/utils/runtime-trace'

// 小程序请求与资源地址工具，统一处理接口前缀、鉴权失效、上传和运行时追踪。
// 页面侧只负责调用 get / post / uploadFile，不再重复处理通用网络细节。
const DEFAULT_SERVER_URL = 'http://localhost:8080'

// 默认保持“拉下来即可联调”，因此小程序开发环境直接走本地 8080。
// 如果需要消除微信开发者工具里的 HTTP 警告，可在 .env.local 中把
// VITE_API_ORIGIN 改为本机 Nginx 反代出的 HTTPS 地址，例如 https://localhost:8443。
const SERVER_URL = (import.meta.env.VITE_API_ORIGIN || DEFAULT_SERVER_URL).replace(/\/$/, '')
// 资源默认与接口同源，只有静态资源拆到单独域名时才通过环境变量覆盖。
const RESOURCE_URL = (import.meta.env.VITE_RESOURCE_ORIGIN || SERVER_URL).replace(/\/$/, '')

const BASE_URL = `${SERVER_URL.replace(/\/$/, '')}/api/v1`
const SUCCESS_CODE = 0
const AUTH_EXPIRED_CODE = 10002
const HTTP_UNAUTHORIZED_STATUS = 401
const ACCESS_DENIED_CODE = 10003
const AUTH_EXPIRED_MESSAGE = '登录状态已失效，请重新登录'
const NETWORK_ERROR_MESSAGE = '网络异常，请稍后重试'
const REQUEST_FAILED_MESSAGE = '请求失败'
const NO_PERMISSION_MESSAGE = '暂无权限访问该功能'

// 判断资源地址是否为 http 绝对地址。
const isHttpUrl = (value) => /^http:\/\//i.test(value)

// 判断资源地址是否为 https 绝对地址。
const isHttpsUrl = (value) => /^https:\/\//i.test(value)

// 统一识别完整资源地址，避免二次拼接域名。
const isAbsoluteUrl = (value) => isHttpUrl(value) || isHttpsUrl(value)

// 解析图片资源完整地址。
export const getImageUrl = (url) => {
  if (!url) return ''
  if (isAbsoluteUrl(url)) {
    // 已是完整地址时不再二次改写，避免把调用方显式传入的 HTTPS / CDN 地址覆盖掉。
    return url
  }
  const path = url.startsWith('/') ? url : `/${url}`
  // 资源域名单独配置，便于本地 API、HTTPS 反代、CDN 场景按环境切换。
  return `${RESOURCE_URL}${path}`
}

// 解析头像地址，缺失时回退默认头像。
export const getAvatarUrl = (url) => {
  if (!url) return '/static/default-avatar.png'
  return getImageUrl(url)
}

// 解析正文图片地址，缺失时回退占位图。
export const getContentImageUrl = (url) => {
  if (!url) return '/static/empty-image.png'
  return getImageUrl(url)
}

// 将 GET 查询参数安全拼接到 URL。
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

let loadingRefCount = 0
let authRedirectInProgress = false
let requestSequence = 0

// 显示全局 loading，并通过引用计数避免并发请求互相关闭。
const showGlobalLoading = (title = '加载中...') => {
  loadingRefCount += 1
  if (loadingRefCount === 1) {
    uni.showLoading({ title, mask: true })
  }
}

// 隐藏全局 loading。
const hideGlobalLoading = () => {
  if (loadingRefCount <= 0) {
    return
  }

  loadingRefCount -= 1
  if (loadingRefCount === 0) {
    uni.hideLoading()
  }
}

// 鉴权失效时统一回到个人中心登录入口。
const redirectToLogin = () => {
  if (authRedirectInProgress) {
    return
  }

  // 认证失效时统一回个人中心，避免多个并发请求重复 reLaunch 导致页面闪动。
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

// 按调用方诉求决定鉴权失效时 resolve 还是 reject。
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

// 清理登录态并处理鉴权失效后的页面跳转。
const handleAuthExpired = ({ hadToken, userStore, rejectOnAuthExpired, resolve, reject, message }) => {
  userStore.logout()

  if (hadToken) {
    uni.showToast({ title: message || AUTH_EXPIRED_MESSAGE, icon: 'none' })
  }

  redirectToLogin()
  resolveOrRejectAuthExpired({ rejectOnAuthExpired, resolve, reject, message })
}

// 基础请求方法

// 统一封装普通接口请求。
const request = (options) => {
  return new Promise((resolve, reject) => {
    // 默认不自动弹全局 loading，避免与页面内手动 loading 管理冲突。
    const { url, method = 'GET', data = {}, params = null, showLoading = false, rejectOnAuthExpired = false } = options
    const userStore = useUserStore()
    const hadToken = Boolean(userStore.token)
    const requestUrl = appendQueryParams(BASE_URL + url, params)
    const requestId = `${Date.now()}-${++requestSequence}`
    const startTime = Date.now()
    const route = getCurrentPageRoute()
    let requestFinished = false

    traceRuntime('request-start', {
      requestId,
      route,
      method,
      url,
      requestUrl
    })

    const pendingWarningTimer = globalThis.setTimeout(() => {
      if (requestFinished) return
      // 超过阈值先打运行时告警，便于区分“请求最终失败”和“请求长时间卡住”两类问题。
      traceRuntime('request-pending-warning', {
        requestId,
        route,
        method,
        url,
        durationMs: Date.now() - startTime
      })
    }, 8000)

    const finishRequest = (category, payload = {}) => {
      if (requestFinished) return
      requestFinished = true
      if (pendingWarningTimer) {
        globalThis.clearTimeout(pendingWarningTimer)
      }
      traceRuntime(category, {
        requestId,
        route,
        method,
        url,
        durationMs: Date.now() - startTime,
        ...payload
      })
    }

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
        finishRequest('request-success', {
          statusCode: res.statusCode,
          resultCode: res.data?.code
        })

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
        finishRequest('request-fail', {
          error: err?.errMsg || JSON.stringify(err || {})
        })
        uni.showToast({ title: NETWORK_ERROR_MESSAGE, icon: 'none' })
        reject(err)
      }
    })
  })
}

// 对外暴露方法

// 发起 GET 请求。
export const get = (url, data, options = {}) => {
  return request({ url, method: 'GET', data, ...options })
}

// 发起 POST 请求。
export const post = (url, data, options = {}) => {
  return request({ url, method: 'POST', data, ...options })
}

// 发起 PUT 请求。
export const put = (url, data, options = {}) => {
  return request({ url, method: 'PUT', data, ...options })
}

// 发起 DELETE 请求。
export const del = (url, data, options = {}) => {
  return request({ url, method: 'DELETE', data, ...options })
}

// 上传文件并统一处理鉴权、loading 和运行时追踪。
export const uploadFile = (url, filePath, name = 'file', formData = {}) => {
  return new Promise((resolve, reject) => {
    const userStore = useUserStore()
    const hadToken = Boolean(userStore.token)
    const requestId = `${Date.now()}-${++requestSequence}`
    const startTime = Date.now()
    const route = getCurrentPageRoute()
    let requestFinished = false

    traceRuntime('upload-start', {
      requestId,
      route,
      url,
      filePath,
      name
    })

    const pendingWarningTimer = globalThis.setTimeout(() => {
      if (requestFinished) return
      // 上传比普通请求更容易卡在网络或文件系统阶段，单独保留 pending 告警便于定位。
      traceRuntime('upload-pending-warning', {
        requestId,
        route,
        url,
        durationMs: Date.now() - startTime
      })
    }, 8000)

    const finishUpload = (category, payload = {}) => {
      if (requestFinished) return
      requestFinished = true
      if (pendingWarningTimer) {
        globalThis.clearTimeout(pendingWarningTimer)
      }
      traceRuntime(category, {
        requestId,
        route,
        url,
        durationMs: Date.now() - startTime,
        ...payload
      })
    }

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
        finishUpload('upload-success', {
          statusCode: res.statusCode
        })
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
        finishUpload('upload-fail', {
          error: err?.errMsg || JSON.stringify(err || {})
        })
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
