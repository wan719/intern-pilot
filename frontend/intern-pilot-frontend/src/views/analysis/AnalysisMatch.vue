<template>
  <PageContainer title="" description="选择简历和岗位 JD，生成匹配分、优势短板、缺失技能和优化建议。">
    <section class="analysis-hero">
      <div>
        <span class="eyebrow">AI 匹配分析</span>
        <h2>从简历和 JD 出发，生成可解释的投递判断</h2>
        <p>分析完成后可以进入报告详情、继续生成面试题，形成完整答辩演示链路。</p>
      </div>
      <div class="hero-flow">
        <span>简历</span>
        <span>岗位 JD</span>
        <span>AI 报告</span>
        <span>面试题</span>
      </div>
    </section>

    <div class="analysis-grid">
      <section class="panel config-panel">
        <div class="panel-header">
          <div>
            <h3>分析配置</h3>
            <span>选择本次 AI 分析的输入材料</span>
          </div>
        </div>

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

          <el-form-item label="简历版本">
            <el-select v-model="form.resumeVersionId" placeholder="默认使用当前版本" clearable filterable :disabled="!form.resumeId">
              <el-option
                v-for="item in versions"
                :key="item.versionId"
                :label="`${displayVersionName(item)}${item.isCurrent === 1 ? '（当前）' : ''}`"
                :value="item.versionId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="岗位 JD">
            <el-select v-model="form.jobId" placeholder="请选择岗位" filterable>
              <el-option
                v-for="item in jobs"
                :key="item.jobId"
                :label="`${item.companyName} - ${item.jobTitle}`"
                :value="item.jobId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="分析模式">
            <el-segmented v-model="analysisMode" :options="analysisModeOptions" />
          </el-form-item>

          <el-form-item label="强制重新分析">
            <el-switch v-model="form.forceRefresh" />
            <p class="field-hint">关闭时会优先复用已有缓存结果；开启后会重新调用 AI 生成报告。</p>
          </el-form-item>

          <el-button type="primary" :icon="MagicStick" :loading="running" :disabled="running" @click="startTask">
            {{ running ? '分析中' : '开始 AI 分析' }}
          </el-button>
        </el-form>

        <div class="quality-check">
          <h4>输入质量检查</h4>
          <div v-for="item in qualityChecks" :key="item.label" class="check-row">
            <el-icon :class="item.ok ? 'ok' : 'warn'">
              <CircleCheck v-if="item.ok" />
              <Warning v-else />
            </el-icon>
            <div>
              <strong>{{ item.label }}</strong>
              <span>{{ item.text }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="preview-column">
        <section class="panel preview-panel">
          <div class="panel-header">
            <div>
              <h3>分析预览</h3>
              <span>开始前确认简历、岗位和输出内容</span>
            </div>
          </div>

          <div v-if="selectedResume || selectedJob" class="preview-stack">
            <div class="preview-card">
              <strong>简历</strong>
              <span>{{ selectedResume?.resumeName || selectedResume?.originalFileName || '未选择简历' }}</span>
              <small>{{ selectedVersion ? displayVersionName(selectedVersion) : '默认当前版本' }}</small>
            </div>

            <div class="preview-card">
              <strong>岗位</strong>
              <span>{{ selectedJob?.companyName || '未选择公司' }} - {{ selectedJob?.jobTitle || '未选择岗位' }}</span>
              <small>{{ selectedJob?.location || '地点未填写' }} · {{ selectedJob?.salaryRange || '薪资未填写' }}</small>
            </div>

            <div class="jd-preview">
              <div class="panel-header compact">
                <h4>JD 摘要</h4>
                <el-tag v-if="selectedJobDetail?.jobType" effect="plain">{{ selectedJobDetail.jobType }}</el-tag>
              </div>
              <p>{{ jdSummary }}</p>
              <div class="skill-tags">
                <el-tag v-for="skill in skillTags" :key="skill" type="primary" effect="plain">{{ skill }}</el-tag>
                <span v-if="!skillTags.length" class="empty-text">暂无技能关键词</span>
              </div>
            </div>
          </div>

          <AppEmpty
            v-else
            title="请选择简历和岗位"
            description="选择后会在这里展示分析输入预览。"
          />
        </section>

        <section class="panel output-panel">
          <h3>本次分析会输出</h3>
          <div class="output-grid">
            <span>匹配分</span>
            <span>优势短板</span>
            <span>缺失技能</span>
            <span>简历建议</span>
            <span>面试准备</span>
            <span>投递判断</span>
          </div>
        </section>
      </section>
    </div>

    <section class="panel progress-panel">
      <div class="panel-header">
        <div>
          <h3>实时进度</h3>
          <span>{{ task.taskNo ? `任务编号：${task.taskNo}` : '等待启动分析任务' }}</span>
        </div>
        <StatusTag v-if="task.taskNo" :status="task.status" />
      </div>

      <template v-if="task.taskNo">
        <el-steps :active="activeStep" finish-status="success" align-center>
          <el-step v-for="step in progressSteps" :key="step.title" :title="step.title" :description="step.description" />
        </el-steps>

        <el-progress class="task-progress" :percentage="task.progress" :status="progressStatus" :stroke-width="10" />

        <div class="progress-message" :class="{ failed: task.status === 'FAILED' }">
          {{ task.errorMessage || task.message || '正在推进分析任务...' }}
        </div>

        <div v-if="task.status === 'FAILED'" class="failed-actions">
          <el-button type="primary" :icon="Refresh" @click="startTask">重试分析</el-button>
        </div>

        <div v-if="task.status === 'COMPLETED'" class="success-actions">
          <el-result icon="success" title="分析完成" :sub-title="`报告 ID：${task.reportId || '-'}`" />
          <div>
            <el-button type="primary" @click="goReports">查看分析报告</el-button>
            <el-button @click="goInterviewQuestions">生成面试题</el-button>
          </div>
        </div>
      </template>

      <AppEmpty
        v-else
        title="等待发起 AI 匹配分析"
        description="选择简历和目标岗位后，系统会展示实时进度。"
        hint="答辩演示建议使用一份已解析简历和完整 JD，进度会从任务创建、读取输入到报告生成逐步推进。"
      />
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, MagicStick, Refresh, Warning } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import type { Client } from '@stomp/stompjs'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import router from '@/router'
import { createAnalysisTaskApi, getAnalysisTaskDetailApi } from '@/api/analysisTask'
import { getJobDetailApi, getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'
import { getResumeVersionListApi } from '@/api/resumeVersion'
import type { AnalysisProgressMessage } from '@/utils/analysisSocket'

const resumes = ref<any[]>([])
const versions = ref<any[]>([])
const jobs = ref<any[]>([])
const selectedJobDetail = ref<any>(null)
const running = ref(false)
const analysisMode = ref('quick')
const route = useRoute()
const TASK_STORAGE_KEY = 'internpilot:analysis:lastTaskNo'
let stompClient: Client | null = null
let pollingTimer: number | undefined

const analysisModeOptions = [
  { label: '快速分析', value: 'quick' },
  { label: '深度分析', value: 'deep' }
]

const progressSteps = [
  { title: '创建任务', description: '记录分析请求' },
  { title: '读取简历', description: '加载当前版本' },
  { title: '解析 JD', description: '提取岗位要求' },
  { title: '调用 AI', description: '生成结构化结果' },
  { title: '生成报告', description: '保存匹配分析' },
  { title: '完成', description: '进入下一步' }
]

const form = reactive({
  resumeId: undefined as number | undefined,
  resumeVersionId: undefined as number | undefined,
  jobId: undefined as number | undefined,
  forceRefresh: false
})

const task = reactive({
  taskNo: '',
  status: '',
  progress: 0,
  message: '',
  reportId: undefined as number | undefined,
  errorMessage: ''
})

const selectedResume = computed(() => resumes.value.find((item) => item.resumeId === form.resumeId))
const selectedVersion = computed(() => versions.value.find((item) => item.versionId === form.resumeVersionId))
const selectedJob = computed(() => jobs.value.find((item) => item.jobId === form.jobId))

const jdSummary = computed(() => {
  const content = selectedJobDetail.value?.jdContent || ''
  if (!content) return '暂未读取到完整 JD 内容，建议进入岗位管理补充岗位职责、任职要求和加分项。'
  const text = content.replace(/\s+/g, ' ').trim()
  return text.length > 160 ? `${text.slice(0, 160)}...` : text
})

const skillTags = computed(() => {
  const value = selectedJobDetail.value?.skillRequirements || ''
  if (!value) return []
  return value
    .split(/[\n,，、;；]/)
    .map((item: string) => item.replace(/^\d+[.、]\s*/, '').trim())
    .filter(Boolean)
    .slice(0, 8)
})

const qualityChecks = computed(() => [
  {
    label: '已选择简历',
    ok: Boolean(form.resumeId),
    text: form.resumeId ? selectedResume.value?.resumeName || selectedResume.value?.originalFileName || '已选择' : '请选择要分析的简历'
  },
  {
    label: '已选择岗位 JD',
    ok: Boolean(form.jobId),
    text: form.jobId ? `${selectedJob.value?.companyName || '未知公司'} - ${selectedJob.value?.jobTitle || '未知岗位'}` : '请选择目标岗位'
  },
  {
    label: 'JD 内容完整',
    ok: Boolean(selectedJobDetail.value?.jdContent),
    text: selectedJobDetail.value?.jdContent ? '已读取到岗位 JD，可用于 AI 匹配' : '建议补充 JD 内容，分析会更准确'
  },
  {
    label: '技能关键词',
    ok: skillTags.value.length > 0,
    text: skillTags.value.length ? `已识别 ${skillTags.value.length} 个技能关键词` : '建议补充岗位技能要求'
  }
])

const activeStep = computed(() => {
  if (task.progress < 10) return 0
  if (task.progress < 25) return 1
  if (task.progress < 45) return 2
  if (task.progress < 70) return 3
  if (task.progress < 95) return 4
  return 6
})

const progressStatus = computed(() => {
  if (task.status === 'FAILED') return 'exception'
  if (task.status === 'COMPLETED') return 'success'
  return undefined
})

async function loadOptions() {
  const [resumeRes, jobRes]: any[] = await Promise.all([
    getResumeListApi({ pageNum: 1, pageSize: 100 }),
    getJobListApi({ pageNum: 1, pageSize: 100 })
  ])
  resumes.value = resumeRes.records || []
  jobs.value = jobRes.records || []
  await applyQueryDefaults()
}

async function applyQueryDefaults() {
  const resumeId = Number(route.query.resumeId)
  const resumeVersionId = Number(route.query.resumeVersionId)
  const jobId = Number(route.query.jobId)
  if (Number.isFinite(resumeId) && resumeId > 0) {
    form.resumeId = resumeId
    await loadVersions()
  }
  if (Number.isFinite(resumeVersionId) && resumeVersionId > 0) {
    form.resumeVersionId = resumeVersionId
  }
  if (Number.isFinite(jobId) && jobId > 0) {
    form.jobId = jobId
  }
}

async function loadVersions() {
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

async function loadSelectedJobDetail() {
  selectedJobDetail.value = null
  if (!form.jobId) return
  try {
    selectedJobDetail.value = await getJobDetailApi(form.jobId)
  } catch {
    selectedJobDetail.value = selectedJob.value || null
  }
}

async function startTask() {
  if (!form.resumeId || !form.jobId) {
    ElMessage.warning('请选择简历和岗位')
    return
  }

  cleanupTaskWatchers()
  running.value = true
  task.errorMessage = ''

  try {
    const res: any = await createAnalysisTaskApi(form)
    applyTaskMessage(res)
    localStorage.setItem(TASK_STORAGE_KEY, res.taskNo)
    connectSocket(res.taskNo)
    startPolling(res.taskNo)
  } catch {
    running.value = false
  }
}

function connectSocket(taskNo: string) {
  import('@/utils/analysisSocket')
    .then(({ createAnalysisSocket }) => {
      stompClient = createAnalysisSocket(
        taskNo,
        (message) => applyTaskMessage(message),
        () => {
          ElMessage.warning('WebSocket 连接异常，已使用轮询兜底')
        }
      )
    })
    .catch(() => {
      ElMessage.warning('WebSocket 初始化失败，已使用轮询兜底')
    })
}

function startPolling(taskNo: string) {
  pollingTimer = window.setInterval(async () => {
    const detail: any = await getAnalysisTaskDetailApi(taskNo)
    applyTaskMessage(detail)
  }, 3000)
}

function applyTaskMessage(message: AnalysisProgressMessage) {
  task.taskNo = message.taskNo
  task.status = message.status
  task.progress = message.progress || 0
  task.message = message.message || ''
  task.reportId = message.reportId
  task.errorMessage = message.errorMessage || ''

  if (message.taskNo) {
    localStorage.setItem(TASK_STORAGE_KEY, message.taskNo)
  }

  if (isTerminalStatus(message.status)) {
    running.value = false
    cleanupTaskWatchers()
  }

  if (message.status === 'COMPLETED') {
    ElMessage.success('AI 分析完成')
  }

  if (message.status === 'FAILED') {
    ElMessage.error(message.errorMessage || 'AI 分析失败')
  }
}

async function restoreLastTask() {
  const queryTaskNo = typeof route.query.taskNo === 'string' ? route.query.taskNo : ''
  const savedTaskNo = localStorage.getItem(TASK_STORAGE_KEY) || ''
  const taskNo = queryTaskNo || savedTaskNo
  if (!taskNo) return

  try {
    const detail: any = await getAnalysisTaskDetailApi(taskNo)
    if (isTerminalStatus(detail.status)) {
      localStorage.removeItem(TASK_STORAGE_KEY)
      return
    }
    applyTaskMessage(detail)
    running.value = true
    connectSocket(detail.taskNo)
    startPolling(detail.taskNo)
  } catch {
    localStorage.removeItem(TASK_STORAGE_KEY)
  }
}

function isTerminalStatus(status: string) {
  return status === 'COMPLETED' || status === 'FAILED'
}

function cleanupTaskWatchers() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  if (pollingTimer) {
    window.clearInterval(pollingTimer)
    pollingTimer = undefined
  }
}

function goReports() {
  router.push('/analysis/reports')
}

function goInterviewQuestions() {
  router.push({
    path: '/interview-questions',
    query: {
      resumeId: form.resumeId,
      resumeVersionId: form.resumeVersionId,
      jobId: form.jobId,
      reportId: task.reportId
    }
  })
}

function displayVersionName(item: any) {
  return item?.versionName || (item?.versionType === 'ORIGINAL' ? '原始版本' : '未命名版本')
}

watch(() => form.resumeId, loadVersions)
watch(() => form.jobId, loadSelectedJobDetail)

onMounted(async () => {
  await loadOptions()
  await loadSelectedJobDetail()
  await restoreLastTask()
})
onBeforeUnmount(cleanupTaskWatchers)
</script>

<style scoped>
.analysis-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
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

.analysis-hero h2 {
  margin: 6px 0 8px;
  font-size: 24px;
}

.analysis-hero p,
.panel-header span,
.field-hint {
  color: var(--color-text-soft);
  line-height: 1.6;
}

.analysis-hero p {
  margin: 0;
}

.hero-flow {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  max-width: 360px;
}

.hero-flow span,
.output-grid span {
  padding: 8px 10px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.analysis-grid {
  display: grid;
  grid-template-columns: minmax(280px, 400px) minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.config-panel {
  position: sticky;
  top: 96px;
}

.field-hint {
  margin: 6px 0 0;
  font-size: 12px;
}

.quality-check {
  display: grid;
  gap: 12px;
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--color-border);
}

.quality-check h4 {
  margin: 0;
}

.check-row {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
}

.check-row .ok {
  color: #16a34a;
}

.check-row .warn {
  color: #f59e0b;
}

.check-row strong,
.check-row span {
  display: block;
}

.check-row span {
  margin-top: 3px;
  color: var(--color-text-soft);
  font-size: 13px;
}

.preview-column {
  display: grid;
  gap: 16px;
}

.preview-stack {
  display: grid;
  gap: 12px;
}

.preview-card,
.jd-preview {
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #f8fafc;
}

.preview-card strong,
.preview-card span,
.preview-card small {
  display: block;
}

.preview-card span {
  margin-top: 6px;
  color: var(--color-text);
  font-weight: 700;
}

.preview-card small {
  margin-top: 4px;
  color: var(--color-text-soft);
}

.panel-header.compact {
  margin-bottom: 8px;
}

.panel-header.compact h4 {
  margin: 0;
}

.jd-preview p {
  margin: 0 0 12px;
  color: var(--color-text-muted);
  line-height: 1.8;
}

.skill-tags,
.output-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-text {
  color: var(--color-text-soft);
  font-size: 13px;
}

.output-panel h3 {
  margin: 0 0 12px;
}

.progress-panel {
  margin-top: 20px;
}

.task-progress {
  margin-top: 24px;
}

.progress-message {
  margin: 16px 0;
  padding: 12px 14px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1d4ed8;
  line-height: 1.7;
}

.progress-message.failed {
  border-color: #fecaca;
  background: #fef2f2;
  color: #dc2626;
}

.failed-actions {
  margin-top: 12px;
}

.success-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

@media (max-width: 900px) {
  .analysis-hero,
  .analysis-grid {
    grid-template-columns: 1fr;
  }

  .analysis-hero {
    flex-direction: column;
  }

  .hero-flow {
    justify-content: flex-start;
  }

  .config-panel {
    position: static;
  }
}
</style>
