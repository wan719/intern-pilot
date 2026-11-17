<template>
  <div class="admin-results">
    <slot name="filters" />
    <slot name="summary" />
    <slot name="after-filters" />
    <div v-if="error && !loading" class="admin-result-error" role="alert">
      <div>
        <strong>{{ errorTitle }}</strong>
        <p>{{ error }}</p>
      </div>
      <el-button plain @click="emit('retry')">{{ retryLabel }}</el-button>
    </div>

    <TableShell
      v-else
      :loading="loading"
      :empty="rows.length === 0"
      :empty-title="emptyTitle"
      :empty-hint="emptyHint"
    >
      <template v-if="$slots.toolbar" #toolbar>
        <slot name="toolbar" />
      </template>
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
      <div v-if="$slots.pagination" class="admin-pagination">
        <slot name="pagination" />
      </div>
    </TableShell>
  </div>
</template>

<script setup lang="ts">
import TableShell from '@/components/common/TableShell.vue'

withDefaults(defineProps<{
  label: string
  rows: unknown[]
  loading: boolean
  emptyTitle: string
  emptyHint: string
  error: string
  errorTitle?: string
  retryLabel?: string
}>(), {
  errorTitle: '列表加载失败',
  retryLabel: '重新加载'
})

const emit = defineEmits<{
  retry: []
}>()
</script>

<style scoped>
.admin-results {
  min-width: 0;
}

.admin-results :deep(.filter-bar) {
  margin-bottom: var(--space-4);
  padding: var(--space-4);
}

.admin-results :deep(.table-shell) {
  min-width: 0;
}

.admin-results :deep(.filter-bar .el-button--primary) {
  --el-button-bg-color: var(--color-primary-hover);
  --el-button-border-color: var(--color-primary-hover);
  --el-button-hover-bg-color: color-mix(in srgb, var(--color-primary-hover) 88%, var(--color-nav));
  --el-button-hover-border-color: var(--el-button-hover-bg-color);
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

.admin-result-error p {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  overflow-wrap: anywhere;
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
  padding-block: 8px;
}

.admin-data-table {
  font-size: 13px;
  --el-table-header-bg-color: var(--color-bg);
  --el-table-header-text-color: var(--color-text);
  --el-table-border-color: var(--color-border);
  --el-table-row-hover-bg-color: var(--color-surface-muted);
}

.admin-data-table :deep(.el-table__header .cell) {
  font-weight: 700;
}

.admin-data-table :deep(.el-button--primary.is-link) {
  --el-button-text-color: var(--color-primary-hover);
}

.admin-pagination :deep(.el-pagination) {
  justify-content: flex-end;
  padding: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.admin-pagination:empty {
  display: none;
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

@media (max-width: 640px) {
  .admin-pagination :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: flex-start;
    row-gap: var(--space-2);
  }

  .admin-pagination :deep(.el-pagination__rightwrapper) {
    flex: 1 1 100%;
    justify-content: flex-start;
  }

  .admin-pagination :deep(.el-pagination__sizes) {
    width: 115px;
  }
}
</style>
