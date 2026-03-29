import request from '@/utils/request'

export function getPreferenceList(params) {
  return request.get('/user-insights/preferences', { params })
}

export function getFavoriteList(params) {
  return request.get('/user-insights/favorites', { params })
}

export function deleteFavorite(id) {
  return request.delete(`/user-insights/favorites/${id}`)
}

export function getViewList(params) {
  return request.get('/user-insights/views', { params })
}

export function deleteView(id) {
  return request.delete(`/user-insights/views/${id}`)
}
