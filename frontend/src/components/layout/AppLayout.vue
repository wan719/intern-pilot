<template>
  <div class="user-app-shell">
    <a class="workspace-skip" href="#main-content" @click.prevent="mainContent?.focus()">跳到主要内容</a>
    <aside class="workspace-sidebar">
      <RouterLink class="workspace-brand" to="/dashboard" aria-label="InternPilot 工作台">
        <img :src="brandLogo" alt="" width="34" height="34" />
        <strong>InternPilot</strong>
      </RouterLink>
      <JourneyNav :items="journeyItems" />
    </aside>
    <div class="workspace-content">
      <AppHeader @refresh="refreshPage" />
      <main id="main-content" ref="mainContent" class="user-app-main" tabindex="-1">
        <router-view :key="refreshKey" />
      </main>
    </div>
    <MobileBottomNav :items="journeyItems" />
    <AiTaskDrawer />
    <FeedbackDrawer />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppHeader from './AppHeader.vue'
import JourneyNav from './JourneyNav.vue'
import MobileBottomNav from './MobileBottomNav.vue'
import brandLogo from '@/assets/brand-logo-optimized.png'
import AiTaskDrawer from '@/components/ai/AiTaskDrawer.vue'
import FeedbackDrawer from '@/components/feedback/FeedbackDrawer.vue'
import { getCurrentUserApi } from '@/api/user'
import { journeyItems } from '@/config/navigation'
import { useAuthStore } from '@/stores/auth'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'
import '@/styles/resume-utilities.css'

const auth = useAuthStore()
const aiTaskCenter = useAiTaskCenterStore()
const refreshKey = ref(0)
const mainContent = ref<HTMLElement>()

function refreshPage() {
  refreshKey.value += 1
}

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
.workspace-sidebar {
  display: none;
}

.workspace-content {
  min-width: 0;
}

.workspace-skip {
  position: fixed;
  z-index: 80;
  inset: var(--space-2) auto auto var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  background: var(--color-primary-hover);
  color: white;
  transform: translateY(-200%);
}

.workspace-skip:focus {
  transform: none;
}

@media (min-width: 1024px) {
  .user-app-shell {
    display: grid;
    grid-template-columns: var(--workspace-sidebar-width) minmax(0, 1fr);
  }

  .workspace-sidebar {
    position: sticky;
    top: 0;
    display: block;
    align-self: start;
    height: 100vh;
    height: 100dvh;
    overflow: auto;
    padding: var(--space-4) var(--space-3);
    border-right: 1px solid var(--color-border);
    background: var(--color-surface);
  }

  .workspace-brand {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-3);
    margin-bottom: var(--space-5);
    color: var(--color-text);
    font-size: 19px;
  }

  .workspace-brand img {
    border-radius: var(--radius-sm);
    object-fit: cover;
  }
}
</style>
