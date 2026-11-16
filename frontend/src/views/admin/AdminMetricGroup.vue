<template>
  <section class="admin-metric-group" :aria-label="label">
    <h2 v-if="showHeading">{{ label }}</h2>
    <div class="admin-metric-group__grid">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  showHeading?: boolean
}>()
</script>

<style scoped>
.admin-metric-group {
  min-width: 0;
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.admin-metric-group h2 {
  margin: 0 0 var(--space-2);
  padding-inline: var(--space-2);
  font-size: 14px;
}

.admin-metric-group__grid {
  display: grid;
  grid-template-columns: repeat(var(--admin-metric-columns, 4), minmax(0, 1fr));
  gap: var(--space-1);
}

.admin-metric-group__grid :slotted(.stat-card) {
  min-width: 0;
  min-height: 64px;
  padding: var(--space-2);
  gap: var(--space-1);
  border: 0;
  box-shadow: none;
  transform: none;
}

.admin-metric-group__grid :deep(.stat-card strong) {
  margin-top: var(--space-1);
  font-size: 22px;
  overflow-wrap: anywhere;
}

.admin-metric-group__grid :deep(.stat-card .el-icon) {
  flex: none;
  width: 28px;
  height: 28px;
  font-size: 16px !important;
}

.admin-metric-group__grid :deep(.stat-card__skeleton) {
  width: min(60px, 100%);
  height: 22px;
  margin-top: var(--space-1);
}

@media (max-width: 800px) {
  .admin-metric-group__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .admin-metric-group__grid :deep(.stat-card .el-icon) {
    display: none;
  }
}
</style>
