import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { ElMessage } from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { loginApi, registerApi, sendRegisterCaptchaApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import Login from '@/views/auth/Login.vue'
import Register from '@/views/auth/Register.vue'

vi.mock('@/api/auth', () => ({
  loginApi: vi.fn(),
  registerApi: vi.fn(),
  sendRegisterCaptchaApi: vi.fn()
}))

const mockedLoginApi = vi.mocked(loginApi)
const mockedRegisterApi = vi.mocked(registerApi)
const mockedSendRegisterCaptchaApi = vi.mocked(sendRegisterCaptchaApi)

async function mountAuth(component: typeof Login | typeof Register, path: '/login' | '/register') {
  const pinia = createPinia()
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', component: { template: '<div>login destination</div>' } },
      { path: '/register', component: { template: '<div>register destination</div>' } },
      { path: '/dashboard', component: { template: '<div>dashboard destination</div>' } }
    ]
  })
  await router.push(path)
  await router.isReady()

  return {
    wrapper: mount(component, {
      global: {
        plugins: [pinia, router],
        stubs: { transition: false, 'transition-group': false }
      }
    }),
    router,
    auth: useAuthStore(pinia)
  }
}

describe('authentication behavior characterization', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('submits the unchanged login payload, stores the session, and opens the dashboard', async () => {
    mockedLoginApi.mockResolvedValue({ token: 'session-token', user: { id: 7, username: 'Ada' } } as never)
    const { wrapper, router, auth } = await mountAuth(Login, '/login')
    const inputs = wrapper.findAll('input')

    await inputs[0].setValue('ada@example.com')
    await inputs[1].setValue('correct horse')
    await wrapper.get('button.el-button--primary').trigger('click')
    await flushPromises()

    expect(mockedLoginApi).toHaveBeenCalledWith({
      account: 'ada@example.com',
      password: 'correct horse'
    })
    expect(auth.token).toBe('session-token')
    expect(auth.user).toEqual({ id: 7, username: 'Ada' })
    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('keeps the user on login and surfaces an invalid-credentials failure', async () => {
    const errorMessage = vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as never)
    mockedLoginApi.mockRejectedValue(new Error('邮箱或密码错误'))
    const { wrapper, router } = await mountAuth(Login, '/login')
    const inputs = wrapper.findAll('input')

    await inputs[0].setValue('ada@example.com')
    await inputs[1].setValue('wrong password')
    await wrapper.get('button.el-button--primary').trigger('click')
    await flushPromises()

    expect(errorMessage).toHaveBeenCalledWith('邮箱或密码错误')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('requests an email registration code with the unchanged payload and starts cooldown', async () => {
    vi.useFakeTimers()
    mockedSendRegisterCaptchaApi.mockResolvedValue(undefined as never)
    const { wrapper } = await mountAuth(Register, '/register')

    await wrapper.findAll('input')[0].setValue('new@example.com')
    await wrapper.findAll('button').find((button) => button.text().includes('发送验证码'))!.trigger('click')
    await flushPromises()

    expect(mockedSendRegisterCaptchaApi).toHaveBeenCalledWith({
      target: 'new@example.com',
      type: 'EMAIL'
    })
    expect(wrapper.text()).toContain('60s')
  })

  it('associates visible registration labels with their inputs', async () => {
    const { wrapper } = await mountAuth(Register, '/register')

    const accountItem = wrapper.findAll('.el-form-item').find((item) => item.text().includes('邮箱'))!
    const accountLabel = accountItem.get('label')
    const accountInput = accountItem.get('input')
    expect(accountLabel.text()).toBe('邮箱')
    expect(accountLabel.attributes('for')).toBe(accountInput.attributes('id'))
  })

  it('submits the unchanged registration payload and returns to login', async () => {
    mockedRegisterApi.mockResolvedValue(undefined as never)
    const { wrapper, router } = await mountAuth(Register, '/register')
    const inputs = wrapper.findAll('input')

    await inputs[0].setValue('new@example.com')
    await inputs[1].setValue('123456')
    await inputs[2].setValue('safe-password')
    await inputs[3].setValue('safe-password')
    await inputs[4].setValue('Example University')
    await inputs[5].setValue('Computer Science')
    await inputs[6].setValue('2026')
    await inputs[7].setValue('new-user')
    await wrapper.get('button.el-button--primary').trigger('click')
    await flushPromises()

    expect(mockedRegisterApi).toHaveBeenCalledWith({
      account: 'new@example.com',
      accountType: 'EMAIL',
      password: 'safe-password',
      confirmPassword: 'safe-password',
      captchaCode: '123456',
      username: 'new-user',
      school: 'Example University',
      major: 'Computer Science',
      grade: '2026'
    })
    expect(router.currentRoute.value.path).toBe('/login')
  })
})

describe.each([
  ['login', Login, '/login' as const],
  ['registration', Register, '/register' as const]
])('%s career-studio shell', (_name, component, path) => {
  it('exposes the form as a labeled main landmark inside the career auth shell', async () => {
    const { wrapper } = await mountAuth(component, path)

    expect(wrapper.get('.auth-page').classes()).toContain('auth-page--career')
    const formLandmark = wrapper.get('main.auth-form')
    expect(formLandmark.attributes('aria-labelledby')).toBe('auth-title')
    expect(formLandmark.get('#auth-title').element.tagName).toMatch(/^H[1-6]$/)
  })
})
