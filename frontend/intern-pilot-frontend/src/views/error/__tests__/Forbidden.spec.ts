import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
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
    const { wrapper, router } = await mountForbidden()
    await router.replace('/source')
    await router.push('/403')

    const actions = wrapper.findAll('button')
    expect(actions.map((action) => action.text())).toEqual(['返回工作台', '返回上一页'])

    await actions[1].trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/source')
  })
})
