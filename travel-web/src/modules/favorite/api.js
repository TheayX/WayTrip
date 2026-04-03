// 收藏相关接口
import client from '@/shared/api/client.js'

/**
 * 添加收藏
 * @param spotId
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const addFavorite = (spotId) => client.post('/favorites', { spotId })

/**
 * 移除收藏
 * @param spotId
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const removeFavorite = (spotId) => client.delete(`/favorites/${spotId}`)

/**
 * 获取收藏列表
 * @param page
 * @param pageSize
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getFavoriteList = (page = 1, pageSize = 10) =>
  client.get('/favorites', { params: { page, pageSize } })

/**
 * 检查是否已收藏
 * @param spotId
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const checkFavorite = (spotId) => client.get(`/favorites/check/${spotId}`)

