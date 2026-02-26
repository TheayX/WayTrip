import request from '@/utils/request'

// 创建订单
export const createOrder = (data) => request.post('/orders', data)

// 获取订单列表
export const getOrderList = (params) => request.get('/orders', { params })

// 获取订单详情
export const getOrderDetail = (id) => request.get(`/orders/${id}`)

// 支付订单
export const payOrder = (id, idempotentKey) =>
  request.post(`/orders/${id}/pay`, null, { params: { idempotentKey } })

// 取消订单
export const cancelOrder = (id) => request.post(`/orders/${id}/cancel`)

