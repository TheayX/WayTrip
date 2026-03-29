// 认证相关的 API 接口
import request from '@/utils/request'

/**
 * 管理员登录
 * @param username
 * @param password
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

/**
 * 获取管理员信息
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getAdminInfo() {
  return request.get('/auth/info')
}

/**
 * 修改管理员密码
 * @param oldPassword
 * @param newPassword
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function changePassword(oldPassword, newPassword) {
  return request.put('/auth/password', { oldPassword, newPassword })
}
