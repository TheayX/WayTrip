import request from '@/utils/request'

export const submitReview = (data) => request.post('/reviews', data)

export const getUserReview = (spotId) => request.get(`/reviews/spot/${spotId}`)

export const getSpotReviews = (spotId, page = 1, pageSize = 10) =>
  request.get(`/reviews/spot/${spotId}/comments`, { params: { page, pageSize } })

export const getMyReviews = (page = 1, pageSize = 10) =>
  request.get('/reviews/mine', { params: { page, pageSize } })

export const deleteReview = (reviewId) => request.delete(`/reviews/${reviewId}`)
