// 旅游目的地相关接口
import request from '@/utils/request'

/**
 * 获取目的地列表
 * @param params
 * @returns {*}
 */
export function getRegions(params) {
  return request({
    url: '/regions',
    method: 'get',
    params
  })
}

/**
 * 创建新目的地
 * @param data
 * @returns {*}
 */
export function createRegion(data) {
  return request({
    url: '/regions',
    method: 'post',
    data
  })
}

/**
 * 更新目的地信息
 * @param id
 * @param data
 * @returns {*}
 */
export function updateRegion(id, data) {
  return request({
    url: `/regions/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除目的地
 * @param id
 * @returns {*}
 */
export function deleteRegion(id) {
  return request({
    url: `/regions/${id}`,
    method: 'delete'
  })
}
