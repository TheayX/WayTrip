import request from '@/utils/request'

// 手机号 + 密码注册
export const register = (data) => request.post('/auth/web-register', data)

// 手机号 + 密码登录
export const login = (data) => request.post('/auth/web-login', data)

// 获取用户信息
export const getUserInfo = () => request.get('/auth/user-info')

// 更新用户信息
export const updateUserInfo = (data) => request.put('/auth/user-info', data)

// 设置偏好标签
export const setPreferences = (tags) => request.post('/auth/preferences', { tags })

