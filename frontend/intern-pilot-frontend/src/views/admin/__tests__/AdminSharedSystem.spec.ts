import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminDashboard from '@/views/admin/AdminDashboard.vue'
import AdminFeedbackList from '@/views/admin/AdminFeedbackList.vue'
import AdminPermissionList from '@/views/admin/AdminPermissionList.vue'
import AdminRagKnowledgeList from '@/views/admin/AdminRagKnowledgeList.vue'
import AdminRoleList from '@/views/admin/AdminRoleList.vue'
import AdminUserList from '@/views/admin/AdminUserList.vue'
import OperationLogList from '@/views/admin/OperationLogList.vue'
import { getAdminUserListApi } from '@/api/adminUser'
import { createRagKnowledgeApi, getRagKnowledgeListApi } from '@/api/adminRagKnowledge'
import { getOperationLogDetailApi } from '@/api/adminOperationLog'
import { mountAdminPage } from './adminTestUtils'

const { activePermissions, hasPermission, feedbackStore } = vi.hoisted(() => {
  const activePermissions = new Set<string>()
  return {
    activePermissions,
    hasPermission: vi.fn((permission: string) => activePermissions.has(permission)),
    feedbackStore: {
      loading: false,
      feedbackList: [] as any[],
      fetchFeedbackList: vi.fn(),
      updateStatus: vi.fn(),
      reply: vi.fn(),
      deleteFeedback: vi.fn()
    }
  }
})

vi.mock('@/stores/auth', () => ({ useAuthStore: () => ({ hasPermission }) }))
vi.mock('@/stores/feedback', () => ({ useFeedbackStore: () => feedbackStore }))
vi.mock('@/api/adminDashboard', () => ({
  getAdminDashboardSummaryApi: vi.fn().mockResolvedValue({
    userCount: 10, resumeCount: 9, jobCount: 8, analysisReportCount: 7,
    interviewQuestionReportCount: 6, applicationCount: 5, todayNewUserCount: 2, failedOperationCount: 1
  })
}))
vi.mock('@/api/adminUser', () => ({
  disableUserApi: vi.fn(), enableUserApi: vi.fn(), getAdminUserDetailApi: vi.fn(),
  getAdminUserListApi: vi.fn(), updateUserRolesApi: vi.fn()
}))
vi.mock('@/api/adminRole', () => ({
  getAdminRoleListApi: vi.fn().mockResolvedValue([{
    roleId: 1, roleCode: 'USER', roleName: '普通用户', description: '用户', permissions: ['user:read'], enabled: 1
  }]),
  updateRolePermissionsApi: vi.fn()
}))
vi.mock('@/api/adminPermission', () => ({
  getAdminPermissionListApi: vi.fn().mockResolvedValue([{
    permissionId: 11, permissionCode: 'user:read', permissionName: '读取用户',
    resourceType: 'ADMIN_USER', description: '读取用户', enabled: 1
  }])
}))
vi.mock('@/api/adminRagKnowledge', () => ({
  createRagKnowledgeApi: vi.fn(), deleteRagKnowledgeApi: vi.fn(), getRagKnowledgeDetailApi: vi.fn(),
  getRagKnowledgeListApi: vi.fn(), rebuildRagKnowledgeApi: vi.fn(), searchRagKnowledgeApi: vi.fn(), updateRagKnowledgeApi: vi.fn()
}))
vi.mock('@/api/adminOperationLog', () => ({
  deleteOperationLogApi: vi.fn(), getOperationLogDetailApi: vi.fn(),
  getOperationLogListApi: vi.fn().mockResolvedValue({ records: [{
    logId: 31, operatorUsername: 'admin', module: '用户管理', operation: '禁用用户', operationType: 'UPDATE',
    requestMethod: 'PUT', requestUri: '/api/admin/users/17/disable', success: 1, costTime: 26,
    createdAt: '2026-08-25T08:00:00'
  }], total: 1 })
}))

const user = {
  userId: 17, nickname: '林同学', username: 'lin', email: 'lin@example.com', school: '示例大学',
  major: '计算机', grade: '2027', roles: ['USER'], enabled: 1, createdAt: '2026-08-25T08:00:00'
}
const ragDocument = {
  documentId: 41, title: 'Java 实习能力模型', direction: 'Java 后端', knowledgeType: 'SKILL_REQUIREMENT',
  summary: 'Spring 与数据库', content: '掌握 Spring Boot 与 MySQL', chunkCount: 2, enabled: 1,
  updatedAt: '2026-08-25T08:00:00'
}
const feedback = {
  id: 51, userId: 17, userName: '林同学', userEmail: 'lin@example.com', type: 'BUG',
  title: '页面按钮异常', content: '保存按钮没有响应', status: 'PENDING', createdAt: '2026-08-25T08:00:00'
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((resolvePromise) => { resolve = resolvePromise })
  return { promise, resolve }
}

function setPermissions(...permissions: string[]) {
  activePermissions.clear()
  permissions.forEach((permission) => activePermissions.add(permission))
}

beforeEach(() => {
  vi.clearAllMocks()
  setPermissions('user:update', 'role:update', 'rag:manage', 'operation-log:delete', 'feedback:write', 'feedback:delete')
  vi.mocked(getAdminUserListApi).mockResolvedValue({ records: [user], total: 1 } as any)
  vi.mocked(getRagKnowledgeListApi).mockResolvedValue({ records: [ragDocument], total: 1 } as any)
  vi.mocked(createRagKnowledgeApi).mockResolvedValue({ documentId: 42 } as any)
  vi.mocked(getOperationLogDetailApi).mockResolvedValue({ logId: 31 } as any)
  feedbackStore.loading = false
  feedbackStore.feedbackList = [feedback]
  feedbackStore.fetchFeedbackList.mockResolvedValue([feedback])
  feedbackStore.updateStatus.mockResolvedValue(undefined)
  feedbackStore.reply.mockResolvedValue(undefined)
  feedbackStore.deleteFeedback.mockResolvedValue(undefined)
})

describe('shared admin page structure', () => {
  it.each([
    [AdminUserList, '/admin/users', true, ['ID', '用户', '学校', '专业', '年级', '角色', '状态', '注册时间', '操作']],
    [AdminRoleList, '/admin/roles', false, ['ID', '角色', '描述', '权限数', '状态', '操作']],
    [AdminPermissionList, '/admin/permissions', true, ['ID', '权限', '资源类型', '描述', '状态']],
    [AdminRagKnowledgeList, '/admin/rag-knowledge', true, ['知识文档', '方向', '类型', '切片数', '状态', '更新时间', '操作']],
    [OperationLogList, '/admin/operation-logs', true, ['ID', '操作人', '模块', '操作', '类型', '请求', '结果', '耗时', '时间']],
    [AdminFeedbackList, '/admin/feedback', true, ['ID', '反馈', '类型', '用户', '状态', '页面路径', '提交时间', '操作']]
  ])('migrates %s to one dense shared table that retains every critical column', async (component, path, hasFilters, criticalColumns) => {
    const wrapper = await mountAdminPage(component, path)

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.findComponent({ name: 'TableShell' }).exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'FilterBar' }).exists()).toBe(hasFilters)
    expect(wrapper.findComponent({ name: 'StatusTag' }).exists()).toBe(true)
    expect(wrapper.findAll('.admin-data-table')).toHaveLength(1)
    expect(wrapper.get('.admin-data-table').attributes('style')).toContain('min-width')
    expect(wrapper.find('.mobile-card-list').exists()).toBe(false)
    expect(wrapper.find('.feedback-mobile-list').exists()).toBe(false)
    const headerText = wrapper.get('.el-table__header-wrapper').text()
    for (const label of criticalColumns) expect(headerText).toContain(label)
  })

  it('uses the existing summary response for shared metrics and semantic panels without creating a chart canvas', async () => {
    const wrapper = await mountAdminPage(AdminDashboard, '/admin/dashboard')

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.findAllComponents({ name: 'StatCard' })).toHaveLength(8)
    expect(wrapper.findComponent({ name: 'StatusTag' }).exists()).toBe(true)
    expect(wrapper.get('[data-admin-health-panel]').text()).toContain('运营健康度')
    expect(wrapper.get('[data-admin-activity-panel]').text()).toContain('业务活动结构')
    expect(wrapper.find('canvas').exists()).toBe(false)
  })

  it('gates operation-log deletion with the existing exact permission string', async () => {
    setPermissions()
    const restricted = await mountAdminPage(OperationLogList, '/admin/operation-logs')
    expect(restricted.findAll('button').map((item) => item.text().trim())).not.toContain('删除')

    setPermissions('operation-log:delete')
    const full = await mountAdminPage(OperationLogList, '/admin/operation-logs')
    expect(full.findAll('button').map((item) => item.text().trim())).toContain('删除')
    expect(hasPermission).toHaveBeenCalledWith('operation-log:delete')
  })
})

describe('admin form and async safety', () => {
  it('shows inline validation and does not submit empty RAG or feedback forms', async () => {
    const rag = await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge')
    ;(rag.vm as any).openCreate()
    await (rag.vm as any).save()
    await flushPromises()
    expect(createRagKnowledgeApi).not.toHaveBeenCalled()
    expect(Object.values((rag.vm as any).formErrors).filter(Boolean).length).toBeGreaterThan(0)

    const feedbackPage = await mountAdminPage(AdminFeedbackList, '/admin/feedback')
    ;(feedbackPage.vm as any).openReply(feedback)
    ;(feedbackPage.vm as any).replyForm.reply = '   '
    await (feedbackPage.vm as any).submitReply()
    await flushPromises()
    expect(feedbackStore.reply).not.toHaveBeenCalled()
    expect((feedbackPage.vm as any).replyError).toBe('请输入回复内容')
  })

  it('keeps the newest user filter result and invalidates pending state after unmount', async () => {
    const wrapper = await mountAdminPage(AdminUserList, '/admin/users')
    const older = deferred<any>()
    const newer = deferred<any>()
    vi.mocked(getAdminUserListApi).mockReturnValueOnce(older.promise).mockReturnValueOnce(newer.promise)

    ;(wrapper.vm as any).query.keyword = '旧筛选'
    const olderLoad = (wrapper.vm as any).loadList()
    ;(wrapper.vm as any).query.keyword = '新筛选'
    const newerLoad = (wrapper.vm as any).loadList()
    newer.resolve({ records: [{ ...user, userId: 19, nickname: '新筛选结果' }], total: 1 })
    await newerLoad
    older.resolve({ records: [{ ...user, userId: 18, nickname: '旧筛选结果' }], total: 1 })
    await olderLoad
    expect((wrapper.vm as any).users[0].nickname).toBe('新筛选结果')

    const pending = deferred<any>()
    vi.mocked(getAdminUserListApi).mockReturnValueOnce(pending.promise)
    const pendingLoad = (wrapper.vm as any).loadList()
    wrapper.unmount()
    pending.resolve({ records: [{ ...user, userId: 20, nickname: '卸载后结果' }], total: 1 })
    await pendingLoad
    expect((wrapper.vm as any).users[0].nickname).toBe('新筛选结果')
  })

  it('keeps the newest operation-log detail when an older drawer request resolves last', async () => {
    const wrapper = await mountAdminPage(OperationLogList, '/admin/operation-logs')
    const older = deferred<any>()
    const newer = deferred<any>()
    vi.mocked(getOperationLogDetailApi).mockReturnValueOnce(older.promise).mockReturnValueOnce(newer.promise)

    const olderDetail = (wrapper.vm as any).openDetail(31)
    const newerDetail = (wrapper.vm as any).openDetail(32)
    newer.resolve({ logId: 32, operation: '最新详情' })
    await newerDetail
    older.resolve({ logId: 31, operation: '过期详情' })
    await olderDetail
    expect((wrapper.vm as any).detail.logId).toBe(32)
  })

  it('keeps RAG saves single-flight and feedback filters sequential with latest parameters', async () => {
    const rag = await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge')
    ;(rag.vm as any).openCreate()
    Object.assign((rag.vm as any).form, {
      title: '前端能力模型', direction: '前端开发', knowledgeType: 'SKILL_REQUIREMENT',
      summary: '', content: '掌握 Vue 3', enabled: 1
    })
    const pendingSave = deferred<any>()
    vi.mocked(createRagKnowledgeApi).mockReturnValueOnce(pendingSave.promise)
    const firstSave = (rag.vm as any).save()
    const secondSave = (rag.vm as any).save()
    await flushPromises()
    expect(createRagKnowledgeApi).toHaveBeenCalledTimes(1)
    pendingSave.resolve({ documentId: 42 })
    await Promise.all([firstSave, secondSave])

    const feedbackPage = await mountAdminPage(AdminFeedbackList, '/admin/feedback')
    const firstFilter = deferred<any>()
    feedbackStore.fetchFeedbackList
      .mockReturnValueOnce(firstFilter.promise)
      .mockResolvedValueOnce([feedback])
    Object.assign((feedbackPage.vm as any).query, { type: 'BUG', status: '' })
    const firstLoad = (feedbackPage.vm as any).loadData()
    Object.assign((feedbackPage.vm as any).query, { type: 'UI_UX', status: 'PENDING' })
    const latestLoad = (feedbackPage.vm as any).loadData()
    expect(feedbackStore.fetchFeedbackList).toHaveBeenCalledTimes(2)
    firstFilter.resolve([feedback])
    await Promise.all([firstLoad, latestLoad])
    expect(feedbackStore.fetchFeedbackList).toHaveBeenLastCalledWith({ type: 'UI_UX', status: 'PENDING' })
  })
})
