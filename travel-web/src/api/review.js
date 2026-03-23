import request from '@/utils/request'

// 提交评价
export const submitReview = (data) => request.post('/reviews', data)

// 获取用户对景点的评价
export const getUserReview = (spotId) => request.get(`/reviews/spot/${spotId}`)

// 获取景点评论列表
export const getSpotReviews = (spotId, page = 1, pageSize = 10) =>
  request.get(`/reviews/spot/${spotId}/comments`, { params: { page, pageSize } })

// 删除评价
export const deleteReview = (reviewId) => request.delete(`/reviews/${reviewId}`)

