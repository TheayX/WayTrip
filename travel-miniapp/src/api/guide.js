import { get } from '@/utils/request'

/**
 * 获取攻略列表
 */
export const getGuideList = (params) => {
  return get('/guides', params)
}

/**
 * 获取攻略详情
 */
export const getGuideDetail = (guideId) => {
  return get(`/guides/${guideId}`)
}

/**
 * 获取攻略分类
 */
export const getCategories = () => {
  return get('/guides/categories')
}
