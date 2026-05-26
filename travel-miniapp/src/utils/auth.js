import { useUserStore } from '@/stores/user'
import { switchTabSafely } from '@/utils/navigation'

// 登录拦截与登录后回跳工具，统一处理页面侧的鉴权提示和跳转策略。
const LOGIN_REDIRECT_KEY = 'waytrip:auth:redirect'

// 记录登录前所在页面，便于登录成功后按用户来路回跳。
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
  // 个人中心本身就是登录入口，不需要再把它当成登录后回跳目标。
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

const redirectAfterLoginCancel = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }

  void switchTabSafely('/pages/index/index')
}

// 弹出登录提示，但不强制当前页面立即退出，适合列表、卡片点击等轻量拦截场景。
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
        void switchTabSafely('/pages/mine/index')
      }
    }
  })
  return false
}

// 当前页面本身依赖登录态时，取消登录后尽量回退，避免停留在不可用页面。
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
        void switchTabSafely('/pages/mine/index')
        return
      }

      redirectAfterLoginCancel()
    }
  })

  return false
}
