import request from '@/utils/request'

export type ApplicationPlanContext = {
  outcome: 'READY' | 'EXISTING' | 'RESUME_UNAVAILABLE'
  reportId: number
  applicationId?: number | null
  job?: { id: number; companyName: string; jobTitle: string } | null
  resume?: { id: number; resumeName: string } | null
  reason?: string | null
}

export type ApplicationFromReportInput = { reportId: number; note?: string; priority?: 'HIGH' | 'MEDIUM' | 'LOW' }
export type ApplicationFromReportResult = { outcome: 'CREATED' | 'EXISTING'; applicationId: number }

export function getApplicationPlanContextApi(reportId: number) {
  return request.get<any, ApplicationPlanContext>('/api/applications/plan-context', { params: { reportId } })
}

export function createApplicationFromReportApi(data: ApplicationFromReportInput) {
  return request.post<any, ApplicationFromReportResult>('/api/applications/from-report', data)
}

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
