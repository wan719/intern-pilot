import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

function setPermission(granted: boolean) {
  localStorage.setItem('internpilot_token', 'permission-test-token')
  setActivePinia(createPinia())
  const auth = useAuthStore()
  auth.setUser({ userId: 1, permissions: granted ? ['analysis:read'] : [] })
}

describe('job recommendation permission contract', () => {
  beforeEach(async () => {
    localStorage.clear()
    setPermission(true)
    await router.replace('/jobs')
  })

  it('keeps analysis:read on both recommendation routes for the shared guard', () => {
    expect(router.resolve('/job-recommendations').meta.permission).toBe('analysis:read')
    expect(router.resolve('/job-recommendations/31').meta.permission).toBe('analysis:read')
  })

  it('allows an authorized direct route through the real shared guard', async () => {
    await router.push('/job-recommendations/31')

    expect(router.currentRoute.value.fullPath).toBe('/job-recommendations/31')
  })

  it('redirects an unauthorized direct route to 403 through the real shared guard', async () => {
    setPermission(false)
    await router.push('/job-recommendations/31')

    expect(router.currentRoute.value.fullPath).toBe('/403')
  })
})
