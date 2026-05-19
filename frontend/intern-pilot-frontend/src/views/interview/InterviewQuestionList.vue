<template>
  <PageContainer title="" description="基于简历、岗位 JD 和匹配分析生成专属面试题，辅助面试前复习与答辩演示。">
    <template #actions>
      <el-button type="primary" :icon="Plus" :disabled="!canGenerate" @click="openGenerate">
        生成面试题
      </el-button>
    </template>

    <el-alert
      v-if="!canGenerate && !loadingOptions"
      class="guide-alert"
      type="info"
      show-icon
      :closable="false"
      title="生成面试题前，需要先准备至少一份简历和一个岗位 JD。"
    >
      <div class="guide-actions">
        <el-button size="small" type="primary" @click="router.push('/resumes')">去上传简历</el-button>
        <el-button size="small" @click="router.push('/jobs')">去新增岗位</el-button>
      </div>
    </el-alert>

    <div v-loading="loading" class="interview-summary-grid">
      <StatCard label="面试题报告" :value="reportStats.total" :icon="Files" />
      <StatCard label="题目总数" :value="reportStats.questionTotal" :icon="Tickets" />
      <StatCard label="关联岗位" :value="reportStats.jobCount" :icon="Briefcase" />
      <StatCard label="最近生成" :value="reportStats.latestText" :icon="Clock" />
    </div>

    <section class="panel toolbar">
      <el-select v-model="query.resumeId" placeholder="按简历筛选" clearable filterable>
        <el-option
          v-for="item in resumes"
          :key="item.resumeId"
          :label="item.resumeName || item.originalFileName"
          :value="item.resumeId"
        />
      </el-select>
      <el-select v-model="query.jobId" placeholder="按岗位筛选" clearable filterable>
        <el-option
          v-for="item in jobs"
          :key="item.jobId"
          :label="`${item.companyName} - ${item.jobTitle}`"
          :value="item.jobId"
        />
      </el-select>
      <el-button type="primary" @click="search">筛选</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </section>

    <section v-loading="loading" class="interview-library">
      <el-alert
        v-if="loadError"
        class="table-alert"
        type="error"
        show-icon
        :closable="false"
        :title="loadError"
      >
        <el-button size="small" type="primary" @click="loadReports">重试</el-button>
      </el-alert>

      <AppEmpty
        v-else-if="!reports.length && !loading"
        title="还没有 AI 面试题"
        description="选择简历和岗位后生成专属面试题，用于面试前复盘。"
        hint="建议先完善岗位 JD 和简历解析结果，生成的问题会更贴合真实面试。"
      >
        <el-button type="primary" :icon="Plus" :disabled="!canGenerate" @click="openGenerate">
          生成第一套面试题
        </el-button>
      </AppEmpty>

      <article v-for="item in reports" v-else :key="item.reportId" class="interview-card">
        <div class="interview-card-main">
          <div class="interview-card-heading">
            <div>
              <span class="eyebrow">AI 面试题报告</span>
              <h3>{{ item.title || reportTitle(item) }}</h3>
            </div>
            <el-tag type="primary" effect="plain">{{ item.questionCount || 0 }} 题</el-tag>
          </div>

          <div class="interview-meta">
            <span>{{ item.companyName || '未知公司' }}</span>
            <span>{{ item.jobTitle || '未知岗位' }}</span>
            <span>{{ formatDateTime(item.createdAt) }}</span>
          </div>

          <div class="question-preview">
            <div>
              <strong>复习重点</strong>
              <p>围绕岗位技能、项目经历、简历追问和 HR 表达组织题目，适合面试前快速过一遍。</p>
            </div>
            <div>
              <strong>建议用法</strong>
              <p>先口述作答，再进入详情页查看参考答案、考察点和追问问题。</p>
            </div>
          </div>

          <div class="coverage-tags">
            <el-tag v-for="tag in coverageTags(item)" :key="tag" effect="plain">{{ tag }}</el-tag>
          </div>
        </div>

        <div class="interview-actions">
          <el-button type="primary" @click="goDetail(item.reportId)">查看详情</el-button>
          <el-button :loading="regeneratingId === item.reportId" @click="regenerateReport(item)">重新生成</el-button>
          <el-button
            v-if="authStore.hasPermission('analysis:delete')"
            type="danger"
            plain
            :loading="deletingId === item.reportId"
            @click="removeReport(item)"
          >
            删除
          </el-button>
        </div>
      </article>

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

    <el-dialog v-model="generateVisible" title="生成 AI 面试题" width="560px">
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
            <el-option
              v-for="item in resumes"
              :key="item.resumeId"
              :label="item.resumeName || item.originalFileName"
              :value="item.resumeId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="岗位">
          <el-select v-model="form.jobId" placeholder="请选择岗位" filterable>
            <el-option
              v-for="item in jobs"
              :key="item.jobId"
              :label="`${item.companyName} - ${item.jobTitle}`"
              :value="item.jobId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="简历版本">
          <el-select v-model="form.resumeVersionId" placeholder="默认使用当前版本" clearable filterable :disabled="!form.resumeId">
            <el-option
              v-for="item in versions"
              :key="item.versionId"
              :label="`${item.versionName}${item.isCurrent === 1 ? '（当前）' : ''}`"
              :value="item.versionId"
            />
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
          <p class="field-hint">
            只展示与当前简历和岗位匹配的分析报告；不选也可以直接根据简历和 JD 生成。
          </p>
        </el-form-item>

        <el-form-item label="题目数量">
          <el-input-number v-model="form.questionCount" :min="3" :max="20" />
          <p class="field-hint">默认 8 道题，范围 3-20。</p>
        </el-form-item>

        <el-form-item label="题目分类">
          <el-select v-model="form.categories" placeholder="不选则生成所有类型" multiple filterable>
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="难度">
          <el-select v-model="form.difficulties" placeholder="不选则混合难度" multiple filterable>
            <el-option
              v-for="item in difficultyOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <div class="switch-row">
          <el-form-item label="生成参考答案">
            <el-switch v-model="form.includeAnswer" />
          </el-form-item>
          <el-form-item label="生成追问问题">
            <el-switch v-model="form.includeFollowUps" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!canGenerate" :loading="generating" @click="generate">
          生成
        </el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Briefcase, Clock, Files, Plus, Tickets } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatCard from '@/components/common/StatCard.vue'
import router from '@/router'
import { getAnalysisReportsApi } from '@/api/analysis'
import {
  deleteInterviewQuestionReportApi,
  generateInterviewQuestionsApi,
  getInterviewQuestionReportsApi,
  regenerateInterviewQuestionsApi
} from '@/api/interviewQuestion'
import { getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'
import { getResumeVersionListApi } from '@/api/resumeVersion'
import { formatDateTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

const reports = ref<any[]>([])
const resumes = ref<any[]>([])
const jobs = ref<any[]>([])
const versions = ref<any[]>([])
const analysisReports = ref<any[]>([])
const loading = ref(false)
const loadingOptions = ref(false)
const generating = ref(false)
const generateVisible = ref(false)
const loadError = ref('')
const total = ref(0)
const regeneratingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)
const authStore = useAuthStore()

const query = reactive<{
  resumeId?: number
  resumeVersionId?: number
  jobId?: number
  pageNum: number
  pageSize: number
}>({
  pageNum: 1,
  pageSize: 10
})

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
}>({
  questionCount: 8,
  includeAnswer: true,
  includeFollowUps: true
})

const canGenerate = computed(() => resumes.value.length > 0 && jobs.value.length > 0)

const reportStats = computed(() => {
  const jobsInReports = new Set(reports.value.map((item) => item.jobId).filter(Boolean))
  const questionTotal = reports.value.reduce((sum, item) => sum + Number(item.questionCount || 0), 0)
  const latest = reports.value[0]?.createdAt
  return {
    total: total.value || reports.value.length,
    questionTotal,
    jobCount: jobsInReports.size,
    latestText: latest ? formatDateOnly(latest) : '-'
  }
})

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

const difficultyOptions = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '较难', value: 'HARD' }
]

const filteredAnalysisReports = computed(() => {
  if (!form.resumeId || !form.jobId) {
    return []
  }
  return analysisReports.value.filter(
    (item) => item.resumeId === form.resumeId && item.jobId === form.jobId
  )
})

watch(
  () => [form.resumeId, form.jobId],
  () => {
    if (!filteredAnalysisReports.value.some((item) => item.reportId === form.analysisReportId)) {
      form.analysisReportId = undefined
    }
  }
)

watch(
  () => form.resumeId,
  async () => {
    if (!form.resumeId) {
      versions.value = []
      form.resumeVersionId = undefined
      return
    }
    const res: any = await getResumeVersionListApi(form.resumeId)
    versions.value = res || []
    const current = versions.value.find((item) => item.isCurrent === 1)
    form.resumeVersionId = current?.versionId
  }
)

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
  } catch (e: any) {
    reports.value = []
    total.value = 0
    loadError.value = getErrorMessage(e, '面试题列表加载失败，请确认数据表已初始化后重试。')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  loadReports()
}

function resetQuery() {
  query.resumeId = undefined
  query.jobId = undefined
  query.pageNum = 1
  loadReports()
}

function handlePageChange(page: number) {
  query.pageNum = page
  loadReports()
}

function handleSizeChange(size: number) {
  query.pageSize = size
  query.pageNum = 1
  loadReports()
}

function openGenerate() {
  form.resumeId = resumes.value[0]?.resumeId
  form.jobId = jobs.value[0]?.jobId
  form.questionCount = form.questionCount || 8
  form.analysisReportId = undefined
  generateVisible.value = true
}

async function generate() {
  if (!form.resumeId || !form.jobId) {
    ElMessage.warning('请选择简历和岗位')
    return
  }

  generating.value = true
  try {
    const res: any = await generateInterviewQuestionsApi(form)
    ElMessage.success('面试题生成成功')
    generateVisible.value = false
    query.pageNum = 1
    await loadReports()
    router.push(`/interview-questions/${res.reportId}`)
  } catch (e: any) {
    ElMessage.error(getErrorMessage(e, '面试题生成失败，请检查简历、岗位和 AI 服务配置。'))
  } finally {
    generating.value = false
  }
}

function goDetail(reportId: number) {
  router.push(`/interview-questions/${reportId}`)
}

async function removeReport(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除“${row.title || '面试题报告'}”吗？删除后将无法继续查看这套题。`, '删除确认', {
      type: 'warning'
    })
  } catch {
    return
  }

  deletingId.value = row.reportId
  try {
    await deleteInterviewQuestionReportApi(row.reportId)
    ElMessage.success('删除成功')
    await loadReports()
  } catch (e: any) {
    ElMessage.error(getErrorMessage(e, '删除失败，请稍后重试'))
  } finally {
    deletingId.value = null
  }
}

async function regenerateReport(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认重新生成“${row.title || '面试题报告'}”吗？旧的题目将被替换。`,
      '重新生成确认',
      { type: 'warning' }
    )
  } catch {
    return
  }

  regeneratingId.value = row.reportId
  try {
    const res: any = await regenerateInterviewQuestionsApi(row.reportId)
    ElMessage.success('面试题重新生成成功')
    await loadReports()
    router.push(`/interview-questions/${res.reportId}`)
  } catch (e: any) {
    ElMessage.error(getErrorMessage(e, '重新生成失败，请稍后重试'))
  } finally {
    regeneratingId.value = null
  }
}

function reportTitle(item: any) {
  return `${item.companyName || '目标岗位'} - ${item.jobTitle || '面试题'}`
}

function coverageTags(item: any) {
  const count = Number(item.questionCount || 0)
  const tags = ['岗位技能', '项目经历', '简历追问']
  if (count >= 6) tags.push('HR 表达')
  if (count >= 10) tags.push('进阶追问')
  return tags
}

function getErrorMessage(error: any, fallback: string) {
  return error?.message || error?.response?.data?.message || fallback
}

function formatDateOnly(value?: string) {
  return value ? formatDateTime(value).slice(0, 10) : '-'
}

onMounted(async () => {
  await loadOptions()
  await loadReports()
})
</script>

<style scoped>
.guide-alert,
.table-alert,
.dialog-alert {
  margin-bottom: 16px;
}

.guide-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.interview-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 20px;
}

.interview-library {
  display: grid;
  gap: 14px;
}

.interview-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  padding: 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.interview-card-heading {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.eyebrow {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
}

.interview-card h3 {
  margin: 6px 0 0;
  font-size: 18px;
}

.interview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 12px 0;
  color: var(--color-text-soft);
  font-size: 13px;
}

.question-preview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.question-preview > div {
  padding: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
}

.question-preview strong {
  color: #1e40af;
  font-size: 13px;
}

.question-preview p {
  margin: 6px 0 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.coverage-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.interview-actions {
  display: flex;
  width: 132px;
  flex-direction: column;
  gap: 8px;
}

.interview-actions .el-button {
  width: 100%;
  margin-left: 0;
}

.field-hint {
  margin: 6px 0 0;
  color: var(--color-text-soft);
  font-size: 12px;
  line-height: 1.5;
}

.switch-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 900px) {
  .interview-summary-grid,
  .interview-card,
  .question-preview,
  .switch-row {
    grid-template-columns: 1fr;
  }

  .interview-actions {
    width: 100%;
  }
}
</style>
