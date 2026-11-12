<template>
  <section class="filter-bar" aria-label="筛选工具栏">
    <div v-if="$slots.filters" class="filter-bar__filters">
      <slot name="filters" />
    </div>
    <div class="filter-bar__actions responsive-actions">
      <slot name="actions" />
      <el-button data-filter-reset plain @click="emit('reset')">重置</el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
const emit = defineEmits<{
  reset: []
}>()
</script>
