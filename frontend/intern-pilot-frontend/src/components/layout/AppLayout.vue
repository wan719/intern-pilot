<template>
  <div class="app-shell">
    <AppSidebar />
    <main class="app-main">
      <AppHeader @refresh="refreshPage" />
      <router-view :key="refreshKey" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'
import { getCurrentUserApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
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
})
</script>
