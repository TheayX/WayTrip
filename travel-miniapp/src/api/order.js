// 订单相关接口
import { get, post } from '@/utils/request'

/**
 * 创建订单
 * @param data
 * @returns {*}
 */
export const createOrder = (data) => {
  return post('/orders', data)
}

/**
 * 获取订单列表
 * @param params
 * @returns {*}
 */
export const getOrderList = (params) => {
  return get('/orders', params)
}

/**
 * 获取订单详情
 * @param id
 * @returns {*}
 */
export const getOrderDetail = (id) => {
  return get(`/orders/${id}`)
}

/**
 * 支付订单
 * @param id
 * @param idempotentKey
 * @returns {*}
 */
export const payOrder = (id, idempotentKey) => {
  return post(`/orders/${id}/pay`, null, { params: { idempotentKey } })
}

/**
 * 取消订单
 * @param id
 * @returns {*}
 */
export const cancelOrder = (id) => {
  return post(`/orders/${id}/cancel`)
}
