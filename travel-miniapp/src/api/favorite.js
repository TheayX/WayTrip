// 收藏相关接口
import { get, post, del } from '@/utils/client'

/**
 * 添加收藏
 * @param spotId
 * @returns {*}
 */
export const addFavorite = (spotId) => {
  return post('/favorites', { spotId })
}

/**
 * 移除收藏
 * @param spotId
 * @returns {*}
 */
export const removeFavorite = (spotId) => {
  return del(`/favorites/${spotId}`)
}

/**
 * 获取收藏列表
 * @param page
 * @param pageSize
 * @returns {*}
 */
export const getFavoriteList = (page = 1, pageSize = 10) => {
  return get('/favorites', { page, pageSize })
}

/**
 * 检查是否已收藏
 * @param spotId
 * @returns {*}
 */
export const checkFavorite = (spotId) => {
  return get(`/favorites/check/${spotId}`)
}
