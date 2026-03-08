import request from '@/utils/request'

export function getRegions(params) {
  return request({
    url: '/regions',
    method: 'get',
    params
  })
}

export function createRegion(data) {
  return request({
    url: '/regions',
    method: 'post',
    data
  })
}

export function updateRegion(id, data) {
  return request({
    url: `/regions/${id}`,
    method: 'put',
    data
  })
}

export function deleteRegion(id) {
  return request({
    url: `/regions/${id}`,
    method: 'delete'
  })
}
