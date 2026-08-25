import { flushPromises } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminDashboard from '@/views/admin/AdminDashboard.vue'
import AdminFeedbackList from '@/views/admin/AdminFeedbackList.vue'
import OperationLogList from '@/views/admin/OperationLogList.vue'
import { getAdminDashboardSummaryApi } from '@/api/adminDashboard'
import { deleteOperationLogApi, getOperationLogDetailApi, getOperationLogListApi } from '@/api/adminOperationLog'
import { buttonByText, mountAdminPage } from './adminTestUtils'

const { hasPermission, feedbackStore } = vi.hoisted(() => ({
  hasPermission: vi.fn(),
  feedbackStore: {
    loading: false,
    feedbackList: [] as any[],
    fetchFeedbackList: vi.fn(),
    updateStatus: vi.fn(),
    reply: vi.fn(),
    deleteFeedback: vi.fn()
  }
}))

vi.mock('@/stores/auth', () => ({ useAuthStore: () => ({ hasPermission }) }))
vi.mock('@/stores/feedback', () => ({ useFeedbackStore: () => feedbackStore }))
vi.mock('@/api/adminDashboard', () => ({ getAdminDashboardSummaryApi: vi.fn() }))
vi.mock('@/api/adminOperationLog', () => ({
  deleteOperationLogApi: vi.fn(),
  getOperationLogDetailApi: vi.fn(),
  getOperationLogListApi: vi.fn()
}))

const summary = {
  userCount: 10,
  resumeCount: 9,
  jobCount: 8,
  analysisReportCount: 7,
  interviewQuestionReportCount: 6,
  applicationCount: 5,
  todayNewUserCount: 2,
  failedOperationCount: 1
}
const log = {
  logId: 31,
  operatorUsername: 'admin',
  module: '用户管理',
  operation: '禁用用户',
  operationType: 'UPDATE',
  requestMethod: 'PUT',
  requestUri: '/api/admin/users/17/disable',
  success: 1,
  costTime: 26,
  createdAt: '2026-08-25T08:00:00'
}
const feedback = {
  id: 51,
  userId: 17,
  userName: '林同学',
  userEmail: 'lin@example.com',
  type: 'BUG',
  title: '页面按钮异常',
  content: '保存按钮没有响应',
  status: 'PENDING',
  pageUrl: '/jobs',
  adminReply: '',
  createdAt: '2026-08-25T08:00:00'
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

beforeEach(() => {
  vi.clearAllMocks()
  hasPermission.mockReturnValue(true)
  feedbackStore.loading = false
  feedbackStore.feedbackList = [feedback]
  feedbackStore.fetchFeedbackList.mockResolvedValue([feedback])
  feedbackStore.updateStatus.mockResolvedValue(undefined)
  feedbackStore.reply.mockResolvedValue(undefined)
  feedbackStore.deleteFeedback.mockResolvedValue(undefined)
  vi.mocked(getAdminDashboardSummaryApi).mockResolvedValue(summary as any)
  vi.mocked(getOperationLogListApi).mockResolvedValue({ records: [log], total: 1 } as any)
  vi.mocked(getOperationLogDetailApi).mockResolvedValue({ ...log, requestParams: '{"token":"secret"}' } as any)
  vi.mocked(deleteOperationLogApi).mockResolvedValue(undefined as any)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as any)
})

describe('admin dashboard behavior characterization', () => {
  it('keeps the sole summary API call argument-free and renders every existing metric', async () => {
    const wrapper = await mountAdminPage(AdminDashboard, '/admin/dashboard')

    expect(getAdminDashboardSummaryApi).toHaveBeenCalledTimes(1)
    expect(getAdminDashboardSummaryApi).toHaveBeenCalledWith()
    expect(wrapper.findAll('.stat-card strong').map((item) => item.text())).toEqual(['10', '9', '8', '7', '6', '5', '2', '1'])
  })

  it('keeps health and activity conclusions unknown until the summary succeeds', async () => {
    const pending = deferred<any>()
    vi.mocked(getAdminDashboardSummaryApi).mockReturnValueOnce(pending.promise)

    const wrapper = await mountAdminPage(AdminDashboard, '/admin/dashboard')

    expect((wrapper.vm as any).loadState).toBe('loading')
    expect(wrapper.find('[data-admin-dashboard-loading]').exists()).toBe(true)
    expect(wrapper.find('[data-admin-health-panel]').exists()).toBe(false)
    expect(wrapper.find('[data-admin-activity-panel]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('运行正常')

    pending.resolve(summary)
    await flushPromises()
    expect((wrapper.vm as any).loadState).toBe('success')
    expect(wrapper.find('[data-admin-dashboard-loading]').exists()).toBe(false)
    expect(wrapper.find('[data-admin-health-panel]').exists()).toBe(true)
    expect(wrapper.find('[data-admin-activity-panel]').exists()).toBe(true)
  })

  it('shows a persistent unavailable state and retries without claiming healthy operation', async () => {
    vi.mocked(getAdminDashboardSummaryApi)
      .mockRejectedValueOnce(new Error('summary unavailable'))
      .mockResolvedValueOnce(summary as any)

    const wrapper = await mountAdminPage(AdminDashboard, '/admin/dashboard')

    expect((wrapper.vm as any).loadState).toBe('error')
    expect(wrapper.get('[data-admin-dashboard-error]').text()).toContain('数据不可用')
    expect(wrapper.text()).not.toContain('运行正常')
    expect(wrapper.find('[data-admin-health-panel]').exists()).toBe(false)
    expect(wrapper.find('[data-admin-activity-panel]').exists()).toBe(false)

    await buttonByText(wrapper, '重试').trigger('click')
    await flushPromises()
    expect(getAdminDashboardSummaryApi).toHaveBeenCalledTimes(2)
    expect((wrapper.vm as any).loadState).toBe('success')
    expect(wrapper.find('[data-admin-dashboard-error]').exists()).toBe(false)
    expect(wrapper.find('[data-admin-health-panel]').exists()).toBe(true)
  })
})

describe('operation log behavior characterization', () => {
  it('keeps list and detail reads exact', async () => {
    const wrapper = await mountAdminPage(OperationLogList, '/admin/operation-logs')

    expect(getOperationLogListApi).toHaveBeenCalledWith({
      module: '', operationType: '', username: '', success: undefined, pageNum: 1, pageSize: 10
    })
    await buttonByText(wrapper, '详情').trigger('click')
    await flushPromises()
    expect(getOperationLogDetailApi).toHaveBeenCalledWith(31)
    expect((wrapper.vm as any).maskSensitive('{"token":"secret"}')).toBe('{"token":"******"}')
  })

  it('keeps deletion behind the exact destructive confirmation and selected id', async () => {
    const wrapper = await mountAdminPage(OperationLogList, '/admin/operation-logs')

    await buttonByText(wrapper, '删除').trigger('click')
    await flushPromises()
    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      '确认删除日志 #31？删除后无法在后台页面恢复。', '删除确认', { type: 'warning' }
    )
    expect(deleteOperationLogApi).toHaveBeenCalledWith(31)
  })

  it('treats a cancelled log deletion as a no-op', async () => {
    const wrapper = await mountAdminPage(OperationLogList, '/admin/operation-logs')
    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel')

    await expect((wrapper.vm as any).removeLog(log)).resolves.toBeUndefined()
    expect(deleteOperationLogApi).not.toHaveBeenCalled()
  })
})

describe('admin feedback behavior characterization', () => {
  it('keeps filtered reads and local detail behavior exact', async () => {
    const wrapper = await mountAdminPage(AdminFeedbackList, '/admin/feedback')
    expect(feedbackStore.fetchFeedbackList).toHaveBeenCalledWith({ type: undefined, status: undefined })

    Object.assign((wrapper.vm as any).query, { type: 'BUG', status: 'PENDING' })
    await (wrapper.vm as any).loadData()
    expect(feedbackStore.fetchFeedbackList).toHaveBeenLastCalledWith({ type: 'BUG', status: 'PENDING' })

    await buttonByText(wrapper, '详情').trigger('click')
    expect((wrapper.vm as any).selected.id).toBe(51)
  })

  it('keeps status and trimmed reply payloads exact', async () => {
    const wrapper = await mountAdminPage(AdminFeedbackList, '/admin/feedback')

    ;(wrapper.vm as any).openStatus(feedback)
    ;(wrapper.vm as any).statusForm.status = 'RESOLVED'
    await (wrapper.vm as any).submitStatus()
    expect(feedbackStore.updateStatus).toHaveBeenCalledWith(51, 'RESOLVED')

    ;(wrapper.vm as any).openReply(feedback)
    ;(wrapper.vm as any).replyForm.reply = '  已修复，请刷新页面  '
    await (wrapper.vm as any).submitReply()
    expect(feedbackStore.reply).toHaveBeenCalledWith(51, '已修复，请刷新页面')
  })

  it('keeps feedback deletion behind the exact confirmation', async () => {
    const wrapper = await mountAdminPage(AdminFeedbackList, '/admin/feedback')

    await buttonByText(wrapper, '删除').trigger('click')
    await flushPromises()
    expect(ElMessageBox.confirm).toHaveBeenCalledWith('确认删除反馈「页面按钮异常」？', '删除确认', { type: 'warning' })
    expect(feedbackStore.deleteFeedback).toHaveBeenCalledWith(51)
  })

  it('treats a cancelled feedback deletion as a no-op', async () => {
    const wrapper = await mountAdminPage(AdminFeedbackList, '/admin/feedback')
    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel')

    await expect((wrapper.vm as any).remove(feedback)).resolves.toBeUndefined()
    expect(feedbackStore.deleteFeedback).not.toHaveBeenCalled()
  })
})
