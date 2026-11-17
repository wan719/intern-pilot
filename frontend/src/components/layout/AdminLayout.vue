<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <img class="brand-logo" :src="brandLogo" alt="InternPilot logo">
        <div>
          <strong>InternPilot</strong>
          <span>管理后台</span>
        </div>
      </div>
      <AdminSidebar :groups="adminNavGroups" :can="auth.hasPermission" />
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <div class="admin-header-title">
          <el-button
            class="admin-menu-trigger"
            :icon="Menu"
            circle
            aria-label="打开后台导航"
            @click="navigationDrawerVisible = true"
          />
          <div class="admin-location">
            <span class="admin-section-label">运营后台</span>
            <span v-if="route.meta.title" class="admin-location-divider" aria-hidden="true">/</span>
            <span v-if="route.meta.title" class="admin-location-page">{{ route.meta.title }}</span>
          </div>
        </div>
        <div class="admin-header-actions">
          <el-button :icon="Refresh" circle aria-label="刷新当前页面" @click="refreshPage" />
          <el-button class="admin-workbench-button" @click="router.push('/dashboard')">
            <img class="admin-action-logo" :src="brandLogo" alt="">
            <span>返回用户工作台</span>
          </el-button>
        </div>
      </header>

      <div class="admin-workspace">
        <router-view :key="refreshKey" />
      </div>
    </main>

    <el-drawer
      v-model="navigationDrawerVisible"
      class="admin-navigation-drawer"
      title="后台导航"
      direction="ltr"
      size="min(84vw, 320px)"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
    >
      <AdminSidebar :groups="adminNavGroups" :can="auth.hasPermission" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Menu, Refresh } from '@element-plus/icons-vue'
import AdminSidebar from './AdminSidebar.vue'
import { getCurrentUserApi } from '@/api/user'
import { adminNavGroups } from '@/config/navigation'
import { useAuthStore } from '@/stores/auth'
import brandLogo from '@/assets/brand-logo-optimized.png'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const refreshKey = ref(0)
const navigationDrawerVisible = ref(false)

function refreshPage() {
  refreshKey.value += 1
}

watch(
  () => route.fullPath,
  () => {
    navigationDrawerVisible.value = false
  }
)

onMounted(async () => {
  try {
    auth.setUser(await getCurrentUserApi())
  } catch {
    // Request interceptor handles invalid sessions.
  }
})
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 216px minmax(0, 1fr);
  min-height: 100vh;
  background: var(--color-bg);
}

.admin-sidebar {
  position: sticky;
  top: 0;
  display: grid;
  height: 100dvh;
  grid-template-rows: auto minmax(0, 1fr);
  gap: var(--space-5);
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: var(--space-5) var(--space-3);
  background: var(--color-nav);
}

.admin-brand {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  padding: 0 var(--space-2);
  color: #ffffff;
}

.admin-brand strong,
.admin-brand span {
  display: block;
}

.admin-brand strong {
  font-size: 19px;
}

.admin-brand span {
  margin-top: 3px;
  color: #cbd5e1;
  font-size: 12px;
}

.brand-logo {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: var(--radius-sm);
  box-shadow: none;
  object-fit: cover;
}

.admin-main {
  min-width: 0;
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  min-height: 60px;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: 0 var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(10px);
}

.admin-header-title,
.admin-header-actions {
  display: flex;
  align-items: center;
}

.admin-header-title {
  min-width: 0;
  gap: var(--space-2);
}

.admin-location {
  display: flex;
  min-width: 0;
  align-items: baseline;
  gap: var(--space-2);
  color: var(--color-text-muted);
  font-size: 13px;
}

.admin-section-label {
  flex: none;
}

.admin-location-page {
  overflow: hidden;
  color: var(--color-text);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-workspace :deep(.page-container) {
  padding: var(--space-5) var(--space-6) calc(var(--space-6) + var(--space-4));
}

.admin-workspace :deep(.app-page-header) {
  margin-bottom: var(--space-5);
}

.admin-workspace :deep(.app-page-title h1) {
  font-size: clamp(24px, 2.7vw, 34px);
  font-weight: 750;
  line-height: 1.2;
}

.admin-workspace :deep(.app-page-title p) {
  margin-top: var(--space-2);
  font-size: 14px;
  line-height: 1.55;
}

.admin-header-actions {
  flex: 0 0 auto;
  gap: var(--space-2);
}

.admin-menu-trigger {
  display: none;
}

.admin-workbench-button {
  gap: var(--space-2);
}

.admin-header-actions .el-button + .el-button {
  margin-left: 0;
}

.admin-action-logo {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  object-fit: cover;
}

:deep(.admin-navigation-drawer) {
  background: var(--color-nav);
}

:deep(.admin-navigation-drawer .el-drawer__header) {
  margin-bottom: 0;
  padding: var(--space-5) var(--space-5) var(--space-3);
  color: #ffffff;
}

:deep(.admin-navigation-drawer .el-drawer__title) {
  font-weight: 700;
}

:deep(.admin-navigation-drawer .el-drawer__close-btn) {
  color: #ffffff;
}

:deep(.admin-navigation-drawer .el-drawer__body) {
  padding: var(--space-3) var(--space-4) var(--space-6);
}

@media (max-width: 900px) {
  .admin-shell {
    grid-template-columns: minmax(0, 1fr);
  }

  .admin-sidebar {
    display: none;
  }

  .admin-menu-trigger {
    display: inline-flex;
  }

  .admin-menu-trigger,
  .admin-header-actions .el-button {
    min-width: 44px;
    min-height: 44px;
  }

  .admin-header {
    min-height: 60px;
    padding: 0 var(--space-4);
  }

  .admin-workspace :deep(.page-container) {
    padding-inline: var(--space-4);
  }
}

@media (max-width: 480px) {
  .admin-header {
    gap: var(--space-2);
    padding: 0 var(--space-3);
  }

  .admin-workbench-button {
    padding-right: var(--space-2);
    padding-left: var(--space-2);
  }

  .admin-workbench-button span {
    font-size: 12px;
  }

  .admin-location-divider,
  .admin-location-page {
    display: none;
  }
}
</style>
