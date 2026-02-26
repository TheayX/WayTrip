import request from '@/utils/request'

// 获取轮播图
export const getBanners = () => request.get('/home/banners')

// 获取热门景点
export const getHotSpots = (limit = 10) => request.get('/home/hot', { params: { limit } })

// 获取个性化推荐
export const getRecommendations = (limit = 10) => request.get('/recommendations', { params: { limit } })

// 刷新推荐
export const refreshRecommendations = (limit = 10) => request.post('/recommendations/refresh', null, { params: { limit } })

