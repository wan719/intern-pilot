<template>
  <header class="app-header">
    <div>
      <span class="eyebrow">InternPilot</span>
      <h2>{{ title }}</h2>
    </div>
    <div class="header-user">
      <el-tag v-if="aiProvider" size="small" :type="aiProvider === 'deepseek' ? '' : 'warning'" effect="plain">
        {{ aiProvider === 'deepseek' ? 'DeepSeek' : 'Mock AI' }}
      </el-tag>
      <el-button :icon="Refresh" circle @click="$emit('refresh')" />
      <span>{{ auth.user?.username || '已登录用户' }}</span>
      <el-button type="primary" plain @click="logout">退出</el-button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getAiProviderApi } from '@/api/health'

defineEmits<{ refresh: [] }>()

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const title = computed(() => route.meta.title || '数据看板')
const aiProvider = ref('')

function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(async () => {
  try {
    const res: any = await getAiProviderApi()
    aiProvider.value = res?.provider || ''
  } catch {
    aiProvider.value = ''
  }
})
</script>
