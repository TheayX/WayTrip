// 评论相关接口
import { del, get, post } from '@/utils/client'

/**
 * 提交评论
 * @param data
 * @returns {*}
 */
export const submitReview = (data) => {
  return post('/reviews', data)
}

/**
 * 获取用户对某景点的评论（如果有的话）
 * @param spotId
 * @returns {*}
 */
export const getUserReview = (spotId) => {
  return get(`/reviews/spot/${spotId}`)
}

/**
 * 获取景点评论列表（分页）
 * @param spotId
 * @param page
 * @param pageSize
 * @returns {*}
 */
export const getSpotReviews = (spotId, page = 1, pageSize = 10) => {
  return get(`/reviews/spot/${spotId}/comments`, { page, pageSize })
}

/**
 * 获取游客口碑流（分页）
 * @param params
 * @returns {*}
 */
export const getReviewFeed = (params) => {
  return get('/reviews/feed', params)
}

/**
 * 获取我的评论列表（分页）
 * @param page
 * @param pageSize
 * @returns {*}
 */
export const getMyReviews = (page = 1, pageSize = 10) => {
  return get('/reviews/mine', { page, pageSize })
}

/**
 * 删除评论
 * @param reviewId
 * @returns {*}
 */
export const deleteReview = (reviewId) => {
  return del(`/reviews/${reviewId}`)
}
