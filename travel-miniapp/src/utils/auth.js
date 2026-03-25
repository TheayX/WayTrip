import { useUserStore } from '@/stores/user'

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
