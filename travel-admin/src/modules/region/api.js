// 旅游目的地相关接口
import request from '@/shared/api/request.js'

/**
 * 获取目的地列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getRegions(params) {
  return request.get('/regions', { params })
}

/**
 * 创建新目的地
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function createRegion(data) {
  return request.post('/regions', data)
}

/**
 * 更新目的地信息
 * @param id
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function updateRegion(id, data) {
  return request.put(`/regions/${id}`, data)
}

/**
 * 删除目的地
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function deleteRegion(id) {
  return request.delete(`/regions/${id}`)
}
