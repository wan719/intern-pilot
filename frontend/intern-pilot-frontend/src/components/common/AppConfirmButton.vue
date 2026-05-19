<template>
  <el-button v-bind="$attrs" :loading="loading" @click="confirm">
    <slot />
  </el-button>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    title?: string
    message: string
    confirmText?: string
    cancelText?: string
    type?: 'success' | 'warning' | 'info' | 'error'
    loading?: boolean
  }>(),
  {
    title: '操作确认',
    confirmText: '确认',
    cancelText: '取消',
    type: 'warning',
    loading: false
  }
)

const emit = defineEmits<{ confirm: [] }>()

async function confirm() {
  try {
    await ElMessageBox.confirm(props.message, props.title, {
      confirmButtonText: props.confirmText,
      cancelButtonText: props.cancelText,
      type: props.type
    })
    emit('confirm')
  } catch {
    // User cancelled.
  }
}
</script>
