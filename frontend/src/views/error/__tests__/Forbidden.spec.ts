import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter, createWebHistory } from 'vue-router'
import { describe, expect, it, vi } from 'vitest'
import Forbidden from '@/views/error/Forbidden.vue'

async function mountForbidden() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/source', component: { template: '<div>previous destination</div>' } },
      { path: '/403', component: Forbidden },
      { path: '/dashboard', component: { template: '<div>dashboard destination</div>' } }
    ]
  })
  await router.push('/403')
  await router.isReady()

  return { wrapper: mount(Forbidden, { global: { plugins: [router] } }), router }
}

async function mountForbiddenWithBrowserHistory(previousPath?: '/source') {
  window.history.replaceState(null, '', '/')
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/source', component: { template: '<div>previous destination</div>' } },
      { path: '/403', component: Forbidden },
      { path: '/dashboard', component: { template: '<div>dashboard destination</div>' } }
    ]
  })
  if (previousPath) await router.replace(previousPath)
  await router[previousPath ? 'push' : 'replace']('/403')
  await router.isReady()

  return { wrapper: mount(Forbidden, { global: { plugins: [router] } }), router }
}

describe('Forbidden behavior characterization', () => {
  it('does not expose internal permission keys in the user-facing explanation', async () => {
    const { wrapper } = await mountForbidden()

    expect(wrapper.text()).not.toMatch(/[a-z][a-z-]*:[a-z][a-z-]*/i)
    expect(wrapper.text()).not.toContain('permission')
  })

  it('offers a dashboard action that navigates to the existing dashboard route', async () => {
    const { wrapper, router } = await mountForbidden()

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/dashboard')
  })

  it('offers both recovery actions and returns to the actual previous page', async () => {
    const { wrapper, router } = await mountForbiddenWithBrowserHistory('/source')

    const actions = wrapper.findAll('button')
    expect(actions.map((action) => action.text())).toEqual(['返回工作台', '返回上一页'])

    await actions[1].trigger('click')
    await vi.waitFor(() => expect(router.currentRoute.value.path).toBe('/source'))

    expect(router.currentRoute.value.path).toBe('/source')
  })

  it('falls back to the dashboard when opened without an in-app history entry', async () => {
    const { wrapper, router } = await mountForbiddenWithBrowserHistory()

    await wrapper.findAll('button')[1].trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/dashboard')
  })
})
