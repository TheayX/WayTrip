// 用户相关接口
import { post, get, put, del, uploadFile } from '@/utils/client'

// 用户资料接口统一走 /user/*。

/**
 * 获取用户信息
 * @returns {*}
 */
export const getUserInfo = () => {
  return get('/user/info')
}

/**
 * 更新用户信息
 * @param data
 * @returns {*}
 */
export const updateUserInfo = (data) => {
  return put('/user/info', data)
}

/**
 * 设置用户偏好
 * @param data
 * @returns {*}
 */
export const setPreferences = (data) => {
  return post('/user/preferences', data)
}

/**
 * 上传头像
 * @param filePath
 * @returns {*}
 */
export const uploadAvatar = (filePath) => {
  return uploadFile('/upload/avatar', filePath, 'file')
}

/**
 * 修改密码
 * @param data
 * @returns {*}
 */
export const changePassword = (data) => {
  return put('/user/password', data)
}

/**
 * 注销账号
 * @returns {*}
 */
export const deactivateAccount = () => {
  return del('/user/account')
}
