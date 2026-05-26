// 统一处理 tabBar 页面跳转，避免重复切到当前页或偶发 switchTab timeout 造成白屏。
const normalizePagePath = (url = '') => {
  return String(url).split('?')[0]
}

const getCurrentPagePath = () => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  return current?.route ? `/${current.route}` : ''
}

export const switchTabSafely = (url) => {
  const targetPath = normalizePagePath(url)
  const currentPath = getCurrentPagePath()

  if (!targetPath || currentPath === targetPath) {
    return Promise.resolve()
  }

  return new Promise((resolve, reject) => {
    uni.switchTab({
      url,
      success: resolve,
      fail: (error) => {
        // switchTab 在真机或弱网环境下偶发 timeout 时，回退到 reLaunch 保证用户能到目标页。
        if (String(error?.errMsg || '').includes('timeout')) {
          uni.reLaunch({
            url: targetPath,
            success: resolve,
            fail: reject
          })
          return
        }
        reject(error)
      }
    })
  })
}
