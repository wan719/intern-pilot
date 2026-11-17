<template>
  <div ref="controls" class="workspace-controls">
    <div class="workspace-tools">
      <button class="header-action" type="button" :title="aiTaskBadgeText" :aria-label="aiTaskBadgeText" @click="aiTaskCenter.openDrawer">
        <Cpu aria-hidden="true" /><span class="header-action-label">AI 任务</span>
        <span v-if="aiTaskBadgeCount" class="header-action-badge">{{ aiTaskBadgeCount }}</span>
      </button>
      <span v-if="aiProvider" class="provider-tag">{{ aiProvider === 'deepseek' ? 'DeepSeek' : 'Mock AI' }}</span>
      <button class="header-action" type="button" title="刷新当前页面" aria-label="刷新当前页面" @click="emit('refresh')">
        <Refresh aria-hidden="true" /><span class="header-action-label">刷新</span>
      </button>
    </div>

    <div class="workspace-account-tools">
      <button class="header-action" type="button" title="打开意见反馈" aria-label="打开意见反馈" @click="feedback.openDrawer">
        <Message aria-hidden="true" /><span class="header-action-label">反馈</span>
      </button>

      <PopoverRoot v-model:open="menuOpen">
        <PopoverTrigger as-child>
          <button class="account-trigger" type="button" aria-label="打开账号菜单">
            <span class="account-avatar">
              <img v-if="avatarUrl" :src="avatarUrl" alt="" />
              <span v-else>{{ avatarText }}</span>
            </span>
            <span class="account-meta">
              <strong>{{ displayName }}</strong>
              <small>账号设置</small>
            </span>
          </button>
        </PopoverTrigger>
        <PopoverPortal>
          <PopoverContent
            class="workspace-account-popover"
            aria-label="账号菜单"
            :side="layout === 'top' ? 'bottom' : 'top'"
            align="start"
            :side-offset="8"
            :collision-padding="12"
          >
            <button class="account-option" type="button" @click="go('/user/center')"><User />个人中心</button>
            <button class="account-option" type="button" @click="openSettings"><Setting />外观设置</button>
            <button v-if="adminHome" class="account-option" type="button" @click="go(adminHome)"><Monitor />管理后台</button>
            <div class="quick-appearance" role="group" aria-label="外观">
              <span class="quick-appearance-label"><Sunny />外观</span>
              <button v-for="choice in modes" :key="choice.value" type="button" class="account-option"
                :aria-pressed="mode === choice.value" @click="switchMode(choice.value)">
                <component :is="choice.icon" />{{ choice.label }}
                <Check v-if="mode === choice.value" class="mode-check" />
              </button>
            </div>
            <button class="account-option" type="button" @click="logout"><SwitchButton />退出登录</button>
          </PopoverContent>
        </PopoverPortal>
      </PopoverRoot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Check, Cpu, Message, Monitor, Moon, Refresh, Setting, Sunny, SwitchButton, User } from '@element-plus/icons-vue'
import { PopoverContent, PopoverPortal, PopoverRoot, PopoverTrigger } from 'reka-ui'
import { getAiProviderApi } from '@/api/health'
import { adminNavGroups } from '@/config/navigation'
import type { Layout, Mode } from '@/composables/useAppearance'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'
import { useAuthStore } from '@/stores/auth'
import { useFeedbackStore } from '@/stores/feedback'

withDefaults(defineProps<{ layout?: Layout; mode?: Mode }>(), { layout: 'left', mode: 'light' })
const emit = defineEmits<{ refresh: []; settings: []; mode: [value: Mode] }>()

const router = useRouter()
const auth = useAuthStore()
const aiTaskCenter = useAiTaskCenterStore()
const feedback = useFeedbackStore()
const menuOpen = ref(false)
const controls = ref<HTMLElement>()
const aiTaskBadgeCount = computed(() => aiTaskCenter.badgeCount + aiTaskCenter.cancelledTasks.length)
const aiTaskBadgeText = computed(() => {
  if (aiTaskCenter.runningTasks.length) return `AI 任务 ${aiTaskCenter.runningTasks.length} 个进行中`
  if (aiTaskCenter.failedTasks.length) return `有 ${aiTaskCenter.failedTasks.length} 个任务失败`
  if (aiTaskCenter.cancelledTasks.length) return `有 ${aiTaskCenter.cancelledTasks.length} 个任务已取消，待处理`
  if (aiTaskCenter.completedTasks.length) return `有 ${aiTaskCenter.completedTasks.length} 个结果可查看`
  return 'AI 任务中心'
})
const displayName = computed(() => auth.user?.nickname || auth.user?.username || auth.user?.email || '已登录用户')
const avatarUrl = computed(() => {
  const url = auth.user?.avatarUrl
  if (!url) return ''
  return /^https?:\/\//i.test(url) ? url : `${import.meta.env.VITE_API_BASE_URL || ''}${url}`
})
const avatarText = computed(() => String(displayName.value).slice(0, 1).toUpperCase())
const adminHome = computed(() => adminNavGroups.flatMap(group => group.items).find(item => auth.hasPermission(item.permission))?.path)
const aiProvider = ref('')
const modes = [
  { value: 'light', label: '浅色', icon: Sunny },
  { value: 'dark', label: '深色', icon: Moon },
  { value: 'system', label: '跟随系统', icon: Monitor }
] as const

function go(path: string) {
  menuOpen.value = false
  void router.push(path)
}
function logout() {
  menuOpen.value = false
  auth.logout()
  void router.replace('/')
}
function switchMode(mode: Mode) {
  emit('mode', mode)
  menuOpen.value = false
}
function openSettings() {
  menuOpen.value = false
  emit('settings')
}
defineExpose({ focusAccount: () => controls.value?.querySelector<HTMLButtonElement>('.account-trigger')?.focus({ preventScroll: true }) })

onMounted(async () => {
  try {
    const response: any = await getAiProviderApi()
    aiProvider.value = response?.provider || ''
  } catch {
    aiProvider.value = ''
  }
})
</script>

<style scoped>
.workspace-controls, .workspace-tools, .workspace-account-tools { display: flex; align-items: center; gap: 5px; min-width: 0; }
.header-action, .account-trigger {
  display: flex; align-items: center; gap: 10px; min-width: 36px; min-height: 40px; padding: 6px 9px;
  border: 0; border-radius: 8px; background: transparent; color: var(--color-text); font: inherit; cursor: pointer;
}
.header-action:hover, .account-trigger:hover { background: var(--color-primary-subtle); }
.header-action svg { width: 17px; height: 17px; flex: none; }
.header-action:focus-visible, .account-trigger:focus-visible, .account-option:focus-visible { outline: var(--focus-ring); outline-offset: -2px; }
.header-action-badge { margin-left: auto; padding: 2px 5px; background: var(--color-primary-subtle); color: var(--color-primary-hover); border-radius: 5px; font-size: 12px; font-weight: 700; }
.provider-tag { white-space: nowrap; border-radius: 5px; padding: 3px 6px; background: var(--color-primary-subtle); color: var(--color-primary-hover); font-size: 11px; }
.account-avatar { display: grid; place-items: center; width: 32px; height: 32px; flex: none; background: var(--color-primary-subtle); color: var(--color-primary-hover); border-radius: 9px; overflow: hidden; font-weight: 700; }
.account-avatar img { width: 100%; height: 100%; object-fit: cover; }
.account-meta { display: grid; text-align: start; min-width: 0; line-height: 1.3; }
.account-meta strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.account-meta small { color: var(--color-text-muted); font-size: 11px; }
:global(.workspace-account-popover) {
  z-index: 2040; padding: 6px; width: min(220px, calc(100vw - 24px));
  border: 1px solid var(--color-border); border-radius: 13px; background: var(--color-surface);
  color: var(--color-text); box-shadow: var(--shadow-floating);
}
.account-option, .quick-appearance-label { display: flex; align-items: center; gap: 8px; width: 100%; min-height: 34px; padding: 5px 8px; font: inherit; color: inherit; text-align: start; }
.account-option { border: 0; border-radius: 7px; background: transparent; cursor: pointer; }
.account-option:hover, .account-option[aria-pressed=true] { background: var(--color-primary-subtle); }
.account-option svg, .quick-appearance-label svg { width: 15px; height: 15px; flex: none; }
.mode-check { margin-left: auto; }
.quick-appearance { border-block: 1px solid var(--color-border); padding-block: 4px; }
.quick-appearance-label { font-weight: 650; }
.quick-appearance .account-option { padding-inline-start: 21px; }

@media (min-width: 1024px) {
  :global(.user-app-shell:not([data-layout=top]) .workspace-controls) { flex: 1; flex-direction: column; align-items: stretch; margin-top: 16px; min-height: 0; }
  :global(.user-app-shell:not([data-layout=top]) .workspace-tools), :global(.user-app-shell:not([data-layout=top]) .workspace-account-tools) { flex-direction: column; align-items: stretch; border-top: 1px solid var(--color-border); padding-top: 8px; }
  :global(.user-app-shell:not([data-layout=top]) .workspace-account-tools) { margin-top: auto; }
  :global(.user-app-shell:not([data-layout=top]) .provider-tag) { align-self: start; margin-left: 36px; }
  :global(.user-app-shell[data-layout=compact] :is(.account-meta, .provider-tag, .header-action-label)) { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
  :global(.user-app-shell[data-layout=compact] .header-action) { justify-content: center; padding-inline: 4px; gap: 2px; }
  :global(.user-app-shell[data-layout=compact] .account-trigger) { padding-inline: 4px; justify-content: center; }
  :global(.user-app-shell[data-layout=top] .account-meta) { display: none; }
  :global(.user-app-shell[data-layout=top] .header-action-label) { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
}
@media (max-width: 1023px) {
  .header-action-label, .account-meta, .provider-tag { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
  .header-action, .account-trigger { min-width: 40px; justify-content: center; padding-inline: 6px; }
}
</style>
