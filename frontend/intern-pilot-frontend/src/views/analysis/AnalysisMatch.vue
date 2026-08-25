<template>
  <PageContainer title="" width="wide">
    <template #hero>
      <PageHero
        eyebrow="AI 匹配分析"
        title="确认这次 AI 匹配"
        description="按顺序选好简历与目标岗位，再确认输入。任务提交后可离开页面，进度会继续同步到 AI 任务中心。"
      >
        <ol class="setup-sequence" aria-label="AI 匹配设置步骤">
          <li class="setup-step" data-setup-step="resume" :class="{ complete: Boolean(form.resumeId) }">
            <span>1</span>
            <strong>选择简历</strong>
          </li>
          <li class="setup-step" data-setup-step="job" :class="{ complete: Boolean(form.jobId) }">
            <span>2</span>
            <strong>选择目标岗位</strong>
          </li>
          <li class="setup-step" data-setup-step="confirm" :class="{ complete: canSubmit }">
            <span>3</span>
            <strong>确认并开始</strong>
          </li>
        </ol>
      </PageHero>
    </template>

    <section v-if="optionsError" class="inline-state inline-state--error" role="alert" data-options-error>
      <div>
        <strong>分析选项暂时无法加载</strong>
        <p>{{ optionsError }}</p>
      </div>
      <el-button type="primary" plain data-options-retry @click="loadOptions">重新加载</el-button>
    </section>

    <div class="setup-grid" :aria-busy="optionsLoading">
      <section class="panel setup-card">
        <div class="step-heading">
          <span class="step-number">01</span>
          <div>
            <h2>选择用于分析的简历</h2>
            <p>默认使用当前版本，也可以指定一份更贴近目标岗位的版本。</p>
          </div>
        </div>

        <el-form :model="form" label-position="top">
          <el-form-item label="简历">
            <el-select
              v-model="form.resumeId"
              placeholder="请选择简历"
              filterable
              :disabled="optionsLoading || running"
            >
              <el-option
                v-for="item in resumes"
                :key="item.resumeId"
                :label="item.resumeName || item.originalFileName"
                :value="item.resumeId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="简历版本">
            <el-select
              v-model="form.resumeVersionId"
              placeholder="默认使用当前版本"
              clearable
              filterable
              :disabled="!form.resumeId || running"
            >
              <el-option
                v-for="item in versions"
                :key="item.versionId"
                :label="`${displayVersionName(item)}${item.isCurrent === 1 ? '（当前）' : ''}`"
                :value="item.versionId"
              />
            </el-select>
          </el-form-item>
        </el-form>

        <div class="selection-summary" :class="{ muted: !selectedResume }">
          <span>本次简历</span>
          <strong>{{ selectedResume?.resumeName || selectedResume?.originalFileName || '尚未选择' }}</strong>
          <small>{{ selectedVersion ? displayVersionName(selectedVersion) : '默认当前版本' }}</small>
        </div>
      </section>

      <section class="panel setup-card">
        <div class="step-heading">
          <span class="step-number">02</span>
          <div>
            <h2>选择要对比的目标岗位</h2>
            <p>完整的岗位职责和技能要求会让匹配结论更有解释力。</p>
          </div>
        </div>

        <el-form :model="form" label-position="top">
          <el-form-item label="目标岗位 JD">
            <el-select
              v-model="form.jobId"
              placeholder="请选择岗位"
              filterable
              :disabled="optionsLoading || running"
            >
              <el-option
                v-for="item in jobs"
                :key="item.jobId"
                :label="`${item.companyName} - ${item.jobTitle}`"
                :value="item.jobId"
              />
            </el-select>
          </el-form-item>
        </el-form>

        <div v-if="selectedJob" class="job-preview">
          <strong>{{ selectedJob.companyName || '未知公司' }}</strong>
          <h3>{{ selectedJob.jobTitle || '未知岗位' }}</h3>
          <p>{{ selectedJob.location || '地点未填写' }} · {{ selectedJob.salaryRange || '薪资未填写' }}</p>
          <div class="jd-summary">
            <span>JD 摘要</span>
            <p>{{ jdSummary }}</p>
          </div>
          <div class="skill-tags">
            <el-tag v-for="skill in skillTags" :key="skill" type="primary" effect="plain">{{ skill }}</el-tag>
            <span v-if="!skillTags.length" class="empty-text">暂无技能关键词</span>
          </div>
        </div>
        <AppEmpty
          v-else
          title="还没有选择目标岗位"
          description="选择岗位后，这里会展示 JD 摘要与技能关键词。"
        />
      </section>

      <section class="panel setup-card confirm-card">
        <div class="step-heading">
          <span class="step-number">03</span>
          <div>
            <h2>确认输入并生成报告</h2>
            <p>系统会输出匹配分、优势、风险和下一步行动，不会改动你的简历或岗位。</p>
          </div>
        </div>

        <div class="quality-check" aria-label="输入确认">
          <div v-for="item in qualityChecks" :key="item.label" class="check-row">
            <el-icon :class="item.ok ? 'ok' : 'warn'" aria-hidden="true">
              <CircleCheck v-if="item.ok" />
              <Warning v-else />
            </el-icon>
            <div>
              <strong>{{ item.label }}</strong>
              <span>{{ item.text }}</span>
            </div>
          </div>
        </div>

        <label class="refresh-choice">
          <span>
            <strong>重新调用 AI 生成</strong>
            <small>关闭时优先复用已有缓存报告；开启后会创建新的分析结果。</small>
          </span>
          <el-switch v-model="form.forceRefresh" :disabled="running" aria-label="强制重新分析" />
        </label>

        <div v-if="submissionError" class="submission-error" role="alert" data-submission-error>
          {{ submissionError }}
        </div>

        <el-button
          class="primary-action"
          data-analysis-submit
          type="primary"
          :icon="MagicStick"
          :loading="submissionPending"
          :disabled="!canSubmit || optionsLoading || running"
          @click="startTask"
        >
          {{ primaryActionText }}
        </el-button>
        <p class="action-hint">任务提交成功后可在页面或 AI 任务中心继续查看进度。</p>
      </section>
    </div>

    <section class="panel progress-panel" aria-labelledby="analysis-progress-title">
      <div class="panel-header">
        <div>
          <span class="section-eyebrow">异步任务</span>
          <h2 id="analysis-progress-title">分析进度</h2>
          <p>{{ task.taskNo ? `任务编号：${task.taskNo}` : '提交任务后会在这里显示实时进度。' }}</p>
        </div>
        <StatusTag v-if="task.taskNo" :status="task.status" />
      </div>

      <template v-if="task.taskNo">
        <div class="progress-overview" aria-live="polite">
          <div class="progress-number" data-progress-percentage>{{ task.progress }}%</div>
          <div>
            <strong data-progress-stage>{{ task.errorMessage || task.message || currentStageText }}</strong>
            <p>{{ currentStageText }}</p>
          </div>
        </div>

        <el-progress
          class="task-progress"
          :percentage="task.progress"
          :status="progressStatus"
          :stroke-width="12"
          :show-text="false"
        />

        <ol class="progress-steps" aria-label="分析处理阶段">
          <li
            v-for="(step, index) in progressSteps"
            :key="step.title"
            :class="{ active: index === activeStep, complete: index < activeStep || task.status === 'COMPLETED' }"
          >
            <span>{{ index + 1 }}</span>
            <div>
              <strong>{{ step.title }}</strong>
              <small>{{ step.description }}</small>
            </div>
          </li>
        </ol>

        <div v-if="running" class="connection-row">
          <span data-connection-state :class="`connection-state connection-state--${connectionState}`">
            {{ connectionText }}
          </span>
          <div class="connection-actions">
            <el-button
              v-if="connectionState === 'fallback'"
              data-reconnect-action
              :icon="Refresh"
              @click="retryConnection"
            >
              重新连接
            </el-button>
            <el-button data-open-task-center @click="aiTaskCenter.openDrawer()">在任务中心查看</el-button>
          </div>
        </div>

        <div v-if="task.status === 'FAILED'" class="terminal-state terminal-state--failed" role="alert">
          <div>
            <strong>本次分析未完成</strong>
            <p>{{ task.errorMessage || 'AI 分析失败，请检查输入后重试。' }}</p>
          </div>
          <el-button type="primary" :icon="Refresh" @click="startTask">重试分析</el-button>
        </div>

        <div v-if="task.status === 'COMPLETED'" class="terminal-state terminal-state--success">
          <div>
            <strong>匹配报告已生成</strong>
            <p>报告 ID：{{ task.reportId || '-' }}。现在可以查看结论，或继续准备面试题。</p>
          </div>
          <div class="responsive-actions">
            <el-button type="primary" @click="goReports">查看分析报告</el-button>
            <el-button @click="goInterviewQuestions">生成面试题</el-button>
          </div>
        </div>
      </template>

      <AppEmpty
        v-else-if="!submissionPending"
        title="等待发起 AI 匹配分析"
        description="按上方三步确认输入，提交后会显示百分比、当前阶段和连接状态。"
        hint="刷新页面不会重复提交正在处理的任务，系统会使用任务编号恢复进度。"
      />

      <div v-else class="submitting-state" role="status" aria-live="polite">
        <el-skeleton :rows="3" animated />
        <span>正在提交分析任务，请稍候…</span>
      </div>
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
import PageHero from '@/components/common/PageHero.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import router from '@/router'
import { createAnalysisTaskApi, getAnalysisTaskDetailApi } from '@/api/analysisTask'
import { getJobDetailApi, getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'
import { getResumeVersionListApi } from '@/api/resumeVersion'
import type { AnalysisProgressMessage } from '@/utils/analysisSocket'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'

type ConnectionState = 'idle' | 'connecting' | 'connected' | 'fallback'

const aiTaskCenter = useAiTaskCenterStore()
const resumes = ref<any[]>([])
const versions = ref<any[]>([])
const jobs = ref<any[]>([])
const selectedJobDetail = ref<any>(null)
const optionsLoading = ref(false)
const optionsError = ref('')
const running = ref(false)
const submissionPending = ref(false)
const submissionError = ref('')
const connectionState = ref<ConnectionState>('idle')
const route = useRoute()
const TASK_STORAGE_KEY = 'internpilot:analysis:lastTaskNo'
let stompClient: Client | null = null
let pollingTimer: number | undefined
let currentLocalTaskId = ''
let activeTaskNo = ''
let taskSession = 0
let socketAttempt = 0

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
const canSubmit = computed(() => Boolean(form.resumeId && form.jobId))

const primaryActionText = computed(() => {
  if (submissionPending.value) return '正在提交分析任务'
  if (running.value && task.taskNo) return `AI 正在分析 · ${task.progress}%`
  if (!canSubmit.value) return '请先选择简历和岗位'
  return '开始生成匹配报告'
})

const jdSummary = computed(() => {
  const content = selectedJobDetail.value?.jdContent || ''
  if (!content) return '暂未读取到完整 JD 内容，建议先补充岗位职责、任职要求和加分项。'
  const text = content.replace(/\s+/g, ' ').trim()
  return text.length > 180 ? `${text.slice(0, 180)}...` : text
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
    label: '简历已确认',
    ok: Boolean(form.resumeId),
    text: form.resumeId ? selectedResume.value?.resumeName || selectedResume.value?.originalFileName || '已选择' : '请选择要分析的简历'
  },
  {
    label: '目标岗位已确认',
    ok: Boolean(form.jobId),
    text: form.jobId ? `${selectedJob.value?.companyName || '未知公司'} - ${selectedJob.value?.jobTitle || '未知岗位'}` : '请选择目标岗位'
  },
  {
    label: 'JD 内容可用',
    ok: Boolean(selectedJobDetail.value?.jdContent),
    text: selectedJobDetail.value?.jdContent ? '已读取岗位 JD，可用于 AI 匹配' : '缺少完整 JD 时，结论可能不够具体'
  },
  {
    label: '技能关键词可用',
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
  return 5
})

const currentStageText = computed(() => progressSteps[activeStep.value]?.description || '正在推进分析任务')

const progressStatus = computed(() => {
  if (task.status === 'FAILED') return 'exception'
  if (task.status === 'COMPLETED') return 'success'
  return undefined
})

const connectionText = computed(() => {
  if (connectionState.value === 'connected') return '实时连接正常'
  if (connectionState.value === 'fallback') return '实时连接中断，轮询同步中'
  if (connectionState.value === 'connecting') return '正在连接实时进度…'
  return '等待连接'
})

async function loadOptions() {
  optionsLoading.value = true
  optionsError.value = ''
  try {
    const [resumeRes, jobRes]: any[] = await Promise.all([
      getResumeListApi({ pageNum: 1, pageSize: 100 }),
      getJobListApi({ pageNum: 1, pageSize: 100 })
    ])
    resumes.value = resumeRes.records || []
    jobs.value = jobRes.records || []
    await applyQueryDefaults()
  } catch (error: any) {
    optionsError.value = error?.message || error?.response?.data?.message || '请检查网络后重新加载简历与岗位。'
  } finally {
    optionsLoading.value = false
  }
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
  const resumeId = form.resumeId
  if (!resumeId) {
    versions.value = []
    form.resumeVersionId = undefined
    return
  }
  const res: any = await getResumeVersionListApi(resumeId)
  if (form.resumeId !== resumeId) return
  versions.value = res || []
  const current = versions.value.find((item) => item.isCurrent === 1)
  form.resumeVersionId = current?.versionId
}

async function loadSelectedJobDetail() {
  const jobId = form.jobId
  selectedJobDetail.value = null
  if (!jobId) return
  try {
    const detail = await getJobDetailApi(jobId)
    if (form.jobId === jobId) selectedJobDetail.value = detail
  } catch {
    if (form.jobId === jobId) selectedJobDetail.value = selectedJob.value || null
  }
}

async function startTask() {
  if (!form.resumeId || !form.jobId) {
    ElMessage.warning('请选择简历和岗位')
    return
  }
  if (running.value) return

  cleanupTaskWatchers()
  const session = ++taskSession
  activeTaskNo = ''
  currentLocalTaskId = ''
  running.value = true
  submissionPending.value = true
  submissionError.value = ''
  connectionState.value = 'idle'
  Object.assign(task, {
    taskNo: '',
    status: 'SUBMITTED',
    progress: 0,
    message: '正在提交分析任务',
    reportId: undefined,
    errorMessage: ''
  })

  try {
    const res: any = await createAnalysisTaskApi(form)
    if (session !== taskSession) return

    activeTaskNo = res.taskNo
    currentLocalTaskId = aiTaskCenter.upsertByTaskNo({
      type: 'ANALYSIS_MATCH',
      title: 'AI 简历匹配分析',
      message: '正在创建分析任务...',
      taskNo: res.taskNo,
      status: res.status as any,
      progress: res.progress || 0,
      sourcePath: '/analysis/match'
    })

    applyTaskMessage(res, res.taskNo, session)
    localStorage.setItem(TASK_STORAGE_KEY, res.taskNo)
    connectSocket(res.taskNo, session)
    startPolling(res.taskNo, session)
  } catch (error: any) {
    if (session !== taskSession) return
    running.value = false
    submissionError.value = error?.message || error?.response?.data?.message || '任务创建失败，请稍后重试。'
    task.status = 'FAILED'
    task.errorMessage = submissionError.value
    aiTaskCenter.updateTask(currentLocalTaskId, { status: 'FAILED', errorMessage: '任务创建失败' })
  } finally {
    if (session === taskSession) submissionPending.value = false
  }
}

function connectSocket(taskNo: string, session = taskSession) {
  connectionState.value = 'connecting'
  const attempt = ++socketAttempt
  const browserRuntime = globalThis as typeof globalThis & { global?: typeof globalThis }
  browserRuntime.global ||= globalThis
  import('@/utils/analysisSocket')
    .then(({ createAnalysisSocket }) => {
      if (session !== taskSession || taskNo !== activeTaskNo || attempt !== socketAttempt) return
      const client = createAnalysisSocket(
        taskNo,
        (message) => {
          if (session !== taskSession || taskNo !== activeTaskNo) return
          connectionState.value = 'connected'
          applyTaskMessage(message, taskNo, session)
        },
        () => {
          if (session !== taskSession || taskNo !== activeTaskNo) return
          connectionState.value = 'fallback'
          ElMessage.warning('WebSocket 连接异常，已使用轮询兜底')
        }
      )
      const previousCloseHandler = client.onWebSocketClose
      client.onWebSocketClose = (event) => {
        previousCloseHandler?.(event)
        if (session !== taskSession || taskNo !== activeTaskNo || isTerminalStatus(task.status)) return
        connectionState.value = 'fallback'
      }
      stompClient = client
    })
    .catch((error) => {
      if (session !== taskSession || taskNo !== activeTaskNo) return
      console.warn('Failed to initialize analysis progress socket', error)
      connectionState.value = 'fallback'
      ElMessage.warning('WebSocket 初始化失败，已使用轮询兜底')
    })
}

async function retryConnection() {
  if (!activeTaskNo || isTerminalStatus(task.status)) return
  const reconnectTaskNo = activeTaskNo
  const reconnectSession = taskSession
  const previousClient = stompClient
  stompClient = null
  socketAttempt += 1
  if (previousClient) await previousClient.deactivate()
  if (reconnectSession !== taskSession || reconnectTaskNo !== activeTaskNo || isTerminalStatus(task.status)) return
  connectSocket(reconnectTaskNo, reconnectSession)
}

function startPolling(taskNo: string, session = taskSession) {
  if (pollingTimer) window.clearInterval(pollingTimer)
  pollingTimer = window.setInterval(async () => {
    try {
      const detail: any = await getAnalysisTaskDetailApi(taskNo)
      if (session !== taskSession || taskNo !== activeTaskNo) return
      applyTaskMessage(detail, taskNo, session)
    } catch {
      if (session === taskSession && taskNo === activeTaskNo && connectionState.value !== 'connected') {
        connectionState.value = 'fallback'
      }
    }
  }, 3000)
}

function applyTaskMessage(message: AnalysisProgressMessage, expectedTaskNo = activeTaskNo, session = taskSession) {
  if (session !== taskSession) return
  if (expectedTaskNo && message.taskNo !== expectedTaskNo) return
  if (activeTaskNo && message.taskNo !== activeTaskNo) return

  task.taskNo = message.taskNo
  task.status = message.status
  task.progress = message.progress || 0
  task.message = message.message || ''
  task.reportId = message.reportId
  task.errorMessage = message.errorMessage || ''

  if (message.taskNo) localStorage.setItem(TASK_STORAGE_KEY, message.taskNo)

  const updates: any = {
    backendTaskNo: message.taskNo,
    status: message.status as any,
    progress: message.progress || 0,
    message: message.message || '',
    resultId: message.reportId,
    errorMessage: message.errorMessage || ''
  }
  if (message.reportId) updates.resultPath = `/analysis/reports?reportId=${message.reportId}`
  aiTaskCenter.updateTask(currentLocalTaskId, updates)

  if (isTerminalStatus(message.status)) {
    running.value = false
    submissionPending.value = false
    cleanupTaskWatchers()
    connectionState.value = 'idle'

    if (message.status === 'COMPLETED') ElMessage.success('AI 分析完成')
    else if (message.status === 'FAILED') ElMessage.error(message.errorMessage || 'AI 分析失败')
    else if (message.status === 'CANCELLED') ElMessage.info('任务已取消')
  }
}

async function restoreLastTask() {
  const queryTaskNo = typeof route.query.taskNo === 'string' ? route.query.taskNo : ''
  const savedTaskNo = localStorage.getItem(TASK_STORAGE_KEY) || ''
  const taskNo = queryTaskNo || savedTaskNo
  if (!taskNo) return

  const session = ++taskSession
  activeTaskNo = taskNo
  try {
    const detail: any = await getAnalysisTaskDetailApi(taskNo)
    if (session !== taskSession) return

    if (detail.resumeId && !form.resumeId) {
      form.resumeId = detail.resumeId
      await loadVersions()
    }
    if (detail.jobId && !form.jobId) {
      form.jobId = detail.jobId
      await loadSelectedJobDetail()
    }

    currentLocalTaskId = aiTaskCenter.upsertByTaskNo({
      type: 'ANALYSIS_MATCH',
      title: 'AI 简历匹配分析',
      taskNo: detail.taskNo,
      status: detail.status as any,
      progress: detail.progress || 0,
      message: detail.message || '',
      reportId: detail.reportId,
      sourcePath: '/analysis/match'
    })
    applyTaskMessage(detail, taskNo, session)

    if (!isTerminalStatus(detail.status)) {
      running.value = true
      connectSocket(detail.taskNo, session)
      startPolling(detail.taskNo, session)
    }
  } catch {
    if (session === taskSession) {
      activeTaskNo = ''
      localStorage.removeItem(TASK_STORAGE_KEY)
    }
  }
}

function isTerminalStatus(status: string) {
  return status === 'COMPLETED' || status === 'FAILED' || status === 'CANCELLED'
}

function cleanupSocket() {
  socketAttempt += 1
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}

function cleanupTaskWatchers() {
  cleanupSocket()
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

watch(() => form.resumeId, () => { void loadVersions() })
watch(() => form.jobId, () => { void loadSelectedJobDetail() })

onMounted(async () => {
  await loadOptions()
  await loadSelectedJobDetail()
  await restoreLastTask()
})

onBeforeUnmount(() => {
  taskSession += 1
  cleanupTaskWatchers()
})
</script>

<style scoped>
.setup-sequence {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
  margin: var(--space-5) 0 0;
  padding: 0;
  list-style: none;
}

.setup-step {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  min-width: 0;
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
  background: var(--color-surface);
  color: var(--color-text-muted);
}

.setup-step > span,
.step-number {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--color-surface-subtle);
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.setup-step.complete {
  border-color: var(--color-primary-soft);
  color: var(--color-primary-strong);
}

.setup-step.complete > span {
  background: var(--color-primary-soft);
  color: var(--color-primary-strong);
}

.inline-state {
  display: flex;
  gap: var(--space-4);
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-danger-border, #fecaca);
  border-radius: var(--radius-card);
  background: var(--color-danger-soft, #fef2f2);
}

.inline-state p {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
}

.setup-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
}

.setup-card {
  min-width: 0;
}

.confirm-card {
  grid-column: 1 / -1;
}

.step-heading {
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
  margin-bottom: var(--space-5);
}

.step-heading h2,
.panel-header h2 {
  margin: 0;
  font-size: var(--font-size-xl);
}

.step-heading p,
.panel-header p,
.action-hint,
.progress-overview p,
.terminal-state p {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.selection-summary,
.job-preview,
.quality-check,
.refresh-choice {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
  background: var(--color-surface-subtle);
}

.selection-summary {
  display: grid;
  gap: var(--space-1);
  padding: var(--space-4);
}

.selection-summary > span,
.selection-summary small,
.job-preview > p,
.jd-summary > span {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.selection-summary.muted strong {
  color: var(--color-text-muted);
}

.job-preview {
  padding: var(--space-4);
}

.job-preview h3 {
  margin: var(--space-1) 0 0;
  overflow-wrap: anywhere;
}

.job-preview > strong {
  color: var(--color-primary-strong);
  overflow-wrap: anywhere;
}

.jd-summary {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

.jd-summary p {
  margin: var(--space-2) 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.quality-check {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
  padding: var(--space-4);
}

.check-row {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: var(--space-2);
  align-items: flex-start;
}

.check-row .ok {
  color: var(--color-success, #15803d);
}

.check-row .warn {
  color: var(--color-warning, #b45309);
}

.check-row strong,
.check-row span {
  display: block;
}

.check-row span {
  margin-top: var(--space-1);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.refresh-choice {
  display: flex;
  gap: var(--space-4);
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-4);
  padding: var(--space-4);
}

.refresh-choice span,
.refresh-choice strong,
.refresh-choice small {
  display: block;
}

.refresh-choice small {
  margin-top: var(--space-1);
  color: var(--color-text-muted);
  line-height: 1.5;
}

.submission-error {
  margin-top: var(--space-4);
  padding: var(--space-3);
  border-radius: var(--radius-control);
  background: var(--color-danger-soft, #fef2f2);
  color: var(--color-danger, #b91c1c);
}

.primary-action {
  width: 100%;
  min-height: 44px;
  margin-top: var(--space-4);
}

.action-hint {
  text-align: center;
  font-size: var(--font-size-sm);
}

.progress-panel {
  margin-top: var(--space-5);
}

.panel-header {
  display: flex;
  gap: var(--space-4);
  align-items: flex-start;
  justify-content: space-between;
}

.section-eyebrow {
  color: var(--color-primary-strong);
  font-size: var(--font-size-sm);
  font-weight: 800;
}

.progress-overview {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: var(--space-4);
  align-items: center;
  margin-top: var(--space-5);
  padding: var(--space-4);
  border: 1px solid var(--color-primary-soft);
  border-radius: var(--radius-card);
  background: var(--color-primary-soft);
}

.progress-number {
  color: var(--color-primary-strong);
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.task-progress {
  margin-top: var(--space-4);
}

.progress-steps {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: var(--space-2);
  margin: var(--space-5) 0 0;
  padding: 0;
  list-style: none;
}

.progress-steps li {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: var(--space-2);
  min-width: 0;
  color: var(--color-text-muted);
}

.progress-steps li > span {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border: 1px solid var(--color-border-strong);
  border-radius: 50%;
  font-size: 11px;
  font-weight: 800;
}

.progress-steps strong,
.progress-steps small {
  display: block;
}

.progress-steps small {
  margin-top: var(--space-1);
  line-height: 1.4;
}

.progress-steps li.active,
.progress-steps li.complete {
  color: var(--color-primary-strong);
}

.progress-steps li.active > span,
.progress-steps li.complete > span {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.connection-row,
.terminal-state {
  display: flex;
  gap: var(--space-4);
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-5);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
}

.connection-state {
  display: inline-flex;
  gap: var(--space-2);
  align-items: center;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.connection-state::before {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  content: '';
}

.connection-state--connected {
  color: var(--color-success, #15803d);
}

.connection-state--fallback {
  color: var(--color-warning, #b45309);
}

.connection-actions,
.responsive-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.terminal-state--failed {
  border-color: var(--color-danger-border, #fecaca);
  background: var(--color-danger-soft, #fef2f2);
}

.terminal-state--success {
  border-color: var(--color-success-border, #bbf7d0);
  background: var(--color-success-soft, #f0fdf4);
}

.submitting-state {
  display: grid;
  gap: var(--space-3);
  margin-top: var(--space-4);
  color: var(--color-text-muted);
}

.empty-text {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

@media (max-width: 900px) {
  .setup-grid,
  .quality-check {
    grid-template-columns: 1fr;
  }

  .confirm-card {
    grid-column: auto;
  }

  .progress-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-4);
  }
}

@media (max-width: 600px) {
  .setup-sequence {
    grid-template-columns: 1fr;
  }

  .inline-state,
  .panel-header,
  .connection-row,
  .terminal-state {
    align-items: stretch;
    flex-direction: column;
  }

  .progress-overview {
    grid-template-columns: 1fr;
  }

  .progress-steps {
    grid-template-columns: 1fr;
  }

  .connection-actions,
  .responsive-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .connection-actions .el-button,
  .responsive-actions .el-button {
    width: 100%;
    margin-left: 0;
  }
}
</style>
