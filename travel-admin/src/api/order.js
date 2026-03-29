// 订单相关接口
import request from '@/utils/request'

/**
 * 获取订单列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getOrderList(params) {
  return request.get('/orders', { params })
}

/**
 * 获取订单详情
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getOrderDetail(id) {
  return request.get(`/orders/${id}`)
}

/**
 * 完成订单
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function completeOrder(id) {
  return request.post(`/orders/${id}/complete`)
}

/**
 * 退款订单
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function refundOrder(id) {
  return request.post(`/orders/${id}/refund`)
}

/**
 * 取消订单
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function cancelOrder(id) {
  return request.post(`/orders/${id}/cancel`)
}

/**
 * 重新打开订单
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function reopenOrder(id) {
  return request.post(`/orders/${id}/reopen`)
}
