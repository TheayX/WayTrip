import request from '@/utils/request'

/**
 * 获取用户偏好列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getPreferenceList(params) {
  return request.get('/user-insights/preferences', { params })
}

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

/**
 * 获取浏览行为列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getViewList(params) {
  return request.get('/user-insights/views', { params })
}

/**
 * 删除浏览记录
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function deleteView(id) {
  return request.delete(`/user-insights/views/${id}`)
}
