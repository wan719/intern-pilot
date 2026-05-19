<template>
  <div class="app-shell">
    <AppSidebar />
    <main class="app-main">
      <AppHeader @refresh="refreshPage" />
      <router-view :key="refreshKey" />
    </main>
    <AiTaskFloat v-if="auth.isLoggedIn" />
    <AiTaskDrawer />
    <FeedbackFloat v-if="auth.isLoggedIn" />
    <FeedbackDrawer />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
import AiTaskFloat from '@/components/ai/AiTaskFloat.vue'
import AiTaskDrawer from '@/components/ai/AiTaskDrawer.vue'
import FeedbackFloat from '@/components/feedback/FeedbackFloat.vue'
import FeedbackDrawer from '@/components/feedback/FeedbackDrawer.vue'
import { getCurrentUserApi } from '@/api/user'
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
