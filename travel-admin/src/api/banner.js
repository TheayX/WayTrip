import request from '@/utils/request'

// 获取轮播图列表
export function getBannerList() {
  return request.get('/banners')
}

// 创建轮播图
export function createBanner(data) {
  return request.post('/banners', data)
}

// 更新轮播图
export function updateBanner(id, data) {
  return request.put(`/banners/${id}`, data)
}

// 删除轮播图
export function deleteBanner(id) {
  return request.delete(`/banners/${id}`)
}

// 切换启用状态
export function toggleBannerEnabled(id) {
  return request.post(`/banners/${id}/toggle`)
}
