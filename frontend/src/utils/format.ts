import dayjs from 'dayjs'

export function formatDateTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

export function percent(value?: number) {
  return `${value ?? 0}%`
}

export const statusLabels: Record<string, string> = {
  TO_APPLY: '待投递',
  APPLIED: '已投递',
  WRITTEN_TEST: '笔试中',
  FIRST_INTERVIEW: '一面',
  SECOND_INTERVIEW: '二面',
  HR_INTERVIEW: 'HR 面',
  OFFER: 'Offer',
  REJECTED: '被拒',
  GIVEN_UP: '放弃'
}

export const statusTypes: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
  TO_APPLY: 'info',
  APPLIED: 'primary',
  WRITTEN_TEST: 'warning',
  FIRST_INTERVIEW: 'warning',
  SECOND_INTERVIEW: 'warning',
  HR_INTERVIEW: 'warning',
  OFFER: 'success',
  REJECTED: 'danger',
  GIVEN_UP: 'info'
}

export const statusOptions = Object.entries(statusLabels).map(([value, label]) => ({ value, label }))
