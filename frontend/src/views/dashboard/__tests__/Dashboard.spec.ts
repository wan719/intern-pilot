import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import Dashboard from '@/views/dashboard/Dashboard.vue'
import { getApplicationListApi } from '@/api/application'
import { getAnalysisReportsApi } from '@/api/analysis'
import { getInterviewQuestionReportsApi } from '@/api/interviewQuestion'
import { getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))

vi.mock('@/router', () => ({ default: { push } }))
vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ user: { nickname: '小林' } })
}))
vi.mock('@/api/application', () => ({ getApplicationListApi: vi.fn() }))
vi.mock('@/api/analysis', () => ({ getAnalysisReportsApi: vi.fn() }))
vi.mock('@/api/interviewQuestion', () => ({ getInterviewQuestionReportsApi: vi.fn() }))
vi.mock('@/api/job', () => ({ getJobListApi: vi.fn() }))
vi.mock('@/api/resume', () => ({ getResumeListApi: vi.fn() }))
vi.mock('echarts/core', () => ({
  init: vi.fn(() => ({ setOption: vi.fn(), resize: vi.fn(), dispose: vi.fn() })),
  use: vi.fn()
}))

const mockedApplications = vi.mocked(getApplicationListApi)
const mockedReports = vi.mocked(getAnalysisReportsApi)
const mockedInterviewReports = vi.mocked(getInterviewQuestionReportsApi)
const mockedJobs = vi.mocked(getJobListApi)
const mockedResumes = vi.mocked(getResumeListApi)

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }]
})

function listResult(total = 0, records: any[] = []) {
  return { total, records, current: 1, size: 100 }
}

function setResults({
  resumes = 0,
  jobs = 0,
  reports = 0,
  applications = 0,
  interview = { total: 0, records: [], current: 1, size: 1 } as any,
  reportRecords = [] as any[],
  applicationRecords = [] as any[]
} = {}) {
  mockedResumes.mockResolvedValue(listResult(resumes) as any)
  mockedJobs.mockResolvedValue(listResult(jobs) as any)
  mockedReports.mockResolvedValue(listResult(reports, reportRecords) as any)
  mockedApplications.mockResolvedValue(listResult(applications, applicationRecords) as any)
  mockedInterviewReports.mockResolvedValue(interview as any)
}

async function mountDashboard() {
  const wrapper = mount(Dashboard, { global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  mockedApplications.mockReset()
  setResults()
})

describe('career action workspace', () => {
  it('links each upcoming interview and routes the main interview action to the soonest application', async () => {
    setResults({
      resumes: 1, jobs: 1, reports: 1, applications: 2,
      applicationRecords: [
        { applicationId: 31, companyName: '星河', jobTitle: '后端实习', interviewDate: '2099-11-19T15:00:00' },
        { applicationId: 32, companyName: '森林', jobTitle: '前端实习', interviewDate: '2099-11-18T15:00:00' }
      ]
    })
    const wrapper = await mountDashboard()
    expect(wrapper.findAll('a').filter(link => link.text() === '准备面试').map(link => link.attributes('href')))
      .toEqual(['/interview-questions?applicationId=32', '/interview-questions?applicationId=31'])
    expect(wrapper.get('.dashboard-schedule-link').attributes('href')).toBe('/applications')
    await wrapper.get('[data-primary-action]').trigger('click')
    expect(push).toHaveBeenCalledWith({ path: '/interview-questions', query: { applicationId: '32' } })
  })

  it('includes a sooner interview from a later page of the existing applications API', async () => {
    mockedApplications.mockResolvedValueOnce(listResult(101, [
      { applicationId: 1, companyName: '稍晚公司', interviewDate: '2099-11-20T15:00:00' },
    ]) as any).mockResolvedValueOnce(listResult(101, [
      { applicationId: 2, companyName: '较早公司', interviewDate: '2099-11-18T14:00:00' },
    ]) as any)
    const wrapper = await mountDashboard()
    expect(mockedApplications).toHaveBeenLastCalledWith({ pageNum: 2, pageSize: 100 })
    expect(wrapper.findAll('.dashboard-schedule-row').map(row => row.get('strong').text()))
      .toEqual(['较早公司 · 未知岗位', '稍晚公司 · 未知岗位'])
  })

  it('renders upcoming interviews and links to the most recent resume and report', async () => {
    mockedResumes.mockResolvedValue(listResult(2, [
      { resumeId: 2, resumeName: '上周简历', createdAt: '2026-11-10T12:00:00' },
      { resumeId: 4, resumeName: '最新简历', createdAt: '2026-11-16T12:00:00' },
    ]) as any)
    mockedReports.mockResolvedValue(listResult(1, [
      { reportId: 13, companyName: '样例公司', jobTitle: '实习生', createdAt: '2026-11-17T12:00:00' },
    ]) as any)
    mockedApplications.mockResolvedValue(listResult(2, [
      { applicationId: 1, companyName: '即将面试', jobTitle: '实习生', interviewDate: '2099-11-17T15:00:00', status: 'FIRST_INTERVIEW' },
      { applicationId: 2, companyName: '已关闭', jobTitle: '实习生', interviewDate: '2099-11-17T14:00:00', status: 'GIVEN_UP' },
    ]) as any)

    const wrapper = await mountDashboard()
    expect(wrapper.get('.dashboard-upcoming').text()).toContain('即将面试')
    expect(wrapper.get('.dashboard-upcoming').text()).not.toContain('已关闭')
    expect(wrapper.findAll('.dashboard-continue-row').map(link => link.attributes('href')))
      .toEqual(['/resumes/4/versions', '/analysis/reports?reportId=13'])
    expect(wrapper.get('.dashboard-continue').text()).toContain('最新简历')
  })

  it('places the current stage before the journey and groups recent analysis with recent applications', async () => {
    const wrapper = await mountDashboard()

    expect(wrapper.get('h1').text()).toBe('工作台')
    const stage = wrapper.get('.dashboard-focus')
    const journey = wrapper.get('ol[aria-labelledby="journey-progress-title"]')
    expect(stage.element.compareDocumentPosition(journey.element) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(wrapper.get('.dashboard-grid').findAll('h3').map((heading) => heading.text())).toEqual([
      '最近分析', '最近投递'
    ])
  })

  it('starts all existing dashboard requests and the interview count request together', async () => {
    let release!: () => void
    const pending = new Promise<any>((resolve) => {
      release = () => resolve(listResult())
    })
    mockedResumes.mockReturnValue(pending)
    mockedJobs.mockReturnValue(pending)
    mockedReports.mockReturnValue(pending)
    mockedApplications.mockReturnValue(pending)
    mockedInterviewReports.mockReturnValue(pending)

    const wrapper = mount(Dashboard, { global: { plugins: [router] } })
    await wrapper.vm.$nextTick()

    expect(mockedResumes).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(mockedJobs).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(mockedReports).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(mockedApplications).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(mockedInterviewReports).toHaveBeenCalledWith({ pageNum: 1, pageSize: 1 })
    expect(wrapper.get('.dashboard-workspace').attributes('aria-busy')).toBe('true')

    release()
    await flushPromises()
  })

  it.each([
    ['total', { total: 2, records: [] }],
    ['records', { records: [{ reportId: 7 }] }],
    ['array', [{ reportId: 7 }]]
  ])('normalizes interview list %s responses into stage completion', async (_shape, interview) => {
    setResults({ resumes: 1, jobs: 1, reports: 1, applications: 1, interview })

    const wrapper = await mountDashboard()

    expect(wrapper.findAll('.journey-step--complete')).toHaveLength(5)
    expect(wrapper.get('[data-primary-action]').text()).toContain('查看最新分析结果')
  })

  it('keeps legacy dashboard data visible when only the interview count fails', async () => {
    setResults({
      resumes: 2,
      jobs: 3,
      reports: 1,
      applications: 1,
      reportRecords: [{
        reportId: 11,
        companyName: '星河科技',
        jobTitle: '前端实习生',
        createdAt: '2026-08-24T08:00:00',
        matchScore: 86,
        matchLevel: '高匹配'
      }],
      applicationRecords: [{
        applicationId: 21,
        companyName: '星河科技',
        jobTitle: '前端实习生',
        status: 'APPLIED',
        interviewDate: null,
        note: '等待反馈'
      }]
    })
    mockedInterviewReports.mockRejectedValue(new Error('interview unavailable'))

    const wrapper = await mountDashboard()

    expect(wrapper.find('.dashboard-error').exists()).toBe(false)
    expect(wrapper.text()).toContain('星河科技 - 前端实习生')
    expect(wrapper.text()).not.toContain('暂无分析报告')
    expect(wrapper.findAll('.stat-card strong').map((item) => item.text())).toEqual(['2', '3', '1', '1'])
    expect(wrapper.get('.journey-step:nth-child(4)').text()).toContain('暂时未知')
    expect(wrapper.get('.journey-inline-error[role="status"]').text()).toContain('面试阶段暂时无法确认')
    expect(wrapper.get('[data-primary-action]').text()).toContain('重试面试阶段数据')
  })

  it.each([
    {
      state: 'resume is incomplete',
      results: { resumes: 0, jobs: 1, reports: 1, applications: 1 },
      expectedAction: '上传第一份简历'
    },
    {
      state: 'analysis is incomplete',
      results: { resumes: 1, jobs: 1, reports: 0, applications: 1 },
      expectedAction: '开始 AI 匹配'
    },
    {
      state: 'the first three stages are complete',
      results: { resumes: 1, jobs: 1, reports: 1, applications: 0 },
      expectedAction: '重试面试阶段数据'
    }
  ])('keeps journey-order primary guidance when $state and interview is unknown', async ({ results, expectedAction }) => {
    setResults(results)
    mockedInterviewReports.mockRejectedValue(new Error('interview unavailable'))

    const wrapper = await mountDashboard()

    expect(wrapper.get('[data-primary-action]').text()).toContain(expectedAction)
    const inlineRetry = wrapper.find('[data-interview-retry]')
    if (expectedAction === '重试面试阶段数据') {
      expect(inlineRetry.exists()).toBe(false)
    } else {
      expect(inlineRetry.text()).toContain('重试面试阶段数据')
    }
  })

  it('retries only the failed interview count and restores journey guidance after recovery', async () => {
    setResults({ resumes: 1, jobs: 1, reports: 1, applications: 1 })
    mockedInterviewReports
      .mockRejectedValueOnce(new Error('interview unavailable'))
      .mockResolvedValueOnce({ total: 1, records: [], current: 1, size: 1 } as any)

    const wrapper = await mountDashboard()

    await wrapper.get('[data-primary-action]').trigger('click')
    await flushPromises()

    expect(mockedInterviewReports).toHaveBeenCalledTimes(2)
    expect(mockedInterviewReports).toHaveBeenLastCalledWith({ pageNum: 1, pageSize: 1 })
    expect(mockedResumes).toHaveBeenCalledTimes(1)
    expect(mockedJobs).toHaveBeenCalledTimes(1)
    expect(mockedReports).toHaveBeenCalledTimes(1)
    expect(mockedApplications).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.journey-inline-error').exists()).toBe(false)
    expect(wrapper.findAll('.journey-step--complete')).toHaveLength(5)
    expect(wrapper.get('[data-primary-action]').text()).toContain('查看最新分析结果')
  })

  it('shows the five-stage zero state and routes the primary action to resume upload', async () => {
    const wrapper = await mountDashboard()

    expect(wrapper.findAll('.journey-step')).toHaveLength(5)
    expect(wrapper.findAll('.journey-step--complete')).toHaveLength(0)
    expect(wrapper.text()).toContain('从上传第一份简历开始')
    expect(wrapper.text()).toContain('暂无分析报告')
    expect(wrapper.text()).toContain('暂无投递动态')

    await wrapper.get('[data-primary-action]').trigger('click')
    expect(push).toHaveBeenCalledWith('/resumes')
  })

  it('routes partial progress to the earliest incomplete stage even if later stages have data', async () => {
    setResults({
      resumes: 2,
      jobs: 3,
      reports: 0,
      applications: 1,
      interview: { total: 1, records: [] }
    })

    const wrapper = await mountDashboard()

    expect(wrapper.findAll('.journey-step--complete')).toHaveLength(4)
    expect(wrapper.get('[data-primary-action]').text()).toContain('开始 AI 匹配')
    await wrapper.get('[data-primary-action]').trigger('click')
    expect(push).toHaveBeenCalledWith('/analysis/match')
  })

  it('puts the only primary action before the five stage destinations', async () => {
    setResults({ resumes: 1, jobs: 1, applications: 1 })

    const wrapper = await mountDashboard()
    const primary = wrapper.get('[data-primary-action]')
    const journey = wrapper.get('ol[aria-labelledby="journey-progress-title"]')
    const current = journey.get('[aria-current="step"]')

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.findAll('button[data-primary-action], button.el-button--primary:not(.is-link)')).toHaveLength(1)
    expect(wrapper.get('.dashboard-focus').text()).toContain('当前阶段匹配分析')
    expect(primary.element.compareDocumentPosition(journey.element) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(current.text()).toContain('匹配分析')
    expect(current.text()).toContain('下一步')

    const routes = ['/resumes', '/jobs', '/analysis/match', '/interview-questions', '/applications']
    const entries = journey.findAll('button')
    expect(entries).toHaveLength(routes.length)
    for (const [index, entry] of entries.entries()) {
      await entry.trigger('click')
      expect(push).toHaveBeenLastCalledWith(routes[index])
    }
  })

  it('shows fully complete recent results and offers a next step on existing routes', async () => {
    setResults({
      resumes: 1,
      jobs: 1,
      reports: 1,
      applications: 1,
      interview: { total: 1, records: [] },
      reportRecords: [{
        reportId: 11,
        companyName: '星河科技',
        jobTitle: '前端实习生',
        createdAt: '2026-08-24T08:00:00',
        matchScore: 86,
        matchLevel: '高匹配'
      }],
      applicationRecords: [{
        applicationId: 21,
        companyName: '星河科技',
        jobTitle: '前端实习生',
        status: 'APPLIED',
        interviewDate: null,
        note: '等待反馈'
      }]
    })

    const wrapper = await mountDashboard()

    expect(wrapper.text()).toContain('星河科技 - 前端实习生')
    expect(wrapper.text()).toContain('五个阶段都已启动')
    await wrapper.get('[data-primary-action]').trigger('click')
    expect(push).toHaveBeenCalledWith('/analysis/reports')
  })

  it('exposes an accessible loading state while requests are pending', async () => {
    const pending = new Promise<any>(() => {})
    mockedResumes.mockReturnValue(pending)
    mockedJobs.mockReturnValue(pending)
    mockedReports.mockReturnValue(pending)
    mockedApplications.mockReturnValue(pending)
    mockedInterviewReports.mockReturnValue(pending)

    const wrapper = mount(Dashboard, { global: { plugins: [router] } })
    await wrapper.vm.$nextTick()

    expect(wrapper.get('.dashboard-workspace[aria-busy="true"]').attributes('aria-label')).toBe('正在加载职业工作台')
    expect(wrapper.findAll('.stat-card[aria-busy="true"]')).toHaveLength(4)
    expect(wrapper.findAll('.stat-card strong')).toHaveLength(0)
    expect(wrapper.find('.journey-loading').exists()).toBe(true)
    expect(wrapper.findAll('.journey-step')).toHaveLength(0)
    expect(wrapper.text()).not.toContain('0/5 已启动')
    expect(wrapper.text()).not.toContain('上传第一份简历')
    expect(wrapper.get('[data-primary-action]').text()).toContain('正在确认下一步')
    expect(wrapper.get('[data-primary-action]').attributes('disabled')).toBeDefined()
  })

  it('keeps an accessible error state with a retry action when any dashboard request fails', async () => {
    mockedJobs.mockRejectedValue(new Error('network unavailable'))

    const wrapper = await mountDashboard()

    expect(wrapper.get('[role="alert"]').text()).toContain('工作台数据暂时无法加载')
    expect(wrapper.get('.dashboard-workspace').attributes('aria-busy')).toBe('false')
    expect(wrapper.text()).toContain('重新加载')
  })
})
