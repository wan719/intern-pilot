<template>
  <div class="stat-card">
    <div>
      <span>{{ label }}</span>
      <strong>{{ value }}</strong>
    </div>
    <el-icon :size="28">
      <component :is="icon" />
    </el-icon>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value: number | string
  icon: any
}>()
</script>
