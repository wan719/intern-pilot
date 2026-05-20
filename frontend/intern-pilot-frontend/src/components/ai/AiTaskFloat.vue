<template>
  <Transition name="fade">
    <button v-if="badgeCount > 0" class="ai-task-float" type="button" @click="store.openDrawer">
      <span class="ai-task-float-icon">
        <el-icon :size="20"><Cpu /></el-icon>
        <span class="ai-task-float-badge">{{ badgeCount }}</span>
      </span>
      <span class="ai-task-float-text">{{ badgeText }}</span>
    </button>
  </Transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Cpu } from '@element-plus/icons-vue'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'

const store = useAiTaskCenterStore()
const badgeCount = computed(() => store.badgeCount)
const badgeText = computed(() => store.badgeText)
</script>

<style scoped>
.ai-task-float {
  position: fixed;
  right: 20px;
  bottom: 96px;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: min(360px, calc(100vw - 40px));
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 999px;
  background: var(--color-primary);
  color: #fff;
  box-shadow: 0 14px 30px rgba(37, 99, 235, 0.28);
  cursor: pointer;
}

.ai-task-float:hover {
  transform: translateY(-1px);
}

.ai-task-float-icon {
  position: relative;
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
}

.ai-task-float-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--color-danger);
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
}

.ai-task-float-text {
  overflow: hidden;
  font-size: 14px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 640px) {
  .ai-task-float {
    right: 14px;
    bottom: calc(78px + env(safe-area-inset-bottom));
    max-width: calc(100vw - 28px);
    gap: 8px;
    padding: 9px 12px;
  }

  .ai-task-float-icon {
    width: 30px;
    height: 30px;
  }

  .ai-task-float-text {
    max-width: 180px;
    font-size: 13px;
  }
}

@media (max-width: 420px) {
  .ai-task-float {
    padding-right: 10px;
  }

  .ai-task-float-text {
    max-width: 128px;
  }
}
</style>
