import request from '@/utils/request'

// 提交评分
export const submitRating = (data) => request.post('/ratings', data)

// 获取用户对景点的评分
export const getUserRating = (spotId) => request.get(`/ratings/spot/${spotId}`)

// 获取景点评论列表
export const getSpotRatings = (spotId, page = 1, pageSize = 10) =>
  request.get(`/ratings/spot/${spotId}/comments`, { params: { page, pageSize } })

