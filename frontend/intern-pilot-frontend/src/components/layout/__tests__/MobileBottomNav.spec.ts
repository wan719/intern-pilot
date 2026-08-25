import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import MobileBottomNav from '@/components/layout/MobileBottomNav.vue'
import { journeyItems } from '@/config/navigation'

describe('MobileBottomNav', () => {
  it('keeps core stages in the bar and reveals remaining journey stages from 更多', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
    })
    await router.push('/dashboard')
    await router.isReady()

    const wrapper = mount(MobileBottomNav, {
      props: { items: journeyItems },
      global: { plugins: [router] }
    })

    const coreItems = journeyItems.filter((item) => item.mobile)
    const secondaryItems = journeyItems.filter((item) => !item.mobile)

    expect(wrapper.findAll('.mobile-bottom-link')).toHaveLength(coreItems.length)
    expect(wrapper.text()).not.toContain(secondaryItems[0].label)

    await wrapper.get('button[aria-label="更多求职旅程入口"]').trigger('click')

    expect(wrapper.get('[role="dialog"]').text()).toContain(secondaryItems[0].label)
    expect(wrapper.findAll('a').map((link) => link.attributes('href'))).toEqual(
      ['/dashboard', '/jobs', '/analysis/match', '/applications', '/resumes', '/interview-questions']
    )
  })
})
