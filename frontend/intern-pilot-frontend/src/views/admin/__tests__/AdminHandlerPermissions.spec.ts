import { ElMessageBox } from 'element-plus'
import { nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminFeedbackList from '@/views/admin/AdminFeedbackList.vue'
import AdminRagKnowledgeList from '@/views/admin/AdminRagKnowledgeList.vue'
import AdminRoleList from '@/views/admin/AdminRoleList.vue'
import AdminUserList from '@/views/admin/AdminUserList.vue'
import OperationLogList from '@/views/admin/OperationLogList.vue'
import {
  disableUserApi, enableUserApi, getAdminUserDetailApi, getAdminUserListApi, updateUserRolesApi
} from '@/api/adminUser'
import { getAdminRoleListApi, updateRolePermissionsApi } from '@/api/adminRole'
import { getAdminPermissionListApi } from '@/api/adminPermission'
import {
  createRagKnowledgeApi, deleteRagKnowledgeApi, getRagKnowledgeDetailApi, getRagKnowledgeListApi,
  rebuildRagKnowledgeApi, updateRagKnowledgeApi
} from '@/api/adminRagKnowledge'
import { deleteOperationLogApi, getOperationLogListApi } from '@/api/adminOperationLog'
import { mountAdminPage } from './adminTestUtils'

const { activePermissions, hasPermission, feedbackStore } = await vi.hoisted(async () => {
  const { reactive } = await import('vue')
  const activePermissions = reactive(new Set<string>())
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
vi.mock('@/api/adminUser', () => ({
  disableUserApi: vi.fn(), enableUserApi: vi.fn(), getAdminUserDetailApi: vi.fn(),
  getAdminUserListApi: vi.fn(), updateUserRolesApi: vi.fn()
}))
vi.mock('@/api/adminRole', () => ({ getAdminRoleListApi: vi.fn(), updateRolePermissionsApi: vi.fn() }))
vi.mock('@/api/adminPermission', () => ({ getAdminPermissionListApi: vi.fn() }))
vi.mock('@/api/adminRagKnowledge', () => ({
  createRagKnowledgeApi: vi.fn(), deleteRagKnowledgeApi: vi.fn(), getRagKnowledgeDetailApi: vi.fn(),
  getRagKnowledgeListApi: vi.fn(), rebuildRagKnowledgeApi: vi.fn(), searchRagKnowledgeApi: vi.fn(),
  updateRagKnowledgeApi: vi.fn()
}))
vi.mock('@/api/adminOperationLog', () => ({
  deleteOperationLogApi: vi.fn(), getOperationLogDetailApi: vi.fn(), getOperationLogListApi: vi.fn()
}))

const user = {
  userId: 17, nickname: '林同学', username: 'lin', email: 'lin@example.com', roles: ['USER'], enabled: 1
}
const role = {
  roleId: 1, roleCode: 'USER', roleName: '普通用户', permissions: ['user:read'], enabled: 1
}
const ragDocument = {
  documentId: 41, title: 'Java 能力模型', direction: 'Java', knowledgeType: 'SKILL_REQUIREMENT',
  summary: 'Spring', content: '掌握 Spring Boot', enabled: 1
}
const log = { logId: 31, operation: '禁用用户' }
const feedback = {
  id: 51, userId: 17, type: 'BUG', title: '页面按钮异常', content: '按钮无响应', status: 'PENDING'
}

function deferredConfirmation() {
  let resolve!: (value: string) => void
  const promise = new Promise<string>((accept) => { resolve = accept })
  return { promise, resolve }
}

beforeEach(() => {
  vi.clearAllMocks()
  activePermissions.clear()
  vi.mocked(getAdminUserListApi).mockResolvedValue({ records: [user], total: 1 } as any)
  vi.mocked(getAdminRoleListApi).mockResolvedValue([role] as any)
  vi.mocked(getAdminPermissionListApi).mockResolvedValue([] as any)
  vi.mocked(getRagKnowledgeListApi).mockResolvedValue({ records: [ragDocument] } as any)
  vi.mocked(getOperationLogListApi).mockResolvedValue({ records: [log], total: 1 } as any)
  feedbackStore.loading = false
  feedbackStore.feedbackList = [feedback]
  feedbackStore.fetchFeedbackList.mockResolvedValue([feedback])
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)
})

describe('admin mutation handler permission guards', () => {
  it('blocks direct user mutation handlers and closes a stale role dialog without user:update + role:read', async () => {
    const wrapper = await mountAdminPage(AdminUserList, '/admin/users')
    const vm = wrapper.vm as any
    vi.clearAllMocks()

    await vm.changeEnabled(user, false)
    vm.openRoleDialog(user)
    expect(vm.roleDialogVisible).toBe(false)
    vm.currentUser = user
    vm.selectedRoleIds = [1]
    vm.roleDialogVisible = true
    await vm.submitRoles()

    expect(vm.roleDialogVisible).toBe(false)
    expect(ElMessageBox.confirm).not.toHaveBeenCalled()
    expect(disableUserApi).not.toHaveBeenCalled()
    expect(enableUserApi).not.toHaveBeenCalled()
    expect(updateUserRolesApi).not.toHaveBeenCalled()
  })

  it('blocks direct role assignment handlers and closes a stale dialog without role:update + permission:read', async () => {
    const wrapper = await mountAdminPage(AdminRoleList, '/admin/roles')
    const vm = wrapper.vm as any
    vi.clearAllMocks()

    vm.openDialog(role)
    expect(vm.visible).toBe(false)
    vm.currentRole = role
    vm.selectedPermissionIds = [11]
    vm.visible = true
    await vm.submit()

    expect(vm.visible).toBe(false)
    expect(updateRolePermissionsApi).not.toHaveBeenCalled()
  })

  it('blocks every direct RAG mutation entry without rag:manage', async () => {
    const wrapper = await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge')
    const vm = wrapper.vm as any
    vi.clearAllMocks()

    vm.openCreate()
    await vm.openEdit(41)
    vm.formVisible = true
    vm.editingId = 41
    await vm.save()
    await vm.rebuild(ragDocument)
    await vm.remove(ragDocument)

    expect(vm.formVisible).toBe(false)
    expect(ElMessageBox.confirm).not.toHaveBeenCalled()
    expect(getRagKnowledgeDetailApi).not.toHaveBeenCalled()
    expect(createRagKnowledgeApi).not.toHaveBeenCalled()
    expect(updateRagKnowledgeApi).not.toHaveBeenCalled()
    expect(rebuildRagKnowledgeApi).not.toHaveBeenCalled()
    expect(deleteRagKnowledgeApi).not.toHaveBeenCalled()
  })

  it('blocks direct log deletion without operation-log:delete', async () => {
    const wrapper = await mountAdminPage(OperationLogList, '/admin/operation-logs')
    vi.clearAllMocks()

    await (wrapper.vm as any).removeLog(log)

    expect(ElMessageBox.confirm).not.toHaveBeenCalled()
    expect(deleteOperationLogApi).not.toHaveBeenCalled()
  })

  it('blocks direct feedback write/delete handlers and closes stale dialogs without exact permissions', async () => {
    const wrapper = await mountAdminPage(AdminFeedbackList, '/admin/feedback')
    const vm = wrapper.vm as any
    vi.clearAllMocks()

    vm.openStatus(feedback)
    vm.openReply(feedback)
    expect(vm.statusVisible).toBe(false)
    expect(vm.replyVisible).toBe(false)
    vm.selected = feedback
    vm.statusVisible = true
    await vm.submitStatus()
    vm.replyVisible = true
    vm.replyForm.reply = '不应提交'
    await vm.submitReply()
    await vm.remove(feedback)

    expect(vm.statusVisible).toBe(false)
    expect(vm.replyVisible).toBe(false)
    expect(ElMessageBox.confirm).not.toHaveBeenCalled()
    expect(feedbackStore.updateStatus).not.toHaveBeenCalled()
    expect(feedbackStore.reply).not.toHaveBeenCalled()
    expect(feedbackStore.deleteFeedback).not.toHaveBeenCalled()
  })

  it('closes an open user role dialog when either required permission is revoked', async () => {
    activePermissions.add('user:update')
    activePermissions.add('role:read')
    const wrapper = await mountAdminPage(AdminUserList, '/admin/users')
    const vm = wrapper.vm as any

    vm.openRoleDialog(user)
    expect(vm.roleDialogVisible).toBe(true)

    activePermissions.delete('role:read')
    await nextTick()

    expect(vm.roleDialogVisible).toBe(false)
    expect(vm.currentUser).toBeNull()
    expect(vm.selectedRoleIds).toEqual([])
  })

  it('closes an open role permission dialog when either required permission is revoked', async () => {
    activePermissions.add('role:update')
    activePermissions.add('permission:read')
    vi.mocked(getAdminPermissionListApi).mockResolvedValue([
      { permissionId: 11, permissionCode: 'user:read', permissionName: '读取用户', resourceType: 'USER' }
    ] as any)
    const wrapper = await mountAdminPage(AdminRoleList, '/admin/roles')
    const vm = wrapper.vm as any

    vm.openDialog(role)
    expect(vm.visible).toBe(true)

    activePermissions.delete('role:update')
    await nextTick()

    expect(vm.visible).toBe(false)
    expect(vm.currentRole).toBeNull()
    expect(vm.selectedPermissionIds).toEqual([])
  })

  it('closes an open RAG mutation form when rag:manage is revoked', async () => {
    activePermissions.add('rag:manage')
    const wrapper = await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge')
    const vm = wrapper.vm as any

    vm.openCreate()
    expect(vm.formVisible).toBe(true)

    activePermissions.delete('rag:manage')
    await nextTick()

    expect(vm.formVisible).toBe(false)
    expect(vm.editingId).toBeUndefined()
  })

  it('closes open feedback write dialogs when feedback:write is revoked', async () => {
    activePermissions.add('feedback:write')
    const wrapper = await mountAdminPage(AdminFeedbackList, '/admin/feedback')
    const vm = wrapper.vm as any

    vm.openStatus(feedback)
    vm.openReply(feedback)
    expect(vm.statusVisible).toBe(true)
    expect(vm.replyVisible).toBe(true)

    activePermissions.delete('feedback:write')
    await nextTick()

    expect(vm.statusVisible).toBe(false)
    expect(vm.replyVisible).toBe(false)
  })

  it('does not change user status when user:update is revoked while confirmation is pending', async () => {
    activePermissions.add('user:update')
    const confirmation = deferredConfirmation()
    vi.mocked(ElMessageBox.confirm).mockReturnValue(confirmation.promise as any)
    const wrapper = await mountAdminPage(AdminUserList, '/admin/users')

    const action = (wrapper.vm as any).changeEnabled(user, false)
    await nextTick()
    activePermissions.delete('user:update')
    confirmation.resolve('confirm')
    await action

    expect(disableUserApi).not.toHaveBeenCalled()
    expect(enableUserApi).not.toHaveBeenCalled()
  })

  it('does not rebuild or delete RAG documents when rag:manage is revoked during confirmation', async () => {
    activePermissions.add('rag:manage')
    const wrapper = await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge')
    const vm = wrapper.vm as any
    const rebuildConfirmation = deferredConfirmation()
    vi.mocked(ElMessageBox.confirm).mockReturnValueOnce(rebuildConfirmation.promise as any)

    const rebuildAction = vm.rebuild(ragDocument)
    await nextTick()
    activePermissions.delete('rag:manage')
    rebuildConfirmation.resolve('confirm')
    await rebuildAction
    expect(rebuildRagKnowledgeApi).not.toHaveBeenCalled()

    activePermissions.add('rag:manage')
    const deleteConfirmation = deferredConfirmation()
    vi.mocked(ElMessageBox.confirm).mockReturnValueOnce(deleteConfirmation.promise as any)
    const deleteAction = vm.remove(ragDocument)
    await nextTick()
    activePermissions.delete('rag:manage')
    deleteConfirmation.resolve('confirm')
    await deleteAction
    expect(deleteRagKnowledgeApi).not.toHaveBeenCalled()
  })

  it('does not delete operation logs when operation-log:delete is revoked during confirmation', async () => {
    activePermissions.add('operation-log:delete')
    const confirmation = deferredConfirmation()
    vi.mocked(ElMessageBox.confirm).mockReturnValue(confirmation.promise as any)
    const wrapper = await mountAdminPage(OperationLogList, '/admin/operation-logs')

    const action = (wrapper.vm as any).removeLog(log)
    await nextTick()
    activePermissions.delete('operation-log:delete')
    confirmation.resolve('confirm')
    await action

    expect(deleteOperationLogApi).not.toHaveBeenCalled()
  })

  it('does not delete feedback when feedback:delete is revoked during confirmation', async () => {
    activePermissions.add('feedback:delete')
    const confirmation = deferredConfirmation()
    vi.mocked(ElMessageBox.confirm).mockReturnValue(confirmation.promise as any)
    const wrapper = await mountAdminPage(AdminFeedbackList, '/admin/feedback')

    const action = (wrapper.vm as any).remove(feedback)
    await nextTick()
    activePermissions.delete('feedback:delete')
    confirmation.resolve('confirm')
    await action

    expect(feedbackStore.deleteFeedback).not.toHaveBeenCalled()
  })
})
