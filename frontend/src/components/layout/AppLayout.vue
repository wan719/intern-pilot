<template>
  <div class="user-app-shell" :class="{ 'appearance-preview': settingsOpen }" :data-layout="theme.layout" :data-menu="theme.menu">
    <a class="workspace-skip" href="#main-content" @click.prevent="mainContent?.focus()">跳到主要内容</a>
    <aside class="workspace-sidebar">
      <RouterLink class="workspace-brand" to="/dashboard" aria-label="InternPilot 工作台">
        <img :src="brandLogo" alt="" width="34" height="34" />
        <strong>InternPilot</strong>
      </RouterLink>
      <JourneyNav :items="journeyItems" />
      <AppHeader ref="header" :layout="theme.layout" :mode="theme.mode" @refresh="refreshPage"
        @settings="openSettings" @mode="appearance.setMode" />
    </aside>
    <main id="main-content" ref="mainContent" class="user-app-main" tabindex="-1">
      <p v-if="storageWarning" class="workspace-storage-warning" role="status">{{ storageWarning }}</p>
      <router-view :key="refreshKey" />
    </main>
    <MobileBottomNav :items="journeyItems" />
    <AiTaskDrawer />
    <FeedbackDrawer />
    <AppearanceSettings surface="consumer" :open="settingsOpen" :value="theme" :custom="custom"
      @close="cancelSettings" @closed="header?.focusAccount()" @save="saveSettings"
      @change="appearance.change" @preset="appearance.selectPreset" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import AppHeader from './AppHeader.vue'
import AppearanceSettings from './AppearanceSettings.vue'
import JourneyNav from './JourneyNav.vue'
import MobileBottomNav from './MobileBottomNav.vue'
import brandLogo from '@/assets/brand-logo-optimized.png'
import AiTaskDrawer from '@/components/ai/AiTaskDrawer.vue'
import FeedbackDrawer from '@/components/feedback/FeedbackDrawer.vue'
import { getCurrentUserApi } from '@/api/user'
import { journeyItems } from '@/config/navigation'
import { useAuthStore } from '@/stores/auth'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'
import { appearanceAccountId, applyWorkspaceTheme, useAppearance } from '@/composables/useAppearance'
import '@/styles/resume-utilities.css'

const auth = useAuthStore()
const aiTaskCenter = useAiTaskCenterStore()
const refreshKey = ref(0)
const mainContent = ref<HTMLElement>()
const header = ref<InstanceType<typeof AppHeader>>()
const settingsOpen = ref(false)
const account = computed(() => appearanceAccountId(auth.user))
const appearance = useAppearance('consumer', account)
const { value: theme, custom, storageWarning } = appearance
applyWorkspaceTheme('consumer', appearance)

function refreshPage() { refreshKey.value += 1 }
function openSettings() {
  appearance.begin()
  settingsOpen.value = true
}
function cancelSettings() {
  appearance.cancel()
  settingsOpen.value = false
}
function saveSettings() {
  appearance.save()
  settingsOpen.value = false
}

watch(account, cancelSettings)
onMounted(async () => {
  try {
    auth.setUser(await getCurrentUserApi())
  } catch {
    // Request interceptor handles invalid sessions.
  }
  aiTaskCenter.initialize()
})
</script>

<style scoped>
.workspace-sidebar { min-width: 0; background: var(--workspace-rail); }
.workspace-brand { display: flex; align-items: center; gap: 9px; color: var(--color-text); font-size: 19px; }
.workspace-brand img { flex: none; border-radius: 8px; object-fit: cover; }
.workspace-skip { position: fixed; z-index: 80; inset: 8px auto auto 8px; padding: 8px 12px; border-radius: 8px; color: white; background: var(--workspace-action); transform: translateY(-200%); }
.workspace-skip:focus { transform: none; }
.user-app-shell { background: var(--workspace-background); }
@media (min-width: 1024px) {
  .user-app-shell:not([data-layout=top]) { display: grid; grid-template-columns: 244px minmax(0, 1fr); }
  .user-app-shell[data-layout=compact] { grid-template-columns: 74px minmax(0, 1fr); }
  .user-app-shell:not([data-layout=top]) .workspace-sidebar {
    position: sticky; top: 0; min-height: 0; height: 100dvh; display: flex; flex-direction: column;
    overflow-y: auto; overscroll-behavior: contain; padding: 22px 14px 14px; border-right: 1px solid var(--color-border);
  }
  .user-app-shell:not([data-layout=top]) .workspace-brand { padding: 5px 8px; margin-bottom: 23px; }
  .user-app-shell:not([data-layout=top])[data-menu=frosted] .workspace-sidebar {
    top: 12px; height: calc(100dvh - 24px); margin: 12px 0 12px 12px; border-radius: 20px;
  }
  .user-app-shell[data-menu=frosted] .workspace-sidebar { background: var(--workspace-glass); border: 1px solid var(--workspace-glass-edge); box-shadow: var(--workspace-glass-shadow); }
  .user-app-shell[data-layout=compact] .workspace-sidebar { padding-inline: 6px; }
  .user-app-shell[data-layout=compact] .workspace-brand { padding-inline: 0; justify-content: center; }
  .user-app-shell[data-layout=compact] .workspace-brand strong,
  .user-app-shell[data-layout=compact] :deep(.journey-nav-link span) { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
  .user-app-shell[data-layout=compact] :deep(.journey-nav-link) { justify-content: center; padding-inline: 4px; }
  .user-app-shell[data-layout=top] .workspace-sidebar { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 9px 16px; border-bottom: 1px solid var(--color-border); }
  .user-app-shell[data-layout=top] :deep(.journey-nav) { display: flex; overflow-x: auto; }
  .user-app-shell[data-layout=top] :deep(.journey-nav-link) { white-space: nowrap; padding-inline: 7px; gap: 5px; }
}
@supports (backdrop-filter: blur(1px)) {
  .user-app-shell[data-menu=frosted] .workspace-sidebar { background: var(--workspace-glass-tint); backdrop-filter: blur(17px) saturate(110%); }
}
@media (max-width: 1023px) {
  .workspace-sidebar { display: flex; justify-content: space-between; align-items: center; gap: 5px; padding: 8px 12px; border-bottom: 1px solid var(--color-border); }
  .workspace-sidebar :deep(.journey-nav) { display: none; }
}
@media (max-width: 420px) {
  .workspace-brand { gap: 5px; font-size: 15px; }
  .workspace-brand img { width: 27px; height: 27px; }
}
</style>
