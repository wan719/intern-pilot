<template>
  <el-tag :type="tagType" effect="plain" :size="size">
    {{ text }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    status?: string | number | boolean
    label?: string
    size?: 'large' | 'default' | 'small'
  }>(),
  { size: 'default' }
)

const normalized = computed(() => String(props.status ?? '').toUpperCase())

const text = computed(() => {
  if (props.label) return props.label
  const labels: Record<string, string> = {
    SUCCESS: '成功',
    COMPLETED: '已完成',
    FAILED: '失败',
    PROCESSING: '处理中',
    RUNNING: '进行中',
    PENDING: '待处理',
    TRUE: '是',
    FALSE: '否',
    '1': '启用',
    '0': '停用'
  }
  return labels[normalized.value] || String(props.status ?? '-')
})

const tagType = computed(() => {
  if (['SUCCESS', 'COMPLETED', 'TRUE', '1'].includes(normalized.value)) return 'success'
  if (['FAILED', 'ERROR', 'FALSE'].includes(normalized.value)) return 'danger'
  if (['PROCESSING', 'RUNNING'].includes(normalized.value)) return 'primary'
  if (['PENDING', '0'].includes(normalized.value)) return 'info'
  return 'warning'
})
</script>
