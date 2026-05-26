// 分类相关接口
import request from '@/shared/api/request.js'

/**
 * 获取分类列表
 * @param params
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function getCategories(params) {
  return request.get('/categories', { params })
}

/**
 * 创建新分类
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function createCategory(data) {
  return request.post('/categories', data)
}

/**
 * 更新分类信息
 * @param id
 * @param data
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}

/**
 * 删除分类
 * @param id
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function deleteCategory(id) {
  return request.delete(`/categories/${id}`)
}
