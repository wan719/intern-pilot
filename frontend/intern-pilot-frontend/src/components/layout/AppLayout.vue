<template>
  <div class="user-app-shell">
    <AppHeader @refresh="refreshPage" />
    <main id="main-content" class="user-app-main">
      <router-view :key="refreshKey" />
    </main>
    <MobileBottomNav :items="journeyItems" />
    <AiTaskDrawer />
    <FeedbackDrawer />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppHeader from './AppHeader.vue'
import MobileBottomNav from './MobileBottomNav.vue'
import AiTaskDrawer from '@/components/ai/AiTaskDrawer.vue'
import FeedbackDrawer from '@/components/feedback/FeedbackDrawer.vue'
import { getCurrentUserApi } from '@/api/user'
import { journeyItems } from '@/config/navigation'
import { useAuthStore } from '@/stores/auth'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'

const auth = useAuthStore()
const aiTaskCenter = useAiTaskCenterStore()
const refreshKey = ref(0)

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
