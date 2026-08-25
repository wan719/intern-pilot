import { flushPromises } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminPermissionList from '@/views/admin/AdminPermissionList.vue'
import AdminRagKnowledgeList from '@/views/admin/AdminRagKnowledgeList.vue'
import AdminRoleList from '@/views/admin/AdminRoleList.vue'
import AdminUserList from '@/views/admin/AdminUserList.vue'
import {
  disableUserApi,
  enableUserApi,
  getAdminUserDetailApi,
  getAdminUserListApi,
  updateUserRolesApi
} from '@/api/adminUser'
import { getAdminRoleListApi, updateRolePermissionsApi } from '@/api/adminRole'
import { getAdminPermissionListApi } from '@/api/adminPermission'
import {
  createRagKnowledgeApi,
  deleteRagKnowledgeApi,
  getRagKnowledgeDetailApi,
  getRagKnowledgeListApi,
  rebuildRagKnowledgeApi,
  searchRagKnowledgeApi,
  updateRagKnowledgeApi
} from '@/api/adminRagKnowledge'
import { buttonByText, mountAdminPage, plain } from './adminTestUtils'

const { hasPermission } = vi.hoisted(() => ({ hasPermission: vi.fn() }))

vi.mock('@/stores/auth', () => ({ useAuthStore: () => ({ hasPermission }) }))
vi.mock('@/api/adminUser', () => ({
  disableUserApi: vi.fn(),
  enableUserApi: vi.fn(),
  getAdminUserDetailApi: vi.fn(),
  getAdminUserListApi: vi.fn(),
  updateUserRolesApi: vi.fn()
}))
vi.mock('@/api/adminRole', () => ({ getAdminRoleListApi: vi.fn(), updateRolePermissionsApi: vi.fn() }))
vi.mock('@/api/adminPermission', () => ({ getAdminPermissionListApi: vi.fn() }))
vi.mock('@/api/adminRagKnowledge', () => ({
  createRagKnowledgeApi: vi.fn(),
  deleteRagKnowledgeApi: vi.fn(),
  getRagKnowledgeDetailApi: vi.fn(),
  getRagKnowledgeListApi: vi.fn(),
  rebuildRagKnowledgeApi: vi.fn(),
  searchRagKnowledgeApi: vi.fn(),
  updateRagKnowledgeApi: vi.fn()
}))

const user = {
  userId: 17,
  nickname: '林同学',
  username: 'lin',
  email: 'lin@example.com',
  school: '示例大学',
  major: '计算机',
  grade: '2027',
  roles: ['USER'],
  enabled: 1,
  createdAt: '2026-08-25T08:00:00'
}
const roles = [
  { roleId: 1, roleCode: 'USER', roleName: '普通用户', description: '用户', permissions: ['user:read'], enabled: 1 },
  { roleId: 2, roleCode: 'ADMIN', roleName: '管理员', description: '管理员', permissions: ['user:update'], enabled: 1 }
]
const permissions = [
  { permissionId: 11, permissionCode: 'user:read', permissionName: '读取用户', resourceType: 'ADMIN_USER', enabled: 1 },
  { permissionId: 12, permissionCode: 'user:update', permissionName: '编辑用户', resourceType: 'ADMIN_USER', enabled: 1 }
]
const ragDocument = {
  documentId: 41,
  title: 'Java 实习能力模型',
  direction: 'Java 后端',
  knowledgeType: 'SKILL_REQUIREMENT',
  summary: 'Spring 与数据库',
  content: '掌握 Spring Boot 与 MySQL',
  chunkCount: 2,
  enabled: 1,
  updatedAt: '2026-08-25T08:00:00',
  chunks: []
}

beforeEach(() => {
  vi.clearAllMocks()
  hasPermission.mockReturnValue(true)
  vi.mocked(getAdminUserListApi).mockResolvedValue({ records: [user], total: 1 } as any)
  vi.mocked(getAdminUserDetailApi).mockResolvedValue({ ...user, permissions: ['user:read'] } as any)
  vi.mocked(getAdminRoleListApi).mockResolvedValue(roles as any)
  vi.mocked(getAdminPermissionListApi).mockResolvedValue(permissions as any)
  vi.mocked(disableUserApi).mockResolvedValue(undefined as any)
  vi.mocked(enableUserApi).mockResolvedValue(undefined as any)
  vi.mocked(updateUserRolesApi).mockResolvedValue(undefined as any)
  vi.mocked(updateRolePermissionsApi).mockResolvedValue(undefined as any)
  vi.mocked(getRagKnowledgeListApi).mockResolvedValue({ records: [ragDocument], total: 1 } as any)
  vi.mocked(getRagKnowledgeDetailApi).mockResolvedValue(ragDocument as any)
  vi.mocked(createRagKnowledgeApi).mockResolvedValue({ documentId: 42 } as any)
  vi.mocked(updateRagKnowledgeApi).mockResolvedValue(undefined as any)
  vi.mocked(rebuildRagKnowledgeApi).mockResolvedValue(undefined as any)
  vi.mocked(deleteRagKnowledgeApi).mockResolvedValue(undefined as any)
  vi.mocked(searchRagKnowledgeApi).mockResolvedValue([{ chunkId: 3, title: 'Java', content: 'Spring', similarity: 0.9 }] as any)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as any)
})

describe('admin user behavior characterization', () => {
  it('keeps list, role and detail reads exact', async () => {
    const wrapper = await mountAdminPage(AdminUserList, '/admin/users')

    expect(getAdminUserListApi).toHaveBeenCalledWith({ keyword: '', roleCode: '', enabled: undefined, pageNum: 1, pageSize: 10 })
    expect(getAdminRoleListApi).toHaveBeenCalledWith()
    await buttonByText(wrapper, '详情').trigger('click')
    await flushPromises()
    expect(getAdminUserDetailApi).toHaveBeenCalledWith(17)
  })

  it('keeps enable, disable and role-assignment payloads behind their exact confirmations', async () => {
    const wrapper = await mountAdminPage(AdminUserList, '/admin/users')

    await (wrapper.vm as any).changeEnabled(user, false)
    expect(ElMessageBox.confirm).toHaveBeenNthCalledWith(1, '确认禁用用户 林同学？', '确认操作', { type: 'warning' })
    expect(disableUserApi).toHaveBeenCalledWith(17)

    await (wrapper.vm as any).changeEnabled(user, true)
    expect(ElMessageBox.confirm).toHaveBeenNthCalledWith(2, '确认启用用户 林同学？', '确认操作', { type: 'warning' })
    expect(enableUserApi).toHaveBeenCalledWith(17)

    ;(wrapper.vm as any).openRoleDialog(user)
    ;(wrapper.vm as any).selectedRoleIds = [2]
    await (wrapper.vm as any).submitRoles()
    expect(updateUserRolesApi).toHaveBeenCalledWith(17, { roleIds: [2] })
  })

  it('keeps cancellation as a no-op for confirmation and role assignment', async () => {
    const wrapper = await mountAdminPage(AdminUserList, '/admin/users')
    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel')

    await expect((wrapper.vm as any).changeEnabled(user, false)).resolves.toBeUndefined()
    expect(disableUserApi).not.toHaveBeenCalled()

    ;(wrapper.vm as any).openRoleDialog(user)
    ;(wrapper.vm as any).roleDialogVisible = false
    await wrapper.vm.$nextTick()
    expect(updateUserRolesApi).not.toHaveBeenCalled()
  })
})

describe('admin role and permission behavior characterization', () => {
  it('keeps role/permission reads and permission assignment payload exact', async () => {
    const wrapper = await mountAdminPage(AdminRoleList, '/admin/roles')

    expect(getAdminRoleListApi).toHaveBeenCalledWith()
    expect(getAdminPermissionListApi).toHaveBeenCalledWith()
    ;(wrapper.vm as any).openDialog(roles[0])
    ;(wrapper.vm as any).selectedPermissionIds = [11, 12]
    await (wrapper.vm as any).submit()
    expect(updateRolePermissionsApi).toHaveBeenCalledWith(1, { permissionIds: [11, 12] })
  })

  it('keeps the permission page read-only and sends only the selected resource filter', async () => {
    const wrapper = await mountAdminPage(AdminPermissionList, '/admin/permissions')

    expect(getAdminPermissionListApi).toHaveBeenCalledWith(undefined)
    ;(wrapper.vm as any).resourceType = 'ADMIN_USER'
    await (wrapper.vm as any).loadData()
    expect(getAdminPermissionListApi).toHaveBeenLastCalledWith({ resourceType: 'ADMIN_USER' })
    const buttons = wrapper.findAll('button').map((item) => item.text().trim())
    expect(buttons).not.toContain('删除')
    expect(buttons).not.toContain('编辑')
  })
})

describe('admin RAG behavior characterization', () => {
  it('keeps list, detail and search payloads exact', async () => {
    const wrapper = await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge')

    expect(getRagKnowledgeListApi).toHaveBeenCalledWith({
      direction: '', knowledgeType: '', enabled: undefined, pageNum: 1, pageSize: 100
    })
    await buttonByText(wrapper, '详情').trigger('click')
    await flushPromises()
    expect(getRagKnowledgeDetailApi).toHaveBeenCalledWith(41)

    ;(wrapper.vm as any).openSearch()
    Object.assign((wrapper.vm as any).searchForm, { query: 'Spring 岗位', direction: 'Java 后端', knowledgeType: 'SKILL_REQUIREMENT', topK: 8 })
    await (wrapper.vm as any).search()
    expect(searchRagKnowledgeApi).toHaveBeenCalledWith(expect.objectContaining({
      query: 'Spring 岗位', direction: 'Java 后端', knowledgeType: 'SKILL_REQUIREMENT', topK: 8
    }))
  })

  it('keeps create and edit payload fields exact', async () => {
    const wrapper = await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge')
    ;(wrapper.vm as any).openCreate()
    Object.assign((wrapper.vm as any).form, {
      title: '前端能力模型',
      direction: '前端开发',
      knowledgeType: 'SKILL_REQUIREMENT',
      summary: 'Vue 与 TypeScript',
      content: '掌握 Vue 3',
      enabled: 1
    })
    await (wrapper.vm as any).save()
    expect(plain(vi.mocked(createRagKnowledgeApi).mock.calls[0][0])).toEqual({
      title: '前端能力模型', direction: '前端开发', knowledgeType: 'SKILL_REQUIREMENT',
      summary: 'Vue 与 TypeScript', content: '掌握 Vue 3', enabled: 1
    })

    await (wrapper.vm as any).openEdit(41)
    ;(wrapper.vm as any).form.enabled = 0
    await (wrapper.vm as any).save()
    expect(updateRagKnowledgeApi).toHaveBeenCalledWith(41, expect.objectContaining({
      title: 'Java 实习能力模型', direction: 'Java 后端', knowledgeType: 'SKILL_REQUIREMENT',
      summary: 'Spring 与数据库', content: '掌握 Spring Boot 与 MySQL', enabled: 0
    }))
  })

  it('keeps rebuild and delete destructive operations behind exact confirmations', async () => {
    const wrapper = await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge')

    await (wrapper.vm as any).rebuild(ragDocument)
    expect(ElMessageBox.confirm).toHaveBeenNthCalledWith(
      1, '确认重建「Java 实习能力模型」的文本切片和向量？', '重建确认', { type: 'warning' }
    )
    expect(rebuildRagKnowledgeApi).toHaveBeenCalledWith(41)

    await (wrapper.vm as any).remove(ragDocument)
    expect(ElMessageBox.confirm).toHaveBeenNthCalledWith(
      2, '确认删除知识文档「Java 实习能力模型」？删除后无法恢复。', '删除确认', { type: 'warning' }
    )
    expect(deleteRagKnowledgeApi).toHaveBeenCalledWith(41)
  })

  it('treats cancelled rebuild and delete confirmations as no-ops', async () => {
    const wrapper = await mountAdminPage(AdminRagKnowledgeList, '/admin/rag-knowledge')
    vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')

    await expect((wrapper.vm as any).rebuild(ragDocument)).resolves.toBeUndefined()
    await expect((wrapper.vm as any).remove(ragDocument)).resolves.toBeUndefined()
    expect(rebuildRagKnowledgeApi).not.toHaveBeenCalled()
    expect(deleteRagKnowledgeApi).not.toHaveBeenCalled()
  })
})
