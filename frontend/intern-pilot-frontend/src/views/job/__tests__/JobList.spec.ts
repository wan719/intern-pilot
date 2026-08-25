import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import JobList from '@/views/job/JobList.vue'
import { createJobApi, deleteJobApi, getJobDetailApi, getJobListApi, updateJobApi } from '@/api/job'

const { push, hasPermission } = vi.hoisted(() => ({ push: vi.fn(), hasPermission: vi.fn() }))

vi.mock('@/router', () => ({ default: { push } }))
vi.mock('@/stores/auth', () => ({ useAuthStore: () => ({ hasPermission }) }))
vi.mock('@/api/job', () => ({
  createJobApi: vi.fn(),
  deleteJobApi: vi.fn(),
  getJobDetailApi: vi.fn(),
  getJobListApi: vi.fn(),
  updateJobApi: vi.fn()
}))

const mockedCreate = vi.mocked(createJobApi)
const mockedDelete = vi.mocked(deleteJobApi)
const mockedDetail = vi.mocked(getJobDetailApi)
const mockedList = vi.mocked(getJobListApi)
const mockedUpdate = vi.mocked(updateJobApi)

const jobs = [
  {
    jobId: 7,
    companyName: '星河科技',
    jobTitle: '前端实习生',
    jobType: '前端开发',
    location: '上海',
    sourcePlatform: '官网',
    salaryRange: '250/天',
    createdAt: '2026-08-25T09:00:00'
  }
]

const hydratedJob = {
  ...jobs[0],
  jobUrl: 'https://example.com/jobs/7',
  jdContent: '负责 Vue 与 TypeScript 项目开发',
  skillRequirements: 'Vue\nTypeScript',
  internshipDuration: '3个月，每周5天'
}

function button(wrapper: VueWrapper, label: string, index = 0) {
  const matches = wrapper.findAll('button').filter((item) => item.text().trim() === label)
  expect(matches.length).toBeGreaterThan(index)
  return matches[index]
}

async function mountPage(records?: any[]) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/jobs', component: { template: '<div />' }, meta: { title: '岗位管理' } }]
  })
  await router.push('/jobs')
  await router.isReady()
  if (records !== undefined) mockedList.mockResolvedValue({ records } as any)
  const wrapper = mount(JobList, {
    global: {
      plugins: [router],
      stubs: { teleport: true }
    }
  })
  await flushPromises()
  return wrapper
}

beforeEach(() => {
  vi.clearAllMocks()
  hasPermission.mockReturnValue(true)
  mockedList.mockResolvedValue({ records: jobs } as any)
  mockedDetail.mockResolvedValue(hydratedJob as any)
  mockedCreate.mockResolvedValue({ jobId: 8 } as any)
  mockedUpdate.mockResolvedValue(undefined as any)
  mockedDelete.mockResolvedValue(undefined as any)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined as any)
})

describe('job list legacy behavior', () => {
  it('keeps the list payload, detail hydration and existing detail and analysis routes', async () => {
    const wrapper = await mountPage()

    expect(mockedList).toHaveBeenCalledWith({ keyword: '', jobType: '', location: '', pageNum: 1, pageSize: 100 })
    expect(mockedDetail).toHaveBeenCalledWith(7)
    expect(wrapper.text()).toContain('负责 Vue 与 TypeScript 项目开发')

    await button(wrapper, '详情').trigger('click')
    await flushPromises()
    expect(mockedDetail).toHaveBeenLastCalledWith(7)

    await button(wrapper, '开始分析').trigger('click')
    expect(push).toHaveBeenCalledWith('/analysis/match?jobId=7')
  })

  it('keeps create and edit payload fields and refreshes after success', async () => {
    const wrapper = await mountPage()
    await button(wrapper, '新建岗位').trigger('click')
    Object.assign((wrapper.vm as any).form, {
      companyName: '远航实验室',
      jobTitle: '产品实习生',
      jobType: '产品',
      location: '远程',
      sourcePlatform: '官网',
      jobUrl: 'https://example.com/jobs/8',
      jdContent: '负责用户调研',
      skillRequirements: '用户研究',
      salaryRange: '200/天',
      workDaysPerWeek: '4',
      internshipDuration: '4个月'
    })

    await (wrapper.vm as any).saveJob()

    expect(JSON.parse(JSON.stringify(mockedCreate.mock.calls[0][0]))).toEqual({
      companyName: '远航实验室',
      internshipDuration: '4个月',
      jdContent: '负责用户调研',
      jobTitle: '产品实习生',
      jobType: '产品',
      jobUrl: 'https://example.com/jobs/8',
      location: '远程',
      salaryRange: '200/天',
      skillRequirements: '用户研究',
      sourcePlatform: '官网',
      workDaysPerWeek: '4'
    })

    await (wrapper.vm as any).openEdit(7)
    ;(wrapper.vm as any).form.jdContent = '更新后的 JD'
    await (wrapper.vm as any).saveJob()

    expect(mockedUpdate).toHaveBeenCalledWith(7, expect.objectContaining({
      jobId: 7,
      companyName: '星河科技',
      jobTitle: '前端实习生',
      jdContent: '更新后的 JD'
    }))
    expect(mockedList).toHaveBeenCalledTimes(3)
  })

  it('keeps deletion behind the existing confirmation and cancellation as a no-op', async () => {
    const wrapper = await mountPage()

    await button(wrapper, '删除').trigger('click')
    await flushPromises()
    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      '确认删除“星河科技 - 前端实习生”吗？相关分析可能无法继续引用。',
      '删除岗位',
      { type: 'warning' }
    )
    expect(mockedDelete).toHaveBeenCalledWith(7)

    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel')
    await expect((wrapper.vm as any).removeJob(hydratedJob)).rejects.toBe('cancel')
    expect(mockedDelete).toHaveBeenCalledTimes(1)
  })
})

describe('job opportunity redesign', () => {
  it('shows an authorized recommendation entry and navigates to the guarded list', async () => {
    const wrapper = await mountPage()

    expect(hasPermission).toHaveBeenCalledWith('analysis:read')
    await button(wrapper, '查看岗位推荐').trigger('click')
    expect(push).toHaveBeenCalledWith('/job-recommendations')
  })

  it('does not expose the recommendation entry without analysis:read', async () => {
    hasPermission.mockReturnValue(false)
    const wrapper = await mountPage()

    expect(wrapper.findAll('button').some((item) => item.text().trim() === '查看岗位推荐')).toBe(false)
    expect(wrapper.text()).not.toContain('查看岗位推荐')
  })

  it('uses one goal-led heading, FilterBar and TableShell with safe long-company wrapping', async () => {
    const longCompany = '这是一家拥有非常非常长公司名称且需要在窄屏安全换行的国际化人工智能科技有限公司'
    mockedDetail.mockResolvedValueOnce({ ...hydratedJob, companyName: longCompany } as any)
    const wrapper = await mountPage([{ ...jobs[0], companyName: longCompany }])

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.get('h1').text()).toBe('锁定目标岗位')
    expect(wrapper.findComponent({ name: 'FilterBar' }).exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'TableShell' }).exists()).toBe(true)
    expect(wrapper.get(`[title="${longCompany}"]`).text()).toBe(longCompany)
  })

  it('keeps a long JD collapsed until its explicit keyboard-operable disclosure is used', async () => {
    const longJd = `负责复杂前端应用架构设计，${'需要 Vue TypeScript 可访问性与性能优化经验。'.repeat(12)}`
    mockedDetail.mockResolvedValueOnce({ ...hydratedJob, jdContent: longJd } as any)
    const wrapper = await mountPage()

    const disclosure = wrapper.get('[data-job-jd-toggle="7"]')
    expect(disclosure.element.tagName).toBe('BUTTON')
    expect(disclosure.attributes('aria-expanded')).toBe('false')
    expect(wrapper.get('[data-job-jd="7"]').text()).not.toContain(longJd)

    await disclosure.trigger('click')
    expect(disclosure.attributes('aria-expanded')).toBe('true')
    expect(disclosure.text()).toContain('收起 JD')
    expect(wrapper.get('[data-job-jd="7"]').text()).toContain(longJd)
  })

  it('renders an actionable empty state outside the list shell content', async () => {
    const wrapper = await mountPage([])

    expect(wrapper.get('.app-empty').text()).toContain('还没有目标岗位')
    expect(wrapper.get('[data-job-empty-action]').text()).toContain('新建岗位')
    expect(wrapper.find('.job-card').exists()).toBe(false)
  })

  it('keeps loading and failed reads honest with an accessible retry', async () => {
    mockedList.mockReturnValueOnce(new Promise<any>(() => {}))
    const loadingPage = await mountPage()
    expect(loadingPage.get('.table-shell').attributes('aria-busy')).toBe('true')
    expect(loadingPage.get('.table-shell__loading').attributes('aria-live')).toBe('polite')
    expect(loadingPage.find('.app-empty').exists()).toBe(false)

    mockedList.mockRejectedValueOnce(new Error('jobs unavailable'))
    const failedPage = await mountPage()
    expect(failedPage.get('[role="alert"]').text()).toContain('目标岗位暂时无法加载')
    expect(failedPage.get('[data-job-retry]').text()).toContain('重新加载')
    expect(failedPage.find('.app-empty').exists()).toBe(false)
  })
})
