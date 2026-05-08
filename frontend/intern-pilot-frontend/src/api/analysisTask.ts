import request from '@/utils/request'

export function createAnalysisTaskApi(data: any) {
  return request.post('/api/analysis/tasks', data)
}

export function getAnalysisTaskDetailApi(taskNo: string) {
  return request.get(`/api/analysis/tasks/${taskNo}`)
}