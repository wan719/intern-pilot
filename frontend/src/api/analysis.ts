import request from '@/utils/request'

export function matchAnalysisApi(data: any) {
  return request.post('/api/analysis/match', data)
}

export function getAnalysisReportsApi(params = {}) {
  return request.get('/api/analysis/reports', { params })
}

export function getAnalysisReportDetailApi(id: number) {
  return request.get(`/api/analysis/reports/${id}`)
}

export function deleteAnalysisReportApi(id: number) {
  return request.delete(`/api/analysis/reports/${id}`)
}
