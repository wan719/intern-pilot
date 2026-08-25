import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import InterviewQuestionList from '@/views/interview/InterviewQuestionList.vue'
import { getAnalysisReportsApi } from '@/api/analysis'
import {
  deleteInterviewQuestionReportApi,
  generateInterviewQuestionsApi,
  getInterviewQuestionDetailApi,
  getInterviewQuestionReportsApi,
  regenerateInterviewQuestionsApi
} from '@/api/interviewQuestion'
import { getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'
import { getResumeVersionListApi } from '@/api/resumeVersion'

const { push, taskCenter } = vi.hoisted(() => ({
  push: vi.fn(),
  taskCenter: {
    tasks: [] as any[],
    createTask: vi.fn(() => 'LOCAL_INTERVIEW_TASK'),
    updateTask: vi.fn(),
    completeTask: vi.fn(),
    failTask: vi.fn()
  }
}))

vi.mock('@/router', () => ({ default: { push } }))
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ hasPermission: (permission: string) => permission === 'analysis:delete' })
}))
vi.mock('@/stores/aiTaskCenter', () => ({ useAiTaskCenterStore: () => taskCenter }))
vi.mock('@/api/analysis', () => ({ getAnalysisReportsApi: vi.fn() }))
vi.mock('@/api/interviewQuestion', () => ({
  deleteInterviewQuestionReportApi: vi.fn(),
  generateInterviewQuestionsApi: vi.fn(),
  getInterviewQuestionDetailApi: vi.fn(),
  getInterviewQuestionReportsApi: vi.fn(),
  regenerateInterviewQuestionsApi: vi.fn()
}))
vi.mock('@/api/job', () => ({ getJobListApi: vi.fn() }))
vi.mock('@/api/resume', () => ({ getResumeListApi: vi.fn() }))
vi.mock('@/api/resumeVersion', () => ({ getResumeVersionListApi: vi.fn() }))

const mockedAnalysisReports = vi.mocked(getAnalysisReportsApi)
const mockedDelete = vi.mocked(deleteInterviewQuestionReportApi)
const mockedGenerate = vi.mocked(generateInterviewQuestionsApi)
const mockedDetail = vi.mocked(getInterviewQuestionDetailApi)
const mockedJobs = vi.mocked(getJobListApi)
const mockedReports = vi.mocked(getInterviewQuestionReportsApi)
const mockedRegenerate = vi.mocked(regenerateInterviewQuestionsApi)
const mockedResumes = vi.mocked(getResumeListApi)
const mockedVersions = vi.mocked(getResumeVersionListApi)

const report = {
  reportId: 41,
  title: '星河科技前端实习生面试题',
  resumeId: 3,
  resumeVersionId: 5,
  jobId: 7,
  companyName: '星河科技',
  jobTitle: '前端实习生',
  questionCount: 8,
  createdAt: '2026-08-25T10:00:00'
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

function reportWithId(reportId: number, overrides: Record<string, unknown> = {}) {
  return {
    ...report,
    reportId,
    title: `题单 ${reportId}`,
    createdAt: `2026-08-25T10:${String(reportId).padStart(2, '0')}:00`,
    ...overrides
  }
}

function button(wrapper: VueWrapper, label: string, index = 0) {
  const matches = wrapper.findAll('button').filter((item) => item.text().trim() === label)
  expect(matches.length).toBeGreaterThan(index)
  return matches[index]
}

async function mountPage() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/interview-questions', component: { template: '<div />' }, meta: { title: 'AI 面试题' } }]
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
  mockedResumes.mockResolvedValue({ records: [{ resumeId: 3, resumeName: '前端简历' }] } as any)
  mockedJobs.mockResolvedValue({ records: [{ jobId: 7, companyName: '星河科技', jobTitle: '前端实习生' }] } as any)
  mockedAnalysisReports.mockResolvedValue({ records: [{ reportId: 11, resumeId: 3, jobId: 7, matchScore: 88 }] } as any)
  mockedVersions.mockResolvedValue([
    { versionId: 4, versionName: '初稿', isCurrent: 0 },
    { versionId: 5, versionName: '当前版', isCurrent: 1 }
  ] as any)
  mockedReports.mockResolvedValue({ records: [report], total: 1 } as any)
  mockedDetail.mockResolvedValue({
    ...report,
    questions: [
      { questionId: 101, questionType: 'SPRING_BOOT', difficulty: 'MEDIUM' },
      { questionId: 102, questionType: 'PROJECT', difficulty: 'HARD' }
    ]
  } as any)
  mockedGenerate.mockResolvedValue({ reportId: 42 } as any)
  mockedRegenerate.mockResolvedValue({ reportId: 43 } as any)
  mockedDelete.mockResolvedValue(undefined as any)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as any)
})

describe('interview question list legacy contracts', () => {
  it('keeps existing category and difficulty API values exact', async () => {
    const wrapper = await mountPage()

    expect((wrapper.vm as any).categoryOptions.map((item: any) => item.value)).toEqual([
      'JAVA_BASIC',
      'SPRING_BOOT',
      'SPRING_SECURITY',
      'MYSQL',
      'REDIS',
      'PROJECT',
      'HR',
      'RESUME',
      'JOB_SKILL'
    ])
    expect((wrapper.vm as any).difficultyOptions.map((item: any) => item.value)).toEqual([
      'EASY',
      'MEDIUM',
      'HARD'
    ])
  })

  it('keeps option/list query payloads and detail route unchanged', async () => {
    const wrapper = await mountPage()

    expect(mockedResumes).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(mockedJobs).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(mockedAnalysisReports).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(mockedReports).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })

    Object.assign((wrapper.vm as any).query, { resumeId: 3, jobId: 7 })
    ;(wrapper.vm as any).search()
    await flushPromises()
    expect(mockedReports).toHaveBeenLastCalledWith({ resumeId: 3, jobId: 7, pageNum: 1, pageSize: 10 })

    await wrapper.get('.primary-study-action').trigger('click')
    expect(push).toHaveBeenCalledWith('/interview-questions/41')
  })

  it('keeps the full generation payload, task states and generated report route', async () => {
    const wrapper = await mountPage()
    Object.assign((wrapper.vm as any).form, {
      resumeId: 3,
      jobId: 7,
      analysisReportId: 11,
      questionCount: 8,
      categories: ['JAVA_BASIC', 'PROJECT'],
      difficulties: ['EASY', 'HARD'],
      includeAnswer: true,
      includeFollowUps: false
    })
    await flushPromises()

    await (wrapper.vm as any).generate()

    expect(mockedGenerate).toHaveBeenCalledWith(expect.objectContaining({
      resumeId: 3,
      resumeVersionId: 5,
      jobId: 7,
      analysisReportId: 11,
      questionCount: 8,
      categories: ['JAVA_BASIC', 'PROJECT'],
      difficulties: ['EASY', 'HARD'],
      includeAnswer: true,
      includeFollowUps: false
    }))
    expect(taskCenter.createTask).toHaveBeenCalledWith({
      type: 'INTERVIEW_QUESTION',
      title: '面试题生成',
      message: '正在生成面试题...',
      resumeId: 3,
      jobId: 7,
      reportId: 11,
      sourcePath: '/interview-questions'
    })
    expect(taskCenter.completeTask).toHaveBeenCalledWith('LOCAL_INTERVIEW_TASK', {
      resultId: 42,
      resultPath: '/interview-questions/42',
      message: '面试题生成完成'
    })
    expect(push).toHaveBeenCalledWith('/interview-questions/42')
  })

  it('keeps failed generation retryable and reports the existing failure state', async () => {
    mockedGenerate.mockRejectedValueOnce(new Error('generation unavailable'))
    const wrapper = await mountPage()
    Object.assign((wrapper.vm as any).form, { resumeId: 3, jobId: 7 })

    await (wrapper.vm as any).generate()

    expect(taskCenter.failTask).toHaveBeenCalledWith('LOCAL_INTERVIEW_TASK', '面试题生成失败')
    expect((wrapper.vm as any).generating).toBe(false)
    expect(push).not.toHaveBeenCalled()
  })

  it('keeps delete and regeneration confirmations, ids and task transitions', async () => {
    const wrapper = await mountPage()

    await (wrapper.vm as any).removeReport(report)
    expect(ElMessageBox.confirm).toHaveBeenNthCalledWith(
      1,
      '确认删除“星河科技前端实习生面试题”吗？删除后将无法继续查看这套题。',
      '删除确认',
      { type: 'warning' }
    )
    expect(mockedDelete).toHaveBeenCalledWith(41)

    await (wrapper.vm as any).regenerateReport(report)
    expect(ElMessageBox.confirm).toHaveBeenNthCalledWith(
      2,
      '确认重新生成“星河科技前端实习生面试题”吗？旧的题目将被替换。',
      '重新生成确认',
      { type: 'warning' }
    )
    expect(mockedRegenerate).toHaveBeenCalledWith(41)
    expect(taskCenter.createTask).toHaveBeenLastCalledWith({
      type: 'INTERVIEW_REGENERATE',
      title: '面试题重新生成',
      message: '正在重新生成面试题...',
      reportId: 41,
      sourcePath: '/interview-questions'
    })
    expect(push).toHaveBeenCalledWith('/interview-questions/43')
  })
})

describe('interview preparation queue redesign', () => {
  it('keeps the newest query snapshot when an older list request resolves last', async () => {
    const first = deferred<any>()
    const second = deferred<any>()
    mockedReports.mockReset()
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise)

    const wrapper = await mountPage()
    ;(wrapper.vm as any).query.pageNum = 2
    const latestLoad = (wrapper.vm as any).loadReports()
    second.resolve({ records: [reportWithId(82)], total: 22 })
    await latestLoad
    await flushPromises()

    first.resolve({ records: [reportWithId(41)], total: 22 })
    await flushPromises()

    expect(mockedReports).toHaveBeenNthCalledWith(1, { pageNum: 1, pageSize: 10 })
    expect(mockedReports).toHaveBeenNthCalledWith(2, { pageNum: 2, pageSize: 10 })
    expect((wrapper.vm as any).reports.map((item: any) => item.reportId)).toEqual([82])
    expect(Object.keys((wrapper.vm as any).reportMetadata)).toEqual(['82'])
    expect(wrapper.find('[data-report-id="82"]').exists()).toBe(true)
    expect(wrapper.find('[data-report-id="41"]').exists()).toBe(false)
  })

  it('stops superseded metadata scheduling and ignores its late commits', async () => {
    const staleDetails = Array.from({ length: 4 }, () => deferred<any>())
    mockedReports.mockReset()
      .mockResolvedValueOnce({ records: Array.from({ length: 6 }, (_, index) => reportWithId(index + 1)), total: 6 } as any)
      .mockResolvedValueOnce({ records: [reportWithId(99)], total: 1 } as any)
    mockedDetail.mockReset().mockImplementation((reportId: number) => {
      if (reportId <= 4) return staleDetails[reportId - 1].promise
      return Promise.resolve({ questions: [{ questionType: 'PROJECT', difficulty: 'HARD' }] }) as any
    })
    const wrapper = await mountPage()
    expect(mockedDetail).toHaveBeenCalledTimes(4)

    ;(wrapper.vm as any).query.pageNum = 2
    await (wrapper.vm as any).loadReports()
    await flushPromises()
    expect(mockedDetail).toHaveBeenCalledWith(99)

    staleDetails.forEach((request) => request.resolve({ questions: [{ questionType: 'JAVA_BASIC', difficulty: 'EASY' }] }))
    await flushPromises()

    expect(mockedDetail).not.toHaveBeenCalledWith(5)
    expect(mockedDetail).not.toHaveBeenCalledWith(6)
    expect(Object.keys((wrapper.vm as any).reportMetadata)).toEqual(['99'])
    expect(wrapper.get('[data-report-id="99"]').text()).toContain('项目经历')
  })

  it('reuses metadata by report signature and invalidates it when the list version changes', async () => {
    const wrapper = await mountPage()
    expect(mockedDetail).toHaveBeenCalledTimes(1)

    await (wrapper.vm as any).loadReports()
    await flushPromises()
    expect(mockedDetail).toHaveBeenCalledTimes(1)

    mockedReports.mockResolvedValueOnce({
      records: [{ ...report, createdAt: '2026-08-26T10:00:00' }],
      total: 1
    } as any)
    await (wrapper.vm as any).loadReports()
    await flushPromises()
    expect(mockedDetail).toHaveBeenCalledTimes(2)
  })

  it('renders list records before slow taxonomy hydration and bounds detail concurrency at four', async () => {
    const detailRequests = Array.from({ length: 6 }, () => deferred<any>())
    mockedReports.mockResolvedValueOnce({
      records: detailRequests.map((_, index) => reportWithId(index + 1)),
      total: 6
    } as any)
    mockedDetail.mockReset()
    detailRequests.forEach(({ promise }) => mockedDetail.mockReturnValueOnce(promise))

    const wrapper = await mountPage()

    expect(wrapper.findAll('.preparation-card')).toHaveLength(6)
    expect(wrapper.find('.queue-loading').exists()).toBe(false)
    expect(mockedDetail).toHaveBeenCalledTimes(4)
    expect(wrapper.findAll('[data-metadata-state="loading"]')).toHaveLength(6)

    detailRequests.slice(0, 4).forEach((request) => request.resolve({ questions: [] }))
    await flushPromises()
    expect(mockedDetail).toHaveBeenCalledTimes(6)
  })

  it('does not refetch list or detail when applying only local taxonomy filters', async () => {
    const wrapper = await mountPage()
    const listCalls = mockedReports.mock.calls.length
    const detailCalls = mockedDetail.mock.calls.length

    ;(wrapper.vm as any).filters.category = 'SPRING_BOOT'
    await button(wrapper, '应用筛选').trigger('click')
    await flushPromises()

    expect(mockedReports).toHaveBeenCalledTimes(listCalls)
    expect(mockedDetail).toHaveBeenCalledTimes(detailCalls)
  })

  it('keeps unavailable metadata visible under active filters and retries it independently', async () => {
    const recovered = deferred<any>()
    mockedReports.mockResolvedValueOnce({ records: [reportWithId(41), reportWithId(42)], total: 2 } as any)
    mockedDetail.mockImplementation((reportId: number) => {
      if (reportId === 41) return Promise.resolve({ questions: [{ questionType: 'SPRING_BOOT', difficulty: 'MEDIUM' }] }) as any
      return Promise.reject(new Error('detail unavailable'))
    })
    const wrapper = await mountPage()
    ;(wrapper.vm as any).filters.category = 'SPRING_BOOT'
    await flushPromises()

    expect(wrapper.findAll('.preparation-card')).toHaveLength(2)
    expect(wrapper.get('[data-metadata-warning]').text()).toContain('筛选结果可能不完整')
    expect(wrapper.get('[data-report-id="42"]').text()).toContain('待确认')

    mockedDetail.mockImplementationOnce(() => recovered.promise)
    const retry = wrapper.get('[data-report-id="42"] [data-metadata-retry]')
    await retry.trigger('click')
    recovered.resolve({ questions: [{ questionType: 'SPRING_BOOT', difficulty: 'HARD' }] })
    await flushPromises()

    expect(mockedReports).toHaveBeenCalledTimes(1)
    expect(mockedDetail).toHaveBeenCalledTimes(3)
    expect((wrapper.vm as any).reportMetadata).toMatchObject({
      41: { state: 'available' },
      42: { state: 'available' }
    })
    expect(wrapper.find('[data-metadata-warning]').exists()).toBe(false)
    expect(wrapper.get('[data-report-id="42"]').text()).toContain('较难')
  })

  it('opens a circuit after repeated metadata failures instead of scheduling every detail', async () => {
    mockedReports.mockResolvedValueOnce({
      records: Array.from({ length: 10 }, (_, index) => reportWithId(index + 1)),
      total: 10
    } as any)
    mockedDetail.mockRejectedValue(new Error('metadata service unavailable'))

    const wrapper = await mountPage()
    await flushPromises()

    expect(mockedDetail.mock.calls.length).toBeLessThanOrEqual(4)
    expect(wrapper.get('[data-metadata-bulk-warning]').text()).toContain('部分题单分类暂时无法确认')
    expect(wrapper.findAll('[data-metadata-state="unavailable"]')).toHaveLength(10)
  })

  it('reconciles restored local generation, uses a retry path, and deduplicates live submission', async () => {
    taskCenter.tasks = [{
      localTaskId: 'RESTORED_INTERVIEW',
      type: 'INTERVIEW_QUESTION',
      status: 'RUNNING'
    }]
    const generation = deferred<any>()
    mockedGenerate.mockReturnValueOnce(generation.promise)
    const wrapper = await mountPage()

    expect(taskCenter.failTask).toHaveBeenCalledWith(
      'RESTORED_INTERVIEW',
      '面试题生成已中断，请返回面试题页面重试'
    )
    expect(taskCenter.updateTask).toHaveBeenCalledWith(
      'RESTORED_INTERVIEW',
      { sourcePath: '/interview-questions' },
      { notify: false }
    )

    Object.assign((wrapper.vm as any).form, { resumeId: 3, jobId: 7 })
    const firstSubmission = (wrapper.vm as any).generate()
    const duplicateSubmission = (wrapper.vm as any).generate()
    await flushPromises()
    expect(mockedGenerate).toHaveBeenCalledTimes(1)
    expect(taskCenter.createTask).toHaveBeenCalledTimes(1)

    generation.resolve({ reportId: 42 })
    await Promise.all([firstSubmission, duplicateSubmission])
  })

  it('uses one action-oriented heading and FilterBar with category, difficulty and status values', async () => {
    const wrapper = await mountPage()

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.get('h1').text()).toBe('练好下一场面试')
    expect(wrapper.findComponent({ name: 'FilterBar' }).exists()).toBe(true)
    expect(wrapper.find('[aria-label="按题目分类筛选"]').exists()).toBe(true)
    expect(wrapper.find('[aria-label="按难度筛选"]').exists()).toBe(true)
    expect(wrapper.find('[aria-label="按准备状态筛选"]').exists()).toBe(true)
    expect((wrapper.vm as any).statusOptions.map((item: any) => item.value)).toEqual([
      'COMPLETED',
      'RUNNING',
      'FAILED'
    ])
  })

  it('renders a hydrated preparation card with role, taxonomy, count, status and one primary study action', async () => {
    const wrapper = await mountPage()
    const card = wrapper.get('.preparation-card')

    expect(mockedDetail).toHaveBeenCalledWith(41)
    expect(card.text()).toContain('星河科技')
    expect(card.text()).toContain('前端实习生')
    expect(card.text()).toContain('Spring Boot')
    expect(card.text()).toContain('项目经历')
    expect(card.text()).toContain('中等')
    expect(card.text()).toContain('较难')
    expect(card.text()).toContain('8 题')
    expect(card.text()).toContain('可开始练习')
    expect(card.findAll('.primary-study-action')).toHaveLength(1)
    expect(card.get('.primary-study-action').text()).toBe('开始练习')
    expect(card.findAll('.el-button--primary')).toHaveLength(1)
  })

  it('filters the current preparation queue locally without changing the list API payload', async () => {
    const wrapper = await mountPage()
    Object.assign((wrapper.vm as any).filters, {
      category: 'SPRING_BOOT',
      difficulty: 'MEDIUM',
      status: 'COMPLETED'
    })
    await flushPromises()
    expect(wrapper.findAll('.preparation-card')).toHaveLength(1)

    ;(wrapper.vm as any).filters.difficulty = 'EASY'
    await flushPromises()
    expect(wrapper.findAll('.preparation-card')).toHaveLength(0)
    expect(wrapper.get('.app-empty').text()).toContain('没有符合当前筛选的面试题')
    expect(mockedReports).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 10 })
  })

  it('keeps list loading, failure/retry and empty states distinct and truthful', async () => {
    mockedReports.mockReturnValueOnce(new Promise<any>(() => {}))
    const loadingPage = await mountPage()
    expect(loadingPage.get('.queue-shell[aria-busy="true"]').text()).toContain('正在加载面试准备队列')
    expect(loadingPage.find('.app-empty').exists()).toBe(false)

    mockedReports.mockRejectedValueOnce(new Error('queue unavailable'))
    const failedPage = await mountPage()
    expect(failedPage.get('[role="alert"]').text()).toContain('queue unavailable')
    expect(failedPage.get('[data-list-retry]').text()).toContain('重新加载')

    mockedReports.mockResolvedValueOnce({ records: [], total: 0 } as any)
    const emptyPage = await mountPage()
    expect(emptyPage.get('.app-empty').text()).toContain('还没有面试准备内容')
    expect(emptyPage.get('[data-empty-generate]').text()).toContain('生成第一套面试题')
  })

  it('shows persistent generation loading and failure with a retry action', async () => {
    let rejectGeneration!: (reason?: unknown) => void
    mockedGenerate.mockReturnValueOnce(new Promise<any>((_resolve, reject) => {
      rejectGeneration = reject
    }))
    const wrapper = await mountPage()
    ;(wrapper.vm as any).openGenerate()
    Object.assign((wrapper.vm as any).form, { resumeId: 3, jobId: 7 })

    const submission = (wrapper.vm as any).generate()
    await flushPromises()
    expect(wrapper.get('[data-generation-state]').attributes('role')).toBe('status')
    expect(wrapper.get('[data-generation-state]').text()).toContain('正在生成面试题')

    rejectGeneration(new Error('generation unavailable'))
    await submission
    await flushPromises()
    expect((wrapper.vm as any).generateVisible).toBe(false)
    expect(wrapper.get('[data-generation-error]').attributes('role')).toBe('alert')
    expect(wrapper.get('[data-generation-error]').text()).toContain('generation unavailable')
    expect(wrapper.get('[data-generation-retry]').text()).toContain('重试生成')
  })

  it('uses one responsive card DOM so mobile does not duplicate focus targets', async () => {
    const wrapper = await mountPage()

    expect(wrapper.findAll('[data-report-id="41"]')).toHaveLength(1)
    expect(wrapper.findAll('.preparation-card .primary-study-action')).toHaveLength(1)
  })
})
