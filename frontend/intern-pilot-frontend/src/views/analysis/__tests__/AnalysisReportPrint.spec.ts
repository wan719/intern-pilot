import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getAnalysisReportDetailApi } from '@/api/analysis'
import AnalysisReportPrint from '@/views/analysis/AnalysisReportPrint.vue'
import analysisReportPrintSource from '@/views/analysis/AnalysisReportPrint.vue?raw'

vi.mock('@/api/analysis', () => ({ getAnalysisReportDetailApi: vi.fn() }))

const mockedReportDetail = vi.mocked(getAnalysisReportDetailApi)
const report = {
  reportId: 91,
  companyName: '星河科技',
  jobTitle: '前端实习生',
  resumeName: '前端简历',
  resumeId: 3,
  resumeVersionId: 5,
  jobId: 7,
  matchScore: 82,
  matchLevel: '高匹配',
  strengths: ['Vue 3 经验'],
  weaknesses: ['性能指标不足'],
  missingSkills: ['ECharts'],
  suggestions: ['补充量化指标'],
  interviewTips: ['准备项目取舍说明'],
  createdAt: '2026-08-26T10:00:00',
  aiProvider: 'DeepSeek',
  aiModel: 'deepseek-v4-flash'
}

async function mountPrint(path = '/analysis/reports/91/print') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/analysis/reports/:id/print', component: { template: '<div />' } }]
  })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(AnalysisReportPrint, { global: { plugins: [router] } })
  await flushPromises()
  return wrapper
}

describe('analysis report print shell', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedReportDetail.mockResolvedValue(report as any)
  })

  it('loads the route report in an isolated document without screen application chrome', async () => {
    const wrapper = await mountPrint()

    expect(mockedReportDetail).toHaveBeenCalledWith(91)
    expect(wrapper.get('main.print-shell').text()).toContain('星河科技 - 前端实习生')
    expect(wrapper.get('.score-box').text()).toContain('82')
    expect(wrapper.find('.app-header').exists()).toBe(false)
    expect(wrapper.find('.mobile-bottom-nav').exists()).toBe(false)
    expect(wrapper.find('.ai-task-drawer').exists()).toBe(false)
    expect(wrapper.find('.feedback-drawer').exists()).toBe(false)
  })

  it('uses the browser print command only from the explicit print action', async () => {
    const print = vi.spyOn(window, 'print').mockImplementation(() => undefined)
    const wrapper = await mountPrint()

    await wrapper.findAll('button').find((item) => item.text().includes('打印 / 保存 PDF'))!.trigger('click')

    expect(print).toHaveBeenCalledTimes(1)
  })

  it('rejects an invalid report id without making a detail request', async () => {
    const wrapper = await mountPrint('/analysis/reports/not-a-number/print')

    expect(mockedReportDetail).not.toHaveBeenCalled()
    expect(wrapper.get('.print-error').text()).toContain('报告 ID 不正确')
  })

  it('keeps narrow viewport layout rules screen-only so they cannot override print resets', () => {
    expect(analysisReportPrintSource).toMatch(/@media\s+screen\s+and\s+\(max-width:\s*760px\)/)
    expect(analysisReportPrintSource).not.toMatch(/@media\s+\(max-width:\s*760px\)/)
  })
})
