// 认证相关接口
import request from '@/utils/request'

// /auth/* 仅用于登录注册。

/**
 * Web 注册
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const register = (data) => request.post('/auth/web-register', data)

/**
 * 预处理注册
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const prepareRegister = (data) => request.post('/auth/web-prepare-register', data)

/**
 * Web 登录
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const login = (data) => request.post('/auth/web-login', data)

