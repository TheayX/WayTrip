import request from '@/utils/request'

// 获取订单列表
export function getOrderList(params) {
  return request.get('/orders', { params })
}

// 获取订单详情
export function getOrderDetail(id) {
  return request.get(`/orders/${id}`)
}

// 完成订单
export function completeOrder(id) {
  return request.post(`/orders/${id}/complete`)
}

// 退款订单
export function refundOrder(id) {
  return request.post(`/orders/${id}/refund`)
}

// 取消未支付订单
export function cancelOrder(id) {
  return request.post(`/orders/${id}/cancel`)
}

// 恢复订单为已支付
export function reopenOrder(id) {
  return request.post(`/orders/${id}/reopen`)
}
