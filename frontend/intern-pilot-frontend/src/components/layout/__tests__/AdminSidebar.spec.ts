import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { ElDrawer } from 'element-plus'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import AdminSidebar from '@/components/layout/AdminSidebar.vue'
import { adminNavGroups } from '@/config/navigation'

vi.mock('@/api/user', () => ({
  getCurrentUserApi: vi.fn().mockResolvedValue({
    id: 7,
    username: 'admin-user',
    permissions: ['admin:dashboard', 'user:read']
  })
}))

describe('AdminSidebar', () => {
  it('only renders navigation items allowed by the supplied permission checker', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
    })
    await router.push('/admin/users')
    await router.isReady()

    const wrapper = mount(AdminSidebar, {
      props: {
        groups: adminNavGroups,
        can: (permission: string) => permission === 'user:read'
      },
      global: { plugins: [router] }
    })

    expect(wrapper.text()).toContain('用户管理')
    expect(wrapper.text()).not.toContain('角色管理')
    expect(wrapper.text()).not.toContain('RAG 知识库')
    expect(wrapper.text()).not.toContain('后台概览')
  })

  it('marks the current permitted route as the active navigation item', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
    })
    await router.push('/admin/users')
    await router.isReady()

    const wrapper = mount(AdminSidebar, {
      props: { groups: adminNavGroups, can: () => true },
      global: { plugins: [router] }
    })

    expect(wrapper.get('[aria-current="page"]').text()).toContain('用户管理')
  })
})

describe('AdminLayout mobile navigation', () => {
  let wrapper: VueWrapper | undefined

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  it('closes the mobile drawer after navigation changes', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/admin/dashboard', component: { template: '<div>Dashboard</div>' } },
        { path: '/admin/users', component: { template: '<div>Users</div>' } }
      ]
    })
    await router.push('/admin/dashboard')
    await router.isReady()

    wrapper = mount(AdminLayout, {
      attachTo: document.body,
      global: { plugins: [createPinia(), router] }
    })
    await flushPromises()

    await wrapper.get('button[aria-label="打开后台导航"]').trigger('click')
    expect(wrapper.findComponent(ElDrawer).props('modelValue')).toBe(true)

    await router.push('/admin/users')
    await flushPromises()

    expect(wrapper.findComponent(ElDrawer).props('modelValue')).toBe(false)
  })
})
