<template>
  <PageContainer title="职业行动工作台" width="wide">
    <template #hero>
      <header class="dashboard-heading">
        <h1>工作台</h1>
        <p>{{ todayLabel }}</p>
      </header>

      <div class="dashboard-overview">
        <section class="dashboard-next" aria-labelledby="dashboard-next-heading">
          <h2 id="dashboard-next-heading">需要处理</h2>

        <div class="dashboard-focus">
          <div class="dashboard-focus__copy" aria-live="polite">
            <span v-if="!loading && !interviewLoading" class="dashboard-focus__label">
              {{ primaryAction.key === 'complete' ? '求职进度' : '当前阶段' }}
            </span>
            <strong v-if="!loading && !interviewLoading">{{ focusTitle }}</strong>
            <p>{{ heroNextStep }}</p>
          </div>
          <Button
            class="dashboard-primary"
            type="button"
            data-primary-action
            :disabled="loading || interviewLoading"
            :aria-busy="loading || interviewLoading"
            @click="handlePrimaryAction"
          >
            <el-icon v-if="loading || interviewLoading" class="is-loading" aria-hidden="true"><Loading /></el-icon>
            <el-icon v-else aria-hidden="true"><ArrowRight /></el-icon>
            {{ loading ? '正在确认下一步' : interviewLoading ? '正在确认面试阶段' : primaryAction.label }}
          </Button>
        </div>
        </section>

        <section class="dashboard-upcoming" aria-labelledby="dashboard-upcoming-heading">
          <h2 id="dashboard-upcoming-heading">即将到来</h2>
          <div class="dashboard-schedule">
            <p v-if="loading || scheduleLoading" role="status">正在加载面试日程</p>
            <p v-else-if="loadError || scheduleError">暂时无法加载面试日程</p>
            <el-button v-if="!loading && !loadError && scheduleError" link @click="loadSchedule">重试日程</el-button>
            <p v-if="!loading && !scheduleLoading && !loadError && !scheduleError && !upcoming.length">暂无即将到来的面试</p>
            <div v-for="item in upcoming" :key="item.applicationId" class="dashboard-schedule-entry">
              <RouterLink to="/applications" class="dashboard-schedule-row">
                <span class="dashboard-date-box" aria-hidden="true">
                  <small>{{ calendarMonth(item.interviewDate) }}</small><b>{{ calendarDay(item.interviewDate) }}</b>
                </span>
                <span>
                  <time :datetime="item.interviewDate" :aria-label="formatDateTime(item.interviewDate)">{{ interviewTime(item.interviewDate) }}</time>
                  <strong>{{ item.companyName || '未知公司' }} · {{ item.jobTitle || '未知岗位' }}</strong>
                </span>
                <el-icon aria-hidden="true"><ArrowRight /></el-icon>
              </RouterLink>
              <RouterLink class="dashboard-schedule-prepare" :to="interviewPreparationPath(item.applicationId)"
                :aria-label="`准备面试：${item.companyName || '未知公司'} · ${item.jobTitle || '未知岗位'}`">准备面试</RouterLink>
            </div>
            <RouterLink v-if="!loading" class="dashboard-schedule-link" to="/applications">查看投递记录</RouterLink>
          </div>
        </section>

        <section class="dashboard-continue" aria-labelledby="dashboard-continue-heading">
          <h2 id="dashboard-continue-heading">从上次中断处继续</h2>
          <p v-if="loading" role="status">正在加载简历和匹配报告</p>
          <p v-else-if="loadError">暂时无法加载最近资料</p>
          <p v-else-if="!continueItems.length">创建简历或生成匹配报告后，可以在这里继续。</p>
          <RouterLink v-for="item in continueItems" :key="item.kind" :to="item.path" class="dashboard-continue-row">
            <span class="dashboard-document-icon"><el-icon aria-hidden="true"><component :is="item.icon" /></el-icon></span>
            <span><strong>{{ item.title }}</strong><small>{{ item.kind }} · {{ formatDateTime(item.createdAt) }}</small></span>
            <el-icon aria-hidden="true"><ArrowRight /></el-icon>
          </RouterLink>
        </section>
      </div>


    </template>

    <section
      class="dashboard-workspace"
      :aria-busy="loading"
      :aria-label="loading ? '正在加载职业工作台' : '职业行动工作台'"
    >
      <div v-if="loadError" class="dashboard-error" role="alert">
        <div>
          <strong>工作台数据暂时无法加载</strong>
          <span>{{ loadError }}</span>
        </div>
        <el-button @click="loadDashboard">重新加载</el-button>
      </div>

      <section class="journey-progress panel" aria-labelledby="journey-progress-title" :aria-busy="interviewLoading">
        <div class="journey-heading">
          <h2 id="journey-progress-title">求职旅程</h2>
          <div v-if="!loading" class="journey-progress__summary">
            <span>{{ interviewReportCount === null ? `${completedStageCount}/4 已知阶段已启动` : `${completedStageCount}/5 已启动` }}</span>
            <progress
              v-if="interviewReportCount !== null"
              :value="completedStageCount"
              :max="journeyStages.length"
              aria-label="已启动阶段比例"
            />
          </div>
          <el-skeleton v-else class="journey-progress__placeholder" animated :rows="0" />
        </div>

        <ol v-if="!loading" class="journey-steps" role="list" aria-labelledby="journey-progress-title">
          <li
            v-for="stage in journeyStages"
            :key="stage.key"
            class="journey-step"
            :class="{ 'journey-step--complete': stage.done }"
            :aria-current="stage.key === currentStageKey ? 'step' : undefined"
          >
            <button type="button" @click="router.push(stage.path)">
              <span class="journey-step__marker" aria-hidden="true">
                <el-icon v-if="stage.done"><CircleCheckFilled /></el-icon>
                <el-icon v-else><component :is="stage.icon" /></el-icon>
              </span>
              <span class="journey-step__copy">
                <strong>{{ stage.title }}</strong>
                <small>{{ stage.hint }}</small>
              </span>
              <span class="journey-step__status">
                <span v-if="stage.done === null">{{ interviewLoading ? '确认中' : '暂时未知' }}</span>
                <span v-else-if="stage.done">已启动</span>
                <span v-else-if="stage.key === currentStageKey">下一步</span>
                <span v-else>待开始</span>
              </span>
              <el-icon class="journey-step__arrow" aria-hidden="true"><ArrowRight /></el-icon>
            </button>
          </li>
        </ol>
        <div v-else class="journey-loading" aria-label="正在加载五阶段准备进度">
          <el-skeleton v-for="index in 5" :key="index" animated :rows="1" />
        </div>
        <div v-if="!loading && interviewError" class="journey-inline-error" role="status" aria-live="polite">
          <span>{{ interviewError }}</span>
          <el-button
            v-if="primaryAction.key !== 'interview-unknown'"
            data-interview-retry
            link
            type="primary"
            :icon="Refresh"
            @click="loadInterviewReportCount"
          >
            重试面试阶段数据
          </el-button>
        </div>
        <div v-else-if="!loading && interviewLoading" class="journey-inline-status" role="status" aria-live="polite">
          正在确认面试阶段，其他工作台数据可以正常查看。
        </div>
      </section>

      <section class="recent-results" aria-labelledby="recent-results-title">
        <h2 id="recent-results-title">近期结果</h2>

        <div class="dashboard-grid">
          <section class="panel">
            <div class="panel-header">
              <div>
                <h3>最近分析</h3>
              </div>
              <el-button link type="primary" @click="router.push('/analysis/reports')">查看全部</el-button>
            </div>
            <div v-if="recentReports.length" class="report-list">
              <article v-for="item in recentReports" :key="item.reportId" class="report-card">
                <div class="result-copy">
                  <strong>{{ item.companyName || '未知公司' }} - {{ item.jobTitle || '未知岗位' }}</strong>
                  <span>{{ formatDateTime(item.createdAt) }}</span>
                </div>
                <div class="report-score">
                  <el-progress :percentage="Number(item.matchScore || 0)" :stroke-width="8" />
                  <el-tag :type="scoreTagType(item.matchScore)" effect="plain">{{ item.matchLevel || scoreLabel(item.matchScore) }}</el-tag>
                </div>
              </article>
            </div>
            <AppEmpty
              v-else-if="!loading"
              title="暂无分析报告"
              description="完成一次 AI 匹配后，这里会展示最近报告"
              hint="先选择简历与目标岗位，再生成一份有行动建议的报告。"
            >
              <el-button @click="router.push('/analysis/match')">开始 AI 匹配</el-button>
            </AppEmpty>
          </section>

          <section class="panel">
            <div class="panel-header">
              <div>
                <h3>最近投递</h3>
              </div>
              <el-button link type="primary" @click="router.push('/applications')">查看全部</el-button>
            </div>
            <div v-if="recentApplications.length" class="application-list">
              <article v-for="item in recentApplications" :key="item.applicationId" class="application-card">
                <div class="result-copy">
                  <strong>{{ item.companyName || '未知公司' }} - {{ item.jobTitle || '未知岗位' }}</strong>
                  <span>{{ item.interviewDate ? `面试时间：${formatDateTime(item.interviewDate)}` : item.note || '暂无备注' }}</span>
                </div>
                <el-tag :type="statusTypes[item.status]" effect="plain">{{ statusLabels[item.status] || item.status }}</el-tag>
              </article>
            </div>
            <AppEmpty
              v-else-if="!loading"
              title="暂无投递记录"
              description="记录投递状态、面试时间和复盘内容"
            >
              <el-button @click="router.push('/applications')">添加投递记录</el-button>
            </AppEmpty>
          </section>
        </div>

        <div class="dashboard-grid">
          <section class="panel">
            <div class="panel-header">
              <div>
                <h3>投递状态概览</h3>
                <span>查看当前投递记录的分布和待跟进情况</span>
              </div>
            </div>
            <div v-if="applications.length" class="status-overview">
              <div ref="statusChartRef" class="chart compact" aria-label="投递状态分布图"></div>
              <div class="status-list">
                <div v-for="item in statusSummary" :key="item.status" class="status-row">
                  <el-tag :type="statusTypes[item.status]" effect="plain">{{ statusLabels[item.status] || item.status }}</el-tag>
                  <strong>{{ item.count }}</strong>
                </div>
              </div>
            </div>
            <AppEmpty
              v-else-if="!loading"
              title="暂无投递动态"
              description="创建投递记录后，这里会展示最近进展"
              hint="记录状态、面试时间和跟进事项，避免错过关键节点。"
            >
              <el-button @click="router.push('/applications')">记录第一次投递</el-button>
            </AppEmpty>
          </section>

          <section class="panel">
            <div class="panel-header">
              <div>
                <h3>匹配分趋势</h3>
                <span>最近报告的匹配分，帮助判断简历优化效果</span>
              </div>
            </div>
            <div v-if="reports.length" ref="scoreChartRef" class="chart" aria-label="最近报告匹配分趋势图"></div>
            <AppEmpty v-else-if="!loading" title="暂无匹配分数据" description="生成分析报告后会展示匹配分趋势" />
          </section>
        </div>
      </section>

      <section class="stat-grid" aria-label="工作台数据概览">
        <StatCard label="简历数量" :value="summary.resumes" :icon="Document" :loading="loading" hint="可用于岗位匹配" />
        <StatCard label="目标岗位" :value="summary.jobs" :icon="Briefcase" :loading="loading" hint="已维护岗位 JD" tone="info" />
        <StatCard label="分析报告" :value="summary.reports" :icon="Tickets" :loading="loading" hint="AI 匹配结果" tone="success" />
        <StatCard label="投递记录" :value="summary.applications" :icon="List" :loading="loading" hint="持续跟进进展" tone="warning" />
      </section>
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { init, use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import {
  ArrowRight,
  Briefcase,
  CircleCheckFilled,
  Document,
  List,
  Loading,
  MagicStick,
  QuestionFilled,
  Refresh,
  Tickets,
  Upload
} from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatCard from '@/components/common/StatCard.vue'
import { Button } from '@/components/ui/button'
import router from '@/router'
import { getApplicationListApi } from '@/api/application'
import { getAnalysisReportsApi } from '@/api/analysis'
import { getInterviewQuestionReportsApi } from '@/api/interviewQuestion'
import { getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'
import { newestWorkspaceItem, upcomingInterviews } from './workspaceContent'
import { formatDateTime, statusLabels, statusTypes } from '@/utils/format'

const summary = reactive({ resumes: 0, jobs: 0, reports: 0, applications: 0 })
const interviewReportCount = ref<number | null>(null)
const interviewLoading = ref(false)
const interviewError = ref('')
const resumeRecords = ref<any[]>([])
const reports = ref<any[]>([])
const applications = ref<any[]>([])
const scheduleRecords = ref<any[]>([])
const applicationPageSize = ref(100)
const scheduleLoading = ref(false)
const scheduleError = ref(false)
const loading = ref(true)
const loadError = ref('')
const statusChartRef = ref<HTMLDivElement>()
const scoreChartRef = ref<HTMLDivElement>()

use([PieChart, LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const journeyProgress = computed(() => ({
  resume: summary.resumes > 0,
  job: summary.jobs > 0,
  analysis: summary.reports > 0,
  interview: interviewReportCount.value === null ? null : interviewReportCount.value > 0,
  application: summary.applications > 0
}))

const journeyStages = computed(() => [
  {
    key: 'resume', title: '简历准备', actionLabel: '上传第一份简历', hint: '维护可用于匹配的简历',
    description: '从上传第一份简历开始，为后续岗位匹配准备可靠材料。', icon: Upload, path: '/resumes', done: journeyProgress.value.resume
  },
  {
    key: 'job', title: '岗位机会', actionLabel: '添加目标岗位', hint: '保存岗位 JD 与要求',
    description: '保存完整 JD，让后续分析围绕这个岗位展开。', icon: Briefcase, path: '/jobs', done: journeyProgress.value.job
  },
  {
    key: 'analysis', title: '匹配分析', actionLabel: '开始 AI 匹配', hint: '找到优势与待补短板',
    description: '对照目标岗位，查看匹配优势、风险和改进建议。', icon: MagicStick, path: '/analysis/match', done: journeyProgress.value.analysis
  },
  {
    key: 'interview', title: '面试准备', actionLabel: '准备下一场面试', hint: '生成岗位针对性问题',
    description: '用针对目标岗位的问题检查表达与知识盲点。', icon: QuestionFilled, path: '/interview-questions', done: journeyProgress.value.interview
  },
  {
    key: 'application', title: '投递追踪', actionLabel: '记录投递进展', hint: '管理状态与跟进时间',
    description: '记录投递状态和关键时间，方便后续跟进。', icon: List, path: '/applications', done: journeyProgress.value.application
  }
])

const completeAction = {
  key: 'complete', stageTitle: '五个阶段都已启动', label: '查看最新分析结果',
  description: '从分析报告中选一项改进建议继续推进。',
  icon: Tickets, path: '/analysis/reports'
}
const primaryAction = computed(() => {
  const stage = journeyStages.value.find((item) => item.done !== true)
  if (!stage) return completeAction
  if (stage.done === null && interviewError.value) {
    return {
      key: 'interview-unknown',
      stageTitle: stage.title,
      label: '重试面试阶段数据',
      description: '其他工作台数据已加载，面试阶段暂时未知。恢复数据后再确认最合适的下一步。',
      icon: Refresh,
      path: '',
      retry: true
    }
  }
  return {
    key: stage.key,
    stageTitle: stage.title,
    label: stage.actionLabel,
    description: stage.description,
    icon: stage.icon,
    path: stage.path
  }
})
const heroNextStep = computed(() => {
  if (loading.value) return '正在汇总你的求职资料与近期进展。'
  if (interviewLoading.value) return '基础工作台数据已就绪，正在确认面试准备阶段。'
  return primaryAction.value.description
})
const currentStageKey = computed(() => {
  if (interviewLoading.value) return null
  return primaryAction.value.key === 'interview-unknown' ? 'interview' : primaryAction.value.key
})
const completedStageCount = computed(() => journeyStages.value.filter((item) => item.done === true).length)
const recentReports = computed(() => reports.value.slice(0, 4))
const recentApplications = computed(() => applications.value.slice(0, 5))
const now = ref(Date.now())
const todayLabel = computed(() => new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(now.value))
const upcoming = computed(() => loading.value || loadError.value || scheduleLoading.value || scheduleError.value ? [] : upcomingInterviews(scheduleRecords.value, now.value))
const monthFormatter = new Intl.DateTimeFormat('zh-CN', { month: 'numeric' })
const dayFormatter = new Intl.DateTimeFormat('zh-CN', { day: 'numeric' })
const interviewFormatter = new Intl.DateTimeFormat('zh-CN', { weekday: 'long', hour: '2-digit', minute: '2-digit', hour12: false })
const calendarMonth = (date: string) => monthFormatter.format(new Date(date))
const calendarDay = (date: string) => dayFormatter.format(new Date(date)).replace('日', '')
const interviewTime = (date: string) => interviewFormatter.format(new Date(date))
const focusTitle = computed(() => primaryAction.value.key === 'interview' && upcoming.value[0]?.companyName
  ? '准备' + upcoming.value[0].companyName + '的面试'
  : primaryAction.value.stageTitle)
const continueItems = computed(() => {
  if (loading.value || loadError.value) return []
  const resume = newestWorkspaceItem(resumeRecords.value)
  const report = newestWorkspaceItem(reports.value)
  const links = []
  if (resume?.resumeId) links.push({
    kind: '简历', title: resume.resumeName || resume.originalFileName || '简历',
    createdAt: resume.createdAt, path: `/resumes/${resume.resumeId}/versions`, icon: Document,
  })
  if (report?.reportId) links.push({
    kind: '匹配报告', title: `${report.companyName || '未知公司'} · ${report.jobTitle || '未知岗位'}`,
    createdAt: report.createdAt, path: { path: '/analysis/reports', query: { reportId: String(report.reportId) } }, icon: Tickets,
  })
  return links
})

const statusSummary = computed(() => {
  const counts = applications.value.reduce<Record<string, number>>((acc, item) => {
    acc[item.status] = (acc[item.status] || 0) + 1
    return acc
  }, {})
  return Object.entries(counts).map(([status, count]) => ({ status, count }))
})

let dashboardRequest = 0
let scheduleRequest = 0
async function loadSchedule() {
  const request = ++scheduleRequest
  scheduleLoading.value = true
  scheduleError.value = false
  const firstPage = applications.value
  const pageSize = applicationPageSize.value
  try {
    const rows = new Map(firstPage.map(item => [item.applicationId, item]))
    const pageCount = Math.ceil(summary.applications / pageSize)
    for (let pageNum = 2; pageNum <= pageCount; pageNum++) {
      const result: any = await getApplicationListApi({ pageNum, pageSize })
      if (request !== scheduleRequest) return
      for (const item of result.records ?? []) {
        if (!rows.has(item.applicationId)) rows.set(item.applicationId, item)
      }
    }
    scheduleRecords.value = [...rows.values()]
  } catch {
    if (request === scheduleRequest) scheduleError.value = true
  } finally {
    if (request === scheduleRequest) scheduleLoading.value = false
  }
}

async function loadDashboard() {
  const request = ++dashboardRequest
  scheduleRequest += 1
  scheduleLoading.value = false
  scheduleError.value = false
  loading.value = true
  loadError.value = ''
  const interviewRequest = loadInterviewReportCount()
  try {
    const [resumeRes, jobRes, reportRes, appRes]: any[] = await Promise.all([
      getResumeListApi({ pageNum: 1, pageSize: 100 }),
      getJobListApi({ pageNum: 1, pageSize: 100 }),
      getAnalysisReportsApi({ pageNum: 1, pageSize: 100 }),
      getApplicationListApi({ pageNum: 1, pageSize: 100 })
    ])

    if (request !== dashboardRequest) return
    summary.resumes = resumeRes.total ?? resumeRes.records?.length ?? 0
    summary.jobs = jobRes.total ?? jobRes.records?.length ?? 0
    summary.reports = reportRes.total ?? reportRes.records?.length ?? 0
    summary.applications = appRes.total ?? appRes.records?.length ?? 0
    resumeRecords.value = resumeRes.records || []
    reports.value = reportRes.records || []
    applications.value = appRes.records || []
    applicationPageSize.value = Number(appRes.size) > 0 ? Number(appRes.size) : 100
    void loadSchedule()
    await nextTick()
    renderCharts()
  } catch {
    if (request === dashboardRequest) loadError.value = '请检查网络连接后重新加载；已有功能入口仍可继续使用。'
  } finally {
    if (request === dashboardRequest) loading.value = false
  }
  await interviewRequest
}

async function loadInterviewReportCount() {
  interviewLoading.value = true
  interviewError.value = ''
  interviewReportCount.value = null
  try {
    const result: any = await getInterviewQuestionReportsApi({ pageNum: 1, pageSize: 1 })
    interviewReportCount.value = result.total
      ?? result.records?.length
      ?? (Array.isArray(result) ? result.length : 0)
  } catch {
    interviewError.value = '面试阶段暂时无法确认，请单独重试此项数据。'
  } finally {
    interviewLoading.value = false
  }
}

function interviewPreparationPath(applicationId: number) {
  return { path: '/interview-questions', query: { applicationId: String(applicationId) } }
}

function handlePrimaryAction() {
  if ('retry' in primaryAction.value && primaryAction.value.retry) {
    loadInterviewReportCount()
    return
  }
  const interview = primaryAction.value.key === 'interview' ? upcoming.value[0] : undefined
  router.push(interview ? interviewPreparationPath(interview.applicationId) : primaryAction.value.path)
}

const charts = new Map<HTMLElement, ReturnType<typeof init>>()
const chartResize = typeof ResizeObserver === 'function' ? new ResizeObserver(entries => {
  for (const { target } of entries) charts.get(target as HTMLElement)?.resize()
}) : null
const chartTheme = new MutationObserver(renderCharts)

function chartFor(element: HTMLElement) {
  const existing = charts.get(element)
  if (existing) return existing
  const chart = init(element)
  charts.set(element, chart)
  chartResize?.observe(element)
  return chart
}

function renderCharts() {
  const elements = [statusChartRef.value, scoreChartRef.value]
  for (const [element, chart] of charts) {
    if (elements.includes(element as HTMLDivElement)) continue
    chartResize?.unobserve(element)
    chart.dispose()
    charts.delete(element)
  }
  const theme = getComputedStyle(document.documentElement)
  const token = (name: string, fallback: string) => theme.getPropertyValue(name).trim() || fallback
  const labelColor = token('--color-text-muted', '#64748b')
  const tooltipColors = {
    backgroundColor: token('--color-surface', '#fff'),
    borderColor: token('--color-border', '#dfe6ee'),
    textStyle: { color: token('--color-text', '#1e293b') },
  }
  if (statusChartRef.value && applications.value.length) {
    chartFor(statusChartRef.value).setOption({
      color: ['#2563eb', '#16a34a', '#f59e0b', '#dc2626', '#64748b'], tooltip: { trigger: 'item', ...tooltipColors },
      series: [{
        type: 'pie', radius: ['52%', '74%'], label: { show: false },
        data: statusSummary.value.map((item) => ({ name: statusLabels[item.status] || item.status, value: item.count }))
      }]
    })
  }

  if (scoreChartRef.value && reports.value.length) {
    const data = reports.value.slice(0, 8).reverse()
    chartFor(scoreChartRef.value).setOption({
      color: [token('--workspace-action', '#2563eb')], tooltip: { trigger: 'axis', ...tooltipColors }, grid: { left: 36, right: 16, top: 24, bottom: 48 },
      xAxis: { type: 'category', data: data.map((item) => item.companyName || `#${item.reportId}`), axisLabel: { interval: 0, rotate: 24, color: labelColor } },
      yAxis: { type: 'value', max: 100, axisLabel: { color: labelColor } },
      series: [{ type: 'line', smooth: true, symbolSize: 8, areaStyle: { opacity: 0.12 }, data: data.map((item) => Number(item.matchScore || 0)) }]
    })
  }
}

function scoreTagType(score: number) {
  if (score >= 85) return 'success'
  if (score >= 70) return 'primary'
  if (score >= 60) return 'warning'
  return 'danger'
}

function scoreLabel(score: number) {
  if (score >= 85) return '高匹配'
  if (score >= 70) return '较匹配'
  if (score >= 60) return '可尝试'
  return '需优化'
}

onMounted(() => {
  chartTheme.observe(document.documentElement, {
    attributes: true, attributeFilter: ['data-workspace-tone', 'data-workspace-accent']
  })
  void loadDashboard()
})
onBeforeUnmount(() => {
  chartTheme.disconnect()
  chartResize?.disconnect()
  for (const chart of charts.values()) chart.dispose()
  charts.clear()
  dashboardRequest += 1
  scheduleRequest += 1
})
</script>

<style scoped>
.dashboard-heading h1 {
  margin: 0 0 var(--space-4);
  color: var(--color-text);
  font-size: clamp(25px, 3vw, 36px);
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.dashboard-focus {
  display: grid;
  justify-items: start;
  gap: var(--space-4);
  padding: clamp(16px, 3vw, 30px);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.dashboard-focus__label {
  display: inline-flex;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  color: var(--color-primary-hover);
  font-size: 13px;
  font-weight: 700;
}

.dashboard-focus__copy strong {
  display: block;
  margin-top: var(--space-2);
  color: var(--color-text);
  font-size: clamp(24px, 3vw, 36px);
  line-height: 1.2;
}

.dashboard-focus__copy p {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.55;
}

.dashboard-primary {
  min-height: 44px;
  font-weight: 700;
}

.dashboard-workspace,
.recent-results {
  display: grid;
  min-width: 0;
  gap: var(--space-4);
}

.dashboard-workspace .stat-grid,
.dashboard-workspace .panel {
  margin-bottom: 0;
}

.dashboard-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid color-mix(in srgb, var(--color-danger) 28%, var(--color-border));
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--color-danger) 7%, var(--color-surface));
}

.dashboard-error strong,
.dashboard-error span {
  display: block;
}

.dashboard-error span {
  margin-top: var(--space-1);
  color: var(--color-text-muted);
  font-size: 13px;
}

.journey-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.journey-heading h2,
.recent-results h2 {
  margin: 0;
  color: var(--color-text);
  font-size: 20px;
}

.journey-progress__summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
  color: var(--color-text-muted);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.journey-progress__summary progress {
  width: 88px;
  height: 8px;
  accent-color: var(--color-primary-hover);
}

.journey-progress__placeholder {
  width: 100px;
}

.journey-steps,
.journey-loading {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-2);
  margin: var(--space-4) 0 0;
  padding: 0;
  list-style: none;
}

.journey-loading .el-skeleton {
  min-width: 0;
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
}

.journey-step button {
  display: grid;
  width: 100%;
  height: 100%;
  grid-template-columns: 26px minmax(0, 1fr) auto;
  gap: var(--space-2);
  align-items: center;
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color var(--motion-fast), background var(--motion-fast);
}

.journey-step button:hover {
  border-color: var(--color-primary-hover);
}

.journey-step button:focus-visible,
.dashboard-focus button:focus-visible {
  outline: 2px solid var(--color-primary-hover);
  outline-offset: 2px;
}

.journey-step[aria-current="step"] button {
  border-color: var(--color-primary-hover);
  background: color-mix(in srgb, var(--color-primary-soft) 44%, var(--color-surface));
}

.journey-step__marker {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: 50%;
  background: var(--color-surface);
  color: var(--color-text-muted);
  font-size: 13px;
  font-weight: 700;
}

.journey-step--complete .journey-step__marker {
  color: var(--color-success);
  font-size: 18px;
}

.journey-step[aria-current="step"] .journey-step__marker {
  background: var(--color-primary-hover);
  color: var(--color-surface);
}

.journey-step__copy strong,
.journey-step__copy small {
  display: block;
}

.journey-step__copy small,
.journey-step__status,
.journey-step__arrow {
  color: var(--color-text-muted);
  font-size: 12px;
}

.journey-step__copy small {
  margin-top: var(--space-1);
}

.journey-step__status {
  grid-column: 2;
}

.journey-step[aria-current="step"] .journey-step__status {
  color: var(--color-primary-hover);
  font-weight: 700;
}

.journey-step__arrow {
  grid-area: 1 / 3;
}

.journey-inline-error,
.journey-inline-status {
  margin-top: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
}

.journey-inline-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  border-color: color-mix(in srgb, var(--color-warning) 28%, var(--color-border));
  background: color-mix(in srgb, var(--color-warning) 7%, var(--color-surface));
}

.application-list, .report-list { display: grid; }
.application-card, .report-card { display: flex; min-width: 0; align-items: center; justify-content: space-between; gap: var(--space-3); padding: var(--space-3) 0; border-block-start: 1px solid var(--color-border); }
.result-copy { min-width: 0; }
.result-copy strong, .result-copy span { display: block; overflow-wrap: anywhere; }
.result-copy strong { color: var(--color-text); }
.result-copy span { margin-top: var(--space-1); color: var(--color-text-muted); font-size: 13px; }
.report-score { display: grid; width: 170px; flex: 0 0 170px; gap: var(--space-2); }
.status-overview { display: grid; grid-template-columns: minmax(180px, 1fr) 180px; gap: var(--space-4); align-items: center; }
.chart.compact { height: 220px; }
.status-list { display: grid; gap: var(--space-2); }
.status-row { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); }

@media (max-width: 1100px) {
  .journey-steps,
  .journey-loading {
    grid-template-columns: 1fr;
  }

  .journey-step button {
    grid-template-columns: 26px minmax(0, 1fr) auto auto;
  }

  .journey-step__status {
    grid-column: auto;
  }

  .journey-step__arrow {
    grid-area: auto;
  }
}

@media (max-width: 900px) {
  .status-overview {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .dashboard-focus {
    padding: var(--space-3);
  }

  .dashboard-error,
  .journey-inline-error,
  .application-card,
  .report-card {
    align-items: stretch;
    flex-direction: column;
  }

  .journey-step button {
    padding-block: var(--space-2);
  }

  .dashboard-workspace .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--space-2);
  }

  .dashboard-workspace .stat-card {
    padding: var(--space-3);
  }

  .report-score {
    width: 100%;
    flex-basis: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .journey-step button {
    transition: none;
  }
}
.dashboard-heading h1 { margin: 0 0 5px; font-size: clamp(32px, 4vw, 46px); line-height: 1.25; letter-spacing: -.045em; }
.dashboard-heading p { margin: 0; color: var(--color-text-muted); }
.dashboard-overview { display: grid; grid-template-columns: minmax(0, 2.2fr) minmax(0, 1fr); gap: 30px 34px; margin-top: 34px; }
.dashboard-overview section { min-width: 0; }
.dashboard-overview h2 { margin: 0 0 13px; color: var(--color-text); line-height: 1.35; font-size: 21px; }
.dashboard-focus, .dashboard-schedule { min-height: 210px; border-radius: 19px; border: 1px solid var(--color-border); }
.dashboard-focus { align-content: center; position: relative; isolation: isolate; overflow: hidden; background: var(--workspace-feature); }
.dashboard-focus__copy strong { font-size: clamp(22px, 2.4vw, 28px); }
.dashboard-focus__copy p { margin-top: 7px; }
.dashboard-primary { background: var(--workspace-action); color: white; }
.dashboard-primary:hover { background: color-mix(in srgb, var(--workspace-action) 86%, black); }
:global([data-workspace-preset=clario] .dashboard-focus::after) {
  content: ''; position: absolute; inset: 0 0 0 52%; z-index: -1; pointer-events: none;
  background: repeating-radial-gradient(ellipse at 108% 60%, transparent 0 14px, color-mix(in srgb, var(--workspace-action) 22%, transparent) 15px, transparent 17px 25px);
  mask-image: linear-gradient(to right, transparent, black);
}
:global([data-workspace-preset=liquid] .user-app-shell :is(.dashboard-focus, .dashboard-schedule)) {
  background: var(--workspace-glass); border-color: var(--workspace-glass-edge); box-shadow: var(--workspace-glass-shadow);
}
@supports (backdrop-filter: blur(1px)) {
  :global([data-workspace-preset=liquid] .user-app-shell :is(.dashboard-focus, .dashboard-schedule)) {
    background: var(--workspace-glass-tint); backdrop-filter: blur(17px) saturate(110%);
  }
}
.dashboard-schedule { padding: 10px 15px; background: var(--color-surface); }
.dashboard-schedule p, .dashboard-continue p { color: var(--color-text-muted); }
.dashboard-schedule-row, .dashboard-continue-row {
  display: grid; grid-template-columns: 20px minmax(0, 1fr) 16px; align-items: center; gap: 9px;
  min-height: 55px; padding-block: 9px; border-bottom: 1px solid var(--color-border); color: var(--color-text);
}
.dashboard-schedule-row strong, .dashboard-continue-row strong { display: block; overflow-wrap: anywhere; }
.dashboard-schedule-row time, .dashboard-continue-row small { font-size: 12px; color: var(--color-text-muted); }
.dashboard-schedule-link { display: inline-block; margin-top: 10px; color: var(--color-primary-hover); }
.dashboard-continue-row { grid-template-columns: 39px minmax(0, 1fr) 16px; min-height: 65px; }
.dashboard-continue-row:first-of-type { border-top: 1px solid var(--color-border); }
.dashboard-continue-row:hover, .dashboard-schedule-row:hover { color: var(--color-primary-hover); }
.dashboard-document-icon { display: grid; place-items: center; height: 38px; border: 1px solid var(--color-border); border-radius: 9px; background: var(--color-surface); }
@media (max-width: 800px) {
  .dashboard-overview { grid-template-columns: minmax(0, 1fr); gap: 22px; margin-top: 22px; }
  .dashboard-schedule { min-height: 95px; }
}

.dashboard-schedule-row { grid-template-columns: 44px minmax(0, 1fr) 16px; }
.dashboard-schedule-entry { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 6px; align-items: center; }
.dashboard-schedule-prepare {
  display: inline-flex; min-height: 44px; align-items: center; justify-content: center;
  padding-inline: 6px; font-size: 12px; color: var(--color-primary); text-decoration: underline;
  text-underline-offset: 3px; white-space: nowrap;
}
.dashboard-date-box { display: grid; min-height: 42px; place-content: center; text-align: center; border: 1px solid var(--color-border); border-radius: 8px; background: var(--color-surface-muted); }
.dashboard-date-box small { font-size: 11px; color: var(--color-text-muted); }
.dashboard-date-box b { line-height: 1.2; font-variant-numeric: tabular-nums; }
:global([data-workspace-preset=clario] .dashboard-schedule) { border-color: transparent; border-top-color: var(--color-border); border-radius: 0; background: transparent; padding-inline: 0; }
</style>
