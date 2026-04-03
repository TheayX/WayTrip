// 景点相关API接口
import { get, post } from '@/utils/client'

/**
 * 获取景点列表
 * @param params
 * @returns {*}
 */
export const getSpotList = (params) => {
  return get('/spots', params)
}

/**
 * 搜索景点
 * @param keyword
 * @param page
 * @param pageSize
 * @returns {*}
 */
export const searchSpots = (keyword, page = 1, pageSize = 10) => {
  return get('/spots/search', { keyword, page, pageSize })
}

/**
 * 获取景点详情
 * @param spotId
 * @returns {*}
 */
export const getSpotDetail = (spotId) => {
  return get(`/spots/${spotId}`)
}

/**
 * 获取我的最近浏览记录
 * @param page
 * @param pageSize
 * @returns {*}
 */
export const getViewHistory = (page = 1, pageSize = 20) => {
  return get('/spots/views', { page, pageSize })
}

/**
 * 获取相似景点推荐
 * @param spotId
 * @param limit
 * @returns {*}
 */
export const getSimilarSpots = (spotId, limit = 6) => {
  return get('/recommendations/similar', { spotId, limit })
}

/**
 * 记录景点浏览
 * @param spotId
 * @param source
 * @param duration
 * @returns {*}
 */
export const recordSpotView = (spotId, source, duration) => {
  // Query parameters
  return post(`/spots/${spotId}/view?source=${source}&duration=${duration}`)
}

/**
 * 获取景点过滤条件
 * @returns {*}
 */
export const getFilters = () => {
  return get('/spots/filters')
}
