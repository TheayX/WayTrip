import { del, get, post } from '@/utils/request'

export const submitReview = (data) => {
  return post('/reviews', data)
}

export const getUserReview = (spotId) => {
  return get(`/reviews/spot/${spotId}`)
}

export const getSpotReviews = (spotId, page = 1, pageSize = 10) => {
  return get(`/reviews/spot/${spotId}/comments`, { page, pageSize })
}

export const getMyReviews = (page = 1, pageSize = 10) => {
  return get('/reviews/mine', { page, pageSize })
}

export const deleteReview = (reviewId) => {
  return del(`/reviews/${reviewId}`)
}
