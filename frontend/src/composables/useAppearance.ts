import { computed, onScopeDispose, ref, watch, type Ref } from 'vue'

export type Surface = 'consumer' | 'admin'
export type Mode = 'light' | 'dark' | 'system'
export type Preset = 'clario' | 'liquid'
export type Layout = 'left' | 'compact' | 'top' | 'mixed' | 'dual'
export type MenuStyle = 'solid' | 'frosted' | 'auto' | 'design' | 'light' | 'dark'
export type Accent = 'teal' | 'clay' | 'blue' | 'violet' | 'orange' | 'rose' | 'graphite'

export interface Appearance {
  mode: Mode
  preset: Preset
  layout: Layout
  menu: MenuStyle
  accent: Accent
}

export const presets = {
  clario: { layout: 'left', menu: 'solid', accent: 'clay' },
  liquid: { layout: 'left', menu: 'frosted', accent: 'teal' },
} as const

const palettes = {
  consumer: { layouts: ['left', 'compact', 'top'], menus: ['solid', 'frosted'], accents: ['teal', 'clay', 'blue', 'violet'] },
  admin: { layouts: ['left', 'top', 'mixed', 'dual'], menus: ['auto', 'design', 'dark', 'light'], accents: ['blue', 'teal', 'violet', 'orange', 'rose', 'graphite'] },
}

function defaults(surface: Surface): Appearance {
  return surface === 'consumer'
    ? { mode: 'light', preset: 'clario', ...presets.clario }
    : { mode: 'light', preset: 'clario', layout: 'left', menu: 'auto', accent: 'blue' }
}

function recover(surface: Surface, key: string | null): Appearance {
  if (!key) return defaults(surface)
  try {
    const saved = JSON.parse(localStorage.getItem(key) ?? 'null')
    const settings = palettes[surface]
    if (saved && ['light', 'dark', 'system'].includes(saved.mode)
      && ['clario', 'liquid'].includes(saved.preset)
      && settings.layouts.includes(saved.layout) && settings.menus.includes(saved.menu)
      && settings.accents.includes(saved.accent)) {
      return { mode: saved.mode, preset: saved.preset, layout: saved.layout, menu: saved.menu, accent: saved.accent }
    }
  } catch {
    // Missing, inaccessible or invalid storage uses the defaults.
  }
  return defaults(surface)
}

export function appearanceAccountId(user: { userId?: number; id?: number } | null | undefined) {
  return user?.userId ?? user?.id ?? null
}

export function useAppearance(surface: Surface, account: Readonly<Ref<number | null>>) {
  const key = computed(() => account.value == null ? null : `internpilot:appearance:v1:${surface}:${account.value}`)
  const saved = ref<Appearance>(defaults(surface))
  const draft = ref<Appearance | null>(null)
  const storageWarning = ref('')
  const value = computed(() => draft.value ?? saved.value)
  const media = typeof window.matchMedia === 'function' ? window.matchMedia('(prefers-color-scheme: dark)') : null
  const systemDark = ref(media?.matches ?? false)
  const updateSystem = () => { systemDark.value = media?.matches ?? false }
  media?.addEventListener('change', updateSystem)
  onScopeDispose(() => media?.removeEventListener('change', updateSystem))

  watch(key, current => {
    saved.value = recover(surface, current)
    draft.value = null
    storageWarning.value = ''
  }, { immediate: true, flush: 'sync' })

  function commit(next: Appearance) {
    saved.value = { ...next }
    draft.value = null
    if (!key.value) return
    try {
      localStorage.setItem(key.value, JSON.stringify(saved.value))
      storageWarning.value = ''
    } catch {
      storageWarning.value = '设置已临时应用，刷新后无法保留。'
    }
  }

  const dark = computed(() => value.value.mode === 'dark' || (value.value.mode === 'system' && systemDark.value))
  const custom = computed(() => {
    const { mode: _mode, preset, ...options } = value.value
    const base = presets[preset]
    return options.layout !== base.layout || options.menu !== base.menu || options.accent !== base.accent
  })

  return {
    value, dark, custom, storageWarning,
    begin: () => { draft.value = { ...saved.value } },
    cancel: () => { draft.value = null },
    change: (options: Partial<Appearance>) => { if (draft.value) draft.value = { ...draft.value, ...options } },
    selectPreset: (preset: Preset) => { if (draft.value) draft.value = { ...draft.value, ...presets[preset], preset } },
    save: () => { if (draft.value) commit(draft.value) },
    setMode: (mode: Mode) => commit({ ...saved.value, mode }),
  }
}

let themeOwner: symbol | undefined

export function applyWorkspaceTheme(surface: Surface, appearance: ReturnType<typeof useAppearance>) {
  const owner = Symbol(surface)
  const root = document.documentElement
  watch([appearance.value, appearance.dark], ([value, dark]) => {
    themeOwner = owner
    root.dataset.workspace = surface
    root.dataset.workspaceTone = dark ? 'dark' : 'light'
    root.dataset.workspacePreset = value.preset
    root.dataset.workspaceAccent = value.accent
  }, { immediate: true })
  onScopeDispose(() => {
    if (themeOwner !== owner) return
    delete root.dataset.workspace
    delete root.dataset.workspaceTone
    delete root.dataset.workspacePreset
    delete root.dataset.workspaceAccent
    themeOwner = undefined
  })
}
