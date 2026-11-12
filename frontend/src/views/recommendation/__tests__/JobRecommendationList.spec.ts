import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import JobRecommendationList from '@/views/recommendation/JobRecommendationList.vue'
import {
  deleteJobRecommendationApi,
  generateJobRecommendationApi,
  getJobRecommendationDetailApi,
  getJobRecommendationListApi
} from '@/api/jobRecommendation'
import { getResumeListApi } from '@/api/resume'
import { getResumeVersionListApi } from '@/api/resumeVersion'

const { push, taskCenter } = vi.hoisted(() => ({
  push: vi.fn(),
  taskCenter: {
    createTask: vi.fn(() => 'LOCAL_RECOMMENDATION_TASK'),
    completeTask: vi.fn(),
    failTask: vi.fn()
  }
}))

vi.mock('@/router', () => ({ default: { push } }))
vi.mock('@/stores/aiTaskCenter', () => ({ useAiTaskCenterStore: () => taskCenter }))
vi.mock('@/api/jobRecommendation', () => ({
  deleteJobRecommendationApi: vi.fn(),
  generateJobRecommendationApi: vi.fn(),
  getJobRecommendationDetailApi: vi.fn(),
  getJobRecommendationListApi: vi.fn()
}))
vi.mock('@/api/resume', () => ({ getResumeListApi: vi.fn() }))
vi.mock('@/api/resumeVersion', () => ({ getResumeVersionListApi: vi.fn() }))

const mockedDelete = vi.mocked(deleteJobRecommendationApi)
const mockedGenerate = vi.mocked(generateJobRecommendationApi)
const mockedDetail = vi.mocked(getJobRecommendationDetailApi)
const mockedList = vi.mocked(getJobRecommendationListApi)
const mockedResumes = vi.mocked(getResumeListApi)
const mockedVersions = vi.mocked(getResumeVersionListApi)

const batches = [{
  batchId: 31,
  title: '前端方向推荐',
  recommendedCount: 1,
  jobCount: 4,
  strategy: '综合推荐',
  createdAt: '2026-08-25T10:00:00'
}]

const detail = {
  ...batches[0],
  resumeId: 3,
  resumeVersionId: 5,
  items: [{
    itemId: 301,
    jobId: 7,
    companyName: '星河科技',
    jobTitle: '前端实习生',
    recommendationScore: 88,
    recommendationLevel: 'HIGH'
  }]
}

function button(wrapper: VueWrapper, label: string, index = 0) {
  const matches = wrapper.findAll('button').filter((item) => item.text().trim() === label)
  expect(matches.length).toBeGreaterThan(index)
  return matches[index]
}

async function mountPage() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/job-recommendations', component: { template: '<div />' }, meta: { title: '岗位推荐' } }]
  })
  await router.push('/job-recommendations')
  await router.isReady()
  const wrapper = mount(JobRecommendationList, {
    global: { plugins: [router], stubs: { teleport: true } }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  mockedResumes.mockResolvedValue({ records: [{ resumeId: 3, resumeName: '前端简历' }] } as any)
  mockedVersions.mockResolvedValue([
    { versionId: 4, versionName: '初稿', isCurrent: 0 },
    { versionId: 5, versionName: '当前版', isCurrent: 1 }
  ] as any)
  mockedList.mockResolvedValue({ records: batches } as any)
  mockedDetail.mockResolvedValue(detail as any)
  mockedGenerate.mockResolvedValue({ batchId: 32 } as any)
  mockedDelete.mockResolvedValue(undefined as any)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as any)
})

describe('job recommendation list legacy behavior', () => {
  it('keeps option, list and batch hydration request payloads and detail route', async () => {
    const wrapper = await mountPage()

    expect(mockedResumes).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(mockedList).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(mockedDetail).toHaveBeenCalledWith(31)

    ;(wrapper.vm as any).form.resumeId = 3
    await flushPromises()
    expect(mockedVersions).toHaveBeenCalledWith(3)
    expect((wrapper.vm as any).form.resumeVersionId).toBe(5)

    await button(wrapper, '查看详情').trigger('click')
    expect(push).toHaveBeenCalledWith('/job-recommendations/31')
  })

  it('keeps the generation payload, task bookkeeping and generated batch route', async () => {
    const wrapper = await mountPage()
    Object.assign((wrapper.vm as any).form, {
      resumeId: 3,
      resumeVersionId: 5,
      includeApplied: true,
      limit: 12
    })
    await flushPromises()

    await (wrapper.vm as any).generateRecommendation()

    expect(mockedGenerate).toHaveBeenCalledWith(expect.objectContaining({
      resumeId: 3,
      resumeVersionId: 5,
      includeApplied: true,
      limit: 12
    }))
    expect(taskCenter.createTask).toHaveBeenCalledWith({
      type: 'JOB_RECOMMENDATION',
      title: '岗位推荐',
      message: '正在生成岗位推荐...',
      resumeId: 3,
      sourcePath: '/job-recommendations'
    })
    expect(taskCenter.completeTask).toHaveBeenCalledWith('LOCAL_RECOMMENDATION_TASK', {
      resultId: 32,
      resultPath: '/job-recommendations/32',
      message: '岗位推荐生成完成'
    })
    expect(push).toHaveBeenCalledWith('/job-recommendations/32')
  })

  it('keeps failed generation retryable and reports it through the existing task center', async () => {
    const wrapper = await mountPage()
    ;(wrapper.vm as any).form.resumeId = 3
    await flushPromises()
    mockedGenerate.mockRejectedValueOnce(new Error('generation failed'))

    await (wrapper.vm as any).generateRecommendation()

    expect(taskCenter.failTask).toHaveBeenCalledWith('LOCAL_RECOMMENDATION_TASK', '推荐生成失败')
    expect((wrapper.vm as any).generating).toBe(false)
    expect(push).not.toHaveBeenCalled()
  })

  it('keeps a batch visible and navigable when preview hydration fails', async () => {
    mockedDetail.mockRejectedValueOnce(new Error('detail unavailable'))
    const wrapper = await mountPage()

    expect(wrapper.text()).toContain('前端方向推荐')
    expect(wrapper.text()).toContain('进入详情查看推荐岗位')
    await button(wrapper, '查看详情').trigger('click')
    expect(push).toHaveBeenCalledWith('/job-recommendations/31')
  })

  it('keeps deletion behind confirmation with the selected batch id', async () => {
    const wrapper = await mountPage()
    await button(wrapper, '删除').trigger('click')
    await flushPromises()

    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      '确认删除“前端方向推荐”吗？删除后无法继续查看这批推荐结果。',
      '删除确认',
      { type: 'warning' }
    )
    expect(mockedDelete).toHaveBeenCalledWith(31)

    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel')
    await expect((wrapper.vm as any).removeBatch(batches[0])).resolves.toBeUndefined()
    expect(mockedDelete).toHaveBeenCalledTimes(1)
  })
})

describe('job recommendation list redesign', () => {
  it('requests and renders history while resume options are still pending without showing false empty', async () => {
    mockedResumes.mockReturnValueOnce(new Promise<any>(() => {}))
    const wrapper = await mountPage()

    expect(mockedList).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(wrapper.text()).toContain('前端方向推荐')
    expect(wrapper.get('.generator-panel').attributes('aria-busy')).toBe('true')
    expect(button(wrapper, '生成推荐').attributes('disabled')).toBeDefined()
    expect(wrapper.find('.app-empty').exists()).toBe(false)
  })

  it('keeps readable history visible when resume options fail and exposes an independent retry', async () => {
    mockedResumes.mockRejectedValueOnce(new Error('resume options unavailable'))
    const wrapper = await mountPage()

    expect(mockedList).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(wrapper.text()).toContain('前端方向推荐')
    expect(wrapper.get('[data-options-error]').text()).toContain('简历选项暂时无法加载')
    expect(wrapper.get('[data-options-retry]').text()).toContain('重新加载简历')
    expect(button(wrapper, '生成推荐').attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-recommendation-retry]').exists()).toBe(false)
  })

  it('recovers the generator independently after retrying failed resume options', async () => {
    mockedResumes.mockRejectedValueOnce(new Error('resume options unavailable'))
    const wrapper = await mountPage()
    mockedResumes.mockResolvedValueOnce({ records: [{ resumeId: 3, resumeName: '前端简历' }] } as any)

    await button(wrapper, '重新加载简历').trigger('click')
    await flushPromises()

    expect(mockedResumes).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-options-error]').exists()).toBe(false)
    expect(wrapper.get('.generator-panel').attributes('aria-busy')).toBe('false')
    expect(button(wrapper, '生成推荐').attributes('disabled')).toBeUndefined()
    expect(wrapper.text()).toContain('前端方向推荐')
  })

  it('uses one opportunity heading and a shared loading-aware list shell', async () => {
    const wrapper = await mountPage()

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.get('h1').text()).toBe('发现更值得投入的岗位')
    expect(wrapper.findComponent({ name: 'TableShell' }).exists()).toBe(true)
    expect(wrapper.get('.recommendation-library').attributes('aria-label')).toBe('岗位推荐批次')
  })

  it('pairs preview scores with text evidence, risk and status including long company names', async () => {
    const longCompany = '这是一家名称特别长需要在手机宽度下安全换行的全球数字产品创新科技有限公司'
    mockedDetail.mockResolvedValueOnce({
      ...detail,
      items: [{
        ...detail.items[0],
        companyName: longCompany,
        matchedSkills: ['Vue'],
        missingSkills: ['数据可视化'],
        reasons: ['核心技术方向匹配'],
        isApplied: 1
      }]
    } as any)
    const wrapper = await mountPage()

    const preview = wrapper.get('.preview-job')
    expect(preview.get(`[title="${longCompany}"]`).text()).toContain(longCompany)
    expect(preview.text()).toContain('强烈推荐')
    expect(preview.text()).toContain('证据：Vue')
    expect(preview.text()).toContain('风险：数据可视化')
    expect(preview.text()).toContain('状态：已投递')
  })

  it('shows actionable empty, list failure and batch-preview failure states with retry', async () => {
    mockedList.mockResolvedValueOnce({ records: [] } as any)
    const emptyPage = await mountPage()
    expect(emptyPage.get('.app-empty').text()).toContain('还没有岗位推荐')
    expect(emptyPage.get('[data-recommendation-empty-action]').text()).toContain('生成第一批推荐')

    mockedList.mockRejectedValueOnce(new Error('history unavailable'))
    const failedPage = await mountPage()
    expect(failedPage.get('[role="alert"]').text()).toContain('推荐批次暂时无法加载')
    expect(failedPage.get('[data-recommendation-retry]').text()).toContain('重新加载')

    mockedDetail.mockRejectedValueOnce(new Error('preview unavailable'))
    const previewFailedPage = await mountPage()
    expect(previewFailedPage.get('[data-batch-preview-error="31"]').text()).toContain('预览暂不可用')
    const batchRetry = previewFailedPage.get('[data-batch-retry="31"]')
    expect(batchRetry.text()).toContain('重试')
    const callsBeforeRetry = mockedList.mock.calls.length
    await batchRetry.trigger('click')
    await flushPromises()
    expect(mockedList).toHaveBeenCalledTimes(callsBeforeRetry + 1)
  })

  it('keeps pending reads in a labelled loading state and generation failures retryable', async () => {
    mockedList.mockReturnValueOnce(new Promise<any>(() => {}))
    const loadingPage = await mountPage()
    expect(loadingPage.get('.table-shell').attributes('aria-busy')).toBe('true')
    expect(loadingPage.get('.table-shell__loading').attributes('aria-live')).toBe('polite')

    mockedGenerate.mockRejectedValueOnce(new Error('generation failed'))
    const failedGenerationPage = await mountPage()
    ;(failedGenerationPage.vm as any).form.resumeId = 3
    await flushPromises()
    await (failedGenerationPage.vm as any).generateRecommendation()
    expect(failedGenerationPage.get('[data-generation-error]').text()).toContain('推荐生成失败')
    expect(failedGenerationPage.get('[data-generation-retry]').text()).toContain('重新生成')
  })
})
