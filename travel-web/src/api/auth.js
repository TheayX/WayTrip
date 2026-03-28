import request from '@/utils/request'

// /auth/* 仅用于登录注册。

// 手机号 + 密码注册
export const register = (data) => request.post('/auth/web-register', data)

// Web 注册第一步校验
export const prepareRegister = (data) => request.post('/auth/web-prepare-register', data)

// 手机号 + 密码登录
export const login = (data) => request.post('/auth/web-login', data)

