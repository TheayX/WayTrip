import { get, post } from '@/utils/request'

/**
 * 获取轮播图
 */
export const getBanners = () => {
  return get('/home/banners')
}

/**
 * 获取热门景点
 */
export const getHotSpots = (limit = 10) => {
  return get('/home/hot', { limit })
}

/**
 * 获取个性化推荐
 */
export const getRecommendations = (limit = 10) => {
  return get('/recommendations', { limit })
}

/**
 * 刷新推荐
 */
export const refreshRecommendations = (limit = 10) => {
  return post('/recommendations/refresh', null, { params: { limit } })
}
