<template>
  <main class="forbidden-page" aria-labelledby="forbidden-title">
    <section class="forbidden-panel" aria-describedby="forbidden-description">
      <span class="forbidden-code" aria-hidden="true">403</span>
      <div class="forbidden-copy">
        <span class="eyebrow">访问受限</span>
        <h1 id="forbidden-title">这个页面暂时无法访问</h1>
        <p id="forbidden-description">当前账号无法打开此页面。你可以回到工作台继续求职流程，或返回上一页。</p>
      </div>
      <div class="forbidden-actions">
        <el-button type="primary" @click="router.push('/dashboard')">返回工作台</el-button>
        <el-button @click="router.back()">返回上一页</el-button>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>
