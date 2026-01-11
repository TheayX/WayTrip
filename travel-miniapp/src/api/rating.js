import { get, post } from '@/utils/request'

/**
 * 提交评分
 */
export const submitRating = (data) => {
  return post('/ratings', data)
}

/**
 * 获取用户对景点的评分
 */
export const getUserRating = (spotId) => {
  return get(`/ratings/spot/${spotId}`)
}

/**
 * 获取景点评论列表
 */
export const getSpotRatings = (spotId, page = 1, pageSize = 10) => {
  return get(`/ratings/spot/${spotId}/comments`, { page, pageSize })
}
