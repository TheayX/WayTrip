import request from '@/utils/request'

// 获取攻略列表
export const getGuideList = (params) => request.get('/guides', { params })

// 获取攻略详情
export const getGuideDetail = (guideId) => request.get(`/guides/${guideId}`)

// 获取攻略分类
export const getCategories = () => request.get('/guides/categories')

