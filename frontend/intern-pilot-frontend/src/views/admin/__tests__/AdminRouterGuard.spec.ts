import { beforeEach, describe, expect, it, vi } from 'vitest'

const { activePermissions, authStore } = vi.hoisted(() => {
  const activePermissions = new Set<string>()
  return {
    activePermissions,
    authStore: {
      user: { userId: 1, permissions: [] as string[] },
      hasPermission: vi.fn((permission: string) => activePermissions.has(permission)),
      fetchCurrentUser: vi.fn(),
      logout: vi.fn()
    }
  }
})
vi.mock('@/utils/token', () => ({
  getToken: vi.fn(() => 'safe-admin-test-token'),
  removeToken: vi.fn()
}))
vi.mock('@/stores/auth', () => ({ useAuthStore: () => authStore }))

import router from '@/router'

const guardedRoutes = [
  ['/admin/dashboard', 'admin:dashboard'],
  ['/admin/users', 'user:read'],
  ['/admin/roles', 'role:read'],
  ['/admin/permissions', 'permission:read'],
  ['/admin/operation-logs', 'operation-log:read'],
  ['/admin/rag-knowledge', 'rag:read'],
  ['/admin/feedback', 'feedback:read']
] as const

function setPermissions(...permissions: string[]) {
  activePermissions.clear()
  permissions.forEach((permission) => activePermissions.add(permission))
  authStore.user.permissions = permissions
}

beforeEach(async () => {
  vi.clearAllMocks()
  setPermissions()
  await router.push('/403')
})

describe('admin router permission matrix characterization', () => {
  it('full admin reaches every unchanged guarded route', async () => {
    setPermissions(...guardedRoutes.map(([, permission]) => permission))

    for (const [path] of guardedRoutes) {
      await router.push(path)
      expect(router.currentRoute.value.fullPath).toBe(path)
    }
  })

  it('user-only admin reaches users and is sent to /403 for another admin domain', async () => {
    setPermissions('user:read', 'user:update')

    await router.push('/admin/users')
    expect(router.currentRoute.value.fullPath).toBe('/admin/users')
    await router.push('/admin/roles')
    expect(router.currentRoute.value.fullPath).toBe('/403')
  })

  it('restricted account is sent to /403 for every admin route', async () => {
    for (const [path] of guardedRoutes) {
      await router.push(path)
      expect(router.currentRoute.value.fullPath).toBe('/403')
    }
  })
})
