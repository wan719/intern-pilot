<template>
  <div class="version-preview" :aria-busy="loading">
    <p v-if="loading" class="version-preview__message" role="status">正在读取版本内容…</p>
    <div v-else-if="failed" class="version-preview__message" role="alert">
      <p>暂时无法读取版本内容。</p>
      <el-button type="primary" plain @click="$emit('retry')">重新读取详情</el-button>
    </div>
    <template v-else-if="name">
      <header class="version-preview__summary">
        <h3>{{ name }}</h3>
        <p>{{ type }}<template v-if="current"> · 当前版本</template></p>
        <p v-if="jobTitle">{{ jobTitle }}</p>
      </header>
      <p class="version-preview__body">{{ content || '暂无版本内容' }}</p>
    </template>
    <p v-else class="version-preview__message">从版本历史中选择一个版本查看内容。</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  name?: string
  type?: string
  jobTitle?: string
  current?: boolean
  content?: string
  loading: boolean
  failed: boolean
}>()

defineEmits<{
  retry: []
}>()
</script>

<style scoped>
.version-preview {
  min-width: 0;
  color: var(--color-text);
}

.version-preview__summary {
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-border);
  overflow-wrap: anywhere;
}

.version-preview__summary h3 {
  margin: 0 0 var(--space-1);
  font-size: 18px;
  line-height: 1.4;
}

.version-preview__summary p,
.version-preview__message {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.version-preview__message p {
  margin: 0 0 var(--space-2);
}

.version-preview__body {
  margin: var(--space-4) 0 0;
  font-size: 14px;
  line-height: 1.75;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}
</style>
