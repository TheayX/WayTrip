// 首页相关接口
import client from '@/shared/api/client.js'

/**
 * 获取首页轮播图
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getBanners = () => client.get('/home/banners')

/**
 * 获取热门景点
 * @param limit
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getHotSpots = (limit = 10) => client.get('/home/hot', { params: { limit } })

/**
 * 获取近期热看
 * @param days
 * @param limit
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getRecentViewedSpots = (days = 14, limit = 12) =>
  client.get('/home/recent-views', { params: { days, limit } })

/**
 * 获取推荐景点
 * @param limit
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getRecommendations = (limit = 10) => client.get('/recommendations', { params: { limit } })

/**
 * 获取附近景点
 * @param latitude
 * @param longitude
 * @param limit
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getNearbySpots = (latitude, longitude, limit = 10) =>
  client.get('/home/nearby', { params: { latitude, longitude, limit } })

/**
 * 轮换推荐景点
 * @param limit
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const rotateRecommendations = (limit = 10) => client.post('/recommendations/rotate', null, { params: { limit } })

/**
 * 重算推荐景点
 * @param limit
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const recomputeRecommendations = (limit = 10) => client.post('/recommendations/recompute', null, { params: { limit } })
