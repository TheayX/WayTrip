// 分类相关接口
import request from '@/utils/request'

/**
 * 获取分类列表
 * @param params
 * @returns {*}
 */
export function getCategories(params) {
  return request({
    url: '/categories',
    method: 'get',
    params
  })
}

/**
 * 创建新分类
 * @param data
 * @returns {*}
 */
export function createCategory(data) {
  return request({
    url: '/categories',
    method: 'post',
    data
  })
}

/**
 * 更新分类信息
 * @param id
 * @param data
 * @returns {*}
 */
export function updateCategory(id, data) {
  return request({
    url: `/categories/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除分类
 * @param id
 * @returns {*}
 */
export function deleteCategory(id) {
  return request({
    url: `/categories/${id}`,
    method: 'delete'
  })
}
