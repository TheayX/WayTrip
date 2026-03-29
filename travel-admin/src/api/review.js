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
