import { expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { syncAdminTabs, closeAdminTab, useAdminWorkTabs } from '../adminWorkTabs'
import { adminNavGroups } from '@/config/navigation'

const pages = adminNavGroups.flatMap(group => group.items)
const visible = pages.filter(page => ['admin:dashboard','user:read','operation-log:read'].includes(page.permission))
const dashboard = '/admin/dashboard'
const users = '/admin/users'
const logs = '/admin/operation-logs'

it('pins the authorized dashboard and tracks only successfully reached allowed pages', () => {
  let paths = syncAdminTabs([], users, visible)
  expect(paths).toEqual([dashboard, users])
  paths = syncAdminTabs(paths, logs, visible)
  expect(syncAdminTabs(paths, '/403', visible.filter(page => page.path !== users))).toEqual([dashboard, logs])
  expect(syncAdminTabs([], users, visible.filter(page => page.path === users))).toEqual([users])
})

it('closes active tabs to the right then the left and protects the dashboard', () => {
  const paths = [dashboard, users, logs]
  expect(closeAdminTab(paths, users, users)).toEqual({ paths: [dashboard, logs], target: logs })
  expect(closeAdminTab(paths, logs, logs)).toEqual({ paths: [dashboard, users], target: users })
  expect(closeAdminTab(paths, users, logs)).toEqual({ paths: [dashboard, logs], target: undefined })
  expect(closeAdminTab(paths, dashboard, dashboard)).toEqual({ paths, target: undefined })
})

it('clears in-memory tabs on a new login for the same account', () => {
  localStorage.clear()
  sessionStorage.clear()
  setActivePinia(createPinia())
  const auth = useAuthStore()
  auth.setLogin('first-session', { userId: 17 })
  const tabs = useAdminWorkTabs()
  tabs.paths = [dashboard, users]
  auth.setLogin('second-session', { userId: 17 })
  expect(tabs.paths).toEqual([])
  expect(sessionStorage.length).toBe(0)
  auth.logout()
})
