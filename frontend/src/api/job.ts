import request from '@/utils/request'

export function createJobApi(data: any) {
  return request.post('/api/jobs', data)
}

export function getJobListApi(params = {}) {
  return request.get('/api/jobs', { params })
}

export function getJobDetailApi(id: number, config?: { silentError?: boolean }) {
  const path = `/api/jobs/${id}`
  return config ? request.get(path, config) : request.get(path)
}

export function updateJobApi(id: number, data: any) {
  return request.put(`/api/jobs/${id}`, data)
}

export function deleteJobApi(id: number) {
  return request.delete(`/api/jobs/${id}`)
}
