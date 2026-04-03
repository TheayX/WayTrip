// 订单相关的 API 接口
import client from '@/shared/api/client.js'

/**
 * 创建订单
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const createOrder = (data) => client.post('/orders', data)

/**
 * 获取订单列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getOrderList = (params) => client.get('/orders', { params })

/**
 * 获取订单详情
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const getOrderDetail = (id) => client.get(`/orders/${id}`)

/**
 * 支付订单
 * @param id
 * @param idempotentKey
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const payOrder = (id, idempotentKey) =>
  client.post(`/orders/${id}/pay`, null, { params: { idempotentKey } })

/**
 * 取消订单
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export const cancelOrder = (id) => client.post(`/orders/${id}/cancel`)

