<template>
  <el-drawer
    v-model="drawerVisible"
    title="AI 任务中心"
    direction="rtl"
    :size="drawerSize"
    :close-on-press-escape="true"
    class="ai-task-drawer"
  >
    <AppEmpty
      v-if="allTasks.length === 0"
      title="暂无 AI 任务"
      description="发起 AI 分析、岗位推荐或面试题生成后，任务会在这里持续显示。"
    />

    <div v-else class="task-list">
      <div class="task-overview" role="status" aria-live="polite">
        <span>{{ runningTasks.length }} 个进行中</span>
        <span>{{ completedTasks.length }} 个结果待查看</span>
        <span>{{ failedTasks.length + cancelledTasks.length }} 个需要处理</span>
      </div>
      <section v-if="runningTasks.length" class="task-section">
        <h4>进行中</h4>
        <AiTaskItem v-for="task in runningTasks" :key="task.localTaskId" :task="task" @cancel="handleCancel" />
      </section>

      <section v-if="completedTasks.length" class="task-section">
        <h4>可查看结果</h4>
        <AiTaskItem
          v-for="task in completedTasks"
          :key="task.localTaskId"
          :task="task"
          @view="store.viewResult"
          @dismiss="handleDismiss"
        />
      </section>

      <section v-if="failedTasks.length" class="task-section">
        <h4>失败任务</h4>
        <AiTaskItem
          v-for="task in failedTasks"
          :key="task.localTaskId"
          :task="task"
          @retry="store.retryTask"
          @dismiss="handleDismiss"
        />
      </section>

      <section v-if="cancelledTasks.length" class="task-section">
        <h4>已取消</h4>
        <AiTaskItem
          v-for="task in cancelledTasks"
          :key="task.localTaskId"
          :task="task"
          @retry="store.retryTask"
          @dismiss="handleDismiss"
        />
      </section>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import AppEmpty from '@/components/common/AppEmpty.vue'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'
import type { GlobalAiTask } from '@/stores/aiTaskCenter'
import { useResponsiveSize } from '@/utils/useResponsiveSize'
import AiTaskItem from './AiTaskItem.vue'

const store = useAiTaskCenterStore()
const { responsiveDrawerSize } = useResponsiveSize()
const drawerSize = responsiveDrawerSize('420px', '72%')
const drawerVisible = computed({
  get: () => store.drawerVisible,
  set: (value) => {
    if (!value) store.closeDrawer()
  }
})

const allTasks = computed(() => store.tasks)
const runningTasks = computed(() => store.runningTasks)
const completedTasks = computed(() => store.completedTasks)
const failedTasks = computed(() => store.failedTasks)
const cancelledTasks = computed(() => store.cancelledTasks)

async function handleCancel(task: GlobalAiTask) {
  await ElMessageBox.confirm('确认停止这个 AI 任务提示？已发出的 AI 请求可能无法立即物理中断。', '停止任务', {
    type: 'warning'
  })
  await store.cancelBackendTask(task.localTaskId)
}

function handleDismiss(task: GlobalAiTask) {
  store.dismissTask(task.localTaskId)
}
</script>

<style scoped>
.task-list {
  display: grid;
  gap: 20px;
}

.task-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
  font-size: 12px;
  text-align: center;
}

.task-section {
  display: grid;
  gap: 12px;
}

.task-section h4 {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 900px) {
  :global(.ai-task-drawer.el-drawer) {
    top: 0;
    bottom: calc(72px + env(safe-area-inset-bottom));
    height: auto !important;
  }

  :deep(.ai-task-drawer .el-drawer__header) {
    margin-bottom: 10px;
    padding: 16px 16px 8px;
  }

  :deep(.ai-task-drawer .el-drawer__body) {
    padding: 0 14px 16px;
  }

  .task-list {
    gap: 16px;
    padding-bottom: 12px;
  }

  .task-overview {
    grid-template-columns: 1fr;
    text-align: left;
  }
}
</style>
