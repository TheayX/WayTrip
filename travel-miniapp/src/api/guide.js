// 攻略相关接口
import { get } from '@/utils/request'

/**
 * 获取攻略列表
 * @param params
 * @returns {*}
 */
export const getGuideList = (params) => {
  return get('/guides', params)
}

/**
 * 获取穷游攻略列表
 * @param params
 * @returns {*}
 */
export const getBudgetGuideList = (params) => {
  return get('/guides/budget', params)
}

/**
 * 获取攻略详情
 * @param guideId
 * @returns {*}
 */
export const getGuideDetail = (guideId) => {
  return get(`/guides/${guideId}`)
}

/**
 * 获取攻略分类列表
 * @returns {*}
 */
export const getCategories = () => {
  return get('/guides/categories')
}
