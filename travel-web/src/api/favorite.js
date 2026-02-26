import request from '@/utils/request'

// 添加收藏
export const addFavorite = (spotId) => request.post('/favorites', { spotId })

// 取消收藏
export const removeFavorite = (spotId) => request.delete(`/favorites/${spotId}`)

// 获取收藏列表
export const getFavoriteList = (page = 1, pageSize = 10) =>
  request.get('/favorites', { params: { page, pageSize } })

// 检查收藏状态
export const checkFavorite = (spotId) => request.get(`/favorites/check/${spotId}`)

