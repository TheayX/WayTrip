// 攻略相关接口
import client from '@/shared/api/client.js'

/**
 * 获取攻略列表
 * @param {object} params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getGuideList = (params) => client.get('/guides', { params })

/**
 * 获取穷游攻略列表
 * @param {object} params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getBudgetGuideList = (params) => client.get('/guides/budget', { params })

/**
 * 获取攻略详情
 * @param guideId
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getGuideDetail = (guideId) => client.get(`/guides/${guideId}`)

/**
 * 获取攻略分类列表
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getCategories = () => client.get('/guides/categories')

