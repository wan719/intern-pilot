<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import brandLogo from '@/assets/brand-logo-optimized.png'

defineProps<{ hasSession: boolean; report?: boolean }>()

const scrolled = ref(false)
function updateScroll() {
  scrolled.value = window.scrollY > 16
}

onMounted(() => {
  updateScroll()
  window.addEventListener('scroll', updateScroll, { passive: true })
})
onUnmounted(() => window.removeEventListener('scroll', updateScroll))
</script>

<template>
  <header class="site-header" :class="{ 'is-scrolled': scrolled }">
    <div class="navigation">
      <RouterLink class="public-brand" to="/" aria-label="InternPilot 首页">
        <img :src="brandLogo" alt="" width="31" height="31" />InternPilot
      </RouterLink>
      <RouterLink v-if="report" class="return-link" to="/"><span aria-hidden="true">←</span> 返回首页</RouterLink>
      <slot v-else />
      <div class="account">
        <RouterLink v-if="hasSession" class="signup" to="/dashboard">进入工作台</RouterLink>
        <template v-else>
          <RouterLink to="/login">登录</RouterLink>
          <RouterLink class="signup" to="/register">注册</RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>
