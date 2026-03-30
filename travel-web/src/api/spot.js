// 景点相关API接口
import request from '@/utils/request'

/**
 * 获取景点列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getSpotList = (params) => request.get('/spots', { params })

/**
 * 搜索景点
 * @param keyword
 * @param page
 * @param pageSize
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const searchSpots = (keyword, page = 1, pageSize = 10) =>
  request.get('/spots/search', { params: { keyword, page, pageSize } })

/**
 * 获取景点详情
 * @param spotId
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getSpotDetail = (spotId) => request.get(`/spots/${spotId}`)

/**
 * 获取我的浏览记录
 * @param page
 * @param pageSize
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getViewHistory = (page = 1, pageSize = 20) =>
  request.get('/spots/views', { params: { page, pageSize } })

/**
 * 获取相似景点推荐
 * @param spotId
 * @param limit
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getSimilarSpots = (spotId, limit = 6) =>
  request.get('/recommendations/similar', { params: { spotId, limit } })

/**
 * 记录景点浏览
 * @param spotId
 * @param source
 * @param duration
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const recordSpotView = (spotId, source, duration) =>
  request.post(`/spots/${spotId}/view`, null, { params: { source, duration } })

/**
 * 获取景点过滤条件
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getFilters = () => request.get('/spots/filters')
