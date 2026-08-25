import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminFeedbackList from '@/views/admin/AdminFeedbackList.vue'
import AdminRagKnowledgeList from '@/views/admin/AdminRagKnowledgeList.vue'
import AdminRoleList from '@/views/admin/AdminRoleList.vue'
import AdminUserList from '@/views/admin/AdminUserList.vue'
import { mountAdminPage } from './adminTestUtils'

const { activePermissions, hasPermission, feedbackStore } = vi.hoisted(() => {
  const activePermissions = new Set<string>()
  return {
    activePermissions,
    hasPermission: vi.fn((permission: string) => activePermissions.has(permission)),
    feedbackStore: {
      loading: false,
      feedbackList: [{
        id: 51, userId: 17, type: 'BUG', title: '页面按钮异常', content: '按钮无响应', status: 'PENDING'
      }],
      fetchFeedbackList: vi.fn().mockResolvedValue([]),
      updateStatus: vi.fn(),
      reply: vi.fn(),
      deleteFeedback: vi.fn()
    }
  }
})
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ user: { permissions: [...activePermissions] }, hasPermission, fetchCurrentUser: vi.fn(), logout: vi.fn() })
}))
vi.mock('@/stores/feedback', () => ({ useFeedbackStore: () => feedbackStore }))
vi.mock('@/api/adminUser', () => ({
  disableUserApi: vi.fn(), enableUserApi: vi.fn(), getAdminUserDetailApi: vi.fn(),
  getAdminUserListApi: vi.fn().mockResolvedValue({ records: [{
    userId: 17, nickname: '林同学', username: 'lin', email: 'lin@example.com', roles: ['USER'], enabled: 1
  }], total: 1 }), updateUserRolesApi: vi.fn()
}))
vi.mock('@/api/adminRole', () => ({
  getAdminRoleListApi: vi.fn().mockResolvedValue([{
    roleId: 1, roleCode: 'USER', roleName: '普通用户', permissions: ['user:read'], enabled: 1
  }]), updateRolePermissionsApi: vi.fn()
}))
vi.mock('@/api/adminPermission', () => ({
  getAdminPermissionListApi: vi.fn().mockResolvedValue([{
    permissionId: 11, permissionCode: 'user:read', permissionName: '读取用户', resourceType: 'ADMIN_USER', enabled: 1
  }])
}))
vi.mock('@/api/adminRagKnowledge', () => ({
  createRagKnowledgeApi: vi.fn(), deleteRagKnowledgeApi: vi.fn(), getRagKnowledgeDetailApi: vi.fn(),
  getRagKnowledgeListApi: vi.fn().mockResolvedValue({ records: [{
    documentId: 41, title: 'Java 能力模型', direction: 'Java', knowledgeType: 'SKILL_REQUIREMENT', enabled: 1
  }] }), rebuildRagKnowledgeApi: vi.fn(), searchRagKnowledgeApi: vi.fn(), updateRagKnowledgeApi: vi.fn()
}))

function setPermissions(...permissions: string[]) {
  activePermissions.clear()
  permissions.forEach((permission) => activePermissions.add(permission))
}

function actionLabels(wrapper: Awaited<ReturnType<typeof mountAdminPage>>) {
  return wrapper.findAll('button').map((item) => item.text().trim()).filter(Boolean)
}

beforeEach(() => {
  vi.clearAllMocks()
  setPermissions()
})

describe('admin page permission matrix characterization', () => {
  it('full admin sees all existing permission-sensitive actions', async () => {
    setPermissions('user:update', 'role:update', 'rag:manage', 'feedback:write', 'feedback:delete')

    expect(actionLabels(await mountAdminPage(AdminUserList, '/admin/users'))).toEqual(expect.arrayContaining(['分配角色', '禁用']))
    expect(actionLabels(await mountAdminPage(AdminRoleList, '/admin/roles'))).toContain('分配权限')
    expect(actionLabels(await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge'))).toEqual(
      expect.arrayContaining(['新增知识', '编辑', '重建', '删除'])
    )
    expect(actionLabels(await mountAdminPage(AdminFeedbackList, '/admin/feedback'))).toEqual(
      expect.arrayContaining(['状态', '回复', '删除'])
    )
  })

  it('user-only admin sees user mutations but no role, RAG or feedback mutations', async () => {
    setPermissions('user:read', 'user:update')

    expect(actionLabels(await mountAdminPage(AdminUserList, '/admin/users'))).toEqual(expect.arrayContaining(['详情', '分配角色', '禁用']))
    expect(actionLabels(await mountAdminPage(AdminRoleList, '/admin/roles'))).not.toContain('分配权限')
    expect(actionLabels(await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge'))).not.toEqual(
      expect.arrayContaining(['新增知识', '编辑', '重建', '删除'])
    )
    expect(actionLabels(await mountAdminPage(AdminFeedbackList, '/admin/feedback'))).not.toEqual(
      expect.arrayContaining(['状态', '回复', '删除'])
    )
  })

  it('restricted read-only account retains detail but no existing mutation action', async () => {
    setPermissions('user:read', 'role:read', 'rag:read', 'feedback:read')

    const userActions = actionLabels(await mountAdminPage(AdminUserList, '/admin/users'))
    expect(userActions).toContain('详情')
    expect(userActions).not.toEqual(expect.arrayContaining(['分配角色', '禁用', '启用']))
    expect(actionLabels(await mountAdminPage(AdminRoleList, '/admin/roles'))).not.toContain('分配权限')
    expect(actionLabels(await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge'))).not.toEqual(
      expect.arrayContaining(['新增知识', '编辑', '重建', '删除'])
    )
    expect(actionLabels(await mountAdminPage(AdminFeedbackList, '/admin/feedback'))).not.toEqual(
      expect.arrayContaining(['状态', '回复', '删除'])
    )
  })
})
