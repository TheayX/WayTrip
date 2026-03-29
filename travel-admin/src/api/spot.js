// 景点相关接口
import request from '@/utils/request'

/**
 * 获取景点列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getSpotList(params) {
  return request.get('/spots', { params })
}

/**
 * 获取景点详情
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getSpotDetail(id) {
  return request.get(`/spots/${id}`)
}

/**
 * 创建新景点
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function createSpot(data) {
  return request.post('/spots', data)
}

/**
 * 更新景点信息
 * @param id
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function updateSpot(id, data) {
  return request.put(`/spots/${id}`, data)
}

/**
 * 切换景点发布状态
 * @param id
 * @param published
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function updatePublishStatus(id, published) {
  return request.put(`/spots/${id}/publish`, { published })
}

/**
 * 删除景点
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function deleteSpot(id) {
  return request.delete(`/spots/${id}`)
}

/**
 * 刷新单个景点评分
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function refreshSpotRating(id) {
  return request.post(`/spots/${id}/rating/refresh`)
}

/**
 * 刷新全部景点评分
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function refreshAllSpotRatings() {
  return request.post('/spots/rating/refresh')
}

/**
 * 刷新单个景点热度
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function refreshSpotHeat(id) {
  return request.post(`/spots/${id}/heat/refresh`)
}

/**
 * 刷新全部景点热度
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function refreshAllSpotHeat() {
  return request.post('/spots/heat/refresh')
}

/**
 * 获取景点过滤条件列表
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getFilters() {
  return request.get('/spots/filters')
}
