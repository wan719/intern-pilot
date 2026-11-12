<template>
  <el-button v-if="allowed || showDisabled" v-bind="$attrs" :disabled="!allowed || disabled">
    <slot />
  </el-button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    permission?: string
    permissions?: string[]
    disabled?: boolean
    showDisabled?: boolean
    any?: boolean
  }>(),
  {
    showDisabled: false,
    any: true
  }
)

const auth = useAuthStore()

const allowed = computed(() => {
  const required = props.permissions?.length ? props.permissions : props.permission ? [props.permission] : []
  if (!required.length) return true
  return props.any
    ? required.some((item) => auth.hasPermission(item))
    : required.every((item) => auth.hasPermission(item))
})
</script>
