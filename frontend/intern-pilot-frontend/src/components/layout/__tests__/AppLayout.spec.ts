import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AppLayout from '@/components/layout/AppLayout.vue'
import AiTaskDrawer from '@/components/ai/AiTaskDrawer.vue'
import FeedbackDrawer from '@/components/feedback/FeedbackDrawer.vue'
import MobileBottomNav from '@/components/layout/MobileBottomNav.vue'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/user', () => ({
  getCurrentUserApi: vi.fn().mockResolvedValue({ id: 7, username: 'journey-user' })
}))

vi.mock('@/api/health', () => ({
  getAiProviderApi: vi.fn().mockResolvedValue({ provider: 'deepseek' })
}))

vi.mock('@/api/analysisTask', () => ({
  cancelTaskApi: vi.fn().mockResolvedValue(undefined),
  listRecentTasksApi: vi.fn().mockResolvedValue([]),
  listRunningTasksApi: vi.fn().mockResolvedValue([])
}))

describe('AppLayout journey shell', () => {
  let wrapper: VueWrapper | undefined

  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  async function mountLayout(onRouteMount?: () => void) {
    const pinia = createPinia()
    const routeComponent = {
      setup() {
        onRouteMount?.()
      },
      template: '<div data-testid="route-content">Route content</div>'
    }
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/dashboard', component: routeComponent },
        { path: '/:pathMatch(.*)*', component: { template: '<div />' } }
      ]
    })
    await router.push('/dashboard')
    await router.isReady()
    wrapper = mount(AppLayout, { global: { plugins: [pinia, router] } })
    await flushPromises()
    return { wrapper, pinia }
  }

  it('mounts the journey shell, mobile navigation, and both existing drawers', async () => {
    const mounted = await mountLayout()

    expect(mounted.wrapper.classes()).toContain('user-app-shell')
    expect(mounted.wrapper.find('main#main-content').exists()).toBe(true)
    expect(mounted.wrapper.findComponent(MobileBottomNav).exists()).toBe(true)
    expect(mounted.wrapper.findComponent(AiTaskDrawer).exists()).toBe(true)
    expect(mounted.wrapper.findComponent(FeedbackDrawer).exists()).toBe(true)
  })

  it('loads the current user and initializes stored AI tasks', async () => {
    localStorage.setItem(
      'internpilot:ai-task-center',
      JSON.stringify([
        {
          localTaskId: 'stored-task',
          type: 'ANALYSIS_MATCH',
          title: 'Stored task',
          status: 'COMPLETED',
          progress: 100,
          createdAt: '2026-08-25T00:00:00.000Z',
          updatedAt: '2026-08-25T00:00:00.000Z',
          dismissible: true,
          cancellable: false
        }
      ])
    )

    const mounted = await mountLayout()

    expect(useAuthStore(mounted.pinia).user?.username).toBe('journey-user')
    expect(useAiTaskCenterStore(mounted.pinia).tasks.map((task) => task.localTaskId)).toContain('stored-task')
  })

  it('remounts the current route when the refresh action is used', async () => {
    let routeMounts = 0
    const mounted = await mountLayout(() => {
      routeMounts += 1
    })

    expect(routeMounts).toBe(1)
    await mounted.wrapper.get('button[aria-label="刷新当前页面"]').trigger('click')

    expect(routeMounts).toBe(2)
  })
})
