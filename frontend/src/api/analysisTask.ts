import request from '@/utils/request'

export function createAnalysisTaskApi(data: any) {
  return request.post('/api/analysis/tasks', data)
}

export function getAnalysisTaskDetailApi(taskNo: string) {
  return request.get(`/api/analysis/tasks/${taskNo}`)
}

export function listRunningTasksApi() {
  return request.get('/api/analysis/tasks/running')
}

export function cancelTaskApi(taskNo: string) {
  return request.post(`/api/analysis/tasks/${taskNo}/cancel`)
}

export function listRecentTasksApi(limit?: number) {
  return request.get('/api/analysis/tasks/recent', { params: { limit } })
}