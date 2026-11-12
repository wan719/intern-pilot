import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AiTaskDrawer from '@/components/ai/AiTaskDrawer.vue'
import AiTaskItem from '@/components/ai/AiTaskItem.vue'
import type { GlobalAiTask } from '@/stores/aiTaskCenter'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('@/router', () => ({ default: { push } }))

beforeEach(() => {
  localStorage.clear()
  push.mockReset()
})

function makeTask(overrides: Partial<GlobalAiTask>): GlobalAiTask {
  return {
    localTaskId: 'task-1',
    type: 'ANALYSIS_MATCH',
    title: 'AI 简历岗位匹配分析',
    status: 'RUNNING',
    progress: 68,
    message: '正在分析岗位技能',
    sourcePath: '/analysis/match',
    createdAt: '2026-08-25T08:00:00.000Z',
    updatedAt: '2026-08-25T08:01:00.000Z',
    dismissible: true,
    cancellable: true,
    ...overrides
  }
}

describe('AI task item contract characterization', () => {
  it('shows each running lifecycle stage with progress and the existing cancel action', async () => {
    const wrapper = mount(AiTaskItem, { props: { task: makeTask({ status: 'CALLING_AI' }) } })

    expect(wrapper.text()).toContain('调用 AI')
    expect(wrapper.findComponent({ name: 'ElProgress' }).props('percentage')).toBe(68)
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('cancel')?.[0]?.[0]).toMatchObject({ localTaskId: 'task-1', status: 'CALLING_AI' })
  })

  it('keeps completed result and failed recovery events bound to their task', async () => {
    const completed = makeTask({ status: 'COMPLETED', progress: 100, resultPath: '/analysis/reports?reportId=9', cancellable: false })
    const completedWrapper = mount(AiTaskItem, { props: { task: completed } })
    await completedWrapper.get('button').trigger('click')
    expect(completedWrapper.emitted('view')?.[0]?.[0]).toEqual(completed)

    const failed = makeTask({ status: 'FAILED', errorMessage: '服务暂时不可用', cancellable: false })
    const failedWrapper = mount(AiTaskItem, { props: { task: failed } })
    expect(failedWrapper.text()).toContain('服务暂时不可用')
    await failedWrapper.get('button').trigger('click')
    expect(failedWrapper.emitted('retry')?.[0]?.[0]).toEqual(failed)
  })
})

describe('AI task item redesign', () => {
  it('exposes a textual stage and percentage alongside visual progress', () => {
    const wrapper = mount(AiTaskItem, { props: { task: makeTask({ status: 'CALLING_AI', progress: 68 }) } })

    expect(wrapper.get('[role="status"]').text()).toContain('阶段：调用 AI')
    expect(wrapper.get('[role="status"]').text()).toContain('进度 68%')
  })

  it('marks a newly completed result as waiting to be viewed without relying on color', () => {
    const wrapper = mount(AiTaskItem, {
      props: {
        task: makeTask({
          status: 'COMPLETED',
          progress: 100,
          resultPath: '/analysis/reports?reportId=9',
          notified: false,
          cancellable: false
        })
      }
    })

    expect(wrapper.get('[data-task-unread]').text()).toBe('待查看')
    expect(wrapper.text()).toContain('查看结果')
  })

  it('explains the failed-task recovery path next to retry', () => {
    const wrapper = mount(AiTaskItem, {
      props: { task: makeTask({ status: 'FAILED', errorMessage: '服务暂时不可用', cancellable: false }) }
    })

    expect(wrapper.get('[data-task-recovery]').text()).toContain('返回发起页面')
    expect(wrapper.text()).toContain('重试')
  })

  it('only offers retry when a failed task has a real recovery route', async () => {
    const recoverable = makeTask({ status: 'FAILED', cancellable: false, sourcePath: '/analysis/match' })
    const recoverableWrapper = mount(AiTaskItem, { props: { task: recoverable } })

    await recoverableWrapper.get('button').trigger('click')
    expect(recoverableWrapper.emitted('retry')?.[0]?.[0]).toEqual(recoverable)

    const unavailableWrapper = mount(AiTaskItem, {
      props: { task: makeTask({ status: 'FAILED', cancellable: false, sourcePath: undefined }) }
    })

    expect(unavailableWrapper.findAll('button').some((item) => item.text().trim() === '重试')).toBe(false)
    expect(unavailableWrapper.get('[data-task-recovery]').text()).toContain('无法从任务中心直接重试')
  })

  it('navigates a real failed task through the drawer retry action', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useAiTaskCenterStore()
    store.upsertByTaskNo({
      type: 'JOB_RECOMMENDATION',
      title: '岗位推荐',
      taskNo: 'FAILED_RECOMMENDATION',
      status: 'FAILED',
      sourcePath: '/job-recommendations',
      silent: true
    })
    store.openDrawer()
    const wrapper = mount(AiTaskDrawer, {
      global: {
        plugins: [pinia],
        stubs: {
          teleport: true,
          ElDrawer: {
            props: ['modelValue', 'title'],
            template: '<aside role="dialog" :aria-label="title"><slot /></aside>'
          }
        }
      }
    })

    await wrapper.get('.task-failed button').trigger('click')

    expect(push).toHaveBeenCalledWith('/job-recommendations')
    expect(store.tasks.some((task) => task.backendTaskNo === 'FAILED_RECOMMENDATION')).toBe(false)
  })
})
