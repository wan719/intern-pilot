<template>
  <PageContainer title="练好下一场面试" width="wide">
    <template #hero>
      <PageHero
        eyebrow="求职旅程 · 面试准备"
        title="练好下一场面试"
        description="用目标岗位的真实语境反复演练：先独立作答，再对照参考答案、关键要点和追问。"
      >
        <template #actions>
          <el-button type="primary" :icon="Plus" :disabled="!canGenerate" @click="openGenerate">
            生成面试题
          </el-button>
        </template>
      </PageHero>
    </template>

    <el-alert
      v-if="!canGenerate && !loadingOptions"
      class="guide-alert"
      type="info"
      show-icon
      :closable="false"
      title="生成面试题前，需要先准备至少一份简历和一个岗位 JD。"
    >
      <div class="guide-actions responsive-actions">
        <el-button size="small" type="primary" @click="router.push('/resumes')">去上传简历</el-button>
        <el-button size="small" @click="router.push('/jobs')">去新增岗位</el-button>
      </div>
    </el-alert>

    <div
      v-if="generating"
      data-generation-state
      class="generation-state generation-state--loading"
      role="status"
      aria-live="polite"
    >
      <span class="generation-state__pulse" aria-hidden="true" />
      <div>
        <strong>正在生成面试题</strong>
        <p>AI 正在结合简历与岗位上下文组织题目，完成后会自动进入详情页。</p>
      </div>
    </div>

    <div v-else-if="generationError" data-generation-error class="generation-state generation-state--error" role="alert">
      <div>
        <strong>面试题生成失败</strong>
        <p>{{ generationError }}</p>
      </div>
      <el-button data-generation-retry type="primary" plain @click="generate">重试生成</el-button>
    </div>

    <div class="interview-summary-grid" :aria-busy="loading">
      <StatCard label="准备题单" :value="reportStats.total" :icon="Files" :loading="loading" />
      <StatCard label="练习题目" :value="reportStats.questionTotal" :icon="Tickets" :loading="loading" />
      <StatCard label="目标岗位" :value="reportStats.jobCount" :icon="Briefcase" :loading="loading" />
      <StatCard label="最近更新" :value="reportStats.latestText" :icon="Clock" :loading="loading" />
    </div>

    <FilterBar @reset="resetQuery">
      <template #filters>
        <el-select v-model="query.resumeId" aria-label="按简历筛选" placeholder="全部简历" clearable filterable>
          <el-option
            v-for="item in resumes"
            :key="item.resumeId"
            :label="item.resumeName || item.originalFileName"
            :value="item.resumeId"
          />
        </el-select>
        <el-select v-model="query.jobId" aria-label="按岗位筛选" placeholder="全部岗位" clearable filterable>
          <el-option
            v-for="item in jobs"
            :key="item.jobId"
            :label="`${item.companyName} - ${item.jobTitle}`"
            :value="item.jobId"
          />
        </el-select>
        <el-select v-model="filters.category" aria-label="按题目分类筛选" placeholder="全部分类" clearable filterable>
          <el-option v-for="item in detailCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.difficulty" aria-label="按难度筛选" placeholder="全部难度" clearable>
          <el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.status" aria-label="按准备状态筛选" placeholder="全部状态" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </template>
      <template #actions>
        <el-button type="primary" @click="search">应用筛选</el-button>
      </template>
    </FilterBar>

    <section class="queue-shell" aria-label="面试准备队列" :aria-busy="loading">
      <div v-if="loading" class="queue-loading" role="status" aria-live="polite">
        <span>正在加载面试准备队列…</span>
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="loadError" class="queue-error" role="alert">
        <div>
          <strong>准备队列暂时无法加载</strong>
          <p>{{ loadError }}</p>
        </div>
        <el-button data-list-retry type="primary" @click="loadReports">重新加载</el-button>
      </div>

      <AppEmpty
        v-else-if="!reports.length"
        title="还没有面试准备内容"
        description="选择简历和目标岗位，生成一套可以反复演练的专属面试题。"
        hint="建议先完善岗位 JD 和简历解析结果，问题会更贴近真实面试。"
      >
        <el-button data-empty-generate type="primary" :icon="Plus" :disabled="!canGenerate" @click="openGenerate">
          生成第一套面试题
        </el-button>
      </AppEmpty>

      <AppEmpty
        v-else-if="!filteredReports.length"
        title="没有符合当前筛选的面试题"
        description="换一个分类、难度或准备状态，继续查看当前页的题单。"
      >
        <el-button type="primary" plain @click="clearClientFilters">清除题目筛选</el-button>
      </AppEmpty>

      <div v-else class="preparation-queue">
        <article
          v-for="item in filteredReports"
          :key="item.reportId"
          class="preparation-card"
          :data-report-id="item.reportId"
        >
          <div class="preparation-card__body">
            <div class="preparation-card__heading">
              <div class="role-context">
                <span class="eyebrow">目标岗位</span>
                <p :title="`${item.companyName || '未知公司'} · ${item.jobTitle || '未知岗位'}`">
                  {{ item.companyName || '未知公司' }}
                  <span aria-hidden="true">·</span>
                  {{ item.jobTitle || '未知岗位' }}
                </p>
                <h2>{{ item.title || reportTitle(item) }}</h2>
              </div>
              <StatusTag :status="reportStatus(item)" :label="reportStatusLabel(item)" />
            </div>

            <dl class="preparation-facts">
              <div><dt>题量</dt><dd>{{ item.questionCount || 0 }} 题</dd></div>
              <div><dt>生成时间</dt><dd>{{ formatDateTime(item.createdAt) }}</dd></div>
              <div><dt>分类</dt><dd>{{ taxonomySummary(item, 'categories') }}</dd></div>
              <div><dt>难度</dt><dd>{{ taxonomySummary(item, 'difficulties') }}</dd></div>
            </dl>

            <div class="taxonomy-tags" aria-label="题目分类与难度">
              <el-tag v-for="category in reportMetadataFor(item).categories" :key="category" effect="plain">
                {{ questionTypeLabel(category) }}
              </el-tag>
              <el-tag
                v-for="difficulty in reportMetadataFor(item).difficulties"
                :key="difficulty"
                :type="difficultyTagType(difficulty)"
                effect="plain"
              >
                {{ difficultyLabel(difficulty) }}
              </el-tag>
            </div>
          </div>

          <div class="preparation-card__actions">
            <el-button class="primary-study-action" type="primary" @click="goDetail(item.reportId)">开始练习</el-button>
            <el-button :loading="regeneratingId === item.reportId" @click="regenerateReport(item)">重新生成</el-button>
            <el-button
              v-if="authStore.hasPermission('analysis:delete')"
              type="danger"
              plain
              :loading="deletingId === item.reportId"
              @click="removeReport(item)"
            >删除</el-button>
          </div>
        </article>
      </div>

      <el-pagination
        v-if="!loadError && total > 0"
        class="pager"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </section>

    <el-dialog v-model="generateVisible" title="生成 AI 面试题" :width="generateDialogWidth">
      <el-alert
        v-if="!canGenerate"
        class="dialog-alert"
        type="warning"
        :closable="false"
        title="当前还不能生成面试题，请先补齐简历和岗位。"
      />
      <el-form :model="form" label-position="top">
        <el-form-item label="简历">
          <el-select v-model="form.resumeId" placeholder="请选择简历" filterable>
            <el-option v-for="item in resumes" :key="item.resumeId" :label="item.resumeName || item.originalFileName" :value="item.resumeId" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.jobId" placeholder="请选择岗位" filterable>
            <el-option v-for="item in jobs" :key="item.jobId" :label="`${item.companyName} - ${item.jobTitle}`" :value="item.jobId" />
          </el-select>
        </el-form-item>
        <el-form-item label="简历版本">
          <el-select v-model="form.resumeVersionId" placeholder="默认使用当前版本" clearable filterable :disabled="!form.resumeId">
            <el-option v-for="item in versions" :key="item.versionId" :label="`${item.versionName}${item.isCurrent === 1 ? '（当前）' : ''}`" :value="item.versionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="AI 匹配分析报告">
          <el-select
            v-model="form.analysisReportId"
            placeholder="可选，选择同一简历和岗位的分析报告"
            clearable
            filterable
            :disabled="!form.resumeId || !form.jobId"
          >
            <el-option
              v-for="item in filteredAnalysisReports"
              :key="item.reportId"
              :label="`${item.companyName || '未知公司'} - ${item.jobTitle || '未知岗位'} - ${item.matchScore || 0}分`"
              :value="item.reportId"
            />
          </el-select>
          <p class="field-hint">只展示与当前简历和岗位匹配的分析报告；不选也可以直接根据简历和 JD 生成。</p>
        </el-form-item>
        <el-form-item label="题目数量">
          <el-input-number v-model="form.questionCount" :min="3" :max="20" />
          <p class="field-hint">默认 8 道题，范围 3-20。</p>
        </el-form-item>
        <el-form-item label="题目分类">
          <el-select v-model="form.categories" placeholder="不选则生成所有类型" multiple filterable>
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulties" placeholder="不选则混合难度" multiple filterable>
            <el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <div class="switch-row">
          <el-form-item label="生成参考答案"><el-switch v-model="form.includeAnswer" /></el-form-item>
          <el-form-item label="生成追问问题"><el-switch v-model="form.includeFollowUps" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!canGenerate" :loading="generating" @click="generate">生成</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Briefcase, Clock, Files, Plus, Tickets } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import PageHero from '@/components/common/PageHero.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import router from '@/router'
import { getAnalysisReportsApi } from '@/api/analysis'
import {
  deleteInterviewQuestionReportApi,
  generateInterviewQuestionsApi,
  getInterviewQuestionDetailApi,
  getInterviewQuestionReportsApi,
  regenerateInterviewQuestionsApi
} from '@/api/interviewQuestion'
import { getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'
import { getResumeVersionListApi } from '@/api/resumeVersion'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'
import { useAuthStore } from '@/stores/auth'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'

type ReportTaxonomy = { categories: string[]; difficulties: string[]; status: string; available: boolean }

const aiTaskCenter = useAiTaskCenterStore()
const reports = ref<any[]>([])
const resumes = ref<any[]>([])
const jobs = ref<any[]>([])
const versions = ref<any[]>([])
const analysisReports = ref<any[]>([])
const reportMetadata = ref<Record<number, ReportTaxonomy>>({})
const loading = ref(false)
const loadingOptions = ref(false)
const generating = ref(false)
const generateVisible = ref(false)
const generationError = ref('')
const { responsiveDialogWidth } = useResponsiveSize()
const generateDialogWidth = responsiveDialogWidth('560px')
const loadError = ref('')
const total = ref(0)
const regeneratingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)
const authStore = useAuthStore()

const query = reactive<{ resumeId?: number; resumeVersionId?: number; jobId?: number; pageNum: number; pageSize: number }>({ pageNum: 1, pageSize: 10 })
const filters = reactive<{ category?: string; difficulty?: string; status?: string }>({})
const form = reactive<{
  resumeId?: number
  resumeVersionId?: number
  jobId?: number
  analysisReportId?: number
  questionCount?: number
  categories?: string[]
  difficulties?: string[]
  includeAnswer: boolean
  includeFollowUps: boolean
}>({ questionCount: 8, includeAnswer: true, includeFollowUps: true })

const categoryOptions = [
  { label: 'Java 基础', value: 'JAVA_BASIC' },
  { label: 'Spring Boot', value: 'SPRING_BOOT' },
  { label: 'Spring Security', value: 'SPRING_SECURITY' },
  { label: 'MySQL', value: 'MYSQL' },
  { label: 'Redis', value: 'REDIS' },
  { label: '项目追问', value: 'PROJECT' },
  { label: 'HR 面试', value: 'HR' },
  { label: '简历深挖', value: 'RESUME' },
  { label: '岗位技能专项', value: 'JOB_SKILL' }
]
const detailCategoryOptions = [
  { label: '技术基础', value: 'JAVA_BASIC' },
  { label: 'Spring Boot', value: 'SPRING_BOOT' },
  { label: 'Spring Security', value: 'SPRING_SECURITY' },
  { label: 'MySQL', value: 'MYSQL' },
  { label: 'Redis', value: 'REDIS' },
  { label: '项目经历', value: 'PROJECT' },
  { label: 'HR / 行为面', value: 'HR' },
  { label: '简历深挖', value: 'RESUME' },
  { label: '岗位技能', value: 'JOB_SKILL' },
  { label: '算法与数据结构', value: 'ALGORITHM' },
  { label: '系统设计', value: 'SYSTEM_DESIGN' }
]
const difficultyOptions = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '较难', value: 'HARD' }
]
const statusOptions = [
  { label: '可开始练习', value: 'COMPLETED' },
  { label: '生成中', value: 'RUNNING' },
  { label: '生成失败', value: 'FAILED' }
]

const canGenerate = computed(() => resumes.value.length > 0 && jobs.value.length > 0)
const reportStats = computed(() => {
  const jobsInReports = new Set(reports.value.map((item) => item.jobId).filter(Boolean))
  const questionTotal = reports.value.reduce((sum, item) => sum + Number(item.questionCount || 0), 0)
  const latest = reports.value[0]?.createdAt
  return { total: total.value || reports.value.length, questionTotal, jobCount: jobsInReports.size, latestText: latest ? formatDateOnly(latest) : '-' }
})
const filteredAnalysisReports = computed(() => {
  if (!form.resumeId || !form.jobId) return []
  return analysisReports.value.filter((item) => item.resumeId === form.resumeId && item.jobId === form.jobId)
})
const filteredReports = computed(() => reports.value.filter((item) => {
  const metadata = reportMetadataFor(item)
  if (filters.category && !metadata.categories.includes(filters.category)) return false
  if (filters.difficulty && !metadata.difficulties.includes(filters.difficulty)) return false
  if (filters.status && reportStatus(item) !== filters.status) return false
  return true
}))

watch(() => [form.resumeId, form.jobId], () => {
  if (!filteredAnalysisReports.value.some((item) => item.reportId === form.analysisReportId)) form.analysisReportId = undefined
})
watch(() => form.resumeId, async () => {
  if (!form.resumeId) {
    versions.value = []
    form.resumeVersionId = undefined
    return
  }
  const res: any = await getResumeVersionListApi(form.resumeId)
  versions.value = res || []
  form.resumeVersionId = versions.value.find((item) => item.isCurrent === 1)?.versionId
})

async function loadOptions() {
  loadingOptions.value = true
  try {
    const [resumeRes, jobRes, analysisRes]: any[] = await Promise.all([
      getResumeListApi({ pageNum: 1, pageSize: 100 }),
      getJobListApi({ pageNum: 1, pageSize: 100 }),
      getAnalysisReportsApi({ pageNum: 1, pageSize: 100 })
    ])
    resumes.value = resumeRes.records || []
    jobs.value = jobRes.records || []
    analysisReports.value = analysisRes.records || []
  } finally {
    loadingOptions.value = false
  }
}

async function loadReports() {
  loading.value = true
  loadError.value = ''
  try {
    const res: any = await getInterviewQuestionReportsApi({ ...query })
    reports.value = res.records || []
    total.value = res.total || 0
    await hydrateReportMetadata(reports.value)
  } catch (e: any) {
    reports.value = []
    reportMetadata.value = {}
    total.value = 0
    loadError.value = getErrorMessage(e, '面试题列表加载失败，请确认数据表已初始化后重试。')
  } finally {
    loading.value = false
  }
}

async function hydrateReportMetadata(items: any[]) {
  const entries = await Promise.all(items.map(async (item): Promise<[number, ReportTaxonomy]> => {
    const reportId = Number(item.reportId)
    try {
      const detail: any = await getInterviewQuestionDetailApi(reportId)
      const questions = Array.isArray(detail?.questions) ? detail.questions : []
      return [reportId, {
        categories: unique(questions.map((question: any) => question.questionType).filter(Boolean)),
        difficulties: unique(questions.map((question: any) => question.difficulty).filter(Boolean)),
        status: reportStatus(item),
        available: true
      }]
    } catch {
      return [reportId, {
        categories: arrayValue(item.categories || item.questionTypes),
        difficulties: arrayValue(item.difficulties),
        status: reportStatus(item),
        available: false
      }]
    }
  }))
  reportMetadata.value = Object.fromEntries(entries)
}

function search() { query.pageNum = 1; loadReports() }
function resetQuery() {
  query.resumeId = undefined
  query.jobId = undefined
  query.pageNum = 1
  clearClientFilters()
  loadReports()
}
function clearClientFilters() {
  filters.category = undefined
  filters.difficulty = undefined
  filters.status = undefined
}
function handlePageChange(page: number) { query.pageNum = page; loadReports() }
function handleSizeChange(size: number) { query.pageSize = size; query.pageNum = 1; loadReports() }
function openGenerate() {
  form.resumeId = resumes.value[0]?.resumeId
  form.jobId = jobs.value[0]?.jobId
  form.questionCount = form.questionCount || 8
  form.analysisReportId = undefined
  generationError.value = ''
  generateVisible.value = true
}

async function generate() {
  if (!form.resumeId || !form.jobId) {
    ElMessage.warning('请选择简历和岗位')
    return
  }
  generating.value = true
  generationError.value = ''
  const localTaskId = aiTaskCenter.createTask({
    type: 'INTERVIEW_QUESTION', title: '面试题生成', message: '正在生成面试题...',
    resumeId: form.resumeId, jobId: form.jobId, reportId: form.analysisReportId
  })
  try {
    const res: any = await generateInterviewQuestionsApi(form)
    aiTaskCenter.completeTask(localTaskId, { resultId: res.reportId, resultPath: `/interview-questions/${res.reportId}`, message: '面试题生成完成' })
    ElMessage.success('面试题生成成功')
    generateVisible.value = false
    query.pageNum = 1
    await loadReports()
    router.push(`/interview-questions/${res.reportId}`)
  } catch (e: any) {
    aiTaskCenter.failTask(localTaskId, '面试题生成失败')
    generateVisible.value = false
    generationError.value = getErrorMessage(e, '面试题生成失败，请检查简历、岗位和 AI 服务配置。')
    ElMessage.error(generationError.value)
  } finally {
    generating.value = false
  }
}

function goDetail(reportId: number) { router.push(`/interview-questions/${reportId}`) }
async function removeReport(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除“${row.title || '面试题报告'}”吗？删除后将无法继续查看这套题。`, '删除确认', { type: 'warning' })
  } catch { return }
  deletingId.value = row.reportId
  try {
    await deleteInterviewQuestionReportApi(row.reportId)
    ElMessage.success('删除成功')
    await loadReports()
  } catch (e: any) {
    ElMessage.error(getErrorMessage(e, '删除失败，请稍后重试'))
  } finally { deletingId.value = null }
}

async function regenerateReport(row: any) {
  try {
    await ElMessageBox.confirm(`确认重新生成“${row.title || '面试题报告'}”吗？旧的题目将被替换。`, '重新生成确认', { type: 'warning' })
  } catch { return }
  const localTaskId = aiTaskCenter.createTask({ type: 'INTERVIEW_REGENERATE', title: '面试题重新生成', message: '正在重新生成面试题...', reportId: row.reportId })
  regeneratingId.value = row.reportId
  try {
    const res: any = await regenerateInterviewQuestionsApi(row.reportId)
    aiTaskCenter.completeTask(localTaskId, { resultId: res.reportId, resultPath: `/interview-questions/${res.reportId}`, message: '面试题重新生成完成' })
    ElMessage.success('面试题重新生成成功')
    await loadReports()
    router.push(`/interview-questions/${res.reportId}`)
  } catch (e: any) {
    aiTaskCenter.failTask(localTaskId, '面试题重新生成失败')
    ElMessage.error(getErrorMessage(e, '重新生成失败，请稍后重试'))
  } finally { regeneratingId.value = null }
}

function reportMetadataFor(item: any): ReportTaxonomy {
  return reportMetadata.value[Number(item.reportId)] || {
    categories: arrayValue(item.categories || item.questionTypes), difficulties: arrayValue(item.difficulties),
    status: reportStatus(item), available: false
  }
}
function reportStatus(item: any) { return typeof item.status === 'string' && item.status ? item.status : 'COMPLETED' }
function reportStatusLabel(item: any) { return statusOptions.find((option) => option.value === reportStatus(item))?.label || reportStatus(item) }
function taxonomySummary(item: any, key: 'categories' | 'difficulties') {
  const metadata = reportMetadataFor(item)
  const labels = metadata[key].map((value) => key === 'categories' ? questionTypeLabel(value) : difficultyLabel(value))
  if (labels.length) return labels.join('、')
  return metadata.available ? '未标注' : '打开后查看'
}
function questionTypeLabel(type: string) { return detailCategoryOptions.find((item) => item.value === type)?.label || type }
function difficultyLabel(difficulty: string) { return difficultyOptions.find((item) => item.value === difficulty)?.label || difficulty || '未标注' }
function difficultyTagType(difficulty: string) {
  if (difficulty === 'EASY') return 'success'
  if (difficulty === 'HARD') return 'danger'
  return 'warning'
}
function reportTitle(item: any) { return `${item.companyName || '目标岗位'} - ${item.jobTitle || '面试题'}` }
function getErrorMessage(error: any, fallback: string) { return error?.message || error?.response?.data?.message || fallback }
function formatDateOnly(value?: string) { return value ? formatDateTime(value).slice(0, 10) : '-' }
function unique(values: string[]) { return Array.from(new Set(values)) }
function arrayValue(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string' && Boolean(item)) : []
}

onMounted(async () => { await Promise.allSettled([loadOptions(), loadReports()]) })
</script>

<style scoped>
.guide-alert, .dialog-alert, .generation-state, .interview-summary-grid, .filter-bar { margin-bottom: var(--space-5); }
.guide-actions, .generation-state, .queue-error, .preparation-card__heading, .preparation-card__actions { display: flex; }
.guide-actions { gap: var(--space-2); margin-top: var(--space-3); }
.generation-state, .queue-error {
  align-items: center; justify-content: space-between; gap: var(--space-4); padding: var(--space-4) var(--space-5);
  border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface);
}
.generation-state p, .queue-error p { margin: var(--space-1) 0 0; color: var(--color-text-muted); line-height: 1.6; }
.generation-state--loading { border-color: var(--color-primary-border); background: var(--color-primary-soft); }
.generation-state--error, .queue-error { border-color: color-mix(in srgb, var(--color-danger) 28%, var(--color-border)); }
.generation-state__pulse {
  width: 12px; height: 12px; flex: 0 0 auto; border-radius: 50%; background: var(--color-primary);
  animation: generation-pulse 1.4s ease-in-out infinite;
}
.interview-summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--space-4); }
.queue-shell, .preparation-queue { display: grid; gap: var(--space-4); }
.queue-loading, .queue-error { min-height: 180px; }
.queue-loading > span { display: block; margin-bottom: var(--space-4); color: var(--color-text-muted); }
.preparation-card {
  display: grid; grid-template-columns: minmax(0, 1fr) 148px; gap: var(--space-5); padding: var(--space-5);
  border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-surface); box-shadow: var(--shadow-card);
}
.preparation-card__body, .role-context { min-width: 0; }
.preparation-card__heading { align-items: flex-start; justify-content: space-between; gap: var(--space-4); }
.eyebrow { color: var(--color-primary); font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; }
.role-context p { margin: var(--space-1) 0; color: var(--color-text-muted); font-size: 14px; overflow-wrap: anywhere; }
.role-context h2 { margin: var(--space-2) 0 0; color: var(--color-text); font-size: 20px; line-height: 1.4; overflow-wrap: anywhere; }
.preparation-facts { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--space-3); margin: var(--space-5) 0 var(--space-4); }
.preparation-facts div { min-width: 0; padding: var(--space-3); border-radius: var(--radius-sm); background: var(--color-surface-muted); }
.preparation-facts dt { margin-bottom: var(--space-1); color: var(--color-text-muted); font-size: 12px; }
.preparation-facts dd { margin: 0; color: var(--color-text); font-size: 14px; font-weight: 600; line-height: 1.5; overflow-wrap: anywhere; }
.taxonomy-tags { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.preparation-card__actions { flex-direction: column; gap: var(--space-2); }
.preparation-card__actions .el-button { width: 100%; margin-left: 0; }
.field-hint { margin: var(--space-2) 0 0; color: var(--color-text-muted); font-size: 12px; line-height: 1.5; }
.switch-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-4); }
.pager { justify-content: flex-end; margin-top: var(--space-2); }
@keyframes generation-pulse { 50% { opacity: 0.35; transform: scale(0.75); } }
@media (prefers-reduced-motion: reduce) { .generation-state__pulse { animation: none; } }
@media (max-width: 900px) {
  .interview-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .preparation-card { grid-template-columns: 1fr; }
  .preparation-facts { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .preparation-card__actions { flex-direction: row; flex-wrap: wrap; }
  .preparation-card__actions .el-button { width: auto; flex: 1 1 128px; }
}
@media (max-width: 600px) {
  .interview-summary-grid, .preparation-facts, .switch-row { grid-template-columns: 1fr; }
  .generation-state, .queue-error, .preparation-card__heading { align-items: stretch; flex-direction: column; }
  .preparation-card { padding: var(--space-4); }
  .pager { justify-content: flex-start; overflow-x: auto; }
}
</style>
