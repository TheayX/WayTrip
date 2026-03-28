import { get, post } from '@/utils/request'

/**
 * 获取轮播图
 * @returns {*}
 */
export const getBanners = () => {
  return get('/home/banners')
}

/**
 * 获取热门景点
 * @param limit
 * @returns {*}
 */
export const getHotSpots = (limit = 10) => {
  return get('/home/hot', { limit })
}

/**
 * 获取附近景点
 * @param latitude
 * @param longitude
 * @param limit
 * @returns {*}
 */
export const getNearbySpots = (latitude, longitude, limit = 3) => {
  return get('/home/nearby', { latitude, longitude, limit }, { rejectOnAuthExpired: true })
}

/**
 * 获取推荐景点
 * @param limit
 * @returns {*}
 */
export const getRecommendations = (limit = 10) => {
  return get('/recommendations', { limit })
}

/**
 * 刷新推荐景点
 * @param limit
 * @returns {*}
 */
export const refreshRecommendations = (limit = 10) => {
  return post('/recommendations/refresh', null, { params: { limit } })
}
