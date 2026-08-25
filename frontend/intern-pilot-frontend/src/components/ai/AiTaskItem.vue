<template>
  <article class="ai-task-item" :class="statusClass" :aria-label="`${task.title}，${statusLabel}`">
    <div class="task-icon">
      <el-icon><component :is="statusIcon" /></el-icon>
    </div>

    <div class="task-content">
      <div class="task-heading">
        <h5>{{ task.title }}</h5>
        <div class="task-state">
          <span v-if="isUnread" class="task-unread" data-task-unread>{{ unreadLabel }}</span>
          <el-tag size="small" :type="statusTagType" effect="plain">{{ statusLabel }}</el-tag>
        </div>
      </div>
      <p>{{ task.errorMessage || task.message || task.description || '任务正在排队处理' }}</p>
      <div v-if="isRunning" class="task-progress-meta" role="status" aria-live="polite">
        <span>阶段：{{ statusLabel }}</span>
        <strong>进度 {{ normalizedProgress }}%</strong>
      </div>
      <el-progress
        v-if="isRunning"
        :percentage="normalizedProgress"
        :show-text="false"
        :stroke-width="5"
      />
      <p v-if="canRecover" class="task-recovery" data-task-recovery>
        返回发起页面后可重新提交，系统不会自动重复请求。
      </p>
      <span class="task-time">{{ timeText }}</span>
    </div>

    <div class="task-actions">
      <el-button v-if="isRunning && task.cancellable" size="small" text type="danger" @click="$emit('cancel', task)">
        停止
      </el-button>
      <el-button v-if="task.status === 'COMPLETED' && task.resultPath" size="small" text type="primary" @click="$emit('view', task)">
        查看结果
      </el-button>
      <el-button v-if="task.status === 'FAILED' || task.status === 'CANCELLED'" size="small" text type="primary" @click="$emit('retry', task)">
        重试
      </el-button>
      <el-button v-if="task.dismissible && !isRunning" size="small" text @click="$emit('dismiss', task)">
        不再提示
      </el-button>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheck, CircleClose, Clock, Cpu, Warning } from '@element-plus/icons-vue'
import type { GlobalAiTask } from '@/stores/aiTaskCenter'

const props = defineProps<{ task: GlobalAiTask }>()

defineEmits<{
  cancel: [task: GlobalAiTask]
  view: [task: GlobalAiTask]
  dismiss: [task: GlobalAiTask]
  retry: [task: GlobalAiTask]
}>()

const runningStatuses = ['PENDING', 'RUNNING', 'PARSING_RESUME', 'BUILDING_CONTEXT', 'CALLING_AI', 'GENERATING_REPORT']
const isRunning = computed(() => runningStatuses.includes(props.task.status))
const normalizedProgress = computed(() => Math.min(100, Math.max(0, Number(props.task.progress) || 0)))
const isUnread = computed(() => ['COMPLETED', 'FAILED', 'CANCELLED'].includes(props.task.status) && props.task.dismissible)
const unreadLabel = computed(() => (props.task.status === 'COMPLETED' ? '待查看' : '待处理'))
const canRecover = computed(() => ['FAILED', 'CANCELLED'].includes(props.task.status) && Boolean(props.task.sourcePath))
const statusClass = computed(() => `task-${props.task.status.toLowerCase().replace(/_/g, '-')}`)

const statusIcon = computed(() => {
  if (isRunning.value) return Clock
  if (props.task.status === 'COMPLETED') return CircleCheck
  if (props.task.status === 'FAILED') return Warning
  if (props.task.status === 'CANCELLED') return CircleClose
  return Cpu
})

const statusLabel = computed(() => {
  const labels: Record<string, string> = {
    PENDING: '等待中',
    RUNNING: '进行中',
    PARSING_RESUME: '解析简历',
    BUILDING_CONTEXT: '构建上下文',
    CALLING_AI: '调用 AI',
    GENERATING_REPORT: '生成报告',
    COMPLETED: '已完成',
    FAILED: '失败',
    CANCELLED: '已取消'
  }
  return labels[props.task.status] || props.task.status
})

const statusTagType = computed(() => {
  if (props.task.status === 'COMPLETED') return 'success'
  if (props.task.status === 'FAILED') return 'danger'
  if (props.task.status === 'CANCELLED') return 'info'
  return 'primary'
})

const timeText = computed(() => {
  const value = props.task.finishedAt || props.task.updatedAt || props.task.createdAt
  return value ? new Date(value).toLocaleString() : ''
})
</script>

<style scoped>
.ai-task-item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.task-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.task-completed .task-icon {
  background: #ecfdf3;
  color: var(--color-success);
}

.task-failed .task-icon {
  background: #fef2f2;
  color: var(--color-danger);
}

.task-cancelled .task-icon {
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
}

.task-content {
  min-width: 0;
}

.task-heading {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.task-state {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
}

.task-unread {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--color-primary-hover);
  font-size: 12px;
  font-weight: 700;
}

.task-unread::before {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
  content: '';
}

.task-heading h5 {
  margin: 0;
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-content p {
  margin: 7px 0 8px;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.task-progress-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
  color: var(--color-text-muted);
  font-size: 12px;
}

.task-progress-meta strong {
  color: var(--color-text);
  font-variant-numeric: tabular-nums;
}

.task-content .task-recovery {
  margin: 8px 0 0;
  padding: 8px 10px;
  border-left: 3px solid var(--color-warning);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--color-warning) 8%, var(--color-surface));
  color: var(--color-text-muted);
}

.task-time {
  display: block;
  margin-top: 8px;
  color: var(--color-text-soft);
  font-size: 12px;
}

.task-actions {
  grid-column: 2;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.task-actions .el-button {
  margin-left: 0;
}

@media (max-width: 640px) {
  .ai-task-item {
    grid-template-columns: 34px minmax(0, 1fr);
    gap: 10px;
    padding: 12px;
  }

  .task-icon {
    width: 34px;
    height: 34px;
  }

  .task-heading {
    align-items: flex-start;
  }

  .task-heading h5 {
    white-space: normal;
  }

  .task-actions {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .task-actions .el-button {
    width: 100%;
  }
}
</style>
