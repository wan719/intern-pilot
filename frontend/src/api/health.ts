import request from '@/utils/request'

export function getAiProviderApi() {
  return request.get('/api/health/ai-provider')
}