// 轮播图相关接口
import request from '@/utils/request'

/**
 * 获取轮播图列表
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getBannerList() {
  return request.get('/banners')
}

/**
 * 创建新轮播图
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function createBanner(data) {
  return request.post('/banners', data)
}

/**
 * 更新轮播图信息
 * @param id
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function updateBanner(id, data) {
  return request.put(`/banners/${id}`, data)
}

/**
 * 删除轮播图
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function deleteBanner(id) {
  return request.delete(`/banners/${id}`)
}

/**
 * 切换轮播图启用状态
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function toggleBannerEnabled(id) {
  return request.post(`/banners/${id}/toggle`)
}
