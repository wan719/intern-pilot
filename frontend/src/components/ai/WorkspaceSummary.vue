<template>
  <dl class="workspace-measures" :aria-label="label" :aria-busy="loading">
    <div v-for="metric in metrics" :key="metric.label">
      <dt>{{ metric.label }}</dt>
      <dd>
        <el-skeleton v-if="loading" :rows="0" animated />
        <template v-else>{{ metric.value }}</template>
      </dd>
    </div>
  </dl>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  metrics: { label: string; value: number | string }[]
  loading: boolean
}>()
</script>

<style scoped>
.workspace-measures {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0 0 var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.workspace-measures > div {
  min-width: 0;
  padding: var(--space-3) var(--space-4);
}

.workspace-measures > div + div {
  border-inline-start: 1px solid var(--color-border);
}

.workspace-measures dt {
  color: var(--color-text-muted);
  font-size: 12px;
}

.workspace-measures dd {
  margin: var(--space-1) 0 0;
  color: var(--color-text);
  font-size: 15px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

@media (max-width: 640px) {
  .workspace-measures {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workspace-measures > div:nth-child(odd) {
    border-inline-start: 0;
  }

  .workspace-measures > div:nth-child(n + 3) {
    border-block-start: 1px solid var(--color-border);
  }
}
</style>
