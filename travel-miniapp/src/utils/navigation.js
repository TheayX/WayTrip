// tabBar 页面跳转工具，统一处理重复跳转和 switchTab 失败时的兜底策略。

// 去掉 query 后只保留页面路径，便于比较当前页和目标页。
const normalizePagePath = (url = '') => {
  return String(url).split('?')[0]
}

// 获取当前页面路径。
const getCurrentPagePath = () => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  return current?.route ? `/${current.route}` : ''
}

// 安全切换 tabBar 页面，避免重复跳转和偶发超时。
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
