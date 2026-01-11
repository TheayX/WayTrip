import request from '@/utils/request'

// 获取攻略列表
export function getGuideList(params) {
  return request.get('/guides', { params })
}

// 获取攻略详情
export function getGuideDetail(id) {
  return request.get(`/guides/${id}`)
}

// 获取攻略分类
export function getCategories() {
  return request.get('/guides/categories')
}

// 创建攻略
export function createGuide(data) {
  return request.post('/guides', data)
}

// 更新攻略
export function updateGuide(id, data) {
  return request.put(`/guides/${id}`, data)
}

// 更新发布状态
export function updatePublishStatus(id, published) {
  return request.put(`/guides/${id}/publish`, { published })
}

// 删除攻略
export function deleteGuide(id) {
  return request.delete(`/guides/${id}`)
}
