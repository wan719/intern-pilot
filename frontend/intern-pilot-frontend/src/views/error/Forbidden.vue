<template>
  <main class="forbidden-page">
    <section class="forbidden-panel">
      <h1>403</h1>
      <p>当前账号没有访问该页面的权限。</p>
      <el-button type="primary" @click="router.push('/dashboard')">返回数据看板</el-button>
    </section>
  </main>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.forbidden-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background: #f3f6fb;
}

.forbidden-panel {
  display: grid;
  gap: 14px;
  justify-items: center;
  padding: 32px;
  text-align: center;
}

.forbidden-panel h1 {
  margin: 0;
  color: #1f2937;
  font-size: 48px;
}

.forbidden-panel p {
  margin: 0;
  color: #64748b;
}
</style>
