<template>
  <button class="feedback-float" type="button" @click="store.openDrawer">
    <span class="feedback-icon">
      <el-icon :size="19"><Message /></el-icon>
    </span>
    <span>反馈</span>
  </button>
</template>

<script setup lang="ts">
import { Message } from '@element-plus/icons-vue'
import { useFeedbackStore } from '@/stores/feedback'

const store = useFeedbackStore()
</script>

<style scoped>
.feedback-float {
  position: fixed;
  right: 20px;
  bottom: 24px;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-surface);
  color: var(--color-text);
  box-shadow: var(--shadow-card);
  cursor: pointer;
}

.feedback-float:hover {
  border-color: var(--color-primary-border);
  transform: translateY(-1px);
}

.feedback-icon {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 50%;
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.feedback-float span:last-child {
  font-weight: 700;
}
</style>
