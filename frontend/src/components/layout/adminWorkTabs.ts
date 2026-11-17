import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'
import type { AdminNavItem } from '@/config/navigation'
import { useAuthStore } from '@/stores/auth'
import { appearanceAccountId } from '@/composables/useAppearance'

import { readAdminTabSession, saveAdminTabSession } from '@/utils/adminTabSession'

const dashboard = '/admin/dashboard'

export function syncAdminTabs(paths: string[], active: string, visible: AdminNavItem[]) {
  const allowed = new Set(visible.map(item => item.path))
  const next = new Set<string>()
  if (allowed.has(dashboard)) next.add(dashboard)
  for (const path of paths) if (allowed.has(path)) next.add(path)
  if (allowed.has(active)) next.add(active)
  return [...next]
}

export function closeAdminTab(paths: string[], path: string, active: string) {
  const index = paths.indexOf(path)
  if (index < 0 || path === dashboard) return { paths, target: undefined }
  return {
    paths: paths.filter(item => item !== path),
    target: path === active ? paths[index + 1] ?? paths[index - 1] : undefined,
  }
}

export const useAdminWorkTabs = defineStore('admin-work-tabs', () => {
  const auth = useAuthStore()
  const paths = ref<string[]>([])
  const account = computed(() => appearanceAccountId(auth.user))
  watch([account, () => auth.loginGeneration], ([current], [previous]) => {
    if (previous != null && previous !== current) saveAdminTabSession(previous, [])
    paths.value = readAdminTabSession(current)
  }, { immediate: true, flush: 'sync' })
  watch(paths, items => saveAdminTabSession(account.value, items), { flush: 'sync' })
  return { paths }
})
