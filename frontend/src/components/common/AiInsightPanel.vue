<template>
  <section class="ai-insight-panel" :class="`ai-insight-panel--${tone}`">
    <h2>{{ title }}</h2>
    <div class="ai-insight-panel__content">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  tone: 'strength' | 'risk' | 'action'
}>()
</script>
