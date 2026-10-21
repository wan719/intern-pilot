import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ApplicationList from '@/views/application/ApplicationList.vue'
import {
  createApplicationApi,
  deleteApplicationApi,
  getApplicationDetailApi,
  getApplicationListApi,
  getApplicationPlanContextApi,
  createApplicationFromReportApi,
  updateApplicationNoteApi,
  updateApplicationStatusApi
} from '@/api/application'
import { getAnalysisReportsApi } from '@/api/analysis'
import { getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'

vi.mock('@/api/application', () => ({
  createApplicationApi: vi.fn(),
  deleteApplicationApi: vi.fn(),
  getApplicationDetailApi: vi.fn(),
  getApplicationListApi: vi.fn(),
  getApplicationPlanContextApi: vi.fn(),
  createApplicationFromReportApi: vi.fn(),
  updateApplicationNoteApi: vi.fn(),
  updateApplicationStatusApi: vi.fn()
}))
vi.mock('@/api/analysis', () => ({ getAnalysisReportsApi: vi.fn() }))
vi.mock('@/api/job', () => ({ getJobListApi: vi.fn() }))
vi.mock('@/api/resume', () => ({ getResumeListApi: vi.fn() }))

const mockedCreate = vi.mocked(createApplicationApi)
const mockedDelete = vi.mocked(deleteApplicationApi)
const mockedDetail = vi.mocked(getApplicationDetailApi)
const mockedList = vi.mocked(getApplicationListApi)
const mockedPlanContext = vi.mocked(getApplicationPlanContextApi)
const mockedFromReport = vi.mocked(createApplicationFromReportApi)
const mockedUpdateNote = vi.mocked(updateApplicationNoteApi)
const mockedUpdateStatus = vi.mocked(updateApplicationStatusApi)

const application = {
  applicationId: 17,
  companyName: '远航人工智能科技有限公司',
  jobTitle: '前端开发实习生',
  status: 'APPLIED',
  priority: 'HIGH',
  applyDate: '2026-08-25',
  interviewDate: '',
  resumeName: '前端简历',
  matchScore: 88,
  note: '官网投递',
  review: ''
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((resolvePromise) => {
    resolve = resolvePromise
  })
  return { promise, resolve }
}

function button(wrapper: VueWrapper, label: string) {
  const match = wrapper.findAll('button').find((item) => item.text().trim() === label)
  expect(match).toBeTruthy()
  return match!
}

async function mountPage(path = '/applications') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/applications', component: { template: '<div />' }, meta: { title: '投递追踪' } }]
  })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(ApplicationList, {
    global: { plugins: [router], stubs: { teleport: true } }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  mockedList.mockResolvedValue({ records: [application] } as any)
  mockedDetail.mockResolvedValue(application as any)
  mockedCreate.mockResolvedValue({ applicationId: 18 } as any)
  mockedUpdateStatus.mockResolvedValue(undefined as any)
  mockedUpdateNote.mockResolvedValue(undefined as any)
  mockedDelete.mockResolvedValue(undefined as any)
  mockedPlanContext.mockResolvedValue({ outcome: 'READY', reportId: 9, job: { id: 7, companyName: '远航科技', jobTitle: '前端实习生' }, resume: { id: 3, resumeName: '前端简历' } } as any)
  mockedFromReport.mockResolvedValue({ outcome: 'CREATED', applicationId: 18 } as any)
  vi.mocked(getJobListApi).mockResolvedValue({ records: [{ jobId: 7, companyName: '远航科技', jobTitle: '前端实习生' }] } as any)
  vi.mocked(getResumeListApi).mockResolvedValue({ records: [{ resumeId: 3, resumeName: '前端简历' }] } as any)
  vi.mocked(getAnalysisReportsApi).mockResolvedValue({ records: [{ reportId: 9, companyName: '远航科技', matchScore: 88 }] } as any)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined as any)
})

describe('from-report plan flow', () => {
  it('shows fixed source summary and creates only after explicit confirmation', async () => {
    const wrapper = await mountPage('/applications?fromReportId=9')
    expect(mockedPlanContext).toHaveBeenCalledWith(9)
    expect((wrapper.vm as any).planState).toBe('READY')
    expect(wrapper.get('[data-plan-confirmation]').text()).toContain('前端实习生')
    expect(wrapper.get('[data-plan-confirmation]').text()).toContain('尚未向招聘平台投递')
    expect(mockedFromReport).not.toHaveBeenCalled()
    ;(wrapper.vm as any).planNote = '准备项目经历'
    await (wrapper.vm as any).confirmPlan()
    await flushPromises()
    expect(mockedFromReport).toHaveBeenCalledWith({ reportId: 9, note: '准备项目经历', priority: 'MEDIUM' })
    expect(mockedDetail).toHaveBeenCalledWith(18)
    expect((wrapper.vm as any).$route.query.fromReportId).toBeUndefined()
  })

  it('opens the returned existing record without writing, even if old resume is gone', async () => {
    mockedPlanContext.mockResolvedValueOnce({ outcome: 'EXISTING', reportId: 9, applicationId: 77 } as any)
    const wrapper = await mountPage('/applications?fromReportId=9')
    expect(mockedDetail).toHaveBeenCalledWith(77)
    expect(mockedFromReport).not.toHaveBeenCalled()
    expect((wrapper.vm as any).$route.query.fromReportId).toBeUndefined()
  })

  it('keeps the source available to retry when an existing detail disappears', async () => {
    mockedPlanContext.mockResolvedValueOnce({ outcome: 'EXISTING', reportId: 9, applicationId: 77 } as any)
    mockedDetail.mockImplementation((id) => id === 77 ? Promise.reject(new Error('记录已变化')) : Promise.resolve(application as any))
    const wrapper = await mountPage('/applications?fromReportId=9')
    expect((wrapper.vm as any).planState).toBe('ERROR')
    expect(wrapper.get('[data-plan-error]').text()).toContain('记录已变化')
    expect((wrapper.vm as any).$route.query.fromReportId).toBe('9')
  })

  it('requires a second explicit choice for ordinary creation and clears reportId', async () => {
    mockedPlanContext.mockResolvedValueOnce({ outcome: 'RESUME_UNAVAILABLE', reportId: 9, reason: '旧简历已失效' } as any)
    const wrapper = await mountPage('/applications?fromReportId=9')
    expect(wrapper.get('[data-plan-resume-unavailable]').text()).toContain('旧简历已失效')
    expect((wrapper.vm as any).createVisible).toBe(false)
    await wrapper.get('[data-plan-ordinary-create]').trigger('click')
    await flushPromises()
    expect((wrapper.vm as any).createVisible).toBe(true)
    expect((wrapper.vm as any).createForm.reportId).toBeNull()
    expect(mockedFromReport).not.toHaveBeenCalled()
    ;(wrapper.vm as any).createForm.jobId = 7
    await (wrapper.vm as any).createApplication()
    expect(mockedCreate).toHaveBeenCalledWith(expect.objectContaining({ jobId: 7, reportId: null }))
  })

  it('does not let a late context response replace a newer report selection', async () => {
    const oldRequest = deferred<any>()
    mockedPlanContext.mockReturnValueOnce(oldRequest.promise)
    mockedPlanContext.mockResolvedValueOnce({ outcome: 'READY', reportId: 10, job: { id: 8, companyName: '新公司', jobTitle: '新岗位' }, resume: { id: 4, resumeName: '新简历' } } as any)
    const wrapper = await mountPage('/applications?fromReportId=9')
    await (wrapper.vm as any).$router.push('/applications?fromReportId=10')
    await flushPromises()
    oldRequest.resolve({ outcome: 'EXISTING', reportId: 9, applicationId: 77 })
    await flushPromises()
    expect((wrapper.vm as any).planContext.reportId).toBe(10)
    expect(wrapper.get('[data-plan-confirmation]').text()).toContain('新岗位')
    expect(mockedDetail).not.toHaveBeenCalledWith(77)
  })

  it('drops stale confirmation data after a resource-change response and can recheck the source', async () => {
    mockedFromReport.mockRejectedValueOnce({ response: { status: 409, data: { message: '报告关联简历已失效' } } })
    const wrapper = await mountPage('/applications?fromReportId=9')
    await (wrapper.vm as any).confirmPlan()
    await flushPromises()

    expect((wrapper.vm as any).planState).toBe('ERROR')
    expect((wrapper.vm as any).planContext).toBeNull()
    expect(wrapper.find('[data-plan-confirmation]').exists()).toBe(false)
    expect(wrapper.get('[data-plan-error]').text()).toContain('报告关联简历已失效')
    expect((wrapper.vm as any).$route.query.fromReportId).toBe('9')

    mockedPlanContext.mockResolvedValueOnce({ outcome: 'RESUME_UNAVAILABLE', reportId: 9, reason: '旧简历已失效' } as any)
    await button(wrapper, '重试核对').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-plan-resume-unavailable]').text()).toContain('旧简历已失效')
    expect(wrapper.find('[data-plan-ordinary-create]').exists()).toBe(true)
  })

  it('hides the old confirmation if its report is deleted before submission', async () => {
    mockedFromReport.mockRejectedValueOnce({ response: { status: 404, data: { message: '来源不可用或无权访问' } } })
    const wrapper = await mountPage('/applications?fromReportId=9')
    await (wrapper.vm as any).confirmPlan()
    await flushPromises()
    expect((wrapper.vm as any).planState).toBe('ERROR')
    expect(wrapper.find('[data-plan-confirmation]').exists()).toBe(false)
    expect(wrapper.get('[data-plan-error]').text()).toContain('来源不可用或无权访问')
  })

  it('does not open an existing detail after the source changes during detail loading', async () => {
    const oldDetail = deferred<any>()
    mockedPlanContext.mockResolvedValueOnce({ outcome: 'EXISTING', reportId: 9, applicationId: 77 } as any)
    mockedPlanContext.mockResolvedValueOnce({ outcome: 'READY', reportId: 10, job: { id: 8, companyName: '新公司', jobTitle: '新岗位' }, resume: { id: 4, resumeName: '新简历' } } as any)
    mockedDetail.mockImplementation((id) => id === 77 ? oldDetail.promise : Promise.resolve(application as any))
    const wrapper = await mountPage('/applications?fromReportId=9')
    await (wrapper.vm as any).$router.push('/applications?fromReportId=10')
    await flushPromises()
    oldDetail.resolve({ ...application, applicationId: 77 })
    await flushPromises()
    expect((wrapper.vm as any).detailVisible).toBe(false)
    expect((wrapper.vm as any).detail).toBeNull()
    expect((wrapper.vm as any).$route.query.fromReportId).toBe('10')
    expect(wrapper.get('[data-plan-confirmation]').text()).toContain('新岗位')
  })

  it('does not clear a newer source after a submitted plan detail resolves late', async () => {
    const oldDetail = deferred<any>()
    mockedPlanContext.mockResolvedValueOnce({ outcome: 'READY', reportId: 9, job: { id: 7, companyName: '旧公司', jobTitle: '旧岗位' }, resume: { id: 3, resumeName: '旧简历' } } as any)
    mockedPlanContext.mockResolvedValueOnce({ outcome: 'READY', reportId: 10, job: { id: 8, companyName: '新公司', jobTitle: '新岗位' }, resume: { id: 4, resumeName: '新简历' } } as any)
    mockedDetail.mockImplementation((id) => id === 77 ? oldDetail.promise : Promise.resolve(application as any))
    mockedFromReport.mockResolvedValueOnce({ outcome: 'CREATED', applicationId: 77 } as any)
    const wrapper = await mountPage('/applications?fromReportId=9')
    const confirmation = (wrapper.vm as any).confirmPlan()
    await flushPromises()
    await (wrapper.vm as any).$router.push('/applications?fromReportId=10')
    await flushPromises()
    oldDetail.resolve({ ...application, applicationId: 77 })
    await confirmation
    await flushPromises()
    expect((wrapper.vm as any).detailVisible).toBe(false)
    expect((wrapper.vm as any).detail).toBeNull()
    expect((wrapper.vm as any).$route.query.fromReportId).toBe('10')
    expect(wrapper.get('[data-plan-confirmation]').text()).toContain('新岗位')
  })

  it('cancels confirmation without submitting and removes the source query', async () => {
    const wrapper = await mountPage('/applications?fromReportId=9')
    await (wrapper.vm as any).cancelPlan()
    expect(mockedFromReport).not.toHaveBeenCalled()
    expect((wrapper.vm as any).$route.query.fromReportId).toBeUndefined()
    expect((wrapper.vm as any).planState).toBe('IDLE')
  })

  it('rejects invalid query IDs and sends only one write during repeated confirmation', async () => {
    const invalid = await mountPage('/applications?fromReportId=9&fromReportId=10')
    expect(invalid.get('[data-plan-error]').text()).toContain('来源 ID')
    expect(mockedPlanContext).not.toHaveBeenCalled()
    invalid.unmount()

    const pending = deferred<any>()
    mockedFromReport.mockReturnValueOnce(pending.promise)
    const wrapper = await mountPage('/applications?fromReportId=9')
    const first = (wrapper.vm as any).confirmPlan()
    const second = (wrapper.vm as any).confirmPlan()
    expect(mockedFromReport).toHaveBeenCalledTimes(1)
    pending.resolve({ outcome: 'EXISTING', applicationId: 77 })
    await Promise.all([first, second])
    expect(mockedDetail).toHaveBeenCalledWith(77)
  })
})

describe('application tracking contract characterization', () => {
  it('keeps status and keyword filters in the list request while priority remains local', async () => {
    const wrapper = await mountPage()

    expect(mockedList).toHaveBeenCalledWith({ keyword: '', status: '', pageNum: 1, pageSize: 100 })
    expect(mockedDetail).toHaveBeenCalledWith(17)

    Object.assign((wrapper.vm as any).query, { keyword: '远航', status: 'APPLIED' })
    ;(wrapper.vm as any).priorityFilter = 'HIGH'
    await (wrapper.vm as any).loadApplications()

    expect(mockedList).toHaveBeenLastCalledWith({ keyword: '远航', status: 'APPLIED', pageNum: 1, pageSize: 100 })
    expect((wrapper.vm as any).filteredApplications).toHaveLength(1)
  })

  it('keeps status and note writes single-flight until each save settles', async () => {
    const statusWrite = deferred<void>()
    const noteWrite = deferred<void>()
    mockedUpdateStatus.mockReturnValueOnce(statusWrite.promise as any)
    mockedUpdateNote.mockReturnValueOnce(noteWrite.promise as any)
    const wrapper = await mountPage()

    ;(wrapper.vm as any).openStatus(application)
    const firstStatusSave = (wrapper.vm as any).saveStatus()
    const duplicateStatusSave = (wrapper.vm as any).saveStatus()
    expect(mockedUpdateStatus).toHaveBeenCalledTimes(1)
    expect((wrapper.vm as any).statusSaving).toBe(true)
    statusWrite.resolve()
    await Promise.all([firstStatusSave, duplicateStatusSave])
    expect((wrapper.vm as any).statusSaving).toBe(false)

    ;(wrapper.vm as any).openNote(application)
    const firstNoteSave = (wrapper.vm as any).saveNote()
    const duplicateNoteSave = (wrapper.vm as any).saveNote()
    expect(mockedUpdateNote).toHaveBeenCalledTimes(1)
    expect((wrapper.vm as any).noteSaving).toBe(true)
    noteWrite.resolve()
    await Promise.all([firstNoteSave, duplicateNoteSave])
    expect((wrapper.vm as any).noteSaving).toBe(false)
  })

  it('keeps create, status and note payloads exact and refreshes after successful writes', async () => {
    const wrapper = await mountPage()
    Object.assign((wrapper.vm as any).createForm, {
      jobId: 7,
      resumeId: 3,
      reportId: 9,
      status: 'TO_APPLY',
      priority: 'HIGH',
      applyDate: '2026-08-26',
      note: '内推渠道'
    })
    await (wrapper.vm as any).createApplication()

    expect(JSON.parse(JSON.stringify(mockedCreate.mock.calls[0][0]))).toEqual({
      jobId: 7,
      resumeId: 3,
      reportId: 9,
      status: 'TO_APPLY',
      priority: 'HIGH',
      applyDate: '2026-08-26',
      note: '内推渠道'
    })

    ;(wrapper.vm as any).openStatus(application)
    ;(wrapper.vm as any).statusForm.status = 'FIRST_INTERVIEW'
    await (wrapper.vm as any).saveStatus()
    expect(mockedUpdateStatus).toHaveBeenCalledWith(17, { status: 'FIRST_INTERVIEW' })

    ;(wrapper.vm as any).openNote(application)
    Object.assign((wrapper.vm as any).noteForm, {
      note: 'HR 已回复',
      review: '补充项目指标',
      interviewDate: '2026-08-28T10:00:00'
    })
    await (wrapper.vm as any).saveNote()
    expect(mockedUpdateNote).toHaveBeenCalledWith(17, {
      note: 'HR 已回复',
      review: '补充项目指标',
      interviewDate: '2026-08-28T10:00:00'
    })
    expect(mockedList).toHaveBeenCalledTimes(4)
  })

  it('keeps detail and destructive actions available and deletion behind confirmation', async () => {
    const wrapper = await mountPage()

    await button(wrapper, '详情').trigger('click')
    await flushPromises()
    expect(mockedDetail).toHaveBeenLastCalledWith(17)

    await button(wrapper, '删除').trigger('click')
    await flushPromises()
    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      '确认删除“远航人工智能科技有限公司 - 前端开发实习生”的投递记录吗？删除后不可恢复。',
      '删除投递',
      { type: 'warning' }
    )
    expect(mockedDelete).toHaveBeenCalledWith(17)
  })
})

describe('application tracking redesign', () => {
  it('uses one goal-led heading, the shared FilterBar and one responsive tracking DOM', async () => {
    const wrapper = await mountPage()

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.get('h1').text()).toBe('投递追踪')
    expect(wrapper.findComponent({ name: 'FilterBar' }).exists()).toBe(true)
    expect(wrapper.findAll('.application-track')).toHaveLength(1)
    expect(wrapper.findAll('.application-track article')).toHaveLength(1)
  })

  it('pairs status color with an icon and keeps the next action visible in the same mobile card', async () => {
    const wrapper = await mountPage()
    const card = wrapper.get('.application-track article')

    expect(card.get('[aria-label="投递状态：已投递"]').text()).toContain('已投递')
    expect(card.get('[data-application-status-icon]').element.tagName).toBe('I')
    expect(card.get('[data-application-next-action]').text()).toContain('关注 HR 回复')
    expect(card.get('[role="group"]').attributes('aria-label')).toContain('远航人工智能科技有限公司')
  })

  it('keeps the newest filter result when an older request resolves last', async () => {
    const wrapper = await mountPage()
    const firstRequest = deferred<any>()
    const secondRequest = deferred<any>()
    mockedList.mockReturnValueOnce(firstRequest.promise).mockReturnValueOnce(secondRequest.promise)
    mockedDetail.mockImplementation(async (id) => ({
      ...application,
      applicationId: id,
      companyName: id === 21 ? '筛选 A 公司' : '筛选 B 公司'
    }) as any)

    ;(wrapper.vm as any).query.keyword = '筛选 A'
    const firstLoad = (wrapper.vm as any).loadApplications()
    ;(wrapper.vm as any).query.keyword = '筛选 B'
    const secondLoad = (wrapper.vm as any).loadApplications()

    secondRequest.resolve({ records: [{ ...application, applicationId: 22 }] })
    await secondLoad
    expect(wrapper.text()).toContain('筛选 B 公司')
    expect((wrapper.vm as any).loading).toBe(false)

    firstRequest.resolve({ records: [{ ...application, applicationId: 21 }] })
    await firstLoad
    expect(wrapper.text()).toContain('筛选 B 公司')
    expect(wrapper.text()).not.toContain('筛选 A 公司')
  })

  it('invalidates a pending application load when the page unmounts', async () => {
    const wrapper = await mountPage()
    const pendingRequest = deferred<any>()
    mockedList.mockReturnValueOnce(pendingRequest.promise)
    mockedDetail.mockRejectedValueOnce(new Error('detail unavailable after unmount'))
    const pendingLoad = (wrapper.vm as any).loadApplications()

    wrapper.unmount()
    pendingRequest.resolve({ records: [{ ...application, applicationId: 23, companyName: '卸载后结果' }] })
    await pendingLoad

    expect((wrapper.vm as any).applications).toEqual([application])
  })

  it('does not let a stale request clear loading while the newest request is pending', async () => {
    const wrapper = await mountPage()
    const staleRequest = deferred<any>()
    const latestRequest = deferred<any>()
    mockedList.mockReturnValueOnce(staleRequest.promise).mockReturnValueOnce(latestRequest.promise)
    mockedDetail.mockImplementation(async (id) => ({ ...application, applicationId: id }) as any)

    const staleLoad = (wrapper.vm as any).loadApplications()
    const latestLoad = (wrapper.vm as any).loadApplications()
    staleRequest.resolve({ records: [{ ...application, applicationId: 24 }] })
    await staleLoad

    expect((wrapper.vm as any).loading).toBe(true)
    latestRequest.resolve({ records: [{ ...application, applicationId: 25 }] })
    await latestLoad
    expect((wrapper.vm as any).loading).toBe(false)
  })

  it('keeps the newest application detail when responses resolve in reverse order', async () => {
    const wrapper = await mountPage()
    const staleRequest = deferred<any>()
    const latestRequest = deferred<any>()
    mockedDetail.mockReturnValueOnce(staleRequest.promise).mockReturnValueOnce(latestRequest.promise)

    const staleOpen = (wrapper.vm as any).openDetail(101)
    const latestOpen = (wrapper.vm as any).openDetail(202)
    latestRequest.resolve({ ...application, applicationId: 202, companyName: '最新投递公司' })
    await latestOpen
    staleRequest.resolve({ ...application, applicationId: 101, companyName: '过期投递公司' })
    await staleOpen
    await flushPromises()

    expect((wrapper.vm as any).detail.applicationId).toBe(202)
    expect((wrapper.vm as any).detail.companyName).toBe('最新投递公司')
    expect((wrapper.vm as any).detail.companyName).not.toBe('过期投递公司')
  })

  it('keeps a closed application drawer empty when its pending detail resolves', async () => {
    const wrapper = await mountPage()
    await (wrapper.vm as any).openDetail(17)
    const pendingRequest = deferred<any>()
    mockedDetail.mockReturnValueOnce(pendingRequest.promise)

    const pendingOpen = (wrapper.vm as any).openDetail(303)
    const drawer = wrapper.findComponent({ name: 'ElDrawer' })
    drawer.vm.$emit('update:modelValue', false)
    drawer.vm.$emit('close')
    await wrapper.vm.$nextTick()
    pendingRequest.resolve({ ...application, applicationId: 303, companyName: '关闭后投递' })
    await pendingOpen
    await flushPromises()

    expect((wrapper.vm as any).detailVisible).toBe(false)
    expect((wrapper.vm as any).detail).toBeNull()
    expect(wrapper.text()).not.toContain('关闭后投递')
  })

  it('does not commit an application detail after the page unmounts', async () => {
    const wrapper = await mountPage()
    const vm = wrapper.vm as any
    const pendingRequest = deferred<any>()
    mockedDetail.mockReturnValueOnce(pendingRequest.promise)

    const pendingOpen = vm.openDetail(404)
    wrapper.unmount()
    pendingRequest.resolve({ ...application, applicationId: 404, companyName: '卸载后投递' })
    await pendingOpen

    expect(vm.detail).toBeNull()
    expect(vm.detailVisible).toBe(false)
  })
})
