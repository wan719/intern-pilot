<template>
  <section class="page-container">
    <AppPageHeader :title="displayTitle" :description="description">
      <template v-if="$slots.actions" #actions>
        <slot name="actions" />
      </template>
    </AppPageHeader>
    <slot />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppPageHeader from '@/components/common/AppPageHeader.vue'

const props = defineProps<{
  title: string
  description?: string
}>()

const route = useRoute()
const displayTitle = computed(() => props.title || String(route.meta.title || 'InternPilot'))
</script>
