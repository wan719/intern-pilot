import request from '@/utils/request'

export function getCurrentUserApi() {
  return request.get('/api/user/me')
}
