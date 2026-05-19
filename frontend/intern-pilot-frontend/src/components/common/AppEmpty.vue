<template>
  <el-empty class="app-empty" :description="description || '暂无数据'">
    <template v-if="title" #image>
      <div class="app-empty-icon">
        <el-icon><component :is="icon || Document" /></el-icon>
      </div>
    </template>
    <div v-if="title || hint" class="app-empty-copy">
      <strong v-if="title">{{ title }}</strong>
      <span v-if="hint">{{ hint }}</span>
    </div>
    <slot />
  </el-empty>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'

defineProps<{
  title?: string
  description?: string
  hint?: string
  icon?: unknown
}>()
</script>
