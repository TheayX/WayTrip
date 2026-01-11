import request from '@/utils/request'

// 管理员登录
export function login(username, password) {
  return request.post('/auth/login', { username, password })
}

// 获取管理员信息
export function getAdminInfo() {
  return request.get('/auth/info')
}

// 修改密码
export function changePassword(oldPassword, newPassword) {
  return request.put('/auth/password', { oldPassword, newPassword })
}
