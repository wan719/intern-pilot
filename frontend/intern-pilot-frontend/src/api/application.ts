import request from '@/utils/request'

export function createApplicationApi(data: any) {
  return request.post('/api/applications', data)
}

export function getApplicationListApi(params = {}) {
  return request.get('/api/applications', { params })
}

export function getApplicationDetailApi(id: number) {
  return request.get(`/api/applications/${id}`)
}

export function updateApplicationStatusApi(id: number, data: any) {
  return request.put(`/api/applications/${id}/status`, data)
}

export function updateApplicationNoteApi(id: number, data: any) {
  return request.put(`/api/applications/${id}/note`, data)
}

export function deleteApplicationApi(id: number) {
  return request.delete(`/api/applications/${id}`)
}
