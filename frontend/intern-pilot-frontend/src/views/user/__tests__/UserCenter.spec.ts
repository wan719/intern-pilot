import { flushPromises, mount } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import UserCenter from '@/views/user/UserCenter.vue'
import { changePasswordApi, getUserProfileApi, updateUserProfileApi, uploadUserAvatarApi } from '@/api/user'
import { getResumeListApi, setDefaultResumeApi } from '@/api/resume'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/user', () => ({
  changePasswordApi: vi.fn(),
  getUserProfileApi: vi.fn(),
  updateUserProfileApi: vi.fn(),
  uploadUserAvatarApi: vi.fn()
}))
vi.mock('@/api/resume', () => ({ getResumeListApi: vi.fn(), setDefaultResumeApi: vi.fn() }))

const profile = {
  id: 5,
  username: 'student',
  nickname: '林同学',
  avatarUrl: '/uploads/avatar.webp',
  email: 'student@example.com',
  emailVerified: true,
  preferredJobTitle: '前端开发实习生',
  preferredCity: '上海',
  expectedSalary: '250/天',
  employmentType: '实习',
  defaultResumeId: 3,
  defaultResumeName: '前端简历',
  roles: ['USER'],
  permissions: [],
  lastLoginTime: '2026-08-25T12:00:00',
  createdAt: '2026-01-01T08:00:00',
  updatedAt: '2026-08-25T12:00:00'
}

let capturedPasswordPayload: unknown

async function mountPage() {
  const pinia = createPinia()
  const auth = useAuthStore(pinia)
  auth.setUser({ username: 'student', school: '西南大学', major: '软件工程' })
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/user/center', component: { template: '<div />' }, meta: { title: '个人中心' } },
      { path: '/login', component: { template: '<div />' } }
    ]
  })
  await router.push('/user/center')
  await router.isReady()
  const wrapper = mount(UserCenter, { global: { plugins: [pinia, router], stubs: { teleport: true } } })
  await flushPromises()
  return { wrapper, auth, router }
}

beforeEach(() => {
  vi.clearAllMocks()
  capturedPasswordPayload = undefined
  vi.mocked(getUserProfileApi).mockResolvedValue(profile as any)
  vi.mocked(updateUserProfileApi).mockImplementation(async (data) => ({ ...profile, ...data }) as any)
  vi.mocked(uploadUserAvatarApi).mockResolvedValue({ ...profile, avatarUrl: '/uploads/new-avatar.webp' } as any)
  vi.mocked(changePasswordApi).mockImplementation(async (data) => {
    capturedPasswordPayload = JSON.parse(JSON.stringify(data))
    return undefined as any
  })
  vi.mocked(getResumeListApi).mockResolvedValue({ records: [{ resumeId: 3, resumeName: '前端简历', isDefault: true }] } as any)
  vi.mocked(setDefaultResumeApi).mockResolvedValue(undefined as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined as any)
})

describe('user center contract characterization', () => {
  it('loads the existing profile and resume sources and keeps auth identity synchronized', async () => {
    const { wrapper, auth } = await mountPage()

    expect(getUserProfileApi).toHaveBeenCalledOnce()
    expect(getResumeListApi).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(wrapper.text()).toContain('student@example.com')
    expect(auth.user).toMatchObject({ username: 'student', nickname: '林同学', avatarUrl: '/uploads/avatar.webp' })
  })

  it('keeps the exact profile and avatar upload payloads', async () => {
    const { wrapper } = await mountPage()
    Object.assign((wrapper.vm as any).profileForm, {
      nickname: '林同学（更新）',
      preferredJobTitle: 'Java 后端实习生',
      preferredCity: '成都',
      expectedSalary: '200/天',
      employmentType: '实习'
    })
    await (wrapper.vm as any).saveProfile()

    expect(JSON.parse(JSON.stringify(vi.mocked(updateUserProfileApi).mock.calls[0][0]))).toEqual({
      nickname: '林同学（更新）',
      preferredJobTitle: 'Java 后端实习生',
      preferredCity: '成都',
      expectedSalary: '200/天',
      employmentType: '实习'
    })

    const file = new File(['avatar'], 'avatar.webp', { type: 'image/webp' })
    await (wrapper.vm as any).uploadAvatar({ raw: file })
    const avatarPayload = vi.mocked(uploadUserAvatarApi).mock.calls[0][0]
    expect(avatarPayload.get('file')).toBe(file)
    expect((wrapper.vm as any).avatarUrl).toContain('/uploads/new-avatar.webp')
  })

  it('keeps password and default-resume session-adjacent actions unchanged', async () => {
    const { wrapper } = await mountPage()
    Object.assign((wrapper.vm as any).passwordForm, {
      oldPassword: 'old-password',
      newPassword: 'new-password',
      confirmPassword: 'new-password'
    })
    await (wrapper.vm as any).changePassword()

    expect(capturedPasswordPayload).toEqual({
      oldPassword: 'old-password',
      newPassword: 'new-password',
      confirmPassword: 'new-password'
    })
    expect((wrapper.vm as any).passwordForm).toEqual({ oldPassword: '', newPassword: '', confirmPassword: '' })

    await (wrapper.vm as any).changeDefaultResume(3)
    expect(setDefaultResumeApi).toHaveBeenCalledWith(3)
  })
})

describe('user center redesign', () => {
  it('shows identity, education and career, security, and session as explicit labelled sections', async () => {
    const { wrapper } = await mountPage()

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.get('[data-account-section="identity"]').text()).toContain('student@example.com')
    expect(wrapper.get('[data-account-section="education"]').text()).toContain('西南大学')
    expect(wrapper.get('[data-account-section="education"]').text()).toContain('软件工程')
    expect(wrapper.get('[data-account-section="education"]').text()).toContain('期望岗位')
    expect(wrapper.get('[data-account-section="security"]').text()).toContain('原密码')
    expect(wrapper.get('[data-account-section="session"]').text()).toContain('最近登录')
    expect(wrapper.get('[data-account-section="session"]').text()).toContain('退出当前账号')
  })

  it('ends the current session through the existing auth store and login route', async () => {
    const { wrapper, auth, router } = await mountPage()

    await wrapper.get('[data-session-logout]').trigger('click')
    await flushPromises()

    expect(auth.user).toBeNull()
    expect(router.currentRoute.value.path).toBe('/login')
  })
})
