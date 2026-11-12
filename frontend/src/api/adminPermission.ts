import request from '@/utils/request'

export function getAdminPermissionListApi(params?: { resourceType?: string }) {
  return request.get('/api/admin/permissions', { params })
}
