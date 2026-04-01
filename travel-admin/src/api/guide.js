// 攻略相关接口
import request from '@/shared/api/request.js'

/**
 * 获取攻略列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getGuideList(params) {
  return request.get('/guides', { params })
}

/**
 * 获取攻略详情
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getGuideDetail(id) {
  return request.get(`/guides/${id}`)
}

/**
 * 获取攻略分类列表
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getCategories() {
  return request.get('/guides/categories')
}

/**
 * 创建新攻略
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function createGuide(data) {
  return request.post('/guides', data)
}

/**
 * 更新攻略信息
 * @param id
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function updateGuide(id, data) {
  return request.put(`/guides/${id}`, data)
}

/**
 * 切换攻略发布状态
 * @param id
 * @param published
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function updatePublishStatus(id, published) {
  return request.put(`/guides/${id}/publish`, { published })
}

/**
 * 删除攻略
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function deleteGuide(id) {
  return request.delete(`/guides/${id}`)
}
