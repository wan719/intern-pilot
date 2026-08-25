import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import JobRecommendationDetail from '@/views/recommendation/JobRecommendationDetail.vue'
import { createApplicationApi } from '@/api/application'
import { getJobRecommendationDetailApi } from '@/api/jobRecommendation'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))

vi.mock('@/router', () => ({ default: { push } }))
vi.mock('@/api/application', () => ({ createApplicationApi: vi.fn() }))
vi.mock('@/api/jobRecommendation', () => ({ getJobRecommendationDetailApi: vi.fn() }))

const mockedApplication = vi.mocked(createApplicationApi)
const mockedDetail = vi.mocked(getJobRecommendationDetailApi)

const recommendation = {
  batchId: 31,
  title: '前端方向推荐',
  resumeId: 3,
  resumeVersionId: 5,
  recommendedCount: 1,
  jobCount: 4,
  strategy: '综合推荐',
  createdAt: '2026-08-25T10:00:00',
  items: [{
    itemId: 301,
    jobId: 7,
    analysisReportId: 41,
    companyName: '星河科技',
    jobTitle: '前端实习生',
    jobType: '前端开发',
    location: '上海',
    salaryRange: '250/天',
    sourcePlatform: '官网',
    recommendationScore: 88,
    recommendationLevel: 'HIGH',
    skillMatchScore: 90,
    aiMatchScore: 86,
    jobTypeScore: 92,
    isApplied: 0,
    matchedSkills: ['Vue', 'TypeScript'],
    missingSkills: ['可视化'],
    reasons: ['技能栈与岗位核心要求高度重合']
  }]
}

function recommendationFixture(items = recommendation.items) {
  return {
    ...recommendation,
    items: items.map((item) => ({
      ...item,
      matchedSkills: [...(item.matchedSkills || [])],
      missingSkills: [...(item.missingSkills || [])],
      reasons: [...(item.reasons || [])]
    }))
  }
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

function button(wrapper: VueWrapper, label: string) {
  const match = wrapper.findAll('button').find((item) => item.text().trim() === label)
  expect(match).toBeDefined()
  return match!
}

async function mountPage() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/job-recommendations/:batchId', component: { template: '<div />' }, meta: { title: '推荐详情' } }]
  })
  await router.push('/job-recommendations/31')
  await router.isReady()
  const wrapper = mount(JobRecommendationDetail, {
    global: { plugins: [router], stubs: { teleport: true } }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  mockedDetail.mockResolvedValue(recommendationFixture() as any)
  mockedApplication.mockResolvedValue({ applicationId: 51 } as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as any)
})

describe('job recommendation detail legacy behavior', () => {
  it('keeps the batch route param request and all existing destination routes', async () => {
    const wrapper = await mountPage()

    expect(mockedDetail).toHaveBeenCalledWith(31)

    await button(wrapper, 'AI 分析').trigger('click')
    expect(push).toHaveBeenCalledWith({
      path: '/analysis/match',
      query: { resumeId: 3, resumeVersionId: 5, jobId: 7 }
    })

    await button(wrapper, '生成面试题').trigger('click')
    expect(push).toHaveBeenCalledWith({
      path: '/interview-questions',
      query: { resumeId: 3, resumeVersionId: 5, jobId: 7, reportId: 41 }
    })

    await button(wrapper, '返回列表').trigger('click')
    expect(push).toHaveBeenCalledWith('/job-recommendations')
  })

  it('keeps the application payload and priority mapping', async () => {
    const wrapper = await mountPage()
    await button(wrapper, '加入投递').trigger('click')
    await flushPromises()

    expect(mockedApplication).toHaveBeenCalledWith({
      jobId: 7,
      resumeId: 3,
      reportId: 41,
      status: 'TO_APPLY',
      priority: 'HIGH',
      note: '来自岗位推荐批次：前端方向推荐'
    })
    expect(ElMessage.success).toHaveBeenCalledWith('已加入投递记录')
    expect((wrapper.vm as any).isApplicationPending(recommendation.items[0])).toBe(false)
  })

  it('keeps a persistent failed-detail state with retry and return actions', async () => {
    mockedDetail.mockRejectedValueOnce(new Error('detail unavailable'))
    const wrapper = await mountPage()

    expect(wrapper.text()).toContain('推荐详情加载失败')
    expect(wrapper.text()).toContain('detail unavailable')
    mockedDetail.mockResolvedValueOnce(recommendation as any)
    await button(wrapper, '重试').trigger('click')
    await flushPromises()
    expect(mockedDetail).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('前端实习生')
  })
})

describe('job recommendation detail redesign', () => {
  it('never offers application creation for an already-applied recommendation', async () => {
    mockedDetail.mockResolvedValueOnce(recommendationFixture([{ ...recommendation.items[0], isApplied: 1 }]) as any)
    const wrapper = await mountPage()

    expect(wrapper.get('.primary-next-action').text()).toBe('准备面试')
    expect(wrapper.findAll('button').some((item) => item.text().trim() === '加入投递')).toBe(false)
    await (wrapper.vm as any).addApplication((wrapper.vm as any).items[0])
    expect(mockedApplication).not.toHaveBeenCalled()
  })

  it('transitions every duplicate job item to applied and the next action immediately after success', async () => {
    mockedDetail.mockResolvedValueOnce(recommendationFixture([
      recommendation.items[0],
      { ...recommendation.items[0], itemId: 304, companyName: '星河科技第二展示位' }
    ]) as any)
    const wrapper = await mountPage()

    await button(wrapper, '加入投递').trigger('click')
    await flushPromises()

    expect((wrapper.vm as any).items.every((item: any) => item.isApplied === 1)).toBe(true)
    expect((wrapper.vm as any).applicationIdsByJob.get(7)).toBe(51)
    expect(wrapper.findAll('.primary-next-action').map((item) => item.text())).toEqual(['准备面试', '准备面试'])
    expect(wrapper.findAll('button').some((item) => item.text().trim() === '加入投递')).toBe(false)
    expect(wrapper.findAll('.job-meta').every((item) => item.text().includes('状态：已投递'))).toBe(true)
  })

  it('recovers the same job after application failure and shows persistent feedback', async () => {
    mockedApplication.mockRejectedValueOnce(new Error('application unavailable'))
    const wrapper = await mountPage()

    await expect((wrapper.vm as any).addApplication((wrapper.vm as any).items[0])).resolves.toBeUndefined()
    await flushPromises()

    expect((wrapper.vm as any).isApplicationPending(recommendation.items[0])).toBe(false)
    expect((wrapper.vm as any).items[0].isApplied).toBe(0)
    expect(wrapper.get('[data-application-error="7"]').text()).toContain('投递记录创建失败')
    expect(button(wrapper, '加入投递').attributes('disabled')).toBeUndefined()
    expect(ElMessage.error).toHaveBeenCalledWith('投递记录创建失败：application unavailable')
  })

  it('locks pending state by job while allowing different jobs to submit concurrently', async () => {
    const secondItem = {
      ...recommendation.items[0],
      itemId: 302,
      jobId: 8,
      companyName: '远航实验室',
      jobTitle: '交互实习生',
      recommendationScore: 76
    }
    mockedDetail.mockResolvedValueOnce(recommendationFixture([recommendation.items[0], secondItem]) as any)
    const firstRequest = deferred<any>()
    const secondRequest = deferred<any>()
    mockedApplication.mockImplementation((payload: any) => payload.jobId === 7 ? firstRequest.promise : secondRequest.promise)
    const wrapper = await mountPage()
    const pageItems = (wrapper.vm as any).items

    const firstSubmission = (wrapper.vm as any).addApplication(pageItems[0])
    const secondSubmission = (wrapper.vm as any).addApplication(pageItems[1])
    await flushPromises()

    expect(mockedApplication).toHaveBeenCalledTimes(2)
    expect((wrapper.vm as any).isApplicationPending(pageItems[0])).toBe(true)
    expect((wrapper.vm as any).isApplicationPending(pageItems[1])).toBe(true)

    await (wrapper.vm as any).addApplication(pageItems[0])
    expect(mockedApplication).toHaveBeenCalledTimes(2)

    secondRequest.resolve({ applicationId: 52 })
    await secondSubmission
    await flushPromises()
    expect(pageItems[1].isApplied).toBe(1)
    expect((wrapper.vm as any).isApplicationPending(pageItems[0])).toBe(true)
    expect((wrapper.vm as any).isApplicationPending(pageItems[1])).toBe(false)

    firstRequest.resolve({ applicationId: 51 })
    await firstSubmission
    expect(pageItems[0].isApplied).toBe(1)
    expect((wrapper.vm as any).isApplicationPending(pageItems[0])).toBe(false)
  })

  it('uses one heading and separates reasons, fit evidence and risk in AI insight panels', async () => {
    const longCompany = '这是一家名称特别长需要在手机宽度下安全换行的全球数字产品创新科技有限公司'
    mockedDetail.mockResolvedValueOnce({
      ...recommendation,
      items: [{ ...recommendation.items[0], companyName: longCompany }]
    } as any)
    const wrapper = await mountPage()

    expect(wrapper.findAll('h1')).toHaveLength(1)
    const panels = wrapper.findAllComponents({ name: 'AiInsightPanel' })
    expect(panels).toHaveLength(3)
    expect(panels.map((panel) => panel.props('title'))).toEqual(['推荐理由', '匹配证据', '风险提醒'])
    expect(wrapper.get(`[title="${longCompany}"]`).text()).toBe(longCompany)

    const card = wrapper.get('.recommendation-card')
    expect(card.text()).toContain('88')
    expect(card.text()).toContain('强烈推荐')
    expect(card.text()).toContain('技能栈与岗位核心要求高度重合')
    expect(card.text()).toContain('Vue')
    expect(card.text()).toContain('可视化')
    expect(card.text()).toContain('状态：待投递')
  })

  it('maps exactly one primary next action from existing application, analysis or interview actions', async () => {
    const wrapper = await mountPage()

    const primary = wrapper.findAll('.primary-next-action')
    expect(primary).toHaveLength(1)
    expect(primary[0].text()).toBe('加入投递')

    mockedDetail.mockResolvedValueOnce({
      ...recommendation,
      items: [{ ...recommendation.items[0], itemId: 302, analysisReportId: undefined }]
    } as any)
    const analysisPage = await mountPage()
    expect(analysisPage.get('.primary-next-action').text()).toBe('先做 AI 分析')

    mockedDetail.mockResolvedValueOnce({
      ...recommendation,
      items: [{ ...recommendation.items[0], itemId: 303, isApplied: 1 }]
    } as any)
    const interviewPage = await mountPage()
    expect(interviewPage.get('.primary-next-action').text()).toBe('准备面试')
  })

  it('keeps loading and empty detail states explicit', async () => {
    mockedDetail.mockReturnValueOnce(new Promise<any>(() => {}))
    const loadingPage = await mountPage()
    expect(loadingPage.get('[aria-busy="true"]').text()).toBeTruthy()
    expect(loadingPage.get('[aria-live="polite"]').attributes('aria-live')).toBe('polite')

    mockedDetail.mockResolvedValueOnce({ ...recommendation, items: [] } as any)
    const emptyPage = await mountPage()
    expect(emptyPage.get('.app-empty').text()).toContain('这批推荐暂时没有可展示的岗位')
    expect(emptyPage.get('[data-detail-empty-action]').text()).toContain('返回推荐列表')
  })
})
