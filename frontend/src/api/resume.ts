import request from '@/utils/request'

export function uploadResumeApi(data: FormData) {
  return request.post('/api/resumes/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getResumeListApi(params = {}) {
  return request.get('/api/resumes', { params })
}

export function getResumeDetailApi(id: number, config?: { silentError?: boolean }) {
  const path = `/api/resumes/${id}`
  return config ? request.get(path, config) : request.get(path)
}

export function getResumeFileApi(id: number, signal: AbortSignal) {
  return request.get<Blob, Blob>(`/api/resumes/${id}/file`, { responseType: 'blob', silentError: true, signal })
}

export function deleteResumeApi(id: number) {
  return request.delete(`/api/resumes/${id}`)
}

export function setDefaultResumeApi(id: number) {
  return request.put(`/api/resumes/${id}/default`)
}
