import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ResumeVersionList from '@/views/resume/ResumeVersionList.vue'
import { getAnalysisReportsApi } from '@/api/analysis'
import { getJobListApi } from '@/api/job'
import {
  compareResumeVersionsApi,
  deleteResumeVersionApi,
  getResumeVersionDetailApi,
  getResumeVersionListApi,
  optimizeResumeVersionApi,
  setCurrentResumeVersionApi
} from '@/api/resumeVersion'

const taskCenter = {
  createTask: vi.fn(() => 'LOCAL_TASK'),
  completeTask: vi.fn(),
  failTask: vi.fn()
}

vi.mock('@/stores/aiTaskCenter', () => ({ useAiTaskCenterStore: () => taskCenter }))
vi.mock('@/api/job', () => ({ getJobListApi: vi.fn() }))
vi.mock('@/api/analysis', () => ({ getAnalysisReportsApi: vi.fn() }))
vi.mock('@/api/resumeVersion', () => ({
  compareResumeVersionsApi: vi.fn(),
  deleteResumeVersionApi: vi.fn(),
  getResumeVersionDetailApi: vi.fn(),
  getResumeVersionListApi: vi.fn(),
  optimizeResumeVersionApi: vi.fn(),
  setCurrentResumeVersionApi: vi.fn(),
}))

const mockedReports = vi.mocked(getAnalysisReportsApi)
const mockedJobs = vi.mocked(getJobListApi)
const mockedCompare = vi.mocked(compareResumeVersionsApi)
const mockedDelete = vi.mocked(deleteResumeVersionApi)
const mockedDetail = vi.mocked(getResumeVersionDetailApi)
const mockedList = vi.mocked(getResumeVersionListApi)
const mockedOptimize = vi.mocked(optimizeResumeVersionApi)
const mockedSetCurrent = vi.mocked(setCurrentResumeVersionApi)

const versions = [
  {
    versionId: 101,
    versionName: '原始版本',
    versionType: 'ORIGINAL',
    contentSummary: '原始内容',
    isCurrent: 1,
    createdAt: '2026-08-20T08:00:00'
  },
  {
    versionId: 202,
    versionName: '岗位定制版',
    versionType: 'JOB_TARGETED',
    contentSummary: '突出 Vue 与 TypeScript',
    isCurrent: 0,
    targetCompanyName: '星河科技',
    targetJobTitle: '前端实习生',
    createdAt: '2026-08-25T09:30:00'
  }
]

function button(wrapper: VueWrapper, label: string, index = 0) {
  const matches = wrapper.findAll('button').filter((item) => item.text().trim() === label)
  expect(matches.length).toBeGreaterThan(index)
  return matches[index]
}

async function mountPage(query = '') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/resumes', component: { template: '<div />' } },
      { path: '/resumes/:resumeId/edit', name: 'resume-edit', component: { template: '<div />' } },
      {
        path: '/resumes/:resumeId/versions',
        component: { template: '<div />' },
        meta: { title: '简历版本管理' }
      }
    ]
  })
  await router.push(`/resumes/7/versions${query}`)
  await router.isReady()
  const wrapper = mount(ResumeVersionList, {
    global: {
      plugins: [router],
      stubs: {
        teleport: true,
        ElDrawer: {
          props: ['modelValue', 'title'],
          template: '<div v-if="modelValue" role="dialog" :aria-label="title"><slot /></div>'
        },
        ElDialog: {
          props: ['modelValue'],
          template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>'
        }
      }
    }
  })
  await flushPromises()
  return { wrapper, router }
}

beforeEach(() => {
  vi.clearAllMocks()
  mockedList.mockResolvedValue(versions as any)
  mockedJobs.mockResolvedValue({ records: [{ jobId: 9, companyName: '星河科技', jobTitle: '前端实习生' }] } as any)
  mockedReports.mockResolvedValue({ records: [{ reportId: 15, resumeId: 7, jobId: 9 }] } as any)
  mockedDetail.mockImplementation(async (_resumeId, versionId) => ({
    ...versions.find((item) => item.versionId === versionId),
    content: `version ${versionId}`
  }) as any)
  mockedSetCurrent.mockResolvedValue(undefined as any)
  mockedDelete.mockResolvedValue(undefined as any)
  mockedCompare.mockResolvedValue({ addedCount: 1, removedCount: 0, addedLines: ['Vue'], removedLines: [] } as any)
  mockedOptimize.mockResolvedValue({ versionId: 404 } as any)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined as any)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('resume version preview', () => {
  it('shows the current version alongside the history on wide screens', async () => {
    vi.stubGlobal('innerWidth', 1440)
    const { wrapper } = await mountPage()

    const preview = wrapper.get('aside[aria-label="版本详情"]')
    expect(preview.text()).toContain('version 101')

    await button(wrapper, '详情', 1).trigger('click')
    await flushPromises()
    expect(preview.text()).toContain('version 202')

    wrapper.unmount()
  })

  it('opens the requested preview in a drawer on narrow screens', async () => {
    vi.stubGlobal('innerWidth', 375)
    const { wrapper } = await mountPage('?versionId=202')

    expect(wrapper.find('aside[aria-label="版本详情"]').exists()).toBe(false)
    expect(wrapper.get('[role="dialog"]').text()).toContain('version 202')

    wrapper.unmount()
  })
})

describe('resume version legacy behavior', () => {
  it('keeps the route param and all initial request payloads', async () => {
    await mountPage()

    expect(mockedList).toHaveBeenCalledWith(7)
    expect(mockedJobs).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(mockedReports).toHaveBeenCalledWith({ resumeId: 7, pageNum: 1, pageSize: 100 })
  })

  it('opens the requested version from the unchanged versionId query', async () => {
    await mountPage('?versionId=202')

    expect(mockedDetail).toHaveBeenCalledWith(7, 202)
  })

  it('preserves version action availability, current payload and delete confirmation', async () => {
    const { wrapper } = await mountPage()

    expect(button(wrapper, '设当前', 0).attributes('disabled')).toBeDefined()
    expect(button(wrapper, '删除', 0).attributes('disabled')).toBeDefined()
    expect(button(wrapper, '删除', 1).attributes('disabled')).toBeUndefined()

    await button(wrapper, '设当前', 1).trigger('click')
    await flushPromises()
    expect(mockedSetCurrent).toHaveBeenCalledWith(7, 202)

    await button(wrapper, '删除', 1).trigger('click')
    await flushPromises()
    expect(ElMessageBox.confirm).toHaveBeenCalledWith('确认删除「岗位定制版」？', '删除版本', { type: 'warning' })
    expect(mockedDelete).toHaveBeenCalledWith(7, 202)
  })

  it('treats cancelling version deletion as a completed no-op', async () => {
    const { wrapper } = await mountPage()
    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel')

    await expect((wrapper.vm as any).removeVersion(versions[1])).resolves.toBeUndefined()

    expect(mockedDelete).not.toHaveBeenCalled()
    expect(mockedList).toHaveBeenCalledTimes(1)
  })

  it('opens the separate editor for a blank draft or a selected version and keeps compare', async () => {
    const { wrapper, router } = await mountPage()
    await button(wrapper, '创建版本').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/resumes/7/edit')

    await router.push('/resumes/7/versions')
    await button(wrapper, '编辑', 1).trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/resumes/7/edit?sourceVersionId=202')

    await (wrapper.vm as any).prepareCompare(versions[1])
    await (wrapper.vm as any).loadCompare()
    expect(mockedCompare).toHaveBeenCalledWith(7, 101, 202)
  })

  it('keeps optimize payload and AI task navigation paths unchanged', async () => {
    const { wrapper } = await mountPage()
    Object.assign((wrapper.vm as any).optimizeForm, {
      sourceVersionId: 101,
      targetJobId: 9,
      aiReportId: 15,
      versionName: 'AI 定制版',
      extraRequirement: '突出前端能力'
    })

    await (wrapper.vm as any).optimizeVersion()

    expect(mockedOptimize).toHaveBeenCalledWith(7, {
      sourceVersionId: 101,
      targetJobId: 9,
      aiReportId: 15,
      versionName: 'AI 定制版',
      extraRequirement: '突出前端能力'
    })
    expect(taskCenter.createTask).toHaveBeenCalledWith(expect.objectContaining({
      type: 'RESUME_OPTIMIZE',
      sourcePath: '/resumes/7/versions'
    }))
    expect(taskCenter.completeTask).toHaveBeenCalledWith('LOCAL_TASK', expect.objectContaining({
      resultId: 404,
      resultPath: '/resumes/7/versions?versionId=404'
    }))
  })
})

describe('resume version timeline redesign', () => {
  it('renders one heading and prioritizes the current version with visible identifiers and dates', async () => {
    mockedList.mockResolvedValue([versions[1], versions[0]] as any)
    const { wrapper } = await mountPage()

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.get('.page-hero').text()).toContain('简历版本')
    const timelineItems = wrapper.findAll('.version-timeline__item')
    expect(timelineItems).toHaveLength(2)
    expect(timelineItems[0].text()).toContain('原始版本')
    expect(timelineItems[0].text()).toContain('当前版本')
    expect(timelineItems[0].text()).toContain('版本 #101')
    expect(timelineItems[0].text()).toContain('2026-08-20 08:00')
    expect(timelineItems[1].get('[role="group"]').attributes('aria-label')).toContain('岗位定制版')
  })

  it('summarizes the selected current version separately from the timeline', async () => {
    const { wrapper } = await mountPage()

    const summary = wrapper.get('[aria-labelledby="version-current-heading"]')
    expect(summary.text()).toContain('当前版本')
    expect(summary.text()).toContain('原始版本')
    expect(summary.text()).toContain('版本 #101')
  })

  it('replaces earlier comparison results with an inline error and supports retry', async () => {
    const { wrapper } = await mountPage()
    await button(wrapper, '对比', 1).trigger('click')
    await flushPromises()
    await button(wrapper, '开始对比').trigger('click')
    await flushPromises()
    expect(wrapper.get('.diff-grid').text()).toContain('Vue')

    mockedCompare.mockRejectedValueOnce(new Error('network unavailable'))
    await button(wrapper, '开始对比').trigger('click')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('对比失败')
    expect(wrapper.find('.diff-grid').exists()).toBe(false)

    await button(wrapper, '重新对比').trigger('click')
    await flushPromises()
    expect(wrapper.get('.diff-grid').text()).toContain('Vue')
    expect(mockedCompare).toHaveBeenNthCalledWith(3, 7, 101, 202)
  })

  it('shows an actionable empty state instead of an empty timeline', async () => {
    mockedList.mockResolvedValue([] as any)
    const { wrapper } = await mountPage()

    expect(wrapper.get('.app-empty').text()).toContain('还没有可管理的简历版本')
    expect(wrapper.get('[data-version-empty-action]').text()).toContain('创建第一个版本')
    expect(wrapper.find('.version-timeline').exists()).toBe(false)
  })

  it('keeps loading and failed reads honest with an accessible retry', async () => {
    mockedList.mockReturnValueOnce(new Promise<any>(() => {}))
    const { wrapper: loadingPage } = await mountPage()
    expect(loadingPage.get('.version-content').attributes('aria-busy')).toBe('true')
    expect(loadingPage.get('.version-loading').attributes('aria-live')).toBe('polite')
    expect(loadingPage.find('.app-empty').exists()).toBe(false)

    mockedList.mockRejectedValueOnce(new Error('network unavailable'))
    const { wrapper: failedPage } = await mountPage()
    expect(failedPage.get('[role="alert"]').text()).toContain('版本列表暂时无法加载')
    expect(failedPage.get('[data-version-retry]').text()).toContain('重新加载')
    expect(failedPage.find('.app-empty').exists()).toBe(false)
  })
})
