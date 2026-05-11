import request from '@/utils/request'

export function getAdminDashboardSummaryApi() {
  return request.get('/api/admin/dashboard/summary')
}
