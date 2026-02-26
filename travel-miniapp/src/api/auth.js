import { post, get, put, uploadFile } from '@/utils/request'

/**
 * 微信登录
 */
export const wxLogin = (code) => {
  return post('/auth/wx-login', { code })
}

/**
 * 获取用户信息
 */
export const getUserInfo = () => {
  return get('/auth/user-info')
}

/**
 * 更新用户信息
 */
export const updateUserInfo = (data) => {
  return put('/auth/user-info', data)
}

/**
 * 设置偏好标签
 */
export const setPreferences = (tags) => {
  return post('/auth/preferences', { tags })
}

/**
 * 更新偏好标签（分类ID列表）
 */
export const updatePreferences = (data) => {
  return post('/auth/preferences', data)
}

/**
 * 修改密码
 */
export const changePassword = (data) => {
  return put('/auth/password', data)
}

/**
 * 上传头像
 */
export const uploadAvatar = (filePath) => {
  return uploadFile('/upload/avatar', filePath, 'file')
}

