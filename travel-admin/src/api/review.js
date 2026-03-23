import request from '@/utils/request'

export function getReviewList(params) {
  return request.get('/reviews', { params })
}
