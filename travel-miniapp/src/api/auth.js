import { post, get, put, del, uploadFile } from '@/utils/request'

/**
 * 微信登录
 */
export const wxLogin = (code) => {
  return post('/auth/wx-login', { code })
}

/**
 * 小程序端绑定手机号（匹配已有账户则合并openid）
 */
export const wxBindPhone = (data) => {
  return post('/auth/wx-bind-phone', data)
}

/**
 * 获取用户信息
 */
export const getUserInfo = () => {
  return get('/user/info')
}

/**
 * 更新用户信息
 */
export const updateUserInfo = (data) => {
  return put('/user/info', data)
}


/**
 * 更新偏好标签
 */
export const updatePreferences = (data) => {
  return post('/user/preferences', data)
}


/**
 * 上传头像
 */
export const uploadAvatar = (filePath) => {
  return uploadFile('/upload/avatar', filePath, 'file')
}

/**
 * 修改密码
 */
export const changePassword = (data) => {
  return put('/user/password', data)
}

/**
 * 注销账户
 */
export const deactivateAccount = () => {
  return del('/user/account')
}

