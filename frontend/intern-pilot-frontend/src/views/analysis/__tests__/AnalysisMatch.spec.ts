import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AnalysisMatch from '@/views/analysis/AnalysisMatch.vue'
import { createAnalysisTaskApi, getAnalysisTaskDetailApi } from '@/api/analysisTask'
import { getJobDetailApi, getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'
import { getResumeVersionListApi } from '@/api/resumeVersion'
import { createAnalysisSocket } from '@/utils/analysisSocket'

const { push, taskCenter } = vi.hoisted(() => ({
  push: vi.fn(),
  taskCenter: {
    upsertByTaskNo: vi.fn(() => 'LOCAL_ANALYSIS_TASK'),
    updateTask: vi.fn(),
    openDrawer: vi.fn()
  }
}))

vi.mock('@/router', () => ({ default: { push } }))
vi.mock('@/stores/aiTaskCenter', () => ({ useAiTaskCenterStore: () => taskCenter }))
vi.mock('@/api/analysisTask', () => ({
  createAnalysisTaskApi: vi.fn(),
  getAnalysisTaskDetailApi: vi.fn()
}))
vi.mock('@/api/job', () => ({ getJobDetailApi: vi.fn(), getJobListApi: vi.fn() }))
vi.mock('@/api/resume', () => ({ getResumeListApi: vi.fn() }))
vi.mock('@/api/resumeVersion', () => ({ getResumeVersionListApi: vi.fn() }))
vi.mock('@/utils/analysisSocket', () => ({ createAnalysisSocket: vi.fn() }))

const mockedCreateTask = vi.mocked(createAnalysisTaskApi)
const mockedTaskDetail = vi.mocked(getAnalysisTaskDetailApi)
const mockedJobDetail = vi.mocked(getJobDetailApi)
const mockedJobs = vi.mocked(getJobListApi)
const mockedResumes = vi.mocked(getResumeListApi)
const mockedVersions = vi.mocked(getResumeVersionListApi)
const mockedSocket = vi.mocked(createAnalysisSocket)

const resume = { resumeId: 3, resumeName: '前端实习简历', originalFileName: 'resume.pdf' }
const versions = [
  { versionId: 4, versionName: '初稿', isCurrent: 0 },
  { versionId: 5, versionName: '当前版', isCurrent: 1 }
]
const job = { jobId: 7, companyName: '星河科技', jobTitle: '前端实习生', location: '上海' }
const jobDetail = {
  ...job,
  jdContent: '负责 Vue 3 与 TypeScript 项目开发',
  skillRequirements: 'Vue 3\nTypeScript'
}

type SocketCallbacks = {
  onMessage: (message: any) => void
  onError?: (error: any) => void
}

type SocketClientMock = {
  deactivate: ReturnType<typeof vi.fn>
  onWebSocketClose?: (event: any) => void
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: any) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

let socketClient: SocketClientMock
let socketClients: SocketClientMock[] = []
let socketReturnIndex = 0
const mountedWrappers = new Set<VueWrapper>()

function lastSocketCallbacks(): SocketCallbacks {
  const call = mockedSocket.mock.calls.at(-1)
  expect(call).toBeDefined()
  return { onMessage: call![1], onError: call![2] }
}

async function mountPage(path = '/analysis/match') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/analysis/match', component: { template: '<div />' }, meta: { title: 'AI 匹配分析' } }]
  })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(AnalysisMatch, {
    global: { plugins: [router], stubs: { teleport: true } }
  })
  mountedWrappers.add(wrapper)
  await flushPromises()
  return wrapper
}

function selectValidInputs(wrapper: VueWrapper) {
  Object.assign((wrapper.vm as any).form, {
    resumeId: 3,
    resumeVersionId: 5,
    jobId: 7,
    forceRefresh: true
  })
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.clearAllMocks()
  socketClient = { deactivate: vi.fn() }
  socketClients = [socketClient]
  socketReturnIndex = 0
  localStorage.clear()
  mockedResumes.mockResolvedValue({ records: [resume] } as any)
  mockedJobs.mockResolvedValue({ records: [job] } as any)
  mockedVersions.mockResolvedValue(versions as any)
  mockedJobDetail.mockResolvedValue(jobDetail as any)
  mockedCreateTask.mockResolvedValue({
    taskNo: 'ANALYSIS_001',
    status: 'PENDING',
    progress: 5,
    message: '任务已提交'
  } as any)
  mockedSocket.mockImplementation(() => {
    const client = socketClients[socketReturnIndex] || { deactivate: vi.fn() }
    socketClients[socketReturnIndex] = client
    socketReturnIndex += 1
    return client as any
  })
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'info').mockImplementation(() => undefined as any)
})

afterEach(async () => {
  mountedWrappers.forEach((wrapper) => wrapper.unmount())
  mountedWrappers.clear()
  await flushPromises()
  vi.clearAllTimers()
  vi.useRealTimers()
})

describe('analysis match state and contract characterization', () => {
  it('keeps idle and validating states from creating or registering a task', async () => {
    const wrapper = await mountPage()

    expect((wrapper.vm as any).running).toBe(false)
    expect((wrapper.vm as any).task.taskNo).toBe('')
    expect(wrapper.text()).toContain('等待发起 AI 匹配分析')

    await (wrapper.vm as any).startTask()

    expect(ElMessage.warning).toHaveBeenCalledWith('请选择简历和岗位')
    expect(mockedCreateTask).not.toHaveBeenCalled()
    expect(taskCenter.upsertByTaskNo).not.toHaveBeenCalled()
    expect(mockedSocket).not.toHaveBeenCalled()
  })

  it('keeps the exact creation payload and an honest submitted state while the request is pending', async () => {
    let resolveCreate!: (value: any) => void
    mockedCreateTask.mockReturnValueOnce(new Promise((resolve) => { resolveCreate = resolve }) as any)
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()

    const submission = (wrapper.vm as any).startTask()
    await flushPromises()

    expect(JSON.parse(JSON.stringify(mockedCreateTask.mock.calls[0][0]))).toEqual({
      resumeId: 3,
      resumeVersionId: 5,
      jobId: 7,
      forceRefresh: true
    })
    expect((wrapper.vm as any).running).toBe(true)
    expect((wrapper.vm as any).task.taskNo).toBe('')
    expect(wrapper.get('[data-analysis-submit]').attributes('disabled')).toBeDefined()
    expect(taskCenter.upsertByTaskNo).not.toHaveBeenCalled()

    resolveCreate({ taskNo: 'ANALYSIS_001', status: 'PENDING', progress: 5, message: '任务已提交' })
    await submission
    await flushPromises()
  })

  it('keeps submitted-to-processing store transitions, one socket and one polling fallback', async () => {
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()

    await (wrapper.vm as any).startTask()
    await flushPromises()

    expect(taskCenter.upsertByTaskNo).toHaveBeenCalledWith({
      type: 'ANALYSIS_MATCH',
      title: 'AI 简历匹配分析',
      message: '正在创建分析任务...',
      taskNo: 'ANALYSIS_001',
      status: 'PENDING',
      progress: 5,
      sourcePath: '/analysis/match'
    })
    expect(taskCenter.updateTask).toHaveBeenLastCalledWith('LOCAL_ANALYSIS_TASK', {
      backendTaskNo: 'ANALYSIS_001',
      status: 'PENDING',
      progress: 5,
      message: '任务已提交',
      resultId: undefined,
      errorMessage: ''
    })
    expect(mockedSocket).toHaveBeenCalledTimes(1)
    expect(mockedSocket).toHaveBeenCalledWith('ANALYSIS_001', expect.any(Function), expect.any(Function))
    expect(localStorage.getItem('internpilot:analysis:lastTaskNo')).toBe('ANALYSIS_001')

    mockedTaskDetail.mockResolvedValueOnce({
      taskNo: 'ANALYSIS_001', status: 'CALLING_AI', progress: 68, message: '正在生成匹配结论'
    } as any)
    await vi.advanceTimersByTimeAsync(3000)

    expect(mockedTaskDetail).toHaveBeenCalledWith('ANALYSIS_001')
    expect((wrapper.vm as any).task).toMatchObject({
      taskNo: 'ANALYSIS_001', status: 'CALLING_AI', progress: 68, message: '正在生成匹配结论'
    })
    expect(mockedCreateTask).toHaveBeenCalledTimes(1)
  })

  it('keeps WebSocket errors on the same reconnecting client while polling continues without duplicate setup', async () => {
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()
    await (wrapper.vm as any).startTask()
    await flushPromises()

    const callbacks = lastSocketCallbacks()
    callbacks.onError?.(new Error('socket interrupted'))
    callbacks.onMessage({
      taskNo: 'ANALYSIS_001', status: 'GENERATING_REPORT', progress: 92, message: '连接恢复，正在保存报告'
    })
    mockedTaskDetail.mockResolvedValueOnce({
      taskNo: 'ANALYSIS_001', status: 'GENERATING_REPORT', progress: 94, message: '轮询同步进度'
    } as any)
    await vi.advanceTimersByTimeAsync(3000)

    expect(ElMessage.warning).toHaveBeenCalledWith('WebSocket 连接异常，已使用轮询兜底')
    expect(mockedSocket).toHaveBeenCalledTimes(1)
    expect(mockedTaskDetail).toHaveBeenCalledTimes(1)
    expect((wrapper.vm as any).task.progress).toBe(94)
    expect(mockedCreateTask).toHaveBeenCalledTimes(1)
  })

  it('serializes polling and rejects an in-flight processing response after WebSocket completion', async () => {
    const pendingPoll = deferred<any>()
    mockedTaskDetail.mockReturnValueOnce(pendingPoll.promise as any)
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()
    await (wrapper.vm as any).startTask()
    await flushPromises()

    await vi.advanceTimersByTimeAsync(6000)
    expect(mockedTaskDetail).toHaveBeenCalledTimes(1)

    lastSocketCallbacks().onMessage({
      taskNo: 'ANALYSIS_001', status: 'COMPLETED', progress: 100, message: '报告已生成', reportId: 91
    })
    pendingPoll.resolve({
      taskNo: 'ANALYSIS_001', status: 'CALLING_AI', progress: 68, message: '过期的轮询响应'
    })
    await flushPromises()
    await vi.advanceTimersByTimeAsync(6000)

    expect((wrapper.vm as any).task).toMatchObject({
      status: 'COMPLETED', progress: 100, message: '报告已生成', reportId: 91
    })
    expect(mockedTaskDetail).toHaveBeenCalledTimes(1)
  })

  it('keeps failed tasks retryable and terminal cleanup prevents old polling updates', async () => {
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()
    await (wrapper.vm as any).startTask()
    await flushPromises()

    lastSocketCallbacks().onMessage({
      taskNo: 'ANALYSIS_001', status: 'FAILED', progress: 42, message: '生成失败', errorMessage: '模型超时'
    })

    expect((wrapper.vm as any).running).toBe(false)
    expect((wrapper.vm as any).task).toMatchObject({ status: 'FAILED', progress: 42, errorMessage: '模型超时' })
    expect(socketClient.deactivate).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(6000)
    expect(mockedTaskDetail).not.toHaveBeenCalled()

    await (wrapper.vm as any).startTask()
    await flushPromises()
    expect(mockedCreateTask).toHaveBeenCalledTimes(2)
  })

  it('keeps completed result bookkeeping and existing report/interview routes', async () => {
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()
    await (wrapper.vm as any).startTask()
    await flushPromises()

    lastSocketCallbacks().onMessage({
      taskNo: 'ANALYSIS_001', status: 'COMPLETED', progress: 100, message: '报告已生成', reportId: 91
    })

    expect(taskCenter.updateTask).toHaveBeenLastCalledWith('LOCAL_ANALYSIS_TASK', {
      backendTaskNo: 'ANALYSIS_001',
      status: 'COMPLETED',
      progress: 100,
      message: '报告已生成',
      resultId: 91,
      errorMessage: '',
      resultPath: '/analysis/reports?reportId=91'
    })
    expect((wrapper.vm as any).running).toBe(false)
    expect(socketClient.deactivate).toHaveBeenCalledTimes(1)

    ;(wrapper.vm as any).goReports()
    ;(wrapper.vm as any).goInterviewQuestions()
    expect(push).toHaveBeenNthCalledWith(1, '/analysis/reports')
    expect(push).toHaveBeenNthCalledWith(2, {
      path: '/interview-questions',
      query: { resumeId: 3, resumeVersionId: 5, jobId: 7, reportId: 91 }
    })
  })

  it('restores a processing task after refresh without resubmitting it', async () => {
    localStorage.setItem('internpilot:analysis:lastTaskNo', 'ANALYSIS_RESTORED')
    mockedTaskDetail.mockResolvedValueOnce({
      taskNo: 'ANALYSIS_RESTORED',
      status: 'BUILDING_CONTEXT',
      progress: 36,
      message: '正在构建岗位上下文',
      resumeId: 3,
      jobId: 7
    } as any)

    const wrapper = await mountPage()

    expect(mockedTaskDetail).toHaveBeenCalledWith('ANALYSIS_RESTORED')
    expect(mockedCreateTask).not.toHaveBeenCalled()
    expect(taskCenter.upsertByTaskNo).toHaveBeenCalledWith({
      type: 'ANALYSIS_MATCH',
      title: 'AI 简历匹配分析',
      taskNo: 'ANALYSIS_RESTORED',
      status: 'BUILDING_CONTEXT',
      progress: 36,
      message: '正在构建岗位上下文',
      reportId: undefined,
      sourcePath: '/analysis/match'
    })
    expect((wrapper.vm as any).form).toMatchObject({ resumeId: 3, jobId: 7 })
    expect((wrapper.vm as any).running).toBe(true)
    expect(mockedSocket).toHaveBeenCalledTimes(1)
    expect(mockedSocket).toHaveBeenCalledWith('ANALYSIS_RESTORED', expect.any(Function), expect.any(Function))
  })
})

describe('analysis match redesign', () => {
  it('uses one action-led heading and an explicit resume-job-confirm sequence', async () => {
    const wrapper = await mountPage()

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.get('h1').text()).toBe('确认这次 AI 匹配')
    expect(wrapper.findAll('.setup-step')).toHaveLength(3)
    expect(wrapper.get('[data-setup-step="resume"]').text()).toContain('1')
    expect(wrapper.get('[data-setup-step="resume"]').text()).toContain('选择简历')
    expect(wrapper.get('[data-setup-step="job"]').text()).toContain('选择目标岗位')
    expect(wrapper.get('[data-setup-step="confirm"]').text()).toContain('确认并开始')

    const primary = wrapper.get('[data-analysis-submit]')
    expect(primary.text()).toContain('请先选择简历和岗位')
    expect(primary.attributes('disabled')).toBeDefined()
  })

  it('keeps primary action copy and disabled/loading state honest through selection and submission', async () => {
    let resolveCreate!: (value: any) => void
    mockedCreateTask.mockReturnValueOnce(new Promise((resolve) => { resolveCreate = resolve }) as any)
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()

    expect(wrapper.get('[data-analysis-submit]').text()).toContain('开始生成匹配报告')
    expect(wrapper.get('[data-analysis-submit]').attributes('disabled')).toBeUndefined()

    const submission = (wrapper.vm as any).startTask()
    await flushPromises()
    expect(wrapper.get('[data-analysis-submit]').text()).toContain('正在提交分析任务')
    expect(wrapper.get('[data-analysis-submit]').attributes('disabled')).toBeDefined()

    resolveCreate({ taskNo: 'ANALYSIS_001', status: 'PENDING', progress: 5, message: '任务已提交' })
    await submission
    await flushPromises()
    expect(wrapper.get('[data-analysis-submit]').text()).toContain('AI 正在分析 · 5%')
  })

  it('shows percentage, textual stage and a retryable reconnect state without duplicating polling', async () => {
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()
    await (wrapper.vm as any).startTask()
    await flushPromises()

    const callbacks = lastSocketCallbacks()
    callbacks.onMessage({
      taskNo: 'ANALYSIS_001', status: 'CALLING_AI', progress: 68, message: '正在生成匹配结论'
    })
    await flushPromises()
    expect(wrapper.get('[data-progress-percentage]').text()).toContain('68%')
    expect(wrapper.get('[data-progress-stage]').text()).toContain('正在生成匹配结论')
    expect(wrapper.get('[data-connection-state]').text()).toContain('实时连接正常')

    callbacks.onError?.(new Error('socket interrupted'))
    await flushPromises()
    expect(wrapper.get('[data-connection-state]').text()).toContain('实时连接中断，轮询同步中')
    await wrapper.get('[data-reconnect-action]').trigger('click')
    await flushPromises()

    expect(mockedSocket).toHaveBeenCalledTimes(2)
    expect(socketClient.deactivate).toHaveBeenCalledTimes(1)
    expect(mockedCreateTask).toHaveBeenCalledTimes(1)
  })

  it('announces current, completed and pending progress steps without relying on color', async () => {
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()
    await (wrapper.vm as any).startTask()
    await flushPromises()

    lastSocketCallbacks().onMessage({
      taskNo: 'ANALYSIS_001', status: 'CALLING_AI', progress: 68, message: '正在生成匹配结论'
    })
    await flushPromises()

    const steps = wrapper.findAll('.progress-steps li')
    expect(steps).toHaveLength(6)
    expect(wrapper.get('.progress-steps li[aria-current="step"]').text()).toContain('调用 AI')
    expect(steps[0].text()).toContain('已完成')
    expect(steps[4].text()).toContain('待处理')
  })

  it('moves to polling fallback when the existing client closes before reconnecting', async () => {
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()
    await (wrapper.vm as any).startTask()
    await flushPromises()

    expect(typeof (socketClient as any).onWebSocketClose).toBe('function')
    ;(socketClient as any).onWebSocketClose({ code: 1011 })
    await flushPromises()

    expect(wrapper.get('[data-connection-state]').text()).toContain('实时连接中断，轮询同步中')
    expect(wrapper.find('[data-reconnect-action]').exists()).toBe(true)
  })

  it('waits for the old client to deactivate before creating a manual reconnect client', async () => {
    let resolveDeactivate!: () => void
    socketClient.deactivate.mockReturnValueOnce(new Promise<void>((resolve) => { resolveDeactivate = resolve }) as any)
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()
    await (wrapper.vm as any).startTask()
    await flushPromises()

    lastSocketCallbacks().onError?.(new Error('socket interrupted'))
    const reconnecting = (wrapper.vm as any).retryConnection()
    await flushPromises()
    expect(mockedSocket).toHaveBeenCalledTimes(1)

    resolveDeactivate()
    await reconnecting
    await flushPromises()
    expect(mockedSocket).toHaveBeenCalledTimes(2)
  })

  it('locks concurrent reconnects and ignores every callback from the replaced socket attempt', async () => {
    const oldDeactivate = deferred<void>()
    socketClient.deactivate.mockReturnValueOnce(oldDeactivate.promise as any)
    const replacementClient: SocketClientMock = { deactivate: vi.fn() }
    socketClients.push(replacementClient)
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()
    await (wrapper.vm as any).startTask()
    await flushPromises()
    const oldCallbacks = lastSocketCallbacks()

    oldCallbacks.onError?.(new Error('socket interrupted'))
    const firstRetry = (wrapper.vm as any).retryConnection()
    const secondRetry = (wrapper.vm as any).retryConnection()
    await flushPromises()
    expect((wrapper.vm as any).reconnectPending).toBe(true)
    expect(mockedSocket).toHaveBeenCalledTimes(1)
    expect(wrapper.get('[data-reconnect-action]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-reconnect-action]').text()).toContain('正在重新连接')

    oldDeactivate.resolve()
    await Promise.all([firstRetry, secondRetry])
    await flushPromises()
    expect(mockedSocket).toHaveBeenCalledTimes(2)

    const replacementCallbacks = lastSocketCallbacks()
    replacementCallbacks.onMessage({
      taskNo: 'ANALYSIS_001', status: 'CALLING_AI', progress: 70, message: '新连接进度'
    })
    oldCallbacks.onMessage({
      taskNo: 'ANALYSIS_001', status: 'BUILDING_CONTEXT', progress: 20, message: '旧连接进度'
    })
    oldCallbacks.onError?.(new Error('old socket error'))
    socketClient.onWebSocketClose?.({ code: 1011 })
    await flushPromises()

    expect((wrapper.vm as any).task).toMatchObject({ progress: 70, message: '新连接进度' })
    expect(wrapper.get('[data-connection-state]').text()).toContain('实时连接正常')
    expect((wrapper.vm as any).reconnectPending).toBe(false)
  })

  it('ignores late messages from an old task so they cannot replace a newer retry', async () => {
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()
    await (wrapper.vm as any).startTask()
    await flushPromises()
    const oldCallbacks = lastSocketCallbacks()
    oldCallbacks.onMessage({
      taskNo: 'ANALYSIS_001', status: 'FAILED', progress: 40, message: '第一次失败', errorMessage: '模型超时'
    })

    mockedCreateTask.mockResolvedValueOnce({
      taskNo: 'ANALYSIS_002', status: 'PENDING', progress: 5, message: '第二次已提交'
    } as any)
    await (wrapper.vm as any).startTask()
    await flushPromises()
    expect((wrapper.vm as any).task.taskNo).toBe('ANALYSIS_002')

    oldCallbacks.onMessage({
      taskNo: 'ANALYSIS_001', status: 'COMPLETED', progress: 100, message: '旧报告完成', reportId: 11
    })

    expect((wrapper.vm as any).task).toMatchObject({
      taskNo: 'ANALYSIS_002', status: 'PENDING', progress: 5, message: '第二次已提交'
    })
    expect(taskCenter.updateTask).not.toHaveBeenLastCalledWith(
      'LOCAL_ANALYSIS_TASK',
      expect.objectContaining({ backendTaskNo: 'ANALYSIS_001', status: 'COMPLETED' })
    )
  })

  it('opens the existing task center from processing state without changing its store contract', async () => {
    const wrapper = await mountPage()
    selectValidInputs(wrapper)
    await flushPromises()
    await (wrapper.vm as any).startTask()
    await flushPromises()

    await wrapper.get('[data-open-task-center]').trigger('click')
    expect(taskCenter.openDrawer).toHaveBeenCalledTimes(1)
  })

  it('locks a query-selected task before restore resolves so it cannot create a duplicate task', async () => {
    const pendingRestore = deferred<any>()
    mockedTaskDetail.mockReturnValueOnce(pendingRestore.promise as any)
    const wrapper = await mountPage(
      '/analysis/match?resumeId=3&resumeVersionId=5&jobId=7&taskNo=ANALYSIS_RESTORED'
    )

    expect((wrapper.vm as any).restoringTask).toBe(true)
    expect(wrapper.get('[data-restore-state]').text()).toContain('正在恢复上次分析任务')
    expect(wrapper.get('[data-analysis-submit]').attributes('disabled')).toBeDefined()
    await wrapper.get('[data-analysis-submit]').trigger('click')
    await (wrapper.vm as any).startTask()
    expect(mockedCreateTask).not.toHaveBeenCalled()

    pendingRestore.resolve({
      taskNo: 'ANALYSIS_RESTORED', status: 'BUILDING_CONTEXT', progress: 36,
      message: '正在构建岗位上下文', resumeId: 3, jobId: 7
    })
    await flushPromises()
    expect((wrapper.vm as any).restoringTask).toBe(false)
    expect((wrapper.vm as any).task.taskNo).toBe('ANALYSIS_RESTORED')
  })

  it('shows visible option loading and actionable resume/job empty states only after loading completes', async () => {
    const pendingResumes = deferred<any>()
    const pendingJobs = deferred<any>()
    mockedResumes.mockReturnValueOnce(pendingResumes.promise as any)
    mockedJobs.mockReturnValueOnce(pendingJobs.promise as any)
    const wrapper = await mountPage()

    expect(wrapper.get('[data-options-loading]').attributes('role')).toBe('status')
    expect(wrapper.get('[data-options-loading]').text()).toContain('正在加载简历与岗位')
    expect(wrapper.find('[data-resumes-empty]').exists()).toBe(false)
    expect(wrapper.find('[data-jobs-empty]').exists()).toBe(false)

    pendingResumes.resolve({ records: [] })
    pendingJobs.resolve({ records: [] })
    await flushPromises()

    expect(wrapper.find('[data-options-loading]').exists()).toBe(false)
    expect(wrapper.get('[data-resumes-empty]').text()).toContain('还没有可用于分析的简历')
    expect(wrapper.get('[data-jobs-empty]').text()).toContain('还没有可用于分析的岗位')
    await wrapper.get('[data-upload-resume]').trigger('click')
    await wrapper.get('[data-add-job]').trigger('click')
    expect(push).toHaveBeenNthCalledWith(1, '/resumes')
    expect(push).toHaveBeenNthCalledWith(2, '/jobs')
  })
})
