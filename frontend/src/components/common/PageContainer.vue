<template>
  <section class="page-container" :class="`page-container--${width}`">
    <AppPageHeader v-if="!$slots.hero" :title="displayTitle" :description="description" :eyebrow="eyebrow">
      <template v-if="$slots.actions" #actions>
        <slot name="actions" />
      </template>
    </AppPageHeader>
    <div v-if="$slots.hero" class="page-container__hero">
      <slot name="hero" />
      <div v-if="$slots.actions" class="page-container__hero-actions responsive-actions">
        <slot name="actions" />
      </div>
    </div>
    <slot />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppPageHeader from '@/components/common/AppPageHeader.vue'

const props = withDefaults(defineProps<{
  title: string
  description?: string
  eyebrow?: string
  width?: 'default' | 'wide'
}>(), {
  width: 'default'
})

const route = useRoute()
const displayTitle = computed(() => props.title || String(route.meta.title || 'InternPilot'))
</script>
