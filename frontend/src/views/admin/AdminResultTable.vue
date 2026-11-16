<template>
  <div class="admin-results">
    <slot name="filters" />
    <div v-if="error && !loading" class="admin-result-error" role="alert">
      <strong>{{ error }}</strong>
      <el-button plain @click="emit('retry')">重新加载</el-button>
    </div>

    <TableShell
      v-else
      :loading="loading"
      :empty="rows.length === 0"
      :empty-title="emptyTitle"
      :empty-hint="emptyHint"
    >
      <template v-if="$slots['empty-actions']" #empty-actions>
        <slot name="empty-actions" />
      </template>
      <p class="admin-table-scroll-hint">横向滚动查看全部字段和操作。</p>
      <el-table
        class="admin-data-table"
        :aria-label="label"
        :data="rows"
        :scrollbar-tabindex="0"
        scrollbar-always-on
        size="small"
      >
        <slot />
      </el-table>
      <slot name="pagination" />
    </TableShell>
  </div>
</template>

<script setup lang="ts">
import TableShell from '@/components/common/TableShell.vue'

defineProps<{
  label: string
  rows: unknown[]
  loading: boolean
  emptyTitle: string
  emptyHint: string
  error: string
}>()

const emit = defineEmits<{
  retry: []
}>()
</script>

<style scoped>
.admin-results {
  min-width: 0;
}

.admin-results :deep(.filter-bar) {
  margin-bottom: var(--space-3);
}

.admin-result-error {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-danger);
}

.admin-table-scroll-hint {
  display: none;
  margin: var(--space-2) var(--space-3);
  color: var(--color-text-muted);
  font-size: 12px;
}

.admin-data-table :deep(.el-scrollbar__wrap:focus-visible) {
  outline: 2px solid var(--color-primary-hover);
  outline-offset: -2px;
}

.admin-data-table :deep(.el-table__cell) {
  padding-block: 7px;
}

@media (max-width: 900px) {
  .admin-table-scroll-hint {
    display: block;
  }

  .admin-results :deep(.filter-bar__filters) {
    display: grid;
    min-width: 0;
    flex: none;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .admin-results :deep(.filter-bar__filters > .el-input),
  .admin-results :deep(.filter-bar__filters > .el-select) {
    width: 100%;
  }
}

@media (max-width: 520px) {
  .admin-results :deep(.filter-bar__filters) {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
