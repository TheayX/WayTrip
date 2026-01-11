import request from '@/utils/request'

/**
 * 获取概览数据
 */
export function getOverview() {
  return request({
    url: '/dashboard/overview',
    method: 'get'
  })
}

/**
 * 获取订单趋势
 */
export function getOrderTrend(days = 7) {
  return request({
    url: '/dashboard/order-trend',
    method: 'get',
    params: { days }
  })
}

/**
 * 获取热门景点
 */
export function getHotSpots(limit = 10) {
  return request({
    url: '/dashboard/hot-spots',
    method: 'get',
    params: { limit }
  })
}
