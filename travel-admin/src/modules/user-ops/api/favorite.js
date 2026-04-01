// 用户收藏相关接口
import request from '@/shared/api/request.js'

/**
 * 获取用户收藏列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getFavoriteList(params) {
  return request.get('/user-insights/favorites', { params })
}

/**
 * 删除收藏记录
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function deleteFavorite(id) {
  return request.delete(`/user-insights/favorites/${id}`)
}
