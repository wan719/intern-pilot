<template>
  <el-drawer
    :model-value="open"
    title="外观设置"
    size="min(100vw, 352px)"
    append-to-body
    :class="['workspace-settings', 'workspace-settings-' + surface]"
    modal-class="workspace-settings-mask"
    @update:model-value="emit('close')"
    @close-auto-focus="restoreFocus"
  >
    <div class="setting-groups">
      <section v-for="group in groups" :key="group.key" class="setting-group">
        <h3 :id="'appearance-' + group.key">{{ group.label }}</h3>
        <div class="setting-options" role="group" :aria-labelledby="'appearance-' + group.key">
          <button
            v-for="option in group.options"
            :key="option.value"
            type="button"
            class="setting-option"
            :class="'setting-' + group.key"
            :aria-pressed="selection(group.key) === option.value"
            @click="select(group.key, option.value)"
          >
            <span
              v-if="group.key === 'accent'"
              class="settings-swatch"
              :data-color="option.value"
              aria-hidden="true"
            ><Check v-if="value.accent === option.value" /></span>
            <span v-else class="settings-preview" :data-choice="option.value" aria-hidden="true">
              <i /><i /><i />
            </span>
            <span class="settings-label">{{ option.label }}</span>
          </button>
        </div>
        <small v-if="group.key === 'preset' && custom" class="custom-label">自定义</small>
      </section>
    </div>
    <template #footer>
      <button class="save-settings" type="button" @click="emit('save')">保存设置</button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElDrawer } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import type { Appearance, Preset, Surface } from '@/composables/useAppearance'

const props = defineProps<{ open: boolean; value: Appearance; surface: Surface; custom?: boolean }>()
const emit = defineEmits<{
  close: []
  closed: []
  save: []
  change: [value: Partial<Appearance>]
  preset: [value: Preset]
}>()

type Key = keyof Appearance
type Option = { value: string; label: string }
type Group = { key: Key; label: string; options: Option[] }

function options(keys: string[], labels: string[]): Option[] {
  return keys.map((value, index) => ({ value, label: labels[index] }))
}

const groups = computed<Group[]>(() => {
  const mode: Group = { key: 'mode', label: '系统外观', options: options(['light','dark','system'], ['浅色','深色','跟随系统']) }
  if (props.surface === 'consumer') return [
    mode,
    { key: 'preset', label: '主题风格', options: options(['liquid','clario'], ['LiquidGlass','Clario']) },
    { key: 'layout', label: '菜单布局', options: options(['left','compact','top'], ['左侧','紧凑侧栏','顶部']) },
    { key: 'menu', label: '菜单风格', options: options(['solid','frosted'], ['实色','磨砂']) },
    { key: 'accent', label: '系统主题色', options: options(['teal','clay','blue','violet'], ['青绿','陶土橙','蓝色','紫色']) },
  ]
  return [
    mode,
    { key: 'layout', label: '菜单布局', options: options(['left','top','mixed','dual'], ['左侧','顶部','混合','双列']) },
    { key: 'menu', label: '菜单风格', options: options(['auto','design','dark','light'], ['跟随外观','设计风格','深色','浅色']) },
    { key: 'accent', label: '系统主题色', options: options(['blue','teal','violet','orange','rose','graphite'], ['蓝色','青绿','紫色','橙色','玫红','石墨']) },
  ]
})

function restoreFocus() {
  requestAnimationFrame(() => emit('closed'))
}

function selection(key: Key) {
  if (key === 'preset' && props.custom) return undefined
  return props.value[key]
}

function select(key: Key, value: string) {
  if (key === 'preset') emit('preset', value as Preset)
  else emit('change', { [key]: value })
}
</script>

<style scoped>
.setting-groups { display: grid; }
.setting-group { padding: 14px 0; border-bottom: 1px solid var(--color-border); }
.setting-group:first-child { padding-top: 0; }
.setting-group h3 { margin: 0 0 10px; font-size: 14px; }
.setting-options { display: flex; gap: 8px; }
.setting-option {
  display: grid;
  align-content: start;
  justify-items: center;
  gap: 7px;
  flex: 1;
  min-width: 0;
  border: 0;
  background: none;
  color: var(--color-text-muted);
  padding: 2px;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
}
.setting-option[aria-pressed=true] { color: var(--color-primary-hover); font-weight: 700; }
.setting-option:focus-visible, .save-settings:focus-visible { outline: var(--focus-ring); outline-offset: 2px; }
.settings-preview {
  --tile: #eff3fb;
  --rail: #ccd5e6;
  --line: #dbe2ee;
  width: 100%;
  height: 42px;
  display: grid;
  grid-template: 7px 1fr / 1fr 2fr;
  gap: 4px;
  padding: 5px;
  background: var(--tile);
  border: 1px solid var(--color-border);
  border-radius: 7px;
}
.settings-preview i { border-radius: 2px; background: var(--line); }
.settings-preview i:first-child { grid-row: span 2; background: var(--rail); }
.setting-option[aria-pressed=true] .settings-preview { outline: 2px solid var(--color-primary); outline-offset: -1px; }
.settings-preview[data-choice=dark] { --tile:#182437; --rail:#51647e; --line:#394b64; }
.settings-preview:is([data-choice=system], [data-choice=auto]) { --tile:linear-gradient(120deg,#eef1f7 55%,#253953 55%); }
.settings-preview[data-choice=top] { grid-template-columns: 1fr; }
.settings-preview[data-choice=top] i:first-child { grid-row: auto; }
.settings-preview[data-choice=top] i:last-child { display: none; }
.settings-preview[data-choice=compact] { grid-template-columns: 1fr 4fr; }
.settings-preview[data-choice=mixed] { grid-template: 7px 1fr / 1fr 2fr; }
.settings-preview[data-choice=mixed] i:first-child { grid-area: 1 / 1 / 2 / 3; }
.settings-preview[data-choice=dual] { grid-template: 1fr / 1fr 1fr 2fr; }
.settings-preview[data-choice=dual] i:first-child { grid-row: auto; }
.settings-preview[data-choice=design] { --rail:#b6c9fa; }
.settings-preview[data-choice=solid] { --tile:#f2ece5; --rail:#ddd0c3; }
.settings-preview:is([data-choice=frosted],[data-choice=liquid]) { --tile:linear-gradient(130deg,#c7dce6,#f3f9fd); --rail:#f4fbff; --line:#eaf5f9; }
.settings-preview[data-choice=clario] { --tile:#f7ede1; --rail:#e4d7ca; --line:#edccb4; }
.setting-preset .settings-preview { height: 65px; }
.settings-swatch { display: grid; place-items: center; width: 27px; height: 27px; border-radius: 50%; background: var(--swatch); color: white; }
.settings-swatch svg { width: 15px; height: 15px; }
.setting-option[aria-pressed=true] .settings-swatch { outline: 2px solid var(--color-primary); outline-offset: 2px; }
.settings-swatch[data-color=blue] { --swatch:#3269dc; }
.settings-swatch[data-color=teal] { --swatch:#0f766e; }
.settings-swatch[data-color=clay] { --swatch:#a94518; }
.settings-swatch[data-color=violet] { --swatch:#7352cc; }
.settings-swatch[data-color=orange] { --swatch:#b75020; }
.settings-swatch[data-color=rose] { --swatch:#bd4779; }
.settings-swatch[data-color=graphite] { --swatch:#475569; }
.custom-label { display: inline-block; margin-top: 8px; color: var(--color-text-muted); }
.save-settings { cursor: pointer; border: 0; border-radius: 8px; padding: 10px 15px; color: white; background: var(--workspace-action); font: inherit; font-weight: 700; }
</style>
