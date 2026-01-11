import request from '@/utils/request'
import axios from 'axios'

// 用户端请求（用于获取公共数据）
const publicRequest = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

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

// 获取筛选选项（地区、分类）- 使用用户端接口
export function getFilters() {
  return publicRequest.get('/spots/filters').then(res => res.data)
}
