// 浏览行为相关接口
import request from '@/shared/api/request.js'

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
