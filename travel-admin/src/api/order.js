import request from '@/utils/request'

/**
 * 获取订单列表
 */
export function getOrderList(params) {
  return request({
    url: '/orders',
    method: 'get',
    params
  })
}

/**
 * 获取订单详情
 */
export function getOrderDetail(id) {
  return request({
    url: `/orders/${id}`,
    method: 'get'
  })
}

/**
 * 完成订单
 */
export function completeOrder(id) {
  return request({
    url: `/orders/${id}/complete`,
    method: 'post'
  })
}

/**
 * 退款订单
 */
export function refundOrder(id) {
  return request({
    url: `/orders/${id}/refund`,
    method: 'post'
  })
}

/**
 * 取消未支付订单
 */
export function cancelOrder(id) {
  return request({
    url: `/orders/${id}/cancel`,
    method: 'post'
  })
}

/**
 * 恢复订单为已支付
 */
export function reopenOrder(id) {
  return request({
    url: `/orders/${id}/reopen`,
    method: 'post'
  })
}
