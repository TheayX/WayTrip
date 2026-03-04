import { get, post } from '@/utils/request'

/**
 * 提交评价
 */
export const submitReview = (data) => {
  return post('/reviews', data)
}

/**
 * 获取用户对景点的评价
 */
export const getUserReview = (spotId) => {
  return get(`/reviews/spot/${spotId}`)
}

/**
 * 获取景点评论列表
 */
export const getSpotReviews = (spotId, page = 1, pageSize = 10) => {
  return get(`/reviews/spot/${spotId}/comments`, { page, pageSize })
}

