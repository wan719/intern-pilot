import { flushPromises, mount } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import InterviewQuestionDetail from '@/views/interview/InterviewQuestionDetail.vue'
import InterviewQuestionList from '@/views/interview/InterviewQuestionList.vue'
import {
  generateInterviewQuestionsApi,
  getInterviewQuestionDetailApi,
  getInterviewQuestionReportsApi,
  regenerateInterviewQuestionsApi
} from '@/api/interviewQuestion'

const { listPush, taskCenter } = vi.hoisted(() => ({
  listPush: vi.fn(),
  taskCenter: {
    tasks: [] as any[],
    initialize: vi.fn(async () => undefined),
    createTask: vi.fn(),
    updateTask: vi.fn(),
    completeTask: vi.fn(),
    failTask: vi.fn()
  }
}))

vi.mock('@/router', () => ({ default: { push: listPush } }))
vi.mock('@/stores/auth', () => ({ useAuthStore: () => ({ hasPermission: () => true }) }))
vi.mock('@/stores/aiTaskCenter', () => ({ useAiTaskCenterStore: () => taskCenter }))
vi.mock('@/api/analysis', () => ({ getAnalysisReportsApi: vi.fn(async () => ({ records: [] })) }))
vi.mock('@/api/job', () => ({ getJobListApi: vi.fn(async () => ({ records: [] })) }))
vi.mock('@/api/resume', () => ({ getResumeListApi: vi.fn(async () => ({ records: [] })) }))
vi.mock('@/api/resumeVersion', () => ({ getResumeVersionListApi: vi.fn(async () => []) }))
vi.mock('@/api/interviewQuestion', () => ({
  deleteInterviewQuestionReportApi: vi.fn(),
  generateInterviewQuestionsApi: vi.fn(),
  getInterviewQuestionDetailApi: vi.fn(),
  getInterviewQuestionReportsApi: vi.fn(),
  regenerateInterviewQuestionsApi: vi.fn()
}))

const mockedDetail = vi.mocked(getInterviewQuestionDetailApi)
const mockedGenerate = vi.mocked(generateInterviewQuestionsApi)
const mockedReports = vi.mocked(getInterviewQuestionReportsApi)
const mockedRegenerate = vi.mocked(regenerateInterviewQuestionsApi)

const report = {
  reportId: 41,
  title: '星河科技前端实习生面试题',
  companyName: '星河科技',
  jobTitle: '前端实习生',
  questionCount: 0,
  createdAt: '2026-08-25T10:00:00',
  questions: []
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

async function mountDetail() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/interview-questions', component: { template: '<div />' } },
      { path: '/interview-questions/:id', component: { template: '<div />' } }
    ]
  })
  await router.push('/interview-questions/41')
  await router.isReady()
  const wrapper = mount(InterviewQuestionDetail, {
    global: { plugins: [router], stubs: { teleport: true } }
  })
  await flushPromises()
  return { router, wrapper }
}

async function mountList() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/interview-questions', component: { template: '<div />' } }]
  })
  await router.push('/interview-questions')
  await router.isReady()
  const wrapper = mount(InterviewQuestionList, {
    global: { plugins: [router], stubs: { teleport: true } }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  taskCenter.tasks = []
  taskCenter.createTask.mockImplementation((options: any) => {
    const localTaskId = `LOCAL_REGENERATE_${taskCenter.tasks.length + 1}`
    taskCenter.tasks.push({ ...options, localTaskId, status: 'RUNNING' })
    return localTaskId
  })
  taskCenter.completeTask.mockImplementation((localTaskId: string) => {
    const task = taskCenter.tasks.find((item) => item.localTaskId === localTaskId)
    if (task) task.status = 'COMPLETED'
  })
  taskCenter.failTask.mockImplementation((localTaskId: string) => {
    const task = taskCenter.tasks.find((item) => item.localTaskId === localTaskId)
    if (task) task.status = 'FAILED'
  })
  mockedDetail.mockResolvedValue(structuredClone(report) as any)
  mockedReports.mockResolvedValue({ records: [], total: 0 } as any)
  mockedGenerate.mockResolvedValue({ reportId: 99 } as any)
  mockedRegenerate.mockResolvedValue({ reportId: 42 } as any)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as any)
})

describe('interview generation cross-page lifecycle', () => {
  it('attaches List to Detail regeneration without orphaning, duplication, or detached Detail effects', async () => {
    const firstRequest = deferred<any>()
    mockedRegenerate.mockReset()
      .mockReturnValueOnce(firstRequest.promise)
      .mockResolvedValueOnce({ reportId: 43 } as any)
    const { router, wrapper: detailPage } = await mountDetail()
    const replace = vi.spyOn(router, 'replace')

    const firstSubmission = (detailPage.vm as any).regenerate()
    await flushPromises()
    expect(mockedRegenerate).toHaveBeenCalledTimes(1)
    expect(taskCenter.tasks[0]).toMatchObject({
      localTaskId: 'LOCAL_REGENERATE_1', type: 'INTERVIEW_REGENERATE', reportId: 41, status: 'RUNNING'
    })

    detailPage.unmount()
    await router.push('/interview-questions')
    const listPage = await mountList()
    expect(taskCenter.failTask).not.toHaveBeenCalled()
    expect((listPage.vm as any).regeneratingId).toBe(41)

    await (listPage.vm as any).regenerateReport(report)
    expect(mockedRegenerate).toHaveBeenCalledTimes(1)
    expect(taskCenter.createTask).toHaveBeenCalledTimes(1)

    firstRequest.resolve({ reportId: 42 })
    await firstSubmission
    await flushPromises()

    expect(taskCenter.completeTask).toHaveBeenCalledTimes(1)
    expect(taskCenter.completeTask).toHaveBeenCalledWith('LOCAL_REGENERATE_1', expect.objectContaining({ resultId: 42 }))
    expect(taskCenter.failTask).not.toHaveBeenCalled()
    expect(mockedDetail).toHaveBeenCalledTimes(1)
    expect(replace).not.toHaveBeenCalled()
    expect(router.currentRoute.value.fullPath).toBe('/interview-questions')
    expect((listPage.vm as any).regeneratingId).toBeNull()

    await (listPage.vm as any).regenerateReport(report)
    await flushPromises()
    expect(mockedRegenerate).toHaveBeenCalledTimes(2)
    expect(taskCenter.createTask).toHaveBeenCalledTimes(2)
    expect(taskCenter.completeTask).toHaveBeenCalledTimes(2)
    listPage.unmount()
  })

  it('settles a detached Detail failure in the task center without stale page effects', async () => {
    const request = deferred<any>()
    mockedRegenerate.mockReset().mockReturnValueOnce(request.promise)
    const { router, wrapper } = await mountDetail()
    const replace = vi.spyOn(router, 'replace')
    const submission = (wrapper.vm as any).regenerate()
    await flushPromises()
    wrapper.unmount()

    request.reject(new Error('regeneration unavailable'))
    await submission
    await flushPromises()

    expect(taskCenter.failTask).toHaveBeenCalledTimes(1)
    expect(taskCenter.completeTask).not.toHaveBeenCalled()
    expect(mockedDetail).toHaveBeenCalledTimes(1)
    expect(replace).not.toHaveBeenCalled()
    expect(ElMessage.error).not.toHaveBeenCalled()
  })
})
