import request from '@/utils/request'

// 获取用户列表
export function getUserList(params) {
  return request.get('/users', { params })
}

// 获取用户详情
export function getUserDetail(id) {
  return request.get(`/users/${id}`)
}

// 重置用户密码
export function resetUserPassword(id, data) {
  return request.put(`/users/${id}/password`, data)
}
