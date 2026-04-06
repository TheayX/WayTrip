import { useUserStore } from '@/stores/user'

const LOGIN_REDIRECT_KEY = 'waytrip:auth:redirect'

const buildCurrentPageUrl = () => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  if (!current?.route) return ''

  const query = current.options && typeof current.options === 'object'
    ? Object.entries(current.options)
        .filter(([, value]) => value !== undefined && value !== null && value !== '')
        .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
        .join('&')
    : ''
  return `/${current.route}${query ? `?${query}` : ''}`
}

const persistLoginRedirect = (redirect) => {
  if (!redirect || redirect.startsWith('/pages/mine/')) {
    return
  }
  uni.setStorageSync(LOGIN_REDIRECT_KEY, redirect)
}

export const consumeLoginRedirect = () => {
  const redirect = uni.getStorageSync(LOGIN_REDIRECT_KEY)
  if (redirect) {
    uni.removeStorageSync(LOGIN_REDIRECT_KEY)
  }
  return redirect || ''
}

// 内部方法
const redirectAfterLoginCancel = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }

  uni.switchTab({ url: '/pages/index/index' })
}

// 对外暴露方法
export const promptLogin = (content = '登录后可查看详情，是否现在去登录？', options = {}) => {
  const userStore = useUserStore()
  if (userStore.isLoggedIn) {
    return true
  }

  persistLoginRedirect(options.redirect || buildCurrentPageUrl())

  uni.showModal({
    title: '请先登录',
    content,
    confirmText: '去登录',
    cancelText: '再看看',
    success: ({ confirm }) => {
      if (confirm) {
        uni.switchTab({ url: '/pages/mine/index' })
      }
    }
  })
  return false
}

export const guardLoginPage = (content = '登录后可查看详情，是否现在去登录？') => {
  const userStore = useUserStore()
  if (userStore.isLoggedIn) {
    return true
  }

  persistLoginRedirect(buildCurrentPageUrl())

  uni.showModal({
    title: '请先登录',
    content,
    confirmText: '去登录',
    cancelText: '返回',
    success: ({ confirm }) => {
      if (confirm) {
        uni.switchTab({ url: '/pages/mine/index' })
        return
      }

      redirectAfterLoginCancel()
    }
  })

  return false
}
