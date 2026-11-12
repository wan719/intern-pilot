import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AppHeader from '@/components/layout/AppHeader.vue'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'
import { useFeedbackStore } from '@/stores/feedback'

vi.mock('@/api/health', () => ({
  getAiProviderApi: vi.fn().mockResolvedValue({ provider: 'deepseek' })
}))

describe('AppHeader global actions', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  async function mountHeader() {
    const pinia = createPinia()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
    })
    await router.push('/dashboard')
    await router.isReady()

    return {
      wrapper: mount(AppHeader, { global: { plugins: [pinia, router] } }),
      aiTaskCenter: useAiTaskCenterStore(pinia),
      feedback: useFeedbackStore(pinia)
    }
  }

  it('opens the existing AI task drawer store from the header action', async () => {
    const { wrapper, aiTaskCenter } = await mountHeader()
    const aiButton = wrapper.findAll('button').find((item) => item.text().includes('AI 任务'))!

    expect(aiTaskCenter.drawerVisible).toBe(false)
    await aiButton.trigger('click')

    expect(aiTaskCenter.drawerVisible).toBe(true)
  })

  it('announces cancelled actionable tasks from the header action', async () => {
    const { wrapper, aiTaskCenter } = await mountHeader()
    const localTaskId = aiTaskCenter.createTask({
      type: 'ANALYSIS_MATCH',
      title: 'AI 简历匹配分析',
      taskNo: 'ANALYSIS_CANCELLED',
      sourcePath: '/analysis/match'
    })
    aiTaskCenter.updateTask(localTaskId, { status: 'CANCELLED' }, { notify: false })
    await wrapper.vm.$nextTick()

    const aiButton = wrapper.findAll('button').find((item) => item.text().includes('AI 任务'))!
    expect(aiButton.attributes('aria-label')).toBe('有 1 个任务已取消，待处理')
    expect(aiButton.get('.header-action-badge').text()).toBe('1')
  })

  it('opens the existing feedback drawer store from the header action', async () => {
    const { wrapper, feedback } = await mountHeader()

    expect(feedback.drawerVisible).toBe(false)
    await wrapper.get('button[aria-label="打开意见反馈"]').trigger('click')

    expect(feedback.drawerVisible).toBe(true)
  })
})
