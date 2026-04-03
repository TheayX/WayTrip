// 认证相关接口
import client from '@/shared/api/client.js'

// /auth/* 仅用于登录注册。

/**
 * Web 注册
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const register = (data) => client.post('/auth/web-register', data)

/**
 * 预处理注册
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const prepareRegister = (data) => client.post('/auth/web-prepare-register', data)

/**
 * Web 登录
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const login = (data) => client.post('/auth/web-login', data)

