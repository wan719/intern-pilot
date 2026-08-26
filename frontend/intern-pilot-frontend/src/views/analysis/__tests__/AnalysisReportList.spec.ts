import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AnalysisReportList from '@/views/analysis/AnalysisReportList.vue'
import { deleteAnalysisReportApi, getAnalysisReportDetailApi, getAnalysisReportsApi } from '@/api/analysis'

const { push, resolve, hasPermission } = vi.hoisted(() => ({
  push: vi.fn(),
  resolve: vi.fn((path: string) => ({ href: path })),
  hasPermission: vi.fn(() => true)
}))

vi.mock('@/router', () => ({ default: { push, resolve } }))
vi.mock('@/stores/auth', () => ({ useAuthStore: () => ({ hasPermission }) }))
vi.mock('@/api/analysis', () => ({
  deleteAnalysisReportApi: vi.fn(),
  getAnalysisReportDetailApi: vi.fn(),
  getAnalysisReportsApi: vi.fn()
}))

const mockedDelete = vi.mocked(deleteAnalysisReportApi)
const mockedDetail = vi.mocked(getAnalysisReportDetailApi)
const mockedList = vi.mocked(getAnalysisReportsApi)

const report = {
  reportId: 91,
  resumeName: '前端实习简历',
  companyName: '星河科技',
  jobTitle: '前端实习生',
  matchScore: 86,
  matchLevel: '高匹配',
  cacheHit: false,
  createdAt: '2026-08-25T10:30:00'
}

const reportDetail = {
  ...report,
  strengths: ['Vue 项目经验扎实', 'TypeScript 基础匹配'],
  weaknesses: ['缺少性能优化量化结果'],
  missingSkills: ['ECharts'],
  suggestions: ['补充首屏性能指标'],
  interviewTips: ['准备组件设计取舍案例'],
  aiProvider: 'DeepSeek',
  aiModel: 'deepseek-chat'
}

function button(wrapper: VueWrapper, label: string, index = 0) {
  const matches = wrapper.findAll('button').filter((item) => item.text().trim() === label)
  expect(matches.length).toBeGreaterThan(index)
  return matches[index]
}

async function mountPage(path = '/analysis/reports') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/analysis/reports', component: { template: '<div />' }, meta: { title: '分析报告' } }]
  })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(AnalysisReportList, {
    global: { plugins: [router], stubs: { teleport: true } }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  hasPermission.mockReturnValue(true)
  mockedList.mockResolvedValue({ records: [report] } as any)
  mockedDetail.mockResolvedValue(reportDetail as any)
  mockedDelete.mockResolvedValue(undefined as any)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as any)
  vi.spyOn(window, 'open').mockImplementation(() => null)
})

describe('analysis report list contract characterization', () => {
  it('keeps list payload, detail hydration and score filter API transitions', async () => {
    const wrapper = await mountPage()

    expect(mockedList).toHaveBeenNthCalledWith(1, { pageNum: 1, pageSize: 100 })
    expect(mockedDetail).toHaveBeenCalledWith(91)
    expect(wrapper.text()).toContain('Vue 项目经验扎实')

    ;(wrapper.vm as any).applyScoreFilter('medium')
    await flushPromises()
    expect(mockedList).toHaveBeenLastCalledWith({ minScore: 60, pageNum: 1, pageSize: 100 })

    ;(wrapper.vm as any).resetQuery()
    await flushPromises()
    expect(mockedList).toHaveBeenLastCalledWith({ minScore: undefined, pageNum: 1, pageSize: 100 })
  })

  it('keeps detail, interview and print routes unchanged', async () => {
    const wrapper = await mountPage()

    await button(wrapper, '查看详情').trigger('click')
    await flushPromises()
    expect(mockedDetail).toHaveBeenLastCalledWith(91)

    await button(wrapper, '生成面试题').trigger('click')
    expect(push).toHaveBeenCalledWith('/interview-questions?reportId=91')

    await button(wrapper, '导出 PDF').trigger('click')
    expect(resolve).toHaveBeenCalledWith('/analysis/reports/91/print')
    expect(window.open).toHaveBeenCalledWith('/analysis/reports/91/print', '_blank', 'noopener,noreferrer')
  })

  it('keeps deletion confirmation, selected id and list refresh semantics', async () => {
    const wrapper = await mountPage()

    await button(wrapper, '删除').trigger('click')
    await flushPromises()

    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      '确定要删除“星河科技 - 前端实习生”的分析报告吗？删除后不可恢复。',
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    expect(mockedDelete).toHaveBeenCalledWith(91)
    expect(mockedList).toHaveBeenCalledTimes(2)
  })

  it('keeps report-id query hydration on the existing list route', async () => {
    await mountPage('/analysis/reports?reportId=91')

    expect(mockedList).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(mockedDetail).toHaveBeenCalledWith(91)
  })
})

describe('analysis report list redesign', () => {
  it('uses one result-led heading, summary metrics and the shared FilterBar', async () => {
    const wrapper = await mountPage()

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.get('h1').text()).toBe('把匹配结果变成下一步行动')
    expect(wrapper.findAllComponents({ name: 'StatCard' })).toHaveLength(4)
    expect(wrapper.findComponent({ name: 'FilterBar' }).exists()).toBe(true)
    expect(wrapper.get('[aria-label="分析报告列表"]').attributes('aria-busy')).toBe('false')
  })

  it('keeps one responsive card DOM with score text, context, strengths, risks and next actions', async () => {
    const longCompany = '这是一家名称很长并且需要在三百七十五像素宽度安全换行的全球人工智能科技有限公司'
    mockedList.mockResolvedValueOnce({ records: [{ ...report, companyName: longCompany }] } as any)
    mockedDetail.mockResolvedValueOnce({ ...reportDetail, companyName: longCompany } as any)
    const wrapper = await mountPage()

    const cards = wrapper.findAll('[data-report-card]')
    expect(cards).toHaveLength(1)
    const card = cards[0]
    expect(card.text()).toContain('86')
    expect(card.text()).toContain('高匹配')
    expect(card.get(`[title="${longCompany}"]`).text()).toBe(longCompany)
    expect(card.text()).toContain('前端实习生')
    expect(card.text()).toContain('2026')
    expect(card.get('[data-report-strengths]').text()).toContain('Vue 项目经验扎实')
    expect(card.get('[data-report-risks]').text()).toContain('缺少性能优化量化结果')
    expect(card.get('[data-report-guidance]').text()).toContain('补充首屏性能指标')
    expect(card.findAllComponents({ name: 'AiInsightPanel' })).toHaveLength(3)
  })

  it('renders a labelled loading skeleton without a false empty state', async () => {
    mockedList.mockReturnValueOnce(new Promise<any>(() => {}))
    const wrapper = await mountPage()

    expect(wrapper.get('[aria-label="分析报告列表"]').attributes('aria-busy')).toBe('true')
    expect(wrapper.get('[data-report-loading]').attributes('aria-live')).toBe('polite')
    expect(wrapper.find('.app-empty').exists()).toBe(false)
    expect(wrapper.find('[data-report-card]').exists()).toBe(false)
  })

  it('renders persistent list failure and actionable empty states with retry', async () => {
    mockedList.mockRejectedValueOnce(new Error('reports unavailable'))
    const failedPage = await mountPage()
    expect(failedPage.get('[role="alert"]').text()).toContain('reports unavailable')
    expect(failedPage.get('[data-report-retry]').text()).toContain('重新加载')
    expect(failedPage.find('.app-empty').exists()).toBe(false)

    mockedList.mockResolvedValueOnce({ records: [] } as any)
    const emptyPage = await mountPage()
    expect(emptyPage.get('.app-empty').text()).toContain('还没有分析报告')
    expect(emptyPage.get('[data-report-empty-action]').text()).toContain('开始第一次匹配')
  })
})
