import request from '@/utils/request'

export function createFeedbackApi(data: any) {
  return request.post('/api/feedback', data)
}

export function listMyFeedbackApi() {
  return request.get('/api/feedback/my')
}

export function getMyFeedbackApi(id: number) {
  return request.get(`/api/feedback/my/${id}`)
}

export function listAdminFeedbackApi(params?: { type?: string; status?: string }) {
  return request.get('/api/admin/feedback', { params })
}

export function getAdminFeedbackApi(id: number) {
  return request.get(`/api/admin/feedback/${id}`)
}

export function updateFeedbackStatusApi(id: number, data: { status: string }) {
  return request.put(`/api/admin/feedback/${id}/status`, data)
}

export function replyFeedbackApi(id: number, data: { reply: string }) {
  return request.put(`/api/admin/feedback/${id}/reply`, data)
}

export function deleteFeedbackApi(id: number) {
  return request.delete(`/api/admin/feedback/${id}`)
}
