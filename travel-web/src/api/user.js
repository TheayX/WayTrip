// 用户相关接口
import request from '@/utils/request'

// 用户资料接口统一走 /user/*。

/**
 * 获取用户信息
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getUserInfo = () => request.get('/user/info')

/**
 * 更新用户信息
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const updateUserInfo = (data) => request.put('/user/info', data)

/**
 * 设置用户偏好
 * @param categoryIds
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const setPreferences = (categoryIds) => request.post('/user/preferences', { categoryIds })

/**
 * 修改密码
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const changePassword = (data) => request.put('/user/password', data)

/**
 * 上传头像
 * @param file
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 注销账号
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const deactivateAccount = () => request.delete('/user/account')
