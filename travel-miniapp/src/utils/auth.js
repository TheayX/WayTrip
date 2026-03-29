import { useUserStore } from '@/stores/user'

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
export const promptLogin = (content = '登录后可查看详情，是否现在去登录？') => {
  const userStore = useUserStore()
  if (userStore.isLoggedIn) {
    return true
  }

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
