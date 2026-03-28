const requestAuthorize = () => new Promise((resolve, reject) => {
  uni.authorize({
    scope: 'scope.userLocation',
    success: resolve,
    fail: reject
  })
})

const openLocationSetting = () => new Promise((resolve) => {
  uni.showModal({
    title: '开启定位',
    content: '开启定位后才能查看附近景点，是否前往设置？',
    success: ({ confirm }) => {
      if (!confirm) {
        resolve(false)
        return
      }

      uni.openSetting({
        success: (settingRes) => {
          resolve(Boolean(settingRes.authSetting?.['scope.userLocation']))
        },
        fail: () => resolve(false)
      })
    },
    fail: () => resolve(false)
  })
})

export const ensureLocationPermission = async () => {
  try {
    await requestAuthorize()
    return true
  } catch (_error) {
    return openLocationSetting()
  }
}

export const getCurrentLocation = () => new Promise((resolve, reject) => {
  uni.getLocation({
    type: 'gcj02',
    success: resolve,
    fail: reject
  })
})

export const getAuthorizedLocation = async () => {
  const granted = await ensureLocationPermission()
  if (!granted) {
    throw new Error('LOCATION_PERMISSION_DENIED')
  }

  return getCurrentLocation()
}
