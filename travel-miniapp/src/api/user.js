import { post, get, put, del, uploadFile } from '@/utils/request'

// 用户资料接口统一走 /user/*。

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
 * 设置偏好标签
 */
export const setPreferences = (data) => {
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
