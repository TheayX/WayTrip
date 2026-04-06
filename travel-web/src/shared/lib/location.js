// 定位能力工具

const LOCATION_CACHE_KEY = 'user_location_cache'

/**
 * 读取本地定位缓存
 * @returns {{latitude:number, longitude:number, timestamp:number}|null}
 */
export const getCachedLocation = () => {
  const raw = localStorage.getItem(LOCATION_CACHE_KEY)
  if (!raw) return null

  const parsed = JSON.parse(raw)
  if (!Number.isFinite(Number(parsed?.latitude)) || !Number.isFinite(Number(parsed?.longitude))) {
    return null
  }

  return {
    latitude: Number(parsed.latitude),
    longitude: Number(parsed.longitude),
    timestamp: Number(parsed.timestamp) || Date.now()
  }
}

/**
 * 写入定位缓存
 * @param {{latitude:number, longitude:number}} location
 */
export const saveLocationCache = (location) => {
  if (!location) return

  localStorage.setItem(LOCATION_CACHE_KEY, JSON.stringify({
    latitude: Number(location.latitude),
    longitude: Number(location.longitude),
    timestamp: Date.now()
  }))
}

/**
 * 获取浏览器当前位置
 * @returns {Promise<{latitude:number, longitude:number}>}
 */
export const getCurrentLocation = () => new Promise((resolve, reject) => {
  if (!navigator.geolocation) {
    reject(new Error('BROWSER_GEOLOCATION_UNSUPPORTED'))
    return
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      const location = {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude
      }
      saveLocationCache(location)
      resolve(location)
    },
    (error) => reject(error),
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 5 * 60 * 1000
    }
  )
})

/**
 * 获取定位快照，优先使用最新定位，其次回退缓存
 * @returns {Promise<{cached:Object|null, latest:Object|null, current:Object|null}>}
 */
export const getLocationSnapshot = async () => {
  const cached = getCachedLocation()

  try {
    const latest = await getCurrentLocation()
    return {
      cached,
      latest,
      current: latest || cached || null
    }
  } catch {
    return {
      cached,
      latest: null,
      current: cached || null
    }
  }
}

/**
 * 清除定位缓存
 */
export const clearLocationCache = () => {
  localStorage.removeItem(LOCATION_CACHE_KEY)
}
