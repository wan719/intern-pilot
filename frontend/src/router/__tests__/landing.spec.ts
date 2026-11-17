import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '@/App.vue'
import router from '@/router'
import { getToken, setToken } from '@/utils/token'

const auth = vi.hoisted(() => ({
  user: null as { userId: number } | null,
  fetchCurrentUser: vi.fn(),
  hasPermission: vi.fn(() => false),
  logout: vi.fn()
}))

vi.mock('@/stores/auth', () => ({ useAuthStore: () => auth }))

beforeEach(async () => {
  localStorage.clear()
  vi.clearAllMocks()
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  auth.user = null
  auth.fetchCurrentUser.mockReset()
  await router.replace('/403')
})

afterEach(() => vi.restoreAllMocks())

describe('public landing and protected routes', () => {
  it('opens the landing with registration and section links without fetching the user', async () => {
    await router.push('/')
    expect(router.currentRoute.value.fullPath).toBe('/')

    const landing = mount(App, { global: { plugins: [router] } })
    expect(landing.get('h1').text()).toBe('实习求职，从简历到投递')
    expect(landing.get('a[href="/register"]').text()).toContain('开始使用')
    expect(landing.get('a[href="/login"]').text()).toBe('登录')
    for (const anchor of ['resumes-and-jobs', 'matching-and-interviews', 'application-tracking']) {
      expect(landing.find(`a[href="#${anchor}"]`).exists()).toBe(true)
      expect(landing.find(`#${anchor}`).exists()).toBe(true)
    }
    expect(auth.fetchCurrentUser).not.toHaveBeenCalled()
    landing.unmount()
  })

  it('validates a saved session only when entering the workspace', async () => {
    setToken('synthetic-valid-session')
    auth.fetchCurrentUser.mockImplementation(async () => { auth.user = { userId: 1 } })
    await router.push('/')
    expect(router.currentRoute.value.fullPath).toBe('/')
    expect(auth.fetchCurrentUser).not.toHaveBeenCalled()

    const landing = mount(App, { global: { plugins: [router] } })
    expect(landing.get('a[href="/dashboard"]').text()).toContain('进入工作台')
    landing.unmount()

    await router.push('/dashboard')
    expect(router.currentRoute.value.fullPath).toBe('/dashboard')
    expect(router.currentRoute.value.matched).toHaveLength(2)
    expect(auth.fetchCurrentUser).toHaveBeenCalledOnce()

    await router.push('/resumes/42/versions?versionId=5')
    expect(router.currentRoute.value.fullPath).toBe('/resumes/42/versions?versionId=5')
    expect(router.currentRoute.value.matched).toHaveLength(2)
  })

  it('routes a stale session to login and clears the saved token', async () => {
    setToken('synthetic-expired-session')
    auth.fetchCurrentUser.mockRejectedValueOnce(new Error('401'))
    await router.push('/')
    expect(router.currentRoute.value.fullPath).toBe('/')

    await router.push('/dashboard')
    expect(router.currentRoute.value.fullPath).toBe('/login')
    expect(getToken()).toBeNull()
    expect(auth.logout).toHaveBeenCalledOnce()
  })

  it.each(['/resumes', '/analysis/reports/42/print', '/admin/users'])('guards anonymous access to %s', async (path) => {
    await router.push(path)
    expect(router.currentRoute.value.fullPath).toBe('/login')
  })

  it('checks admin permission after an authenticated visit to the landing', async () => {
    setToken('synthetic-limited-session')
    auth.user = { userId: 1 }
    await router.push('/')
    await router.push('/admin/users')
    expect(router.currentRoute.value.fullPath).toBe('/403')
    expect(auth.hasPermission).toHaveBeenCalledWith('user:read')
  })
})
