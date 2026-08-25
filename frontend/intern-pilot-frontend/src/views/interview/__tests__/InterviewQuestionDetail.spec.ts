import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import InterviewQuestionDetail from '@/views/interview/InterviewQuestionDetail.vue'
import { getInterviewQuestionDetailApi, regenerateInterviewQuestionsApi } from '@/api/interviewQuestion'

const { taskCenter } = vi.hoisted(() => ({
  taskCenter: {
    createTask: vi.fn(() => 'LOCAL_REGENERATE_TASK'),
    completeTask: vi.fn(),
    failTask: vi.fn()
  }
}))

vi.mock('@/stores/aiTaskCenter', () => ({ useAiTaskCenterStore: () => taskCenter }))
vi.mock('@/api/interviewQuestion', () => ({
  getInterviewQuestionDetailApi: vi.fn(),
  regenerateInterviewQuestionsApi: vi.fn()
}))

const mockedDetail = vi.mocked(getInterviewQuestionDetailApi)
const mockedRegenerate = vi.mocked(regenerateInterviewQuestionsApi)

const detail = {
  reportId: 41,
  title: '星河科技前端实习生面试题',
  resumeId: 3,
  resumeVersionId: 5,
  jobId: 7,
  analysisReportId: 11,
  companyName: '星河科技',
  jobTitle: '前端实习生',
  questionCount: 1,
  createdAt: '2026-08-25T10:00:00',
  questions: [{
    questionId: 101,
    questionType: 'SPRING_BOOT',
    difficulty: 'MEDIUM',
    question: '请解释自动配置的工作原理。',
    answer: '先说明条件装配，再结合实际排查过程。',
    answerPoints: ['条件注解', '配置导入'],
    relatedSkills: ['Spring Boot'],
    followUps: ['如何定位未生效的自动配置？'],
    keywords: ['AutoConfiguration', 'Condition'],
    source: '岗位要求：熟悉 Spring Boot',
    sortOrder: 1
  }]
}

function button(wrapper: VueWrapper, label: string) {
  const match = wrapper.findAll('button').find((item) => item.text().trim() === label)
  expect(match).toBeDefined()
  return match!
}

async function mountPage() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/interview-questions', component: { template: '<div />' } },
      { path: '/interview-questions/:id', component: { template: '<div />' }, meta: { title: '面试题详情' } }
    ]
  })
  await router.push('/interview-questions/41')
  await router.isReady()
  const wrapper = mount(InterviewQuestionDetail, {
    global: { plugins: [router], stubs: { teleport: true } }
  })
  await flushPromises()
  return { wrapper, router }
}

beforeEach(() => {
  vi.clearAllMocks()
  mockedDetail.mockResolvedValue(structuredClone(detail) as any)
  mockedRegenerate.mockResolvedValue({ reportId: 42 } as any)
  vi.spyOn(ElMessage, 'success').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => undefined as any)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as any)
})

describe('interview question detail legacy contracts', () => {
  it('uses the numeric route id and preserves question, answer, key-point, follow-up and keyword data', async () => {
    const { wrapper } = await mountPage()

    expect(mockedDetail).toHaveBeenCalledWith(41)
    expect(wrapper.text()).toContain('请解释自动配置的工作原理。')

    await button(wrapper, '查看答案').trigger('click')
    expect(wrapper.text()).toContain('先说明条件装配，再结合实际排查过程。')
    expect(wrapper.text()).toContain('条件注解')
    expect(wrapper.text()).toContain('配置导入')
    expect(wrapper.text()).toContain('Spring Boot')
    expect(wrapper.text()).toContain('如何定位未生效的自动配置？')
    expect(wrapper.text()).toContain('AutoConfiguration')
    expect(wrapper.text()).toContain('Condition')
    expect(wrapper.text()).toContain('岗位要求：熟悉 Spring Boot')
  })

  it('keeps exact category and difficulty labels without changing their values', async () => {
    const { wrapper } = await mountPage()
    const vm = wrapper.vm as any

    expect([
      'JAVA_BASIC', 'SPRING_BOOT', 'SPRING_SECURITY', 'MYSQL', 'REDIS', 'PROJECT',
      'ALGORITHM', 'SYSTEM_DESIGN', 'HR', 'RESUME', 'JOB_SKILL'
    ].map((value) => [value, vm.questionTypeLabel(value)])).toEqual([
      ['JAVA_BASIC', '技术基础'],
      ['SPRING_BOOT', 'Spring Boot'],
      ['SPRING_SECURITY', 'Spring Security'],
      ['MYSQL', 'MySQL'],
      ['REDIS', 'Redis'],
      ['PROJECT', '项目经历'],
      ['ALGORITHM', '算法与数据结构'],
      ['SYSTEM_DESIGN', '系统设计'],
      ['HR', 'HR / 行为面'],
      ['RESUME', '简历深挖'],
      ['JOB_SKILL', '岗位技能']
    ])
    expect(['EASY', 'MEDIUM', 'HARD'].map((value) => [value, vm.difficultyLabel(value)])).toEqual([
      ['EASY', '简单'],
      ['MEDIUM', '中等'],
      ['HARD', '较难']
    ])
  })

  it('keeps return and regenerated detail routes plus regeneration task state', async () => {
    const { wrapper, router } = await mountPage()

    await (wrapper.vm as any).regenerate()
    await flushPromises()
    expect(mockedRegenerate).toHaveBeenCalledWith(41)
    expect(taskCenter.createTask).toHaveBeenCalledWith({
      type: 'INTERVIEW_REGENERATE',
      title: '面试题重新生成',
      message: '正在重新生成面试题...',
      reportId: 41,
      sourcePath: '/interview-questions/41'
    })
    expect(taskCenter.completeTask).toHaveBeenCalledWith('LOCAL_REGENERATE_TASK', {
      resultId: 42,
      resultPath: '/interview-questions/42',
      message: '面试题重新生成完成'
    })
    expect(router.currentRoute.value.fullPath).toBe('/interview-questions/42')

    await button(wrapper, '返回列表').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/interview-questions')
  })

  it('keeps detail and regeneration failures recoverable', async () => {
    mockedDetail.mockRejectedValueOnce(new Error('detail unavailable'))
    const failedLoad = await mountPage()
    expect(failedLoad.wrapper.text()).toContain('面试题加载失败')
    expect(failedLoad.wrapper.text()).toContain('detail unavailable')
    mockedDetail.mockResolvedValueOnce(structuredClone(detail) as any)
    await button(failedLoad.wrapper, '重试').trigger('click')
    await flushPromises()
    expect(failedLoad.wrapper.text()).toContain('请解释自动配置的工作原理。')

    mockedRegenerate.mockRejectedValueOnce(new Error('regeneration unavailable'))
    await (failedLoad.wrapper.vm as any).regenerate()
    expect(taskCenter.failTask).toHaveBeenCalledWith('LOCAL_REGENERATE_TASK', '面试题重新生成失败')
    expect((failedLoad.wrapper.vm as any).regenerating).toBe(false)
  })
})

describe('interview question reading redesign', () => {
  it('uses one page heading, labelled category navigation and keyboard-readable answer controls', async () => {
    const { wrapper } = await mountPage()

    expect(wrapper.findAll('h1')).toHaveLength(1)
    expect(wrapper.get('h1').text()).toContain('星河科技前端实习生面试题')
    expect(wrapper.find('nav[aria-label="题目分类"]').exists()).toBe(true)
    expect(wrapper.get('nav[aria-label="题目分类"] button').attributes('aria-current')).toBe('page')
    const toggle = button(wrapper, '查看答案')
    expect(toggle.attributes('aria-expanded')).toBe('false')
    expect(toggle.attributes('aria-controls')).toBe('answer-101')
    await toggle.trigger('click')
    expect(button(wrapper, '隐藏答案').attributes('aria-expanded')).toBe('true')
  })

  it('orders each reading flow as question, answer, key points and follow-ups', async () => {
    const { wrapper } = await mountPage()
    await button(wrapper, '查看答案').trigger('click')

    expect(wrapper.get('.question-card').findAll('[data-study-section]').map((section) => section.attributes('data-study-section'))).toEqual([
      'question',
      'answer',
      'key-points',
      'follow-ups'
    ])
  })

  it('keeps long answers readable, wraps many keywords and gives actionable no-follow-up guidance', async () => {
    const longAnswer = '这是一个需要保持可读行长的详细答案。'.repeat(30)
    mockedDetail.mockResolvedValueOnce({
      ...structuredClone(detail),
      questions: [{
        ...structuredClone(detail.questions[0]),
        answer: longAnswer,
        keywords: Array.from({ length: 16 }, (_, index) => `关键词-${index + 1}`),
        followUps: []
      }]
    } as any)
    const { wrapper } = await mountPage()
    await button(wrapper, '查看答案').trigger('click')

    expect(wrapper.get('.answer-copy').text()).toBe(longAnswer)
    expect(wrapper.get('.keyword-list').findAll('.el-tag')).toHaveLength(16)
    expect(wrapper.get('[data-study-section="follow-ups"]').text()).toContain('暂无追问建议')
    expect(wrapper.get('[data-study-section="follow-ups"]').text()).toContain('尝试说明一个真实项目案例')
  })

  it('keeps desktop study actions sticky with one DOM that can return to normal mobile flow', async () => {
    const { wrapper } = await mountPage()

    expect(wrapper.findAll('.study-actions')).toHaveLength(1)
    expect(wrapper.findAll('[data-study-action="regenerate"]')).toHaveLength(1)
    expect(wrapper.findAll('[data-study-action="back"]')).toHaveLength(1)
  })

  it('exposes honest loading, failure/retry and no-question states', async () => {
    mockedDetail.mockReturnValueOnce(new Promise<any>(() => {}))
    const loadingPage = await mountPage()
    expect(loadingPage.wrapper.get('[aria-busy="true"]').text()).toContain('正在加载面试题')

    mockedDetail.mockRejectedValueOnce(new Error('detail unavailable'))
    const failedPage = await mountPage()
    expect(failedPage.wrapper.get('[role="alert"]').text()).toContain('detail unavailable')
    expect(button(failedPage.wrapper, '重试').text()).toBe('重试')

    mockedDetail.mockResolvedValueOnce({ ...structuredClone(detail), questionCount: 0, questions: [] } as any)
    const emptyPage = await mountPage()
    expect(emptyPage.wrapper.get('.app-empty').text()).toContain('这份报告暂无题目')
    expect(button(emptyPage.wrapper, '重新生成').text()).toBe('重新生成')
  })
})
