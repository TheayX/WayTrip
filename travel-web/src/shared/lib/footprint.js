// 浏览足迹缓存工具

const FOOTPRINT_CACHE_KEY = 'spot_footprints'
const MAX_FOOTPRINTS = 20

/**
 * 规范化浏览足迹项，避免本地缓存被脏数据污染
 * @param {Array} list
 * @returns {Array}
 */
export const normalizeFootprints = (list) => {
  if (!Array.isArray(list)) return []

  return list
    .filter((item) => item?.id)
    .map((item) => ({
      id: item.id,
      name: item.name || '',
      coverImage: item.coverImage || '',
      regionName: item.regionName || '',
      categoryName: item.categoryName || '',
      viewedAt: item.viewedAt || Date.now()
    }))
}

/**
 * 读取本地浏览足迹
 * @returns {Array}
 */
export const getFootprints = () => {
  const raw = localStorage.getItem(FOOTPRINT_CACHE_KEY)
  return normalizeFootprints(raw ? JSON.parse(raw) : [])
}

/**
 * 写入本地浏览足迹
 * @param {Array} list
 */
export const setFootprints = (list) => {
  localStorage.setItem(
    FOOTPRINT_CACHE_KEY,
    JSON.stringify(normalizeFootprints(list).slice(0, MAX_FOOTPRINTS))
  )
}

/**
 * 追加一条浏览足迹，并将最近浏览置顶
 * @param {Object} spot
 */
export const saveSpotFootprint = (spot) => {
  if (!spot?.id) return

  const current = getFootprints()
  const nextItem = {
    id: spot.id,
    name: spot.name,
    coverImage: spot.coverImage,
    regionName: spot.regionName,
    categoryName: spot.categoryName,
    viewedAt: Date.now()
  }

  setFootprints([nextItem, ...current.filter((item) => item.id !== spot.id)])
}

/**
 * 清理浏览足迹
 */
export const clearFootprints = () => {
  localStorage.removeItem(FOOTPRINT_CACHE_KEY)
}
