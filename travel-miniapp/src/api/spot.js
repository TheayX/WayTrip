import { get, post } from '@/utils/request'

/**
 * 获取景点列表
 */
export const getSpotList = (params) => {
  return get('/spots', params)
}

/**
 * 搜索景点
 */
export const searchSpots = (keyword, page = 1, pageSize = 10) => {
  return get('/spots/search', { keyword, page, pageSize })
}

/**
 * 获取景点详情
 */
export const getSpotDetail = (spotId) => {
  return get(`/spots/${spotId}`)
}

/**
 * 获取相似景点推荐
 */
export const getSimilarSpots = (spotId, limit = 6) => {
  return get('/recommendations/similar', { spotId, limit })
}

/**
 * 上报景点浏览行为
 */
export const recordSpotView = (spotId, source, duration) => {
  // Query parameters
  return post(`/spots/${spotId}/view?source=${source}&duration=${duration}`)
}

/**
 * 获取筛选选项
 */
export const getFilters = () => {
  return get('/spots/filters')
}
