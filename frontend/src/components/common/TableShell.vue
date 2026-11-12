<template>
  <section class="table-shell" :aria-busy="loading">
    <div v-if="$slots.toolbar" class="table-shell__toolbar">
      <slot name="toolbar" />
    </div>
    <div v-if="loading" class="table-shell__loading" aria-live="polite">
      <el-skeleton :rows="4" animated />
    </div>
    <AppEmpty
      v-else-if="empty"
      class="table-shell__empty"
      :title="emptyTitle"
      :description="emptyHint"
    >
      <template v-if="$slots['empty-actions']">
        <slot name="empty-actions" />
      </template>
    </AppEmpty>
    <div v-else class="table-shell__content">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
import AppEmpty from '@/components/common/AppEmpty.vue'

defineProps<{
  loading: boolean
  empty: boolean
  emptyTitle: string
  emptyHint?: string
}>()
</script>
