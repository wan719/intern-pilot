<template>
  <header class="page-hero">
    <div class="page-hero__copy">
      <span v-if="eyebrow" class="page-hero__eyebrow">{{ eyebrow }}</span>
      <h1>{{ title }}</h1>
      <p v-if="description">{{ description }}</p>
      <div v-if="$slots.default" class="page-hero__content">
        <slot />
      </div>
    </div>
    <div v-if="$slots.actions" class="page-hero__actions responsive-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow?: string
  title: string
  description?: string
}>()
</script>
