<template>
  <div class="admin-shell" :class="{ 'appearance-preview': settingsOpen }" :data-layout="theme.layout">
    <aside class="admin-sidebar" :data-menu-tone="menuTone">
      <div class="admin-brand">
        <img class="brand-logo" :src="brandLogo" alt="">
        <div><strong>InternPilot</strong><span>管理后台</span></div>
      </div>
      <div v-if="theme.layout === 'dual'" class="admin-dual-nav">
        <nav aria-label="后台分组" class="admin-group-nav">
          <button v-for="group in visibleGroups" :key="group.label" type="button"
            :aria-pressed="selectedGroup?.label === group.label" @click="groupLabel = group.label">{{ group.label }}</button>
        </nav>
        <AdminSidebar :groups="selectedGroup ? [selectedGroup] : []" :can="auth.hasPermission" />
      </div>
      <AdminSidebar v-else :groups="theme.layout === 'mixed' ? (selectedGroup ? [selectedGroup] : []) : adminNavGroups"
        :can="auth.hasPermission" />
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <div class="admin-header-title">
          <el-button class="admin-menu-trigger" :icon="Menu" circle aria-label="打开后台导航" @click="navigationDrawerVisible = true" />
          <div class="admin-location">
            <span class="admin-section-label">运营后台</span>
            <span v-if="route.meta.title" class="admin-location-divider" aria-hidden="true">/</span>
            <span v-if="route.meta.title" class="admin-location-page">{{ route.meta.title }}</span>
          </div>
        </div>
        <div class="admin-header-actions">
          <el-button :icon="dark ? Sunny : Moon" circle :aria-label="dark ? '切换到浅色' : '切换到深色'"
            @click="appearance.setMode(dark ? 'light' : 'dark')" />
          <el-button ref="settingsButton" :icon="Setting" circle aria-label="外观设置"
            :aria-expanded="settingsOpen" @click="openSettings" />
          <el-button :icon="Refresh" circle aria-label="刷新当前页面" @click="refreshPage" />
          <el-button class="admin-workbench-button" aria-label="返回用户工作台" @click="router.push('/dashboard')">
            <img class="admin-action-logo" :src="brandLogo" alt="">
            <span>返回用户工作台</span>
          </el-button>
        </div>
      </header>

      <nav v-if="theme.layout === 'top' || theme.layout === 'mixed'" aria-label="后台分组"
        class="admin-group-nav horizontal" :data-menu-tone="menuTone">
        <template v-for="group in visibleGroups" :key="group.label">
          <el-dropdown v-if="theme.layout === 'top'" trigger="click" @command="navigate">
            <button type="button" :aria-pressed="group.items.some(item => item.path === route.path)">
              {{ group.label }} <ArrowDown aria-hidden="true" />
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in group.items" :key="item.path" :command="item.path">{{ item.label }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <button v-else type="button" :aria-pressed="selectedGroup?.label === group.label"
            @click="groupLabel = group.label">{{ group.label }}</button>
        </template>
      </nav>

      <nav v-if="tabs.length" ref="tabBar" class="admin-tabs" aria-label="工作页签">
        <div v-for="tab in tabs" :key="tab.path" class="admin-tab" :class="{ 'is-active': route.path === tab.path }">
          <RouterLink :to="tab.path" :aria-current="route.path === tab.path ? 'page' : undefined">{{ tab.label }}</RouterLink>
          <span v-if="tab.path === dashboard || visiblePages.length === 1" class="tab-pin" title="固定页签" aria-label="固定页签"><Lock /></span>
          <button v-else class="tab-close" type="button" :aria-label="'关闭' + tab.label" @click="closeTab(tab.path, $event)"><Close aria-hidden="true" /></button>
        </div>
      </nav>

      <p v-if="storageWarning" class="workspace-storage-warning" role="status">{{ storageWarning }}</p>
      <div class="admin-workspace"><router-view :key="refreshKey" /></div>
    </main>

    <el-drawer v-model="navigationDrawerVisible" class="admin-navigation-drawer" title="后台导航" direction="ltr"
      size="min(84vw, 320px)" :close-on-click-modal="true" :close-on-press-escape="true">
      <div :data-menu-tone="menuTone"><AdminSidebar :groups="adminNavGroups" :can="auth.hasPermission" /></div>
    </el-drawer>
    <AppearanceSettings surface="admin" :open="settingsOpen" :value="theme"
      @close="cancelSettings" @closed="settingsButton?.ref?.focus()" @save="saveSettings" @change="appearance.change" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Close, Lock, Menu, Moon, Refresh, Setting, Sunny } from '@element-plus/icons-vue'
import type { ButtonInstance } from 'element-plus'
import AppearanceSettings from './AppearanceSettings.vue'
import AdminSidebar from './AdminSidebar.vue'
import { closeAdminTab, syncAdminTabs, useAdminWorkTabs } from './adminWorkTabs'
import { getCurrentUserApi } from '@/api/user'
import { adminNavGroups } from '@/config/navigation'
import { useAuthStore } from '@/stores/auth'
import { appearanceAccountId, applyWorkspaceTheme, useAppearance } from '@/composables/useAppearance'
import brandLogo from '@/assets/brand-logo-optimized.png'

const dashboard = '/admin/dashboard'
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const workTabs = useAdminWorkTabs()
const refreshKey = ref(0)
const navigationDrawerVisible = ref(false)
const settingsOpen = ref(false)
const settingsButton = ref<ButtonInstance>()
const tabBar = ref<HTMLElement>()
const groupLabel = ref('')
const account = computed(() => appearanceAccountId(auth.user))
const appearance = useAppearance('admin', account)
const { value: theme, dark, storageWarning } = appearance
applyWorkspaceTheme('admin', appearance)

const menuTone = computed(() => theme.value.menu === 'auto' ? (dark.value ? 'dark' : 'light') : theme.value.menu)
const visibleGroups = computed(() => adminNavGroups
  .map(group => ({ ...group, items: group.items.filter(item => auth.hasPermission(item.permission)) }))
  .filter(group => group.items.length))
const visiblePages = computed(() => visibleGroups.value.flatMap(group => group.items))
const selectedGroup = computed(() => visibleGroups.value.find(group => group.label === groupLabel.value) ?? visibleGroups.value[0])
const tabs = computed(() => workTabs.paths.flatMap(path => {
  const item = visiblePages.value.find(page => page.path === path)
  return item ? [item] : []
}))

function refreshPage() { refreshKey.value += 1 }
function navigate(path: string) { void router.push(path) }
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

async function closeTab(path: string, event: MouseEvent) {
  const restoreFocus = event.currentTarget === document.activeElement
  const active = route.path
  const next = closeAdminTab(workTabs.paths, path, active)
  if (path === dashboard || !workTabs.paths.includes(path)) return
  if (path === active) {
    const target = next.target ?? visiblePages.value.find(item => item.path !== path)?.path
    if (!target || target === active) return
    await router.push(target)
    if (route.path === active) return
  }
  workTabs.paths = workTabs.paths.filter(item => item !== path)
  if (restoreFocus) {
    await nextTick()
    tabBar.value?.querySelector<HTMLAnchorElement>('a[aria-current=page]')?.focus()
  }
}

watch([() => route.path, visiblePages, account], ([path, pages]) => {
  workTabs.paths = syncAdminTabs(workTabs.paths, path, pages)
  const currentGroup = visibleGroups.value.find(group => group.items.some(item => item.path === path))
  if (currentGroup) groupLabel.value = currentGroup.label
}, { immediate: true })

watch(() => route.fullPath, () => { navigationDrawerVisible.value = false })
watch(account, cancelSettings)
onMounted(async () => {
  try {
    auth.setUser(await getCurrentUserApi())
  } catch {
    // Request interceptor handles invalid sessions.
  }
})
</script>

<style scoped>
.admin-shell { display: grid; grid-template-columns: 222px minmax(0, 1fr); min-height: 100vh; background: var(--color-bg); }
.admin-shell[data-layout=top] { grid-template-columns: minmax(0, 1fr); }
.admin-shell[data-layout=top] .admin-sidebar { display: none; }
.admin-shell[data-layout=dual] { grid-template-columns: 285px minmax(0, 1fr); }
.admin-sidebar { position: sticky; top: 0; align-self: start; display: grid; align-content: start; gap: 22px; height: 100dvh; overflow-y: auto; padding: 20px 13px; border-right: 1px solid var(--admin-menu-border); background: var(--admin-menu-background); }
.admin-brand { display: flex; align-items: center; gap: 11px; padding: 0 5px; color: var(--admin-menu-title); }
.admin-brand strong, .admin-brand span { display: block; }
.admin-brand strong { font-size: 19px; }
.admin-brand span { margin-top: 3px; color: var(--admin-menu-muted); font-size: 12px; }
.brand-logo { width: 34px; height: 34px; border-radius: 8px; }
.admin-main { min-width: 0; }
.admin-header { position: sticky; top: 0; z-index: 10; display: flex; min-height: 56px; align-items: center; justify-content: space-between; gap: 10px; padding: 0 25px; border-bottom: 1px solid var(--color-border); background: var(--color-surface); }
.admin-header-title, .admin-header-actions { display: flex; align-items: center; gap: 8px; }
.admin-header-title { min-width: 0; }
.admin-header-actions { flex: 0 0 auto; }
.admin-location { display: flex; min-width: 0; align-items: baseline; gap: 8px; color: var(--color-text-muted); font-size: 13px; }
.admin-section-label { flex: none; }
.admin-location-page { overflow: hidden; color: var(--color-text); font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.admin-header-actions .el-button + .el-button { margin-left: 0; }
.admin-workbench-button { gap: 8px; }
.admin-action-logo { width: 20px; height: 20px; border-radius: 5px; object-fit: cover; }
.admin-menu-trigger { display: none; }
.admin-tabs { display: flex; align-items: center; gap: 8px; min-height: 42px; overflow-x: auto; padding: 6px 25px; background: var(--color-surface); border-bottom: 1px solid var(--color-border); }
.admin-tab { display: flex; align-items: center; gap: 4px; flex: none; min-height: 30px; padding: 0 6px 0 9px; border: 1px solid var(--color-border); border-radius: 6px; color: var(--color-text-muted); }
.admin-tab a { display: flex; align-items: center; min-height: 29px; color: inherit; font-size: 12px; }
.admin-tab.is-active { border-color: transparent; background: var(--color-primary-subtle); color: var(--color-primary-hover); font-weight: 700; }
.tab-close, .tab-pin { display: grid; place-items: center; width: 23px; height: 25px; padding: 0; border: 0; border-radius: 4px; background: transparent; color: inherit; }
.tab-close { cursor: pointer; }
.tab-close:hover { background: var(--color-surface-muted); }
.tab-close svg, .tab-pin svg { width: 12px; height: 12px; }
.admin-tab :is(a,button):focus-visible, .admin-group-nav button:focus-visible { outline: var(--focus-ring); outline-offset: -2px; }
.admin-group-nav { display: grid; gap: 5px; align-content: start; }
.admin-group-nav.horizontal { display: flex; overflow-x: auto; padding: 6px 25px; background: var(--admin-menu-background); border-bottom: 1px solid var(--admin-menu-border); }
.admin-group-nav button { display: flex; align-items: center; gap: 6px; min-height: 38px; padding: 6px 10px; white-space: nowrap; border: 0; border-radius: 6px; background: transparent; color: var(--admin-menu-text); cursor: pointer; font: inherit; font-size: 13px; }
.admin-group-nav button svg { width: 12px; height: 12px; }
.admin-group-nav button[aria-pressed=true] { color: var(--admin-menu-active-text); background: var(--admin-menu-active); }
.admin-dual-nav { display: grid; grid-template-columns: 90px minmax(0, 1fr); gap: 12px; }
.admin-dual-nav .admin-group-nav { border-right: 1px solid var(--admin-menu-border); padding-right: 6px; }
.admin-workspace :deep(.page-container) { padding: 20px 25px 36px; }
.admin-workspace :deep(.app-page-title h1) { font-size: clamp(24px, 2.7vw, 32px); }
@media (max-width: 900px) {
  .admin-shell[data-layout] { grid-template-columns: minmax(0, 1fr); }
  .admin-sidebar, .admin-group-nav.horizontal { display: none; }
  .admin-menu-trigger { display: inline-flex; }
  .admin-header, .admin-tabs { padding-inline: 12px; }
  .admin-menu-trigger, .admin-header-actions .el-button { min-width: 42px; min-height: 42px; }
  .admin-workspace :deep(.page-container) { padding-inline: 15px; }
}
@media (max-width: 560px) {
  .admin-workbench-button { padding-inline: 8px; }
  .admin-location-divider, .admin-location-page, .admin-workbench-button span { display: none; }
}
</style>
