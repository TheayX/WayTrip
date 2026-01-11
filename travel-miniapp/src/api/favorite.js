import { get, post, del } from '@/utils/request'

/**
 * 添加收藏
 */
export const addFavorite = (spotId) => {
  return post('/favorites', { spotId })
}

/**
 * 取消收藏
 */
export const removeFavorite = (spotId) => {
  return del(`/favorites/${spotId}`)
}

/**
 * 获取收藏列表
 */
export const getFavoriteList = (page = 1, pageSize = 10) => {
  return get('/favorites', { page, pageSize })
}

/**
 * 检查收藏状态
 */
export const checkFavorite = (spotId) => {
  return get(`/favorites/check/${spotId}`)
}
