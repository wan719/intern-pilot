<template>
  <div class="stat-card" :class="`stat-card--${tone}`" :aria-busy="loading">
    <div class="stat-card__content">
      <span class="stat-card__label">{{ label }}</span>
      <el-skeleton v-if="loading" animated>
        <template #template><el-skeleton-item variant="text" class="stat-card__skeleton" /></template>
      </el-skeleton>
      <strong v-else>{{ value }}</strong>
      <span v-if="hint" class="stat-card__hint">{{ hint }}</span>
      <span v-if="trend" class="stat-card__trend">{{ trend }}</span>
    </div>
    <el-icon v-if="icon" :size="28" aria-hidden="true">
      <component :is="icon" />
    </el-icon>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  value: number | string
  icon?: unknown
  hint?: string
  trend?: string
  tone?: 'primary' | 'info' | 'success' | 'warning'
  loading?: boolean
}>(), {
  tone: 'primary',
  loading: false
})
</script>
