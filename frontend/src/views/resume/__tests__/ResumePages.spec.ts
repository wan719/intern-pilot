import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import ResumeDetail from '@/views/resume/ResumeDetail.vue'
import ResumeEditor from '@/views/resume/ResumeEditor.vue'
import { getResumeDetailApi, getResumeFileApi } from '@/api/resume'
import { createResumeVersionApi, getResumeVersionDetailApi, getResumeVersionListApi, setCurrentResumeVersionApi } from '@/api/resumeVersion'

const auth = vi.hoisted(() => ({
  user: { userId: 1 },
  hasPermission: vi.fn((_permission: string) => true)
}))
vi.mock('@/stores/auth', () => ({ useAuthStore: () => auth }))
vi.mock('@/api/resume', () => ({ getResumeDetailApi: vi.fn(), getResumeFileApi: vi.fn() }))
vi.mock('@/api/resumeVersion', () => ({
  createResumeVersionApi: vi.fn(), getResumeVersionDetailApi: vi.fn(), getResumeVersionListApi: vi.fn(), setCurrentResumeVersionApi: vi.fn()
}))

const metadata = { resumeId: 7, resumeName: '校园简历', fileType: 'PDF', originalFileName: '校园简历.pdf', parsedText: '上传时保存的文字' }
const current = { versionId: 11, versionName: '原始版本', isCurrent: 1, content: '原始版本正文\n保留换行' }
const copy = { versionId: 12, versionName: '申请版本', isCurrent: 0, content: '改写版本正文', sourceVersionId: 11 }
const pdf = new Blob(['%PDF synthetic'], { type: 'application/pdf' })
const docx = new Blob(['PK synthetic'], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
const read = vi.mocked(getResumeDetailApi)
const file = vi.mocked(getResumeFileApi)
const list = vi.mocked(getResumeVersionListApi)
const detail = vi.mocked(getResumeVersionDetailApi)
const create = vi.mocked(createResumeVersionApi)
const setCurrent = vi.mocked(setCurrentResumeVersionApi)
const createUrl = vi.fn(() => 'blob:ui-test/original')
const releaseUrl = vi.fn()
let viewerProperty: PropertyDescriptor | undefined
const mounted: VueWrapper[] = []

function pending<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>(done => { resolve = done })
  return { promise, resolve }
}

async function open(path: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/resumes', component: { template: '<p>简历中心</p>' } },
      { path: '/resumes/:resumeId', name: 'resume-detail', component: ResumeDetail },
      { path: '/resumes/:resumeId/edit', name: 'resume-edit', component: ResumeEditor },
      { path: '/resumes/:resumeId/versions', component: { template: '<p>版本管理</p>' } },
      { path: '/analysis/match', component: { template: '<p>匹配分析</p>' } },
      { path: '/403', component: { template: '<p>无权限</p>' } }
    ]
  })
  await router.push(path)
  const wrapper = mount({ template: '<RouterView />' }, { global: { plugins: [router], stubs: { teleport: true } } })
  mounted.push(wrapper)
  await flushPromises()
  return { wrapper, router }
}

function button(wrapper: VueWrapper, text: string) {
  const match = wrapper.findAll('button').find(item => item.text() === text)
  if (!match) throw new Error('Missing button ' + text)
  return match
}

beforeEach(() => {
  vi.clearAllMocks()
  auth.hasPermission.mockReturnValue(true)
  read.mockReset().mockImplementation(async id => id === 7 ? metadata as any : { ...metadata, resumeId: id, fileType: 'DOCX', originalFileName: '副本.docx', resumeName: '另一份简历' } as any)
  file.mockReset().mockImplementation(async id => id === 7 ? pdf : docx)
  list.mockReset().mockResolvedValue([current, copy] as any)
  detail.mockReset().mockImplementation(async (_id, versionId) => (versionId === 11 ? current : copy) as any)
  create.mockReset().mockResolvedValue({ versionId: 12 } as any)
  setCurrent.mockReset().mockResolvedValue(undefined as any)
  viewerProperty = Object.getOwnPropertyDescriptor(navigator, 'pdfViewerEnabled')
  Object.defineProperty(navigator, 'pdfViewerEnabled', { configurable: true, value: true })
  vi.stubGlobal('URL', class extends URL { static createObjectURL = createUrl; static revokeObjectURL = releaseUrl })
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
})

afterEach(() => {
  for (const wrapper of mounted.splice(0)) wrapper.unmount()
  if (viewerProperty) Object.defineProperty(navigator, 'pdfViewerEnabled', viewerProperty)
  else Reflect.deleteProperty(navigator, 'pdfViewerEnabled')
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('standalone resume detail and editing', () => {
  it('opens the original by default, downloads the exact PDF and releases it when switching to parsed text', async () => {
    const { wrapper, router } = await open('/resumes/7')
    expect(wrapper.get('#tab-original').attributes('aria-selected')).toBe('true')
    expect(wrapper.get('iframe').attributes('src')).toBe('blob:ui-test/original')
    const names: string[] = []
    vi.mocked(HTMLAnchorElement.prototype.click).mockImplementation(function (this: HTMLAnchorElement) { names.push(this.download) })
    await button(wrapper, '下载原件').trigger('click')
    await flushPromises()
    expect(names).toEqual(['校园简历.pdf'])
    expect(file).toHaveBeenCalledTimes(1)
    expect(createUrl).toHaveBeenCalledWith(pdf)
    await wrapper.get('#tab-text').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/resumes/7?tab=text')
    expect(wrapper.get('#pane-text').text()).toContain(metadata.parsedText)
    expect(releaseUrl).toHaveBeenCalledWith('blob:ui-test/original')
  })

  it('keeps parsed text usable after a failed file request and lets the user retry the original', async () => {
    file.mockRejectedValueOnce({ response: { status: 404 } })
    const { wrapper, router } = await open('/resumes/7')
    expect(wrapper.get('[role="alert"]').text()).toContain('原件不存在')
    await router.push('/resumes/7?tab=text')
    await flushPromises()
    expect(wrapper.get('#pane-text').text()).toContain(metadata.parsedText)
    file.mockRejectedValueOnce({ response: { status: 404 } })
    await router.push('/resumes/7')
    await flushPromises()
    await button(wrapper, '重试').trigger('click')
    await flushPromises()
    expect(wrapper.find('iframe').exists()).toBe(true)
  })

  it('aborts the old PDF on a fast switch and downloads the new DOCX on demand', async () => {
    const stale = pending<Blob>()
    file.mockReturnValueOnce(stale.promise)
    const { wrapper, router } = await open('/resumes/7')
    const signal = file.mock.calls[0][1]
    await router.push('/resumes/8')
    await flushPromises()
    stale.resolve(pdf)
    await flushPromises()
    expect(signal.aborted).toBe(true)
    expect(wrapper.get('h1').text()).toBe('另一份简历')
    expect(wrapper.find('iframe').exists()).toBe(false)
    expect(createUrl).not.toHaveBeenCalled()
    await button(wrapper, '下载原件').trigger('click')
    await flushPromises()
    expect(createUrl).toHaveBeenCalledWith(docx)
  })

  it('ignores metadata returned after routing to another resume', async () => {
    const stale = pending<any>()
    read.mockReturnValueOnce(stale.promise)
    const { wrapper, router } = await open('/resumes/7')
    await router.push('/resumes/8')
    await flushPromises()
    stale.resolve(metadata)
    await flushPromises()
    expect(wrapper.get('h1').text()).toBe('另一份简历')
    expect(file).not.toHaveBeenCalled()
  })

  it('shows the selected version and its lineage, then uses exactly that version for matching', async () => {
    const { wrapper, router } = await open('/resumes/7?tab=version&versionId=12')
    expect(wrapper.get('.resume-paper__text').text()).toBe(copy.content)
    expect(wrapper.text()).toContain('未设为当前')
    expect(wrapper.get('.resume-version-source').text()).toContain(current.versionName)
    expect(wrapper.get('a[href="/analysis/match?resumeId=7&resumeVersionId=12"]').text()).toBe('用此版本做匹配')

    list.mockResolvedValue([ { ...current, isCurrent: 0 }, { ...copy, isCurrent: 1 } ] as any)
    detail.mockResolvedValueOnce({ ...copy, isCurrent: 1 } as any)
    await button(wrapper, '设为当前').trigger('click')
    await flushPromises()
    expect(setCurrent).toHaveBeenCalledWith(7, 12)
    expect(wrapper.get('.resume-version-status').text()).toBe('当前版本')

    await router.push('/resumes/7?tab=version&versionId=999')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('指定版本已不可用')
    expect(wrapper.get('a[href="/resumes/7?tab=version"]').text()).toBe('查看当前版本')
  })

  it('hides write actions for read-only users and prevents direct editor access', async () => {
    auth.hasPermission.mockImplementation(permission => permission === 'resume:read')
    const { wrapper, router } = await open('/resumes/7?tab=version&versionId=12')
    expect(wrapper.findAll('button').some(item => item.text() === '设为当前')).toBe(false)
    expect(wrapper.text()).not.toContain('编辑版本')
    await router.push('/resumes/7/edit?sourceVersionId=11')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/403')
    expect(create).not.toHaveBeenCalled()
  })

  it('preserves a failed draft, prevents duplicate submissions and saves a new version with its source', async () => {
    const { wrapper, router } = await open('/resumes/7/edit?sourceVersionId=11')
    expect((wrapper.get('textarea').element as HTMLTextAreaElement).value).toBe(current.content)
    await wrapper.get('#resume-draft-name').setValue('全栈申请版')
    await wrapper.get('textarea').setValue('更新正文\n第二行')
    expect(wrapper.get('.resume-paper__text').text()).toBe('更新正文\n第二行')
    create.mockRejectedValueOnce(new Error('unavailable'))
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(wrapper.get('[role="alert"]').text()).toContain('保留')
    expect((wrapper.get('textarea').element as HTMLTextAreaElement).value).toBe('更新正文\n第二行')

    const saving = pending<any>()
    create.mockReturnValueOnce(saving.promise)
    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')
    expect(create).toHaveBeenCalledTimes(2)
    expect(create).toHaveBeenLastCalledWith(7, {
      versionName: '全栈申请版', content: '更新正文\n第二行', versionType: 'MANUAL', sourceVersionId: 11
    })
    saving.resolve({ versionId: 12 })
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/resumes/7?tab=version&versionId=12')
    expect(setCurrent).not.toHaveBeenCalled()
  })

  it('warns before discarding an edited draft and explains what the first blank version will do', async () => {
    list.mockResolvedValueOnce([] as any)
    const { wrapper, router } = await open('/resumes/7/edit')
    expect(wrapper.text()).toContain('第一份版本，保存后会成为当前版本')
    expect(wrapper.get('#resume-draft-type').findAll('option').map(option => option.attributes('value')))
      .toEqual(['MANUAL', 'JOB_TARGETED', 'IMPORTED'])
    await wrapper.get('textarea').setValue('未保存的内容')
    const confirm = vi.spyOn(ElMessageBox, 'confirm').mockRejectedValueOnce('cancel').mockResolvedValueOnce('confirm' as any)
    await router.push('/resumes')
    expect(router.currentRoute.value.path).toBe('/resumes/7/edit')
    expect((wrapper.get('textarea').element as HTMLTextAreaElement).value).toBe('未保存的内容')
    await router.push('/resumes')
    expect(router.currentRoute.value.path).toBe('/resumes')
    expect(confirm).toHaveBeenCalledTimes(2)
  })
})
