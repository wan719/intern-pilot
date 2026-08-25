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
})
