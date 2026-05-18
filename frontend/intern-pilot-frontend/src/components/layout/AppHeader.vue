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
      <el-dropdown trigger="click" @command="handleCommand">
        <el-button plain>
          <el-icon><User /></el-icon>
          <span>{{ displayName }}</span>
          <el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Refresh, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getAiProviderApi } from '@/api/health'

defineEmits<{ refresh: [] }>()

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const title = computed(() => route.meta.title || '数据看板')
const displayName = computed(() => auth.user?.nickname || auth.user?.username || auth.user?.email || '已登录用户')
const aiProvider = ref('')

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/user/center')
    return
  }
  if (command === 'logout') {
    auth.logout()
    router.push('/login')
  }
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
