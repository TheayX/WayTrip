// 收藏相关接口
import request from '@/utils/request'

/**
 * 添加收藏
 * @param spotId
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const addFavorite = (spotId) => request.post('/favorites', { spotId })

/**
 * 移除收藏
 * @param spotId
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const removeFavorite = (spotId) => request.delete(`/favorites/${spotId}`)

/**
 * 获取收藏列表
 * @param page
 * @param pageSize
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getFavoriteList = (page = 1, pageSize = 10) =>
  request.get('/favorites', { params: { page, pageSize } })

/**
 * 检查是否已收藏
 * @param spotId
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const checkFavorite = (spotId) => request.get(`/favorites/check/${spotId}`)

