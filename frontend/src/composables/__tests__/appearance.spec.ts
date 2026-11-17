import { computed, effectScope, ref } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useAppearance } from '../useAppearance'

const scopes: ReturnType<typeof effectScope>[] = []
const query = new EventTarget() as MediaQueryList
function open(surface: 'consumer' | 'admin', account = ref<number | null>(1)) {
  const scope = effectScope()
  scopes.push(scope)
  return scope.run(() => useAppearance(surface, computed(() => account.value)))!
}

beforeEach(() => {
  localStorage.clear()
  Object.defineProperty(query, 'matches', { value: false, configurable: true })
  vi.stubGlobal('matchMedia', vi.fn(() => query))
})
afterEach(() => {
  scopes.splice(0).forEach(scope => scope.stop())
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

describe('workspace appearance', () => {
  it('previews a preset and discards unsaved changes', () => {
    const theme = open('consumer')
    expect(theme.value.value.preset).toBe('clario')
    theme.begin()
    theme.selectPreset('liquid')
    expect(theme.value.value).toMatchObject({ preset: 'liquid', layout: 'left', menu: 'frosted', accent: 'teal' })
    theme.change({ accent: 'violet' })
    expect(theme.custom.value).toBe(true)
    theme.selectPreset('liquid')
    expect(theme.custom.value).toBe(false)
    theme.cancel()
    expect(theme.value.value.preset).toBe('clario')
  })

  it('preserves mode, restores saved options and isolates account and surface', () => {
    const account = ref<number | null>(1)
    const theme = open('consumer', account)
    theme.setMode('dark')
    theme.begin()
    theme.selectPreset('liquid')
    expect(theme.value.value.mode).toBe('dark')
    theme.change({ layout: 'compact' })
    theme.save()
    expect(open('consumer', account).value.value).toMatchObject({ preset: 'liquid', layout: 'compact' })
    expect(open('admin', account).value.value.mode).toBe('light')
    account.value = 2
    expect(theme.value.value.preset).toBe('clario')
    account.value = 1
    expect(theme.value.value.mode).toBe('dark')
    expect(theme.value.value.layout).toBe('compact')
  })

  it('responds to system mode and retains a temporary selection when storage fails', () => {
    const theme = open('admin')
    theme.setMode('system')
    expect(theme.dark.value).toBe(false)
    Object.defineProperty(query, 'matches', { value: true, configurable: true })
    query.dispatchEvent(new Event('change'))
    expect(theme.dark.value).toBe(true)
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('storage denied') })
    theme.setMode('light')
    expect(theme.dark.value).toBe(false)
    expect(theme.storageWarning.value).toContain('刷新')
  })
})
