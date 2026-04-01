// 管理员相关 API 接口
import request from '@/shared/api/request.js'

/**
 * 获取管理员列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getAdminList(params) {
  return request.get('/admins', { params })
}

/**
 * 创建新管理员
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function createAdmin(data) {
  return request.post('/admins', data)
}

/**
 * 更新管理员信息
 * @param id
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function updateAdmin(id, data) {
  return request.put(`/admins/${id}`, data)
}

/**
 * 重置管理员密码
 * @param id
 * @param password
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function resetAdminPassword(id, password) {
  return request.put(`/admins/${id}/password`, { password })
}

/**
 * 删除管理员
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function deleteAdmin(id) {
  return request.delete(`/admins/${id}`)
}
