<template>
  <PageContainer title="" description="根据简历、岗位和 AI 匹配报告生成定制化面试题。">
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
      title="生成面试题前，需要先准备至少一份已解析简历和一个岗位 JD。"
    >
      <div class="guide-actions">
        <el-button size="small" type="primary" @click="router.push('/resumes')">去上传简历</el-button>
        <el-button size="small" @click="router.push('/jobs')">去新增岗位</el-button>
      </div>
    </el-alert>

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
      <el-button type="primary" @click="loadReports">筛选</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </section>

    <section class="panel">
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

      <el-table v-else v-loading="loading" :data="reports">
        <el-table-column prop="title" label="报告标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="companyName" label="公司" min-width="120" />
        <el-table-column prop="jobTitle" label="岗位" min-width="160" show-overflow-tooltip />
        <el-table-column prop="questionCount" label="题目数" width="90" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.reportId)">练习</el-button>
            <el-button link type="success" @click="regenerateReport(row)">重新生成</el-button>
            <el-button link type="danger" @click="removeReport(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无面试题报告">
            <el-button type="primary" :disabled="!canGenerate" @click="openGenerate">生成第一套面试题</el-button>
          </el-empty>
        </template>
      </el-table>

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

        <el-form-item label="强制重新生成">
          <el-switch v-model="form.forceRefresh" />
          <p class="field-hint">关闭时若已有相同组合的历史报告，会直接打开历史结果。</p>
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

        <el-form-item label="生成参考答案">
          <el-switch v-model="form.includeAnswer" />
        </el-form-item>

        <el-form-item label="生成追问问题">
          <el-switch v-model="form.includeFollowUps" />
        </el-form-item>
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
import { Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
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
  forceRefresh: boolean
  questionCount?: number
  categories?: string[]
  difficulties?: string[]
  includeAnswer: boolean
  includeFollowUps: boolean
}>({
  forceRefresh: false,
  includeAnswer: true,
  includeFollowUps: true
})

const canGenerate = computed(() => resumes.value.length > 0 && jobs.value.length > 0)

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
  } catch {
    reports.value = []
    total.value = 0
    loadError.value = '面试题列表加载失败，请确认数据库表已初始化后重试。'
  } finally {
    loading.value = false
  }
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
  form.analysisReportId = undefined
  form.forceRefresh = false
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
    ElMessage.success(res.cacheHit ? '已打开历史面试题报告' : '面试题生成成功')
    generateVisible.value = false
    query.pageNum = 1
    await loadReports()
    router.push(`/interview-questions/${res.reportId}`)
  } finally {
    generating.value = false
  }
}

function goDetail(reportId: number) {
  router.push(`/interview-questions/${reportId}`)
}

async function removeReport(row: any) {
  await ElMessageBox.confirm(`确认删除「${row.title || '面试题报告'}」？`, '删除确认', {
    type: 'warning'
  })
  await deleteInterviewQuestionReportApi(row.reportId)
  ElMessage.success('删除成功')
  loadReports()
}

async function regenerateReport(row: any) {
  await ElMessageBox.confirm(
    `确认重新生成「${row.title || '面试题报告'}」？旧的题目将被替换。`,
    '重新生成确认',
    { type: 'warning' }
  )
  try {
    const res: any = await regenerateInterviewQuestionsApi(row.reportId)
    ElMessage.success('面试题重新生成成功')
    await loadReports()
    router.push(`/interview-questions/${res.reportId}`)
  } catch {
    ElMessage.error('重新生成失败，请稍后重试')
  }
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

.field-hint {
  margin: 6px 0 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}

.pager {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
