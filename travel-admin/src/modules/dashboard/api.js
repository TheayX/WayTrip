// 仪表盘相关接口
import request from '@/shared/api/request.js'

/**
 * 获取仪表盘概览数据
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getOverview() {
  return request.get('/dashboard/overview')
}

/**
 * 获取订单趋势数据
 * @param days
 * @param mode
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getOrderTrend(days = 7, mode = 'weekday') {
  return request.get('/dashboard/order-trend', { params: { days, mode } })
}

/**
 * 获取热门景点数据
 * @param limit
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getHotSpots(limit = 10) {
  return request.get('/dashboard/hot-spots', { params: { limit } })
}
