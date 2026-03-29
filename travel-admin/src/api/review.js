// 评论相关接口
import request from '@/utils/request'

/**
 * 获取评论列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getReviewList(params) {
  return request.get('/reviews', { params })
}

/**
 * 删除评论
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function deleteReview(id) {
  return request.delete(`/reviews/${id}`)
}
