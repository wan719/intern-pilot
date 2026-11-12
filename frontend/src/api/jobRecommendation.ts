import request from '@/utils/request'

export function generateJobRecommendationApi(data: any) {
  return request.post('/api/job-recommendations/generate', data)
}

export function getJobRecommendationListApi(params: any) {
  return request.get('/api/job-recommendations', { params })
}

export function getJobRecommendationDetailApi(batchId: number) {
  return request.get(`/api/job-recommendations/${batchId}`)
}

export function deleteJobRecommendationApi(batchId: number) {
  return request.delete(`/api/job-recommendations/${batchId}`)
}