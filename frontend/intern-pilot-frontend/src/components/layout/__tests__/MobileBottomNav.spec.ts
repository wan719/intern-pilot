import { mount, type VueWrapper } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it } from 'vitest'
import MobileBottomNav from '@/components/layout/MobileBottomNav.vue'
import { journeyItems } from '@/config/navigation'

describe('MobileBottomNav', () => {
  let wrapper: VueWrapper | undefined

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  async function mountNavigation(path: string) {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
    })
    await router.push(path)
    await router.isReady()
    wrapper = mount(MobileBottomNav, {
      attachTo: document.body,
      props: { items: journeyItems },
      global: { plugins: [router] }
    })
    return { router, wrapper }
  }

  it('keeps core stages in the bar and reveals remaining journey stages from 更多', async () => {
    const mounted = await mountNavigation('/dashboard')

    const coreItems = journeyItems.filter((item) => item.mobile)
    const secondaryItems = journeyItems.filter((item) => !item.mobile)

    expect(mounted.wrapper.findAll('.mobile-bottom-link')).toHaveLength(coreItems.length)
    expect(mounted.wrapper.text()).not.toContain(secondaryItems[0].label)

    await mounted.wrapper.get('button[aria-label="更多求职旅程入口"]').trigger('click')

    expect(mounted.wrapper.get('[role="dialog"]').text()).toContain(secondaryItems[0].label)
    expect(mounted.wrapper.findAll('a').map((link) => link.attributes('href'))).toEqual(
      ['/dashboard', '/jobs', '/analysis/match', '/applications', '/resumes', '/interview-questions']
    )
  })

  it('moves focus into 更多, traps Tab in both directions, closes on Escape, and restores focus', async () => {
    const mounted = await mountNavigation('/dashboard')
    const moreButton = mounted.wrapper.get('button[aria-label="更多求职旅程入口"]')
    const moreButtonElement = moreButton.element as HTMLButtonElement
    moreButtonElement.focus()

    await moreButton.trigger('click')
    await nextTick()

    const dialog = mounted.wrapper.get('[role="dialog"]')
    expect(dialog.element.contains(document.activeElement)).toBe(true)

    const focusable = dialog.findAll('a[href], button:not([disabled])')
    const first = focusable[0].element as HTMLElement
    const last = focusable[focusable.length - 1].element as HTMLElement

    last.focus()
    const tab = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true })
    last.dispatchEvent(tab)
    expect(tab.defaultPrevented).toBe(true)
    expect(document.activeElement).toBe(first)

    first.focus()
    const shiftTab = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true, cancelable: true })
    first.dispatchEvent(shiftTab)
    expect(shiftTab.defaultPrevented).toBe(true)
    expect(document.activeElement).toBe(last)

    last.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', bubbles: true, cancelable: true }))
    await nextTick()

    expect(moreButton.attributes('aria-expanded')).toBe('false')
    expect(document.activeElement).toBe(moreButtonElement)
  })

  it.each([
    ['/resumes', '简历中心', '简历'],
    ['/interview-questions/8', '面试准备', '面试']
  ])('exposes the current secondary stage from 更多 on %s', async (path, label, shortLabel) => {
    const mounted = await mountNavigation(path)
    const moreButton = mounted.wrapper.get('.mobile-bottom-more')

    expect(moreButton.attributes('aria-current')).toBe('page')
    expect(moreButton.attributes('aria-label')).toContain(`当前阶段：${label}`)
    expect(moreButton.text()).toContain(shortLabel)
  })
})
