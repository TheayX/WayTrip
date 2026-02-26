import request from '@/utils/request'

// 获取景点列表
export const getSpotList = (params) => request.get('/spots', { params })

// 搜索景点
export const searchSpots = (keyword, page = 1, pageSize = 10) =>
  request.get('/spots/search', { params: { keyword, page, pageSize } })

// 获取景点详情
export const getSpotDetail = (spotId) => request.get(`/spots/${spotId}`)

// 获取筛选选项
export const getFilters = () => request.get('/spots/filters')

