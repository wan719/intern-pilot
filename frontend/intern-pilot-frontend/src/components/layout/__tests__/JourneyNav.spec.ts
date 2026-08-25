import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import JourneyNav from '@/components/layout/JourneyNav.vue'
import { journeyItems } from '@/config/navigation'

describe('JourneyNav', () => {
  it('marks 岗位机会 active for a recommendation detail route', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
    })
    await router.push('/job-recommendations/3')
    await router.isReady()

    const wrapper = mount(JourneyNav, {
      props: { items: journeyItems },
      global: { plugins: [router] }
    })

    expect(wrapper.get('[aria-current="page"]').text()).toContain('岗位机会')
  })
})
