import request from '@/utils/request'

export function getAdminRoleListApi() {
  return request.get('/api/admin/roles')
}

export function updateRolePermissionsApi(id: number, data: { permissionIds: number[] }) {
  return request.put(`/api/admin/roles/${id}/permissions`, data)
}
