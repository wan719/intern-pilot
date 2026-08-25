import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'
import { listRecentTasksApi, listRunningTasksApi } from '@/api/analysisTask'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))

vi.mock('@/router', () => ({ default: { push } }))
vi.mock('@/api/analysisTask', () => ({
  cancelTaskApi: vi.fn(),
  listRecentTasksApi: vi.fn().mockResolvedValue([]),
  listRunningTasksApi: vi.fn().mockResolvedValue([])
}))
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return { ...actual, ElNotification: vi.fn() }
})

describe('AI task center interview retry', () => {
  beforeEach(() => {
    localStorage.clear()
    push.mockReset()
    setActivePinia(createPinia())
  })

  it('returns a failed synchronous interview task to its valid source path', () => {
    const taskCenter = useAiTaskCenterStore()
    const localTaskId = taskCenter.createTask({
      type: 'INTERVIEW_QUESTION',
      title: '面试题生成',
      message: '正在生成面试题...',
      sourcePath: '/interview-questions'
    })
    taskCenter.failTask(localTaskId, '面试题生成已中断')
    const failedTask = taskCenter.tasks.find((task) => task.localTaskId === localTaskId)

    expect(failedTask?.status).toBe('FAILED')
    expect(failedTask?.sourcePath).toBe('/interview-questions')
    taskCenter.retryTask(failedTask!)

    expect(push).toHaveBeenCalledWith('/interview-questions')
    expect(taskCenter.tasks.some((task) => task.localTaskId === localTaskId)).toBe(false)
  })

  it('initializes storage and backend reconciliation only once for concurrent callers', async () => {
    const taskCenter = useAiTaskCenterStore()

    const first = taskCenter.initialize()
    const second = taskCenter.initialize()
    await Promise.all([first, second])

    expect(listRunningTasksApi).toHaveBeenCalledTimes(1)
    expect(listRecentTasksApi).toHaveBeenCalledTimes(1)
  })
})
