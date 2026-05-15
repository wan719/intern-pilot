import request from '@/utils/request'

export function generateInterviewQuestionsApi(data: any) {
  return request.post('/api/interview-questions/generate', data)
}

export function getInterviewQuestionReportsApi(params = {}) {
  return request.get('/api/interview-questions', { params })
}

export function getInterviewQuestionDetailApi(reportId: number) {
  return request.get(`/api/interview-questions/${reportId}`)
}

export function deleteInterviewQuestionReportApi(reportId: number) {
  return request.delete(`/api/interview-questions/${reportId}`)
}

export function regenerateInterviewQuestionsApi(reportId: number) {
  return request.post(`/api/interview-questions/${reportId}/regenerate`)
}
