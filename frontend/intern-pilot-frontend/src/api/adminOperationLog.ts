import request from '@/utils/request'

export function getOperationLogListApi(params: any) {
  return request.get('/api/admin/operation-logs', { params })
}

export function getOperationLogDetailApi(id: number) {
  return request.get(`/api/admin/operation-logs/${id}`)
}

export function deleteOperationLogApi(id: number) {
  return request.delete(`/api/admin/operation-logs/${id}`)
}
