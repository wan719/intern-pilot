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
    try {
      expect(landing.get('h1').text()).toBe('投递前，先看你和岗位差在哪')
      expect(landing.findAll('a[href="/sample-report"]')).toHaveLength(2)
      expect(landing.get('.hero mark').text()).toBe('参与开发校园报修系统')
      expect(landing.get('.requirement-note').text()).toContain('能开发网页和后端接口')
      expect(landing.get('.revision-note').text()).toContain('实现报修提交和进度查询')
      expect(landing.get('#revision').text()).toContain('全栈开发申请版')
      expect(landing.get('.hero a[href="/register"]').text()).toContain('开始使用')
      expect(landing.get('header a[href="/register"]').text()).toBe('注册')
      expect(landing.get('a[href="/login"]').text()).toBe('登录')
      for (const anchor of ['match', 'revision', 'interview', 'application']) {
        expect(landing.find(`a[href="#${anchor}"]`).exists()).toBe(true)
        expect(landing.find(`#${anchor}`).exists()).toBe(true)
      }
      expect(auth.fetchCurrentUser).not.toHaveBeenCalled()
    } finally {
      landing.unmount()
    }
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

  it('keeps a replacement session when the previous session validation fails late', async () => {
    setToken('previous-session')
    await router.push('/')
    auth.fetchCurrentUser.mockImplementationOnce(async () => {
      setToken('replacement-session')
      throw new Error('401')
    })

    await router.push('/dashboard')
    expect(router.currentRoute.value.fullPath).toBe('/')
    expect(getToken()).toBe('replacement-session')
    expect(auth.logout).not.toHaveBeenCalled()
  })

  it.each(['/register', '/dashboard'])('shows a public sample with its main action linking to %s', async (destination) => {
    if (destination === '/dashboard') setToken('synthetic-saved-session')
    await router.push('/sample-report')
    expect(router.currentRoute.value.fullPath).toBe('/sample-report')
    const sample = mount(App, { global: { plugins: [router] } })
    try {
      expect(sample.get('h1').text()).toBe('样例报告')
      expect(sample.get(`main a[href="${destination}"]`).text()).toContain('用我的简历开始')
      expect(sample.text()).toContain('全栈开发实习生')
      expect(sample.text()).toContain('参与开发校园报修系统')
      expect(sample.text()).toContain('用 Vue 开发报修表单，用 Spring Boot 实现报修提交和状态查询接口，让学生能在线提交报修并查看处理进度。')
      expect(sample.text()).toContain('学生提交报修后，系统怎么保存记录并让他查到处理进度？')
      expect(auth.fetchCurrentUser).not.toHaveBeenCalled()
    } finally {
      sample.unmount()
    }
  })

  it.each(['/resumes', '/resumes/7', '/resumes/7/edit', '/analysis/match', '/analysis/reports', '/analysis/reports/42/print', '/admin/users'])('guards anonymous access to %s', async (path) => {
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
