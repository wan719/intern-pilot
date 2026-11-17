<template>
  <article class="resume-paper" aria-label="版本纸面预览">
    <h2 v-if="title">{{ title }}</h2>
    <p v-if="content" class="resume-paper__text">{{ content }}</p>
    <p v-else class="resume-paper__empty">输入正文后会在这里显示。</p>
  </article>
</template>

<script setup lang="ts">
defineProps<{ title?: string; content?: string }>()
</script>

<style scoped>
.resume-paper {
  box-sizing: border-box;
  max-width: 760px;
  min-width: 0;
  margin-inline: auto;
  min-height: 300px;
  padding: clamp(20px, 4vw, 56px);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  color: var(--color-text);
}
.resume-paper h2 { margin: 0 0 20px; font-size: clamp(20px, 3vw, 30px); overflow-wrap: anywhere; }
.resume-paper__text { margin: 0; line-height: 1.8; white-space: pre-wrap; overflow-wrap: anywhere; }
.resume-paper__empty { margin: 0; color: var(--color-text-muted); }
</style>
