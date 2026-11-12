import { computed, h, ref } from 'vue'
import { defineStore } from 'pinia'
import { ElNotification } from 'element-plus'
import { cancelTaskApi, listRecentTasksApi, listRunningTasksApi } from '@/api/analysisTask'
import router from '@/router'

export type AiTaskType =
  | 'ANALYSIS_MATCH'
  | 'JOB_RECOMMENDATION'
  | 'INTERVIEW_QUESTION'
  | 'INTERVIEW_REGENERATE'
  | 'RAG_IMPORT'
  | 'RESUME_OPTIMIZE'

export type AiTaskStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'PARSING_RESUME'
  | 'BUILDING_CONTEXT'
  | 'CALLING_AI'
  | 'GENERATING_REPORT'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'DISMISSED'

export interface GlobalAiTask {
  localTaskId: string
  backendTaskNo?: string
  type: AiTaskType
  title: string
  description?: string
  status: AiTaskStatus
  progress: number
  message?: string
  errorMessage?: string
  resultId?: number | string
  resultPath?: string
  sourcePath?: string
  createdAt: string
  updatedAt: string
  finishedAt?: string
  dismissible: boolean
  cancellable: boolean
  notified?: boolean
}

type UpsertTaskOptions = {
  type: AiTaskType
  title: string
  taskNo: string
  status?: AiTaskStatus | string
  progress?: number
  message?: string
  reportId?: number
  sourcePath?: string
  silent?: boolean
}

type UpdateOptions = {
  notify?: boolean
}

const STORAGE_KEY = 'internpilot:ai-task-center'
const DISMISSED_BACKEND_TASKS_KEY = 'internpilot:ai-task-center:dismissed-backend-task-nos'
const RUNNING_STATUSES: AiTaskStatus[] = [
  'PENDING',
  'RUNNING',
  'PARSING_RESUME',
  'BUILDING_CONTEXT',
  'CALLING_AI',
  'GENERATING_REPORT'
]
const TERMINAL_STATUSES: AiTaskStatus[] = ['COMPLETED', 'FAILED', 'CANCELLED']

export const useAiTaskCenterStore = defineStore('aiTaskCenter', () => {
  const tasks = ref<GlobalAiTask[]>([])
  const dismissedBackendTaskNos = ref<Set<string>>(new Set())
  const drawerVisible = ref(false)
  let initializationPromise: Promise<void> | null = null

  const visibleTasks = computed(() => tasks.value.filter((task) => task.status !== 'DISMISSED'))
  const runningTasks = computed(() => visibleTasks.value.filter((task) => RUNNING_STATUSES.includes(task.status)))
  const completedTasks = computed(() => visibleTasks.value.filter((task) => task.status === 'COMPLETED'))
  const failedTasks = computed(() => visibleTasks.value.filter((task) => task.status === 'FAILED'))
  const cancelledTasks = computed(() => visibleTasks.value.filter((task) => task.status === 'CANCELLED'))

  const badgeCount = computed(() => runningTasks.value.length + completedTasks.value.length + failedTasks.value.length)
  const badgeText = computed(() => {
    if (runningTasks.value.length) return `AI 任务 ${runningTasks.value.length} 个进行中`
    if (failedTasks.value.length) return `有 ${failedTasks.value.length} 个任务失败`
    if (completedTasks.value.length) return `有 ${completedTasks.value.length} 个结果可查看`
    return 'AI 任务中心'
  })

  function createLocalTask(options: {
    type: AiTaskType
    title: string
    description?: string
    sourcePath?: string
    cancellable?: boolean
    dismissible?: boolean
  }): GlobalAiTask {
    const now = new Date().toISOString()
    const task: GlobalAiTask = {
      localTaskId: generateLocalTaskId(),
      type: options.type,
      title: options.title,
      description: options.description,
      status: 'PENDING',
      progress: 0,
      message: options.description,
      sourcePath: options.sourcePath,
      createdAt: now,
      updatedAt: now,
      cancellable: options.cancellable ?? false,
      dismissible: options.dismissible ?? true
    }
    tasks.value.unshift(task)
    saveToStorage()
    return task
  }

  function createTask(options: {
    type: AiTaskType
    title: string
    message?: string
    taskNo?: string
    status?: AiTaskStatus
    progress?: number
    reportId?: number
    resumeId?: number
    jobId?: number
    sourcePath?: string
  }): string {
    const task = createLocalTask({
      type: options.type,
      title: options.title,
      description: options.message,
      sourcePath: options.sourcePath,
      cancellable: Boolean(options.taskNo),
      dismissible: true
    })
    updateTask(task.localTaskId, {
      backendTaskNo: options.taskNo,
      status: normalizeStatus(options.status || 'RUNNING'),
      progress: options.progress ?? 10,
      message: options.message,
      resultId: options.reportId
    })
    return task.localTaskId
  }

  function upsertByTaskNo(options: UpsertTaskOptions): string {
    const existing = tasks.value.find((task) => task.backendTaskNo === options.taskNo)

    if (dismissedBackendTaskNos.value.has(options.taskNo)) {
      return existing?.localTaskId || ''
    }

    const resultPath = options.reportId ? `/analysis/reports?reportId=${options.reportId}` : undefined
    if (existing) {
      if (existing.status === 'DISMISSED') {
        rememberDismissedBackendTask(existing.backendTaskNo)
        return existing.localTaskId
      }
      updateTask(
        existing.localTaskId,
        {
          type: options.type,
          title: options.title,
          status: normalizeStatus(options.status),
          progress: options.progress ?? existing.progress,
          message: options.message,
          resultId: options.reportId,
          resultPath,
          sourcePath: options.sourcePath ?? existing.sourcePath
        },
        { notify: options.silent !== true }
      )
      return existing.localTaskId
    }

    const initialStatus = normalizeStatus(options.status)
    const task = createLocalTask({
      type: options.type,
      title: options.title,
      description: options.message,
      sourcePath: options.sourcePath,
      cancellable: RUNNING_STATUSES.includes(initialStatus),
      dismissible: true
    })
    updateTask(
      task.localTaskId,
      {
        backendTaskNo: options.taskNo,
        status: initialStatus,
        progress: options.progress ?? 0,
        message: options.message,
        resultId: options.reportId,
        resultPath,
        notified: options.silent && TERMINAL_STATUSES.includes(initialStatus)
      },
      { notify: options.silent !== true }
    )
    return task.localTaskId
  }

  function updateTask(localTaskId: string, updates: Partial<GlobalAiTask>, options: UpdateOptions = {}): void {
    const index = tasks.value.findIndex((task) => task.localTaskId === localTaskId)
    if (index < 0) return

    const previous = tasks.value[index]
    if (previous.status === 'DISMISSED') {
      return
    }
    const nextStatus = normalizeStatus(updates.status)
    const next: GlobalAiTask = {
      ...previous,
      ...updates,
      status: nextStatus,
      updatedAt: new Date().toISOString()
    }

    if (TERMINAL_STATUSES.includes(next.status) && !next.finishedAt) {
      next.finishedAt = new Date().toISOString()
      next.cancellable = false
    }

    tasks.value[index] = next
    saveToStorage()

    const shouldNotify = options.notify !== false
    if (shouldNotify && !previous.notified && !next.notified && TERMINAL_STATUSES.includes(next.status)) {
      notifyTask(next)
      tasks.value[index] = { ...tasks.value[index], notified: true }
      saveToStorage()
    }
  }

  function completeTask(localTaskId: string, result: { resultId?: number | string; resultPath?: string; message?: string }): void {
    updateTask(localTaskId, {
      status: 'COMPLETED',
      progress: 100,
      resultId: result.resultId,
      resultPath: result.resultPath,
      message: result.message || '任务已完成'
    })
  }

  function failTask(localTaskId: string, errorMessage?: string): void {
    updateTask(localTaskId, {
      status: 'FAILED',
      errorMessage,
      message: errorMessage || '任务执行失败'
    })
  }

  function dismissTask(localTaskId: string): void {
    const task = tasks.value.find((item) => item.localTaskId === localTaskId)
    if (!task) return
    rememberDismissedBackendTask(task.backendTaskNo)
    updateTask(localTaskId, {
      status: 'DISMISSED',
      dismissible: false,
      cancellable: false,
      notified: true
    })
  }

  async function cancelBackendTask(localTaskId: string): Promise<void> {
    const task = tasks.value.find((item) => item.localTaskId === localTaskId)
    if (!task) return

    if (task.backendTaskNo) {
      await cancelTaskApi(task.backendTaskNo)
    }
    updateTask(localTaskId, {
      status: 'CANCELLED',
      progress: 100,
      message: '任务已取消',
      cancellable: false
    })
  }

  function updateFromBackend(taskNo: string, status: AiTaskStatus, progress: number, message: string, reportId?: number): void {
    if (dismissedBackendTaskNos.value.has(taskNo)) return
    const task = tasks.value.find((item) => item.backendTaskNo === taskNo)
    if (!task) return
    updateTask(task.localTaskId, {
      status: normalizeStatus(status),
      progress,
      message,
      resultId: reportId,
      resultPath: reportId ? `/analysis/reports?reportId=${reportId}` : task.resultPath
    })
  }

  async function syncRunningAnalysisTasks(): Promise<void> {
    try {
      const backendTasks = await listRunningTasksApi()
      if (!Array.isArray(backendTasks)) return
      backendTasks.forEach((backendTask: any) => {
        if (dismissedBackendTaskNos.value.has(backendTask.taskNo)) return
        upsertByTaskNo({
          type: 'ANALYSIS_MATCH',
          title: 'AI 简历岗位匹配分析',
          taskNo: backendTask.taskNo,
          status: backendTask.status,
          progress: backendTask.progress || 0,
          message: backendTask.message,
          reportId: backendTask.reportId,
          sourcePath: '/analysis/match',
          silent: true
        })
      })
    } catch (error) {
      console.warn('Failed to sync running AI tasks', error)
    }
  }

  async function loadRecentTasks(): Promise<void> {
    try {
      const backendTasks = await listRecentTasksApi(5)
      if (!Array.isArray(backendTasks)) return
      backendTasks.forEach((backendTask: any) => {
        if (dismissedBackendTaskNos.value.has(backendTask.taskNo)) return

        const status = normalizeStatus(backendTask.status)
        const hasLocalTask = tasks.value.some((task) => task.backendTaskNo === backendTask.taskNo)
        if (TERMINAL_STATUSES.includes(status) && !hasLocalTask) return

        upsertByTaskNo({
          type: 'ANALYSIS_MATCH',
          title: 'AI 简历岗位匹配分析',
          taskNo: backendTask.taskNo,
          status,
          progress: backendTask.progress || 0,
          message: backendTask.message,
          reportId: backendTask.reportId,
          sourcePath: '/analysis/match',
          silent: true
        })
      })
    } catch (error) {
      console.warn('Failed to load recent AI tasks', error)
    }
  }

  function restoreFromStorage(): void {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (!stored) return
    try {
      const storedTasks = JSON.parse(stored) as GlobalAiTask[]
      tasks.value = storedTasks
      storedTasks.forEach((task) => {
        if (task.status === 'DISMISSED') {
          rememberDismissedBackendTask(task.backendTaskNo)
        }
      })
    } catch {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  function saveToStorage(): void {
    const safeTasks = tasks.value.slice(0, 30).map((task) => ({
      localTaskId: task.localTaskId,
      backendTaskNo: task.backendTaskNo,
      type: task.type,
      title: task.title,
      description: task.description,
      status: task.status,
      progress: task.progress,
      message: task.message,
      errorMessage: task.errorMessage,
      resultId: task.resultId,
      resultPath: task.resultPath,
      sourcePath: task.sourcePath,
      createdAt: task.createdAt,
      updatedAt: task.updatedAt,
      finishedAt: task.finishedAt,
      dismissible: task.dismissible,
      cancellable: task.cancellable,
      notified: task.notified
    }))
    localStorage.setItem(STORAGE_KEY, JSON.stringify(safeTasks))
  }

  function restoreDismissedBackendTasks(): void {
    const stored = localStorage.getItem(DISMISSED_BACKEND_TASKS_KEY)
    if (!stored) return
    try {
      const taskNos = JSON.parse(stored) as string[]
      dismissedBackendTaskNos.value = new Set(taskNos.filter(Boolean))
    } catch {
      localStorage.removeItem(DISMISSED_BACKEND_TASKS_KEY)
    }
  }

  function saveDismissedBackendTasks(): void {
    localStorage.setItem(DISMISSED_BACKEND_TASKS_KEY, JSON.stringify(Array.from(dismissedBackendTaskNos.value).slice(-100)))
  }

  function rememberDismissedBackendTask(taskNo?: string): void {
    if (!taskNo) return
    dismissedBackendTaskNos.value.add(taskNo)
    saveDismissedBackendTasks()
  }

  function openDrawer(): void {
    drawerVisible.value = true
  }

  function closeDrawer(): void {
    drawerVisible.value = false
  }

  function toggleDrawer(): void {
    drawerVisible.value = !drawerVisible.value
  }

  function viewResult(task: GlobalAiTask): void {
    if (!task.resultPath) return
    router.push(task.resultPath)
    dismissTask(task.localTaskId)
    closeDrawer()
  }

  function retryTask(task: GlobalAiTask): void {
    if (!task.sourcePath) return
    router.push(task.sourcePath)
    dismissTask(task.localTaskId)
    closeDrawer()
  }

  function initialize(): Promise<void> {
    if (initializationPromise) return initializationPromise
    restoreDismissedBackendTasks()
    restoreFromStorage()
    initializationPromise = Promise.allSettled([
      syncRunningAnalysisTasks(),
      loadRecentTasks()
    ]).then(() => undefined)
    return initializationPromise
  }

  function notifyTask(task: GlobalAiTask): void {
    const notificationType = task.status === 'COMPLETED' ? 'success' : task.status === 'FAILED' ? 'error' : 'info'
    const title = task.status === 'COMPLETED' ? `${task.title}完成` : task.status === 'FAILED' ? `${task.title}失败` : `${task.title}已取消`
    const message =
      task.status === 'COMPLETED'
        ? task.message || '结果已生成，可在任务中心查看'
        : task.errorMessage || task.message || '请打开任务中心查看详情'
    ElNotification({
      title,
      message: h('span', { style: 'cursor:pointer;' }, message),
      type: notificationType,
      duration: 0,
      onClick: openDrawer,
      onClose: () => {
        dismissTask(task.localTaskId)
      }
    })
  }

  function generateLocalTaskId(): string {
    const date = new Date().toISOString().replace(/[-T:.Z]/g, '').slice(0, 14)
    const random = Math.random().toString(36).slice(2, 8).toUpperCase()
    return `LOCAL_${date}_${random}`
  }

  function normalizeStatus(status?: AiTaskStatus | string): AiTaskStatus {
    if (!status) return 'RUNNING'
    if (status === 'SUCCESS') return 'COMPLETED'
    return status as AiTaskStatus
  }

  return {
    tasks: visibleTasks,
    drawerVisible,
    runningTasks,
    completedTasks,
    failedTasks,
    cancelledTasks,
    badgeCount,
    badgeText,
    createLocalTask,
    createTask,
    upsertByTaskNo,
    updateTask,
    completeTask,
    failTask,
    dismissTask,
    cancelBackendTask,
    updateFromBackend,
    syncRunningAnalysisTasks,
    loadRecentTasks,
    openDrawer,
    closeDrawer,
    toggleDrawer,
    viewResult,
    retryTask,
    initialize
  }
})
