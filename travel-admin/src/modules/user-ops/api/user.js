// 用户相关接口
import request from '@/shared/api/request.js'

/**
 * 获取用户列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getUserList(params) {
  return request.get('/users', { params })
}

/**
 * 获取用户详情
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getUserDetail(id) {
  return request.get(`/users/${id}`)
}

/**
 * 重置用户密码
 * @param id
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function resetUserPassword(id, data) {
  return request.put(`/users/${id}/password`, data)
}

/**
 * 封禁用户
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function suspendUserAccount(id) {
  return request.delete(`/users/${id}/account`)
}

/**
 * 解封用户
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function restoreUserAccount(id) {
  return request.put(`/users/${id}/account/restore`)
}
