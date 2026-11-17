import request from '@/utils/request'

export function matchAnalysisApi(data: any) {
  return request.post('/api/analysis/match', data)
}

export function getAnalysisReportsApi(params = {}) {
  return request.get('/api/analysis/reports', { params })
}

export function getAnalysisReportDetailApi(id: number, config?: { silentError?: boolean }) {
  const path = `/api/analysis/reports/${id}`
  return config ? request.get(path, config) : request.get(path)
}

export function deleteAnalysisReportApi(id: number) {
  return request.delete(`/api/analysis/reports/${id}`)
}
