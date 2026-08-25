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
  init: vi.fn(() => ({ setOption: vi.fn() })),
  use: vi.fn()
}))

const mockedApplications = vi.mocked(getApplicationListApi)
const mockedReports = vi.mocked(getAnalysisReportsApi)
const mockedInterviewReports = vi.mocked(getInterviewQuestionReportsApi)
const mockedJobs = vi.mocked(getJobListApi)
const mockedResumes = vi.mocked(getResumeListApi)

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/', component: { template: '<div />' } }]
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
  setResults()
})

describe('career action workspace', () => {
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

  it('retries only the failed interview count and restores journey guidance after recovery', async () => {
    setResults({ resumes: 1, jobs: 1, reports: 1, applications: 1 })
    mockedInterviewReports
      .mockRejectedValueOnce(new Error('interview unavailable'))
      .mockResolvedValueOnce({ total: 1, records: [], current: 1, size: 1 } as any)

    const wrapper = await mountDashboard()

    await wrapper.get('[data-interview-retry]').trigger('click')
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

  it('renders one page heading and keeps the required workspace section order', async () => {
    const wrapper = await mountDashboard()
    const hero = wrapper.get('.page-hero').element
    const journey = wrapper.get('.journey-progress').element
    const stats = wrapper.get('.stat-grid').element
    const actions = wrapper.get('.today-actions').element
    const recent = wrapper.get('.recent-results').element

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(hero.compareDocumentPosition(journey) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(journey.compareDocumentPosition(stats) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(stats.compareDocumentPosition(actions) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(actions.compareDocumentPosition(recent) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
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
