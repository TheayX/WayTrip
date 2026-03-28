import request from '@/utils/request'

// 用户资料接口统一走 /user/*。

// 获取用户信息
export const getUserInfo = () => request.get('/user/info')

// 更新用户信息
export const updateUserInfo = (data) => request.put('/user/info', data)

// 设置偏好标签
export const setPreferences = (categoryIds) => request.post('/user/preferences', { categoryIds })

// 修改密码
export const changePassword = (data) => request.put('/user/password', data)

// 上传头像
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 注销账户
export const deactivateAccount = () => request.delete('/user/account')
