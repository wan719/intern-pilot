import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ResumeList from '@/views/resume/ResumeList.vue'
import {
  deleteResumeApi,
  getResumeDetailApi,
  getResumeListApi,
  setDefaultResumeApi,
  uploadResumeApi
} from '@/api/resume'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))

vi.mock('@/router', () => ({ default: { push } }))
vi.mock('@/api/resume', () => ({
  deleteResumeApi: vi.fn(),
  getResumeDetailApi: vi.fn(),
  getResumeListApi: vi.fn(),
  setDefaultResumeApi: vi.fn(),
  uploadResumeApi: vi.fn()
}))

const mockedDelete = vi.mocked(deleteResumeApi)
const mockedDetail = vi.mocked(getResumeDetailApi)
const mockedList = vi.mocked(getResumeListApi)
const mockedSetDefault = vi.mocked(setDefaultResumeApi)
const mockedUpload = vi.mocked(uploadResumeApi)

const resumes = [
  {
    resumeId: 11,
    resumeName: '默认简历',
    originalFileName: 'default.pdf',
    fileType: 'PDF',
    fileSize: 2048,
    parseStatus: 'SUCCESS',
    isDefault: true,
    createdAt: '2026-08-24T08:00:00'
  },
  {
    resumeId: 22,
    resumeName: '前端实习简历',
    originalFileName: 'frontend.docx',
    fileType: 'DOCX',
    fileSize: 4096,
    parseStatus: 'FAILED',
    isDefault: false,
    createdAt: '2026-08-25T09:30:00'
  }
]

function button(wrapper: VueWrapper, label: string, index = 0) {
  const matches = wrapper.findAll('button').filter((item) => item.text().trim() === label)
  expect(matches.length).toBeGreaterThan(index)
  return matches[index]
}

async function mountPage(records?: any[]) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/resumes', component: { template: '<div />' }, meta: { title: '简历管理' } }]
  })
  await router.push('/resumes')
  await router.isReady()
  if (records !== undefined) mockedList.mockResolvedValue({ records } as any)
  const wrapper = mount(ResumeList, {
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
  mockedList.mockResolvedValue({ records: resumes } as any)
  mockedDelete.mockResolvedValue(undefined as any)
  mockedDetail.mockResolvedValue({ ...resumes[1], parsedText: '项目经历' } as any)
  mockedSetDefault.mockResolvedValue(undefined as any)
  mockedUpload.mockResolvedValue(undefined as any)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined as any)
})

describe('resume list legacy behavior', () => {
  it('loads the existing page payload and preserves detail, version and default actions', async () => {
    const wrapper = await mountPage()

    expect(mockedList).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })

    await button(wrapper, '详情', 1).trigger('click')
    await flushPromises()
    expect(mockedDetail).toHaveBeenCalledWith(22)

    await button(wrapper, '版本管理', 1).trigger('click')
    expect(push).toHaveBeenCalledWith('/resumes/22/versions')

    await button(wrapper, '设为默认').trigger('click')
    await flushPromises()
    expect(mockedSetDefault).toHaveBeenCalledWith(22)
    expect(mockedList).toHaveBeenCalledTimes(2)
  })

  it('keeps the upload FormData keys and refreshes after success', async () => {
    const wrapper = await mountPage()
    const file = new File(['resume'], 'candidate.pdf', { type: 'application/pdf' })
    ;(wrapper.vm as any).selectedFile = file
    ;(wrapper.vm as any).uploadForm.resumeName = 'Java 后端简历'
    await (wrapper.vm as any).submitUpload()

    const data = mockedUpload.mock.calls[0][0]
    expect(data.get('file')).toBe(file)
    expect(data.get('resumeName')).toBe('Java 后端简历')
    expect(mockedList).toHaveBeenCalledTimes(2)
    expect(ElMessage.success).toHaveBeenCalledWith('上传成功')
  })

  it('keeps a failed upload available for retry and always releases its loading state', async () => {
    const wrapper = await mountPage()
    mockedUpload.mockRejectedValueOnce(new Error('upload failed'))
    const file = new File(['resume'], 'candidate.pdf', { type: 'application/pdf' })
    ;(wrapper.vm as any).selectedFile = file
    ;(wrapper.vm as any).uploadVisible = true

    await expect((wrapper.vm as any).submitUpload()).rejects.toThrow('upload failed')

    expect((wrapper.vm as any).uploadVisible).toBe(true)
    expect((wrapper.vm as any).uploading).toBe(false)
    expect(mockedList).toHaveBeenCalledTimes(1)
  })

  it('keeps deletion behind confirmation and deletes only the selected id', async () => {
    const wrapper = await mountPage()

    await button(wrapper, '删除', 1).trigger('click')
    await flushPromises()

    expect(ElMessageBox.confirm).toHaveBeenCalledWith('确认删除这份简历吗？', '删除简历', { type: 'warning' })
    expect(mockedDelete).toHaveBeenCalledWith(22)
    expect(mockedList).toHaveBeenCalledTimes(2)
  })

  it('treats cancelling delete confirmation as a completed no-op', async () => {
    const wrapper = await mountPage()
    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel')

    await expect((wrapper.vm as any).removeResume(22)).resolves.toBeUndefined()

    expect(mockedDelete).not.toHaveBeenCalled()
    expect(mockedList).toHaveBeenCalledTimes(1)
  })
})

describe('resume center redesign', () => {
  it('renders one goal-led heading with separate desktop and mobile list semantics', async () => {
    const longName = '2026-超长文件名-前端工程师-包含项目经历与技能关键词-用于验证安全换行和可访问完整名称.pdf'
    const wrapper = await mountPage([{
      ...resumes[1],
      resumeName: longName,
      originalFileName: longName
    }])

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.get('.page-hero').text()).toContain('简历中心')
    expect(wrapper.get('.resume-desktop').attributes('aria-label')).toBe('简历列表')
    expect(wrapper.get('.resume-mobile').attributes('aria-label')).toBe('移动端简历列表')
    expect(wrapper.get('.resume-mobile article').text()).toContain('默认状态')
    expect(wrapper.get('.resume-mobile article').text()).toContain('上传时间')
    expect(wrapper.findAll(`[title="${longName}"]`).length).toBeGreaterThanOrEqual(2)
    expect(wrapper.get('.resume-mobile [role="group"]').attributes('aria-label')).toContain(longName)
  })

  it('shows an actionable empty state without rendering empty list shells', async () => {
    const wrapper = await mountPage([])

    expect(wrapper.get('.app-empty').text()).toContain('上传简历后即可开始 AI 匹配分析')
    expect(wrapper.get('[data-resume-empty-action]').text()).toContain('上传简历')
    expect(wrapper.find('.resume-desktop').exists()).toBe(false)
    expect(wrapper.find('.resume-mobile').exists()).toBe(false)
  })

  it('keeps loading and failed reads honest with an accessible retry', async () => {
    const pending = new Promise<any>(() => {})
    mockedList.mockReturnValueOnce(pending)
    const loadingPage = await mountPage()
    expect(loadingPage.get('.resume-content').attributes('aria-busy')).toBe('true')
    expect(loadingPage.get('.resume-loading').attributes('aria-live')).toBe('polite')
    expect(loadingPage.find('.app-empty').exists()).toBe(false)

    mockedList.mockRejectedValueOnce(new Error('network unavailable'))
    const failedPage = await mountPage()
    expect(failedPage.get('[role="alert"]').text()).toContain('简历列表暂时无法加载')
    expect(failedPage.get('[data-resume-retry]').text()).toContain('重新加载')
    expect(failedPage.find('.app-empty').exists()).toBe(false)
  })
})
