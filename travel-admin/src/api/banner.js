import request from '@/utils/request'

/**
 * 获取轮播图列表
 */
export function getBannerList() {
  return request({
    url: '/banners',
    method: 'get'
  })
}

/**
 * 创建轮播图
 */
export function createBanner(data) {
  return request({
    url: '/banners',
    method: 'post',
    data
  })
}

/**
 * 更新轮播图
 */
export function updateBanner(id, data) {
  return request({
    url: `/banners/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除轮播图
 */
export function deleteBanner(id) {
  return request({
    url: `/banners/${id}`,
    method: 'delete'
  })
}

/**
 * 切换启用状态
 */
export function toggleBannerEnabled(id) {
  return request({
    url: `/banners/${id}/toggle`,
    method: 'post'
  })
}
