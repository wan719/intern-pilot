import request from '@/utils/request'

export function getAdminUserListApi(params: any) {
  return request.get('/api/admin/users', { params })
}

export function getAdminUserDetailApi(id: number) {
  return request.get(`/api/admin/users/${id}`)
}

export function disableUserApi(id: number) {
  return request.put(`/api/admin/users/${id}/disable`)
}

export function enableUserApi(id: number) {
  return request.put(`/api/admin/users/${id}/enable`)
}

export function updateUserRolesApi(id: number, data: { roleIds: number[] }) {
  return request.put(`/api/admin/users/${id}/roles`, data)
}
