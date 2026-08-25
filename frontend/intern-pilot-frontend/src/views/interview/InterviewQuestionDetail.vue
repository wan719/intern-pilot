<template>
  <PageContainer title="面试题深度练习" width="wide">
    <template #hero>
      <PageHero
        eyebrow="面试准备 · 深度练习"
        :title="detail?.title || '面试题深度练习'"
        description="先独立组织答案，再按题目、参考答案、关键要点和追问的顺序校准表达。"
      >
        <template v-if="detail" #default>
          <div class="hero-context">
            <span>{{ detail.companyName || '未知公司' }}</span>
            <span>{{ detail.jobTitle || '未知岗位' }}</span>
            <span>{{ detail.questionCount || 0 }} 道题</span>
            <span>{{ formatDateTime(detail.createdAt) }}</span>
            <strong>{{ answeredCount }}/{{ questionTotal }} 已展开答案</strong>
          </div>
        </template>
      </PageHero>
    </template>

    <div class="study-actions" aria-label="面试题操作">
      <div>
        <el-button data-study-action="back" @click="router.push('/interview-questions')">返回列表</el-button>
        <el-button
          data-study-action="regenerate"
          type="primary"
          :disabled="loading"
          :loading="regenerating"
          @click="regenerate"
        >重新生成</el-button>
      </div>
      <p v-if="regenerationError" role="alert">
        {{ regenerationError }}
        <el-button link type="primary" @click="regenerate">重试</el-button>
      </p>
    </div>

    <section v-if="loading" class="panel detail-loading" aria-busy="true" role="status" aria-live="polite">
      <span>正在加载面试题…</span>
      <el-skeleton :rows="10" animated />
    </section>

    <section v-else-if="errorText" class="panel state-panel" role="alert">
      <el-result icon="error" title="面试题加载失败" :sub-title="errorText">
        <template #extra>
          <el-button type="primary" @click="loadDetail">重试</el-button>
          <el-button @click="router.push('/interview-questions')">返回列表</el-button>
        </template>
      </el-result>
    </section>

    <template v-else-if="detail">
      <section class="practice-shell">
        <nav class="question-nav" aria-label="题目分类">
          <span class="question-nav__label">按分类练习</span>
          <button
            v-for="group in groupedQuestions"
            :key="group.type"
            type="button"
            :class="{ active: activeType === group.type }"
            :aria-current="activeType === group.type ? 'page' : undefined"
            @click="activeType = group.type"
          >
            <span>{{ questionTypeLabel(group.type) }}</span>
            <strong>{{ group.items.length }}</strong>
          </button>
        </nav>

        <main class="question-stage">
          <AppEmpty
            v-if="questionTotal === 0"
            title="这份报告暂无题目"
            description="返回准备队列选择其他题单，或重新生成一套面试题。"
          >
            <el-button type="primary" :loading="regenerating" @click="regenerate">重新生成</el-button>
          </AppEmpty>

          <template v-else>
            <header class="stage-header">
              <div>
                <span class="eyebrow">当前分类</span>
                <h2>{{ questionTypeLabel(activeType) }}</h2>
                <p>{{ activeQuestions.length }} 道题，建议先用 1-2 分钟口述，再展开答案。</p>
              </div>
              <el-button @click="toggleCurrentAnswers">
                {{ areCurrentAnswersVisible ? '收起本组答案' : '展开本组答案' }}
              </el-button>
            </header>

            <article v-for="item in activeQuestions" :key="item.questionId" class="question-card">
              <section class="question-section" data-study-section="question" :aria-labelledby="`question-${item.questionId}`">
                <div class="question-card-head">
                  <div>
                    <span class="question-index">Q{{ item.sortOrder || item.questionId }}</span>
                    <h3 :id="`question-${item.questionId}`">{{ item.question }}</h3>
                  </div>
                  <div class="question-actions">
                    <el-tag :type="difficultyTagType(item.difficulty)" effect="plain">
                      {{ difficultyLabel(item.difficulty) }}
                    </el-tag>
                    <el-button
                      link
                      type="primary"
                      :aria-expanded="isAnswerVisible(item.questionId)"
                      :aria-controls="`answer-${item.questionId}`"
                      @click.stop="toggleAnswer(item.questionId)"
                    >
                      {{ isAnswerVisible(item.questionId) ? '隐藏答案' : '查看答案' }}
                    </el-button>
                  </div>
                </div>
              </section>

              <div v-if="!isAnswerVisible(item.questionId)" class="practice-hint">
                先说清结论、依据和项目例子，再展开答案检查是否遗漏关键点。
              </div>

              <template v-else>
                <section
                  :id="`answer-${item.questionId}`"
                  class="study-section answer-section"
                  data-study-section="answer"
                  :aria-labelledby="`answer-title-${item.questionId}`"
                >
                  <div class="section-title">
                    <h4 :id="`answer-title-${item.questionId}`">参考答案</h4>
                    <el-button link type="primary" @click="copyAnswer(item)">复制答案</el-button>
                  </div>
                  <p class="answer-copy">{{ item.answer || '暂无参考答案' }}</p>
                </section>

                <section
                  class="study-section key-points-section"
                  data-study-section="key-points"
                  :aria-labelledby="`points-title-${item.questionId}`"
                >
                  <h4 :id="`points-title-${item.questionId}`">关键要点</h4>
                  <div class="point-columns">
                    <div>
                      <strong>回答结构</strong>
                      <ul v-if="item.answerPoints?.length">
                        <li v-for="point in item.answerPoints" :key="point">{{ point }}</li>
                      </ul>
                      <p v-else class="empty-text">暂无回答要点，可从结论、依据和案例三个层次组织。</p>
                    </div>
                    <div>
                      <strong>相关技能</strong>
                      <div class="tag-row">
                        <el-tag v-for="skill in item.relatedSkills || []" :key="skill" type="success" effect="plain">
                          {{ skill }}
                        </el-tag>
                        <span v-if="!item.relatedSkills?.length" class="empty-text">暂无技能标签</span>
                      </div>
                    </div>
                  </div>
                  <div v-if="item.keywords?.length" class="keyword-block">
                    <strong>表达关键词</strong>
                    <div class="tag-row keyword-list">
                      <el-tag v-for="keyword in item.keywords" :key="keyword" type="info" effect="plain">
                        {{ keyword }}
                      </el-tag>
                    </div>
                  </div>
                  <p v-if="item.source" class="source-copy"><strong>生成依据：</strong>{{ item.source }}</p>
                </section>

                <section
                  class="study-section follow-up-section"
                  data-study-section="follow-ups"
                  :aria-labelledby="`follow-ups-title-${item.questionId}`"
                >
                  <h4 :id="`follow-ups-title-${item.questionId}`">可能追问</h4>
                  <ol v-if="item.followUps?.length">
                    <li v-for="(followUp, index) in item.followUps" :key="index">{{ followUp }}</li>
                  </ol>
                  <p v-else class="follow-up-empty">
                    暂无追问建议。尝试说明一个真实项目案例，并准备回答你的取舍、结果和复盘。
                  </p>
                </section>
              </template>
            </article>
          </template>
        </main>
      </section>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import PageHero from '@/components/common/PageHero.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import { getInterviewQuestionDetailApi, regenerateInterviewQuestionsApi } from '@/api/interviewQuestion'
import { formatDateTime } from '@/utils/format'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'
import {
  findActiveInterviewGeneration,
  registerInterviewGeneration,
  type ActiveInterviewGeneration,
  type InterviewGenerationOutcome
} from './interviewGenerationRegistry'

const route = useRoute()
const router = useRouter()
const aiTaskCenter = useAiTaskCenterStore()
const detail = ref<any>(null)
const loading = ref(false)
const errorText = ref('')
const regenerationError = ref('')
const activeType = ref('')
const visibleAnswerIds = ref<Set<number>>(new Set())
const regenerating = ref(false)
let componentActive = true
let regenerationObservation: Promise<void> | null = null

const groupedQuestions = computed(() => {
  const map = new Map<string, any[]>()
  for (const item of detail.value?.questions || []) {
    const type = item.questionType || 'JOB_SKILL'
    if (!map.has(type)) map.set(type, [])
    map.get(type)!.push(item)
  }
  return Array.from(map.entries()).map(([type, items]) => ({
    type,
    items: items.sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0))
  }))
})

const questionTotal = computed(() => detail.value?.questions?.length || 0)
const answeredCount = computed(() => visibleAnswerIds.value.size)
const activeQuestions = computed(() => groupedQuestions.value.find((group) => group.type === activeType.value)?.items || [])
const areCurrentAnswersVisible = computed(() =>
  activeQuestions.value.length > 0 && activeQuestions.value.every((item) => visibleAnswerIds.value.has(item.questionId))
)

watch(groupedQuestions, (groups) => {
  if (!groups.some((group) => group.type === activeType.value)) activeType.value = groups[0]?.type || ''
})

function questionTypeLabel(type: string) {
  const labels: Record<string, string> = {
    JAVA_BASIC: '技术基础', SPRING_BOOT: 'Spring Boot', SPRING_SECURITY: 'Spring Security',
    MYSQL: 'MySQL', REDIS: 'Redis', PROJECT: '项目经历', ALGORITHM: '算法与数据结构',
    SYSTEM_DESIGN: '系统设计', HR: 'HR / 行为面', RESUME: '简历深挖', JOB_SKILL: '岗位技能'
  }
  return labels[type] || type
}

function difficultyLabel(difficulty: string) {
  const labels: Record<string, string> = { EASY: '简单', MEDIUM: '中等', HARD: '较难' }
  return labels[difficulty] || difficulty || '未标注'
}

function difficultyTagType(difficulty: string) {
  if (difficulty === 'EASY') return 'success'
  if (difficulty === 'HARD') return 'danger'
  return 'warning'
}

function isAnswerVisible(questionId: number) { return visibleAnswerIds.value.has(questionId) }
function toggleAnswer(questionId: number) {
  const next = new Set(visibleAnswerIds.value)
  if (next.has(questionId)) next.delete(questionId)
  else next.add(questionId)
  visibleAnswerIds.value = next
}

function toggleCurrentAnswers() {
  const next = new Set(visibleAnswerIds.value)
  if (areCurrentAnswersVisible.value) activeQuestions.value.forEach((item) => next.delete(item.questionId))
  else activeQuestions.value.forEach((item) => next.add(item.questionId))
  visibleAnswerIds.value = next
}

async function copyAnswer(item: any) {
  const text = item.answer || ''
  if (!text) {
    ElMessage.warning('当前题目暂无参考答案')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('参考答案已复制')
  } catch {
    ElMessage.error('复制失败，请手动选择文本复制')
  }
}

async function loadDetail() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    errorText.value = '报告 ID 不正确'
    return
  }
  loading.value = true
  errorText.value = ''
  detail.value = null
  visibleAnswerIds.value = new Set()
  try {
    detail.value = await getInterviewQuestionDetailApi(id)
    activeType.value = groupedQuestions.value[0]?.type || ''
  } catch (e: any) {
    errorText.value = e?.message || e?.response?.data?.message || '报告不存在、已被删除，或当前账号没有访问权限。'
  } finally {
    loading.value = false
  }
}

async function regenerate() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id) || regenerating.value) return
  regenerationError.value = ''
  const signature = String(id)
  const existing = findActiveInterviewGeneration('INTERVIEW_REGENERATE', signature)
  if (existing) return observeRegeneration(existing)
  const localTaskId = aiTaskCenter.createTask({
    type: 'INTERVIEW_REGENERATE', title: '面试题重新生成', message: '正在重新生成面试题...',
    reportId: id, sourcePath: `/interview-questions/${id}`
  })
  const entry = registerInterviewGeneration({
    kind: 'INTERVIEW_REGENERATE',
    signature,
    localTaskId,
    reportId: id,
    run: async () => {
      try {
        const res: any = await regenerateInterviewQuestionsApi(id)
        const reportId = Number(res?.reportId || id)
        aiTaskCenter.completeTask(localTaskId, {
          resultId: reportId,
          resultPath: `/interview-questions/${reportId}`,
          message: '面试题重新生成完成'
        })
        return { ok: true, reportId }
      } catch (error) {
        aiTaskCenter.failTask(localTaskId, '面试题重新生成失败')
        return { ok: false, error }
      }
    }
  })
  return observeRegeneration(entry)
}

function observeRegeneration(entry: ActiveInterviewGeneration): Promise<void> {
  if (regenerationObservation) return regenerationObservation
  regenerating.value = true
  regenerationError.value = ''
  regenerationObservation = entry.promise.then(async (outcome) => {
    if (!componentActive) return
    await finishRegeneration(outcome)
  }).finally(() => {
    regenerationObservation = null
    if (componentActive) regenerating.value = false
  })
  return regenerationObservation
}

async function finishRegeneration(outcome: InterviewGenerationOutcome) {
  if (!outcome.ok) {
    const error: any = outcome.error
    regenerationError.value = error?.message || error?.response?.data?.message || '重新生成失败，请稍后重试'
    ElMessage.error(regenerationError.value)
    return
  }
  ElMessage.success('面试题重新生成成功')
  const currentId = Number(route.params.id)
  if (outcome.reportId !== currentId) {
    await router.replace(`/interview-questions/${outcome.reportId}`)
    if (!componentActive) return
  }
  await loadDetail()
}

function attachActiveRegeneration() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) return
  const active = findActiveInterviewGeneration('INTERVIEW_REGENERATE', String(id))
  if (active) void observeRegeneration(active)
}

onMounted(() => {
  componentActive = true
  void loadDetail()
  attachActiveRegeneration()
})
onBeforeUnmount(() => {
  componentActive = false
})
</script>

<style scoped>
.hero-context { display: flex; flex-wrap: wrap; gap: var(--space-2) var(--space-4); color: var(--color-text-muted); font-size: 14px; }
.hero-context strong { color: var(--color-primary-hover); }
.study-actions {
  position: sticky; top: 76px; z-index: 5; display: flex; align-items: center; justify-content: space-between;
  gap: var(--space-3); margin-bottom: var(--space-4); padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border); border-radius: var(--radius-md); background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  box-shadow: var(--shadow-card); backdrop-filter: blur(12px);
}
.study-actions > div { display: flex; gap: var(--space-2); }
.study-actions p { margin: 0; color: var(--color-danger); font-size: 13px; }
.state-panel, .detail-loading { min-height: 360px; }
.detail-loading > span { display: block; margin-bottom: var(--space-4); color: var(--color-text-muted); }
.practice-shell { display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: var(--space-5); align-items: start; }
.question-nav {
  position: sticky; top: 148px; display: grid; gap: var(--space-2); padding: var(--space-3);
  border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface); box-shadow: var(--shadow-card);
}
.question-nav__label { padding: var(--space-2); color: var(--color-text-muted); font-size: 12px; font-weight: 700; letter-spacing: 0.06em; }
.question-nav button {
  display: flex; align-items: center; justify-content: space-between; width: 100%; padding: var(--space-3);
  border: 1px solid transparent; border-radius: var(--radius-sm); background: transparent; color: var(--color-text-muted);
  cursor: pointer; font: inherit; text-align: left;
}
.question-nav button:hover { background: var(--color-surface-muted); color: var(--color-text); }
.question-nav button.active { border-color: var(--color-primary-border); background: var(--color-primary-soft); color: var(--color-primary-hover); }
.question-nav button:focus-visible, .question-actions .el-button:focus-visible { outline: 3px solid color-mix(in srgb, var(--color-primary) 34%, transparent); outline-offset: 2px; }
.question-stage { display: grid; gap: var(--space-4); min-width: 0; }
.stage-header, .question-card { border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-surface); box-shadow: var(--shadow-card); }
.stage-header { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); padding: var(--space-4) var(--space-5); }
.eyebrow { color: var(--color-primary); font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; }
.stage-header h2 { margin: var(--space-1) 0; color: var(--color-text); font-size: 20px; }
.stage-header p { margin: 0; color: var(--color-text-muted); font-size: 13px; line-height: 1.6; }
.question-card { padding: var(--space-5); overflow: hidden; }
.question-card-head { display: flex; gap: var(--space-4); align-items: flex-start; justify-content: space-between; }
.question-index { color: var(--color-primary); font-size: 13px; font-weight: 700; }
.question-card h3 { max-width: 68ch; margin: var(--space-2) 0 0; color: var(--color-text); font-size: 19px; line-height: 1.65; overflow-wrap: anywhere; }
.question-actions { display: flex; flex-shrink: 0; gap: var(--space-2); align-items: center; }
.practice-hint { margin-top: var(--space-4); padding: var(--space-3) var(--space-4); border-left: 3px solid var(--color-warning); background: var(--color-surface-muted); color: var(--color-text-muted); line-height: 1.7; }
.study-section { margin-top: var(--space-4); padding: var(--space-4); border-radius: var(--radius-md); }
.study-section h4 { margin: 0 0 var(--space-3); color: var(--color-text); font-size: 16px; }
.answer-section { border: 1px solid var(--color-primary-border); background: color-mix(in srgb, var(--color-primary-soft) 48%, var(--color-surface)); }
.section-title { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); }
.section-title h4 { margin: 0; }
.answer-copy { max-width: 68ch; margin: var(--space-3) 0 0; color: var(--color-text); font-size: 15px; line-height: 1.9; white-space: pre-wrap; overflow-wrap: anywhere; }
.key-points-section { border: 1px solid var(--color-border); background: var(--color-surface-muted); }
.point-columns { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-4); }
.point-columns > div { min-width: 0; }
.point-columns strong, .keyword-block > strong { display: block; margin-bottom: var(--space-2); color: var(--color-text); font-size: 13px; }
.point-columns ul { margin: 0; padding-left: var(--space-5); color: var(--color-text-muted); line-height: 1.8; }
.tag-row { display: flex; flex-wrap: wrap; gap: var(--space-2); min-width: 0; }
.keyword-block { margin-top: var(--space-4); }
.keyword-list .el-tag { max-width: 100%; }
.source-copy { margin: var(--space-4) 0 0; color: var(--color-text-muted); line-height: 1.7; overflow-wrap: anywhere; }
.follow-up-section { border: 1px solid color-mix(in srgb, var(--color-success) 24%, var(--color-border)); background: color-mix(in srgb, var(--color-success) 6%, var(--color-surface)); }
.follow-up-section ol { margin: 0; padding-left: var(--space-5); color: var(--color-text-muted); line-height: 1.85; }
.follow-up-empty, .empty-text { margin: 0; color: var(--color-text-muted); line-height: 1.7; }
.empty-text { font-size: 13px; }
@media (max-width: 900px) {
  .study-actions, .question-nav { position: static; }
  .practice-shell, .point-columns { grid-template-columns: 1fr; }
  .question-nav { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .question-nav__label { grid-column: 1 / -1; }
}
@media (max-width: 600px) {
  .study-actions, .stage-header, .question-card-head { align-items: stretch; flex-direction: column; }
  .study-actions > div, .question-actions { display: grid; grid-template-columns: 1fr; }
  .study-actions .el-button, .question-actions .el-button { width: 100%; margin-left: 0; }
  .question-nav { grid-template-columns: 1fr; }
  .question-card { padding: var(--space-4); }
  .study-section { padding: var(--space-3); }
}
</style>
