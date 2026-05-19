<template>
  <el-drawer v-model="drawerVisible" title="AI 任务中心" direction="rtl" size="420px">
    <AppEmpty
      v-if="allTasks.length === 0"
      title="暂无 AI 任务"
      description="发起 AI 分析、岗位推荐或面试题生成后，任务会在这里持续显示。"
    />

    <div v-else class="task-list">
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
import AiTaskItem from './AiTaskItem.vue'

const store = useAiTaskCenterStore()
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
</style>
