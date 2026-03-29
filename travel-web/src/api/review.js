// 评论相关API
import request from '@/utils/request'

/**
 * 提交评论
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const submitReview = (data) => request.post('/reviews', data)

/**
 * 获取用户对某个景点的评论
 * @param spotId
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getUserReview = (spotId) => request.get(`/reviews/spot/${spotId}`)

/**
 * 获取景点的评论列表
 * @param spotId
 * @param page
 * @param pageSize
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getSpotReviews = (spotId, page = 1, pageSize = 10) =>
  request.get(`/reviews/spot/${spotId}/comments`, { params: { page, pageSize } })

/**
 * 获取我的评论列表
 * @param page
 * @param pageSize
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getMyReviews = (page = 1, pageSize = 10) =>
  request.get('/reviews/mine', { params: { page, pageSize } })

/**
 * 删除评论
 * @param reviewId
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const deleteReview = (reviewId) => request.delete(`/reviews/${reviewId}`)
