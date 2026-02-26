import request from '@/utils/request'

/**
 * 获取用户列表
 */
export function getUserList(params) {
  return request({
    url: '/users',
    method: 'get',
    params
  })
}

/**
 * 获取用户详情
 */
export function getUserDetail(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

/**
 * 重置用户密码
 */
export function resetUserPassword(id, data) {
  return request({
    url: `/users/${id}/password`,
    method: 'put',
    data
  })
}

