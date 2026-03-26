import request from '@/utils/request'

// 获取概览数据
export function getOverview() {
  return request.get('/dashboard/overview')
}

// 获取订单趋势
export function getOrderTrend(days = 7) {
  return request.get('/dashboard/order-trend', { params: { days } })
}

// 获取热门景点
export function getHotSpots(limit = 10) {
  return request.get('/dashboard/hot-spots', { params: { limit } })
}
