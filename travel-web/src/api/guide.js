// 攻略相关接口
import request from '@/utils/request'

/**
 * 获取攻略列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getGuideList = (params) => request.get('/guides', { params })

/**
 * 获取攻略详情
 * @param guideId
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getGuideDetail = (guideId) => request.get(`/guides/${guideId}`)

/**
 * 获取攻略分类列表
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getCategories = () => request.get('/guides/categories')

