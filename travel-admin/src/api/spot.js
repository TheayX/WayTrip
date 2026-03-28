import request from '@/utils/request'

// 获取景点列表
export function getSpotList(params) {
  return request.get('/spots', { params })
}

// 获取景点详情
export function getSpotDetail(id) {
  return request.get(`/spots/${id}`)
}

// 创建景点
export function createSpot(data) {
  return request.post('/spots', data)
}

// 更新景点
export function updateSpot(id, data) {
  return request.put(`/spots/${id}`, data)
}

// 更新发布状态
export function updatePublishStatus(id, published) {
  return request.put(`/spots/${id}/publish`, { published })
}

// 删除景点
export function deleteSpot(id) {
  return request.delete(`/spots/${id}`)
}

// 获取筛选选项（地区、分类）
export function refreshSpotRating(id) {
  return request.post(`/spots/${id}/rating/refresh`)
}

export function refreshAllSpotRatings() {
  return request.post('/spots/rating/refresh')
}

export function refreshSpotHeat(id) {
  return request.post(`/spots/${id}/heat/refresh`)
}

export function refreshAllSpotHeat() {
  return request.post('/spots/heat/refresh')
}

export function getFilters() {
  return request.get('/spots/filters')
}
