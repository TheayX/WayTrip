import { get, post } from '@/utils/request'

/**
 * 创建订单
 */
export const createOrder = (data) => {
  return post('/orders', data)
}

/**
 * 获取订单列表
 */
export const getOrderList = (params) => {
  return get('/orders', params)
}

/**
 * 获取订单详情
 */
export const getOrderDetail = (id) => {
  return get(`/orders/${id}`)
}

/**
 * 支付订单
 */
export const payOrder = (id, idempotentKey) => {
  return post(`/orders/${id}/pay`, null, { params: { idempotentKey } })
}

/**
 * 取消订单
 */
export const cancelOrder = (id) => {
  return post(`/orders/${id}/cancel`)
}
