import { flushPromises, mount } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FeedbackDrawer from '@/components/feedback/FeedbackDrawer.vue'
import { createFeedbackApi } from '@/api/feedback'
import { useFeedbackStore } from '@/stores/feedback'

vi.mock('@/api/feedback', () => ({
  createFeedbackApi: vi.fn(),
  deleteFeedbackApi: vi.fn(),
  listAdminFeedbackApi: vi.fn(),
  listMyFeedbackApi: vi.fn(),
  replyFeedbackApi: vi.fn(),
  updateFeedbackStatusApi: vi.fn()
}))

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

async function mountDrawer() {
  const pinia = createPinia()
  const store = useFeedbackStore(pinia)
  store.openDrawer()
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/applications', component: { template: '<div />' } }]
  })
  await router.push('/applications?status=APPLIED')
  await router.isReady()
  const wrapper = mount(FeedbackDrawer, {
    global: {
      plugins: [pinia, router],
      stubs: {
        teleport: true,
        ElDrawer: {
          props: ['modelValue', 'title'],
          template: '<aside role="dialog" :aria-label="title"><slot /></aside>'
        }
      }
    }
  })
  await flushPromises()
  return { wrapper, store }
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(createFeedbackApi).mockResolvedValue({ id: 31 } as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as any)
})

describe('feedback submission contract characterization', () => {
  it('submits the exact existing feedback payload including route and browser context', async () => {
    const { wrapper } = await mountDrawer()
    Object.assign((wrapper.vm as any).form, {
      type: 'UI_UX',
      title: '移动端操作被遮挡',
      content: '在投递页面打开抽屉后，底部按钮不可见。',
      contact: 'student@example.com',
      allowContact: true
    })
    ;(wrapper.vm as any).formRef = { validate: vi.fn().mockResolvedValue(true) }

    await (wrapper.vm as any).handleSubmit()

    expect(JSON.parse(JSON.stringify(vi.mocked(createFeedbackApi).mock.calls[0][0]))).toEqual({
      type: 'UI_UX',
      title: '移动端操作被遮挡',
      content: '在投递页面打开抽屉后，底部按钮不可见。',
      contact: 'student@example.com',
      allowContact: true,
      pageUrl: '/applications?status=APPLIED',
      browserInfo: navigator.userAgent
    })
  })

  it('retains every user-entered field when submission fails', async () => {
    vi.mocked(createFeedbackApi).mockRejectedValueOnce(new Error('网络连接失败'))
    const { wrapper, store } = await mountDrawer()
    Object.assign((wrapper.vm as any).form, {
      type: 'BUG',
      title: '保存失败',
      content: '请保留这段用于重试的详细描述。',
      contact: '13800000000',
      allowContact: true
    })
    ;(wrapper.vm as any).formRef = { validate: vi.fn().mockResolvedValue(true) }

    await (wrapper.vm as any).handleSubmit()

    expect((wrapper.vm as any).form).toMatchObject({
      type: 'BUG',
      title: '保存失败',
      content: '请保留这段用于重试的详细描述。',
      contact: '13800000000',
      allowContact: true
    })
    expect(store.drawerVisible).toBe(true)
    expect(ElMessage.error).toHaveBeenCalledWith('网络连接失败')
  })
})

describe('feedback drawer redesign', () => {
  it('shows an in-drawer success state and keeps the drawer available for another report', async () => {
    const { wrapper, store } = await mountDrawer()
    Object.assign((wrapper.vm as any).form, {
      type: 'SUGGESTION',
      title: '增加筛选记忆',
      content: '希望返回页面后保留上次筛选。'
    })
    ;(wrapper.vm as any).formRef = { validate: vi.fn().mockResolvedValue(true) }

    await (wrapper.vm as any).handleSubmit()

    expect(store.drawerVisible).toBe(true)
    expect(wrapper.get('.feedback-success').attributes('role')).toBe('status')
    expect(wrapper.get('.feedback-success').text()).toContain('反馈已提交')
    expect(wrapper.get('[data-feedback-another]').text()).toContain('继续反馈')
  })

  it('renders a persistent accessible error while retaining the retryable form', async () => {
    vi.mocked(createFeedbackApi).mockRejectedValueOnce({
      message: 'Request failed with status code 503',
      response: { data: { message: '受控反馈失败：输入应保留' } }
    })
    const { wrapper } = await mountDrawer()
    Object.assign((wrapper.vm as any).form, {
      type: 'BUG',
      title: '状态更新失败',
      content: '保存投递状态时出现错误。'
    })
    ;(wrapper.vm as any).formRef = { validate: vi.fn().mockResolvedValue(true) }

    await (wrapper.vm as any).handleSubmit()

    expect(wrapper.get('.feedback-error[role="alert"]').text()).toContain('受控反馈失败：输入应保留')
    expect((wrapper.vm as any).form.title).toBe('状态更新失败')
    expect(wrapper.text()).toContain('提交反馈')
  })

  it('ignores completion from a closed session without clearing or relabelling the reopened draft', async () => {
    const firstRequest = deferred<any>()
    const secondRequest = deferred<any>()
    vi.mocked(createFeedbackApi)
      .mockReturnValueOnce(firstRequest.promise)
      .mockReturnValueOnce(secondRequest.promise)
    const { wrapper, store } = await mountDrawer()
    ;(wrapper.vm as any).formRef = { validate: vi.fn().mockResolvedValue(true) }
    Object.assign((wrapper.vm as any).form, {
      type: 'BUG',
      title: '反馈 A',
      content: '旧会话内容'
    })
    const firstSubmit = (wrapper.vm as any).handleSubmit()
    await flushPromises()

    store.closeDrawer()
    await flushPromises()
    store.openDrawer()
    await flushPromises()
    Object.assign((wrapper.vm as any).form, {
      type: 'SUGGESTION',
      title: '反馈 B',
      content: '新会话内容'
    })
    const secondSubmit = (wrapper.vm as any).handleSubmit()
    await flushPromises()

    firstRequest.resolve({ id: 41 })
    await firstSubmit
    await flushPromises()

    expect((wrapper.vm as any).form).toMatchObject({ title: '反馈 B', content: '新会话内容' })
    expect((wrapper.vm as any).submitState).toBe('form')
    expect((wrapper.vm as any).submitting).toBe(true)
    expect(wrapper.find('.feedback-error').exists()).toBe(false)

    secondRequest.resolve({ id: 42 })
    await secondSubmit
    await flushPromises()
    expect(wrapper.get('.feedback-success').text()).toContain('反馈已提交')
  })

  it('locks before deferred validation so simultaneous calls create only one feedback', async () => {
    const validation = deferred<boolean>()
    const { wrapper } = await mountDrawer()
    ;(wrapper.vm as any).formRef = { validate: vi.fn(() => validation.promise) }
    Object.assign((wrapper.vm as any).form, {
      type: 'BUG',
      title: '防止重复提交',
      content: '验证期间再次触发提交。'
    })

    const firstSubmit = (wrapper.vm as any).handleSubmit()
    const duplicateSubmit = (wrapper.vm as any).handleSubmit()
    expect((wrapper.vm as any).submitting).toBe(true)
    validation.resolve(true)
    await Promise.all([firstSubmit, duplicateSubmit])

    expect(createFeedbackApi).toHaveBeenCalledTimes(1)
  })

  it('releases the single-flight lock when validation fails', async () => {
    const { wrapper } = await mountDrawer()
    const validate = vi.fn().mockResolvedValueOnce(false).mockResolvedValueOnce(true)
    ;(wrapper.vm as any).formRef = { validate }
    Object.assign((wrapper.vm as any).form, {
      type: 'BUG',
      title: '修正后可提交',
      content: '首次验证失败，修正后再次提交。'
    })

    await (wrapper.vm as any).handleSubmit()
    expect((wrapper.vm as any).submitting).toBe(false)
    await (wrapper.vm as any).handleSubmit()

    expect(validate).toHaveBeenCalledTimes(2)
    expect(createFeedbackApi).toHaveBeenCalledTimes(1)
  })

  it('invalidates a pending submission when the drawer component unmounts', async () => {
    const pendingRequest = deferred<any>()
    vi.mocked(createFeedbackApi).mockReturnValueOnce(pendingRequest.promise)
    const { wrapper } = await mountDrawer()
    ;(wrapper.vm as any).formRef = { validate: vi.fn().mockResolvedValue(true) }
    Object.assign((wrapper.vm as any).form, {
      type: 'BUG',
      title: '卸载前草稿',
      content: '旧请求完成时不能再修改组件状态。'
    })
    const draft = (wrapper.vm as any).form
    const pendingSubmit = (wrapper.vm as any).handleSubmit()
    await flushPromises()

    wrapper.unmount()
    pendingRequest.resolve({ id: 43 })
    await pendingSubmit

    expect(draft).toMatchObject({ title: '卸载前草稿', content: '旧请求完成时不能再修改组件状态。' })
    expect(ElMessage.success).not.toHaveBeenCalled()
  })
})
