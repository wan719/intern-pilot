import { flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminFeedbackList from '@/views/admin/AdminFeedbackList.vue'
import AdminRagKnowledgeList from '@/views/admin/AdminRagKnowledgeList.vue'
import AdminRoleList from '@/views/admin/AdminRoleList.vue'
import AdminUserList from '@/views/admin/AdminUserList.vue'
import { getAdminRoleListApi, updateRolePermissionsApi } from '@/api/adminRole'
import { getAdminPermissionListApi } from '@/api/adminPermission'
import { updateUserRolesApi } from '@/api/adminUser'
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
    setPermissions('user:update', 'role:read', 'role:update', 'permission:read', 'rag:manage', 'feedback:write', 'feedback:delete')

    expect(actionLabels(await mountAdminPage(AdminUserList, '/admin/users'))).toEqual(expect.arrayContaining(['分配角色', '禁用']))
    expect(actionLabels(await mountAdminPage(AdminRoleList, '/admin/roles'))).toContain('分配权限')
    expect(actionLabels(await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge'))).toEqual(
      expect.arrayContaining(['新增知识', '编辑', '重建', '删除'])
    )
    expect(actionLabels(await mountAdminPage(AdminFeedbackList, '/admin/feedback'))).toEqual(
      expect.arrayContaining(['状态', '回复', '删除'])
    )
  })

  it('user-only admin skips role options and cannot enter role assignment without role:read', async () => {
    setPermissions('user:read', 'user:update')

    const wrapper = await mountAdminPage(AdminUserList, '/admin/users')
    expect(getAdminRoleListApi).not.toHaveBeenCalled()
    expect(actionLabels(wrapper)).toContain('详情')
    expect(actionLabels(wrapper)).toContain('禁用')
    expect(actionLabels(wrapper)).not.toContain('分配角色')
    expect(actionLabels(await mountAdminPage(AdminRoleList, '/admin/roles'))).not.toContain('分配权限')
    const ragActionsWithoutManage = actionLabels(await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge'))
    expect(ragActionsWithoutManage, 'rag:manage forbids 新增知识').not.toContain('新增知识')
    expect(ragActionsWithoutManage, 'rag:manage forbids 编辑').not.toContain('编辑')
    expect(ragActionsWithoutManage, 'rag:manage forbids 重建').not.toContain('重建')
    expect(ragActionsWithoutManage, 'rag:manage forbids 删除').not.toContain('删除')
    const feedbackActionsWithoutWriteOrDelete = actionLabels(await mountAdminPage(AdminFeedbackList, '/admin/feedback'))
    expect(feedbackActionsWithoutWriteOrDelete, 'feedback:write forbids 状态').not.toContain('状态')
    expect(feedbackActionsWithoutWriteOrDelete, 'feedback:write forbids 回复').not.toContain('回复')
    expect(feedbackActionsWithoutWriteOrDelete, 'feedback:delete forbids 删除').not.toContain('删除')
  })

  it('enables user role assignment only after role:read makes role options available', async () => {
    setPermissions('user:read', 'user:update', 'role:read')

    const wrapper = await mountAdminPage(AdminUserList, '/admin/users')

    expect(getAdminRoleListApi).toHaveBeenCalledTimes(1)
    expect(actionLabels(wrapper)).toContain('分配角色')
  })

  it('role-only admin skips permission options until permission:read is present', async () => {
    setPermissions('role:read', 'role:update')
    const roleOnly = await mountAdminPage(AdminRoleList, '/admin/roles')
    expect(getAdminPermissionListApi).not.toHaveBeenCalled()
    expect(actionLabels(roleOnly)).not.toContain('分配权限')

    vi.clearAllMocks()
    setPermissions('role:read', 'role:update', 'permission:read')
    const withPermissionRead = await mountAdminPage(AdminRoleList, '/admin/roles')
    expect(getAdminPermissionListApi).toHaveBeenCalledTimes(1)
    expect(actionLabels(withPermissionRead)).toContain('分配权限')
  })

  it('keeps failed auxiliary role options unavailable until an explicit retry succeeds', async () => {
    setPermissions('user:read', 'user:update', 'role:read')
    vi.mocked(getAdminRoleListApi)
      .mockRejectedValueOnce(Object.assign(new Error('Forbidden'), { response: { status: 403 } }))
      .mockResolvedValueOnce([{
        roleId: 1, roleCode: 'USER', roleName: '普通用户', permissions: ['user:read'], enabled: 1
      }] as any)

    const wrapper = await mountAdminPage(AdminUserList, '/admin/users')
    expect(wrapper.text()).toContain('角色选项加载失败')
    expect(actionLabels(wrapper)).not.toContain('分配角色')
    ;(wrapper.vm as any).currentUser = { userId: 17, roles: ['USER'] }
    ;(wrapper.vm as any).selectedRoleIds = []
    ;(wrapper.vm as any).roleDialogVisible = true
    await (wrapper.vm as any).submitRoles()
    expect((wrapper.vm as any).roleDialogVisible).toBe(false)
    expect(updateUserRolesApi).not.toHaveBeenCalled()

    await (wrapper.vm as any).loadRoles()
    await flushPromises()
    expect(getAdminRoleListApi).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).not.toContain('角色选项加载失败')
    expect(actionLabels(wrapper)).toContain('分配角色')
  })

  it('keeps failed permission options unavailable until an explicit retry succeeds', async () => {
    setPermissions('role:read', 'role:update', 'permission:read')
    vi.mocked(getAdminPermissionListApi)
      .mockRejectedValueOnce(Object.assign(new Error('Forbidden'), { response: { status: 403 } }))
      .mockResolvedValueOnce([{
        permissionId: 11, permissionCode: 'user:read', permissionName: '读取用户', resourceType: 'ADMIN_USER', enabled: 1
      }] as any)

    const wrapper = await mountAdminPage(AdminRoleList, '/admin/roles')
    expect(getAdminRoleListApi).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('权限选项加载失败')
    expect(actionLabels(wrapper)).not.toContain('分配权限')
    ;(wrapper.vm as any).currentRole = { roleId: 1, permissions: ['user:read'] }
    ;(wrapper.vm as any).selectedPermissionIds = []
    ;(wrapper.vm as any).visible = true
    await (wrapper.vm as any).submit()
    expect((wrapper.vm as any).visible).toBe(false)
    expect(updateRolePermissionsApi).not.toHaveBeenCalled()

    await (wrapper.vm as any).loadPermissions()
    await flushPromises()
    expect(getAdminPermissionListApi).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).not.toContain('权限选项加载失败')
    expect(actionLabels(wrapper)).toContain('分配权限')
  })

  it('restricted read-only account retains detail but no existing mutation action', async () => {
    setPermissions('user:read', 'role:read', 'rag:read', 'feedback:read')

    const userActions = actionLabels(await mountAdminPage(AdminUserList, '/admin/users'))
    expect(userActions).toContain('详情')
    expect(userActions, 'user:update + role:read forbids 分配角色').not.toContain('分配角色')
    expect(userActions, 'user:update forbids 禁用').not.toContain('禁用')
    expect(userActions, 'user:update forbids 启用').not.toContain('启用')
    expect(actionLabels(await mountAdminPage(AdminRoleList, '/admin/roles'))).not.toContain('分配权限')
    const ragActions = actionLabels(await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge'))
    expect(ragActions, 'rag:manage forbids 新增知识').not.toContain('新增知识')
    expect(ragActions, 'rag:manage forbids 编辑').not.toContain('编辑')
    expect(ragActions, 'rag:manage forbids 重建').not.toContain('重建')
    expect(ragActions, 'rag:manage forbids 删除').not.toContain('删除')
    const feedbackActions = actionLabels(await mountAdminPage(AdminFeedbackList, '/admin/feedback'))
    expect(feedbackActions, 'feedback:write forbids 状态').not.toContain('状态')
    expect(feedbackActions, 'feedback:write forbids 回复').not.toContain('回复')
    expect(feedbackActions, 'feedback:delete forbids 删除').not.toContain('删除')
  })
})
