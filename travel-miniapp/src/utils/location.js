// 常量配置
const LOCATION_CACHE_KEY = 'user_location_cache'

// 内部方法
const saveLocationCache = (location) => {
  if (!location) return
  uni.setStorageSync(LOCATION_CACHE_KEY, {
    latitude: location.latitude,
    longitude: location.longitude,
    timestamp: Date.now()
  })
}

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

// 对外暴露方法
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
    success: (res) => {
      saveLocationCache(res)
      resolve(res)
    },
    fail: reject
  })
})

export const getCachedLocation = () => {
  const cached = uni.getStorageSync(LOCATION_CACHE_KEY)
  if (!cached?.latitude || !cached?.longitude) {
    return null
  }
  return cached
}

export const getLocationSnapshot = async () => {
  const cached = getCachedLocation()

  try {
    const latest = await getLocationIfAuthorized()
    return {
      cached,
      latest,
      current: latest || cached || null
    }
  } catch (_error) {
    return {
      cached,
      latest: null,
      current: cached || null
    }
  }
}

export const getLocationPermissionState = () => new Promise((resolve, reject) => {
  uni.getSetting({
    success: (res) => {
      if (res.authSetting?.['scope.userLocation']) {
        resolve('granted')
        return
      }
      resolve('prompt')
    },
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

export const getLocationIfAuthorized = async () => {
  const permissionState = await getLocationPermissionState()
  if (permissionState !== 'granted') {
    return null
  }

  return getCurrentLocation()
}
