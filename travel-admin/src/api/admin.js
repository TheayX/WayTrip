import request from '@/utils/request'

export function getAdminList(params) {
  return request.get('/admins', { params })
}

export function createAdmin(data) {
  return request.post('/admins', data)
}

export function updateAdmin(id, data) {
  return request.put(`/admins/${id}`, data)
}

export function resetAdminPassword(id, password) {
  return request.put(`/admins/${id}/password`, { password })
}

export function deleteAdmin(id) {
  return request.delete(`/admins/${id}`)
}
