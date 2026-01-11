import { post, get, put } from '@/utils/request'

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
