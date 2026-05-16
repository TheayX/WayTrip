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
