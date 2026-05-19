<template>
  <PageContainer title="面试题详情" :description="detail?.title || '先自测，再查看参考答案、考察点和追问。'">
    <template #actions>
      <el-button @click="router.push('/interview-questions')">返回列表</el-button>
      <el-button type="primary" :loading="regenerating" @click="regenerate">重新生成</el-button>
    </template>

    <section v-if="loading" class="panel">
      <el-skeleton :rows="10" animated />
    </section>

    <section v-else-if="errorText" class="panel state-panel">
      <el-result icon="error" title="面试题加载失败" :sub-title="errorText">
        <template #extra>
          <el-button type="primary" @click="loadDetail">重试</el-button>
          <el-button @click="router.push('/interview-questions')">返回列表</el-button>
        </template>
      </el-result>
    </section>

    <template v-else-if="detail">
      <section class="detail-hero">
        <div>
          <span class="eyebrow">AI 面试题练习</span>
          <h2>{{ detail.title }}</h2>
          <div class="hero-meta">
            <span>{{ detail.companyName || '未知公司' }}</span>
            <span>{{ detail.jobTitle || '未知岗位' }}</span>
            <span>{{ detail.questionCount || 0 }} 道题</span>
            <span>{{ formatDateTime(detail.createdAt) }}</span>
          </div>
        </div>
        <div class="hero-stat">
          <strong>{{ answeredCount }}/{{ questionTotal }}</strong>
          <span>已展开答案</span>
        </div>
      </section>

      <section class="practice-shell">
        <aside class="question-nav">
          <button
            v-for="group in groupedQuestions"
            :key="group.type"
            type="button"
            :class="{ active: activeType === group.type }"
            @click="activeType = group.type"
          >
            <span>{{ questionTypeLabel(group.type) }}</span>
            <strong>{{ group.items.length }}</strong>
          </button>
        </aside>

        <main class="question-stage">
          <AppEmpty
            v-if="questionTotal === 0"
            title="这份报告暂无题目"
            description="可以返回列表重新生成一套面试题。"
          >
            <el-button type="primary" :loading="regenerating" @click="regenerate">重新生成</el-button>
          </AppEmpty>

          <template v-else>
            <div class="stage-header">
              <div>
                <h3>{{ questionTypeLabel(activeType) }}</h3>
                <span>{{ activeQuestions.length }} 道题，建议先口述再查看答案。</span>
              </div>
              <el-button @click="toggleCurrentAnswers">
                {{ areCurrentAnswersVisible ? '收起本组答案' : '展开本组答案' }}
              </el-button>
            </div>

            <article v-for="item in activeQuestions" :key="item.questionId" class="question-card">
              <div class="question-card-head">
                <div>
                  <span class="question-index">Q{{ item.sortOrder || item.questionId }}</span>
                  <h4>{{ item.question }}</h4>
                </div>
                <div class="question-actions">
                  <el-tag :type="difficultyTagType(item.difficulty)" effect="plain">
                    {{ difficultyLabel(item.difficulty) }}
                  </el-tag>
                  <el-button link type="primary" @click.stop="toggleAnswer(item.questionId)">
                    {{ isAnswerVisible(item.questionId) ? '隐藏答案' : '查看答案' }}
                  </el-button>
                </div>
              </div>

              <div v-if="!isAnswerVisible(item.questionId)" class="practice-hint">
                先尝试用 1-2 分钟口述作答，再展开参考答案对照表达结构。
              </div>

              <template v-else>
                <section class="answer-block">
                  <div class="block-title">
                    <strong>参考答案</strong>
                    <el-button link type="primary" @click="copyAnswer(item)">复制</el-button>
                  </div>
                  <p>{{ item.answer || '暂无参考答案' }}</p>
                </section>

                <section class="insight-grid">
                  <div class="insight-block">
                    <strong>考察点</strong>
                    <div class="tag-row">
                      <el-tag v-for="point in item.answerPoints || []" :key="point" type="success" effect="plain">
                        {{ point }}
                      </el-tag>
                      <span v-if="!item.answerPoints?.length" class="empty-text">暂无要点</span>
                    </div>
                  </div>

                  <div class="insight-block">
                    <strong>相关技能</strong>
                    <div class="tag-row">
                      <el-tag v-for="skill in item.relatedSkills || []" :key="skill" type="primary" effect="plain">
                        {{ skill }}
                      </el-tag>
                      <span v-if="!item.relatedSkills?.length" class="empty-text">暂无技能标签</span>
                    </div>
                  </div>
                </section>

                <section v-if="item.followUps?.length" class="coach-block">
                  <strong>可能追问</strong>
                  <ul>
                    <li v-for="(fu, idx) in item.followUps" :key="idx">{{ fu }}</li>
                  </ul>
                </section>

                <section v-if="item.keywords?.length || item.source" class="source-block">
                  <div v-if="item.keywords?.length" class="tag-row">
                    <el-tag v-for="kw in item.keywords" :key="kw" type="info" effect="plain">
                      {{ kw }}
                    </el-tag>
                  </div>
                  <p v-if="item.source">{{ item.source }}</p>
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import { getInterviewQuestionDetailApi, regenerateInterviewQuestionsApi } from '@/api/interviewQuestion'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const detail = ref<any>(null)
const loading = ref(false)
const errorText = ref('')
const activeType = ref('')
const visibleAnswerIds = ref<Set<number>>(new Set())
const regenerating = ref(false)

const groupedQuestions = computed(() => {
  const map = new Map<string, any[]>()
  for (const item of detail.value?.questions || []) {
    const type = item.questionType || 'JOB_SKILL'
    if (!map.has(type)) {
      map.set(type, [])
    }
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
  if (!groups.some((group) => group.type === activeType.value)) {
    activeType.value = groups[0]?.type || ''
  }
})

function questionTypeLabel(type: string) {
  const labels: Record<string, string> = {
    JAVA_BASIC: '技术基础',
    SPRING_BOOT: 'Spring Boot',
    SPRING_SECURITY: 'Spring Security',
    MYSQL: 'MySQL',
    REDIS: 'Redis',
    PROJECT: '项目经历',
    ALGORITHM: '算法与数据结构',
    SYSTEM_DESIGN: '系统设计',
    HR: 'HR / 行为面',
    RESUME: '简历深挖',
    JOB_SKILL: '岗位技能'
  }
  return labels[type] || type
}

function difficultyLabel(difficulty: string) {
  const labels: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '较难'
  }
  return labels[difficulty] || difficulty || '未标注'
}

function difficultyTagType(difficulty: string) {
  if (difficulty === 'EASY') return 'success'
  if (difficulty === 'HARD') return 'danger'
  return 'warning'
}

function isAnswerVisible(questionId: number) {
  return visibleAnswerIds.value.has(questionId)
}

function toggleAnswer(questionId: number) {
  const next = new Set(visibleAnswerIds.value)
  if (next.has(questionId)) {
    next.delete(questionId)
  } else {
    next.add(questionId)
  }
  visibleAnswerIds.value = next
}

function toggleCurrentAnswers() {
  const next = new Set(visibleAnswerIds.value)
  if (areCurrentAnswersVisible.value) {
    activeQuestions.value.forEach((item) => next.delete(item.questionId))
  } else {
    activeQuestions.value.forEach((item) => next.add(item.questionId))
  }
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

onMounted(loadDetail)

async function regenerate() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) return

  regenerating.value = true
  try {
    const res: any = await regenerateInterviewQuestionsApi(id)
    ElMessage.success('面试题重新生成成功')
    if (res?.reportId && res.reportId !== id) {
      await router.replace(`/interview-questions/${res.reportId}`)
    }
    await loadDetail()
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.message || '重新生成失败，请稍后重试')
  } finally {
    regenerating.value = false
  }
}
</script>

<style scoped>
.state-panel {
  min-height: 360px;
}

.detail-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
  padding: 22px 24px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.eyebrow {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
}

.detail-hero h2 {
  margin: 6px 0 10px;
  font-size: 24px;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--color-text-soft);
  font-size: 13px;
}

.hero-stat {
  display: grid;
  min-width: 128px;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #eff6ff;
  color: #1d4ed8;
  text-align: right;
}

.hero-stat strong {
  font-size: 22px;
}

.hero-stat span {
  font-size: 12px;
}

.practice-shell {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.question-nav {
  position: sticky;
  top: 96px;
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.question-nav button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.question-nav button.active {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.question-stage {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.stage-header,
.question-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.stage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
}

.stage-header h3 {
  margin: 0 0 4px;
  font-size: 18px;
}

.stage-header span {
  color: var(--color-text-soft);
  font-size: 13px;
}

.question-card {
  padding: 18px;
}

.question-card-head {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  justify-content: space-between;
}

.question-index {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
}

.question-card h4 {
  margin: 6px 0 0;
  color: var(--color-text);
  font-size: 17px;
  line-height: 1.7;
}

.question-actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
  align-items: center;
}

.practice-hint {
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid #fde68a;
  border-radius: 8px;
  background: #fffbeb;
  color: #92400e;
  line-height: 1.6;
}

.answer-block,
.coach-block,
.source-block {
  margin-top: 14px;
  padding: 14px;
  border-radius: 8px;
}

.answer-block {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
}

.block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.answer-block p,
.source-block p {
  margin: 8px 0 0;
  color: var(--color-text-muted);
  line-height: 1.8;
  white-space: pre-wrap;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.insight-block {
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #f8fafc;
}

.insight-block strong,
.coach-block strong {
  display: block;
  margin-bottom: 10px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.coach-block {
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
}

.coach-block ul {
  margin: 0;
  padding-left: 20px;
  color: var(--color-text-muted);
  line-height: 1.8;
}

.source-block {
  border: 1px solid var(--color-border);
  background: #f8fafc;
}

.empty-text {
  color: var(--color-text-soft);
  font-size: 13px;
}

@media (max-width: 900px) {
  .detail-hero,
  .stage-header,
  .question-card-head {
    flex-direction: column;
  }

  .hero-stat {
    width: 100%;
    text-align: left;
  }

  .practice-shell,
  .insight-grid {
    grid-template-columns: 1fr;
  }

  .question-nav {
    position: static;
  }
}
</style>
