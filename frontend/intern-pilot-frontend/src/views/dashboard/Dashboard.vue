<template>
  <PageContainer title="数据看板" description="汇总求职资料、AI 分析和投递进度，快速进入下一步操作。">
    <section class="dashboard-hero">
      <div>
        <span class="hero-kicker">InternPilot 工作台</span>
        <h2>{{ greeting }}，{{ displayName }}</h2>
        <p>继续完善你的求职工作台。上传简历、维护岗位 JD，再用 AI 生成匹配分析和面试准备材料。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" :icon="MagicStick" @click="router.push('/analysis/match')">开始 AI 匹配</el-button>
        <el-button :icon="Upload" @click="router.push('/resumes')">上传简历</el-button>
      </div>
    </section>

    <div v-loading="loading" class="stat-grid">
      <StatCard label="简历数量" :value="summary.resumes" :icon="Document" />
      <StatCard label="岗位数量" :value="summary.jobs" :icon="Briefcase" />
      <StatCard label="分析报告" :value="summary.reports" :icon="Tickets" />
      <StatCard label="投递记录" :value="summary.applications" :icon="List" />
    </div>

    <div class="dashboard-grid action-grid">
      <section class="panel">
        <div class="panel-header">
          <div>
            <h3>求职准备进度</h3>
            <span>按完整求职流程检查当前准备情况</span>
          </div>
          <el-progress type="circle" :width="78" :percentage="preparationPercent" />
        </div>
        <div class="progress-checklist">
          <div v-for="item in preparationSteps" :key="item.label" class="progress-item" :class="{ done: item.done }">
            <el-icon>
              <component :is="item.done ? CircleCheckFilled : CircleCloseFilled" />
            </el-icon>
            <div>
              <strong>{{ item.label }}</strong>
              <span>{{ item.description }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h3>快捷入口</h3>
            <span>把答辩演示和日常操作的关键动作放在首页</span>
          </div>
        </div>
        <div class="quick-actions">
          <button v-for="action in quickActions" :key="action.label" class="quick-action" type="button" @click="router.push(action.path)">
            <el-icon><component :is="action.icon" /></el-icon>
            <span>{{ action.label }}</span>
            <small>{{ action.hint }}</small>
          </button>
        </div>
      </section>
    </div>

    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-header">
          <div>
            <h3>最近 AI 分析报告</h3>
            <span>优先查看最新匹配结果和优化建议</span>
          </div>
          <el-button link type="primary" @click="router.push('/analysis/reports')">查看全部</el-button>
        </div>
        <div v-if="recentReports.length" class="report-list">
          <article v-for="item in recentReports" :key="item.reportId" class="report-card">
            <div>
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
          v-else
          title="暂无分析报告"
          description="完成一次 AI 匹配后，这里会展示最近报告"
          hint="答辩演示时可从 AI 分析页发起一轮完整流程。"
        >
          <el-button type="primary" @click="router.push('/analysis/match')">开始分析</el-button>
        </AppEmpty>
      </section>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h3>投递状态概览</h3>
            <span>查看当前投递记录的分布和待跟进情况</span>
          </div>
        </div>
        <div v-if="applications.length" class="status-overview">
          <div ref="statusChartRef" class="chart compact"></div>
          <div class="status-list">
            <div v-for="item in statusSummary" :key="item.status" class="status-row">
              <el-tag :type="statusTypes[item.status]" effect="plain">{{ statusLabels[item.status] || item.status }}</el-tag>
              <strong>{{ item.count }}</strong>
            </div>
          </div>
        </div>
        <AppEmpty
          v-else
          title="暂无投递动态"
          description="创建投递记录后，这里会展示最近进展"
          hint="可用于演示从岗位管理到投递跟踪的闭环。"
        >
          <el-button @click="router.push('/applications')">管理投递</el-button>
        </AppEmpty>
      </section>
    </div>

    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-header">
          <div>
            <h3>最近投递记录</h3>
            <span>把投递状态当成任务列表持续跟进</span>
          </div>
          <el-button link type="primary" @click="router.push('/applications')">查看全部</el-button>
        </div>
        <div v-if="recentApplications.length" class="application-list">
          <article v-for="item in recentApplications" :key="item.applicationId" class="application-card">
            <div>
              <strong>{{ item.companyName || '未知公司' }} - {{ item.jobTitle || '未知岗位' }}</strong>
              <span>{{ item.interviewDate ? `面试时间：${formatDateTime(item.interviewDate)}` : item.note || '暂无备注' }}</span>
            </div>
            <el-tag :type="statusTypes[item.status]" effect="plain">{{ statusLabels[item.status] || item.status }}</el-tag>
          </article>
        </div>
        <AppEmpty v-else title="暂无投递记录" description="记录投递状态、面试时间和复盘内容" />
      </section>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h3>匹配分趋势</h3>
            <span>最近报告的匹配分，帮助判断简历优化效果</span>
          </div>
        </div>
        <div v-if="reports.length" ref="scoreChartRef" class="chart"></div>
        <AppEmpty v-else title="暂无匹配分数据" description="生成分析报告后会展示匹配分趋势" />
      </section>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import {
  Briefcase,
  CircleCheckFilled,
  CircleCloseFilled,
  DataAnalysis,
  Document,
  List,
  MagicStick,
  QuestionFilled,
  Tickets,
  Upload
} from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatCard from '@/components/common/StatCard.vue'
import router from '@/router'
import { getApplicationListApi } from '@/api/application'
import { getAnalysisReportsApi } from '@/api/analysis'
import { getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime, statusLabels, statusTypes } from '@/utils/format'

const auth = useAuthStore()
const summary = reactive({ resumes: 0, jobs: 0, reports: 0, applications: 0 })
const reports = ref<any[]>([])
const applications = ref<any[]>([])
const loading = ref(false)
const statusChartRef = ref<HTMLDivElement>()
const scoreChartRef = ref<HTMLDivElement>()

const displayName = computed(() => auth.user?.nickname || auth.user?.username || auth.user?.email || '同学')
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const recentReports = computed(() => reports.value.slice(0, 4))
const recentApplications = computed(() => applications.value.slice(0, 5))
const statusSummary = computed(() => {
  const counts = applications.value.reduce<Record<string, number>>((acc, item) => {
    acc[item.status] = (acc[item.status] || 0) + 1
    return acc
  }, {})
  return Object.entries(counts).map(([status, count]) => ({ status, count }))
})
const preparationSteps = computed(() => [
  { label: '简历上传', description: '至少维护一份可用于分析的简历', done: summary.resumes > 0 },
  { label: '岗位维护', description: '录入目标岗位 JD，作为 AI 分析输入', done: summary.jobs > 0 },
  { label: 'AI 匹配分析', description: '生成匹配分、优势短板和优化建议', done: summary.reports > 0 },
  { label: '投递跟踪', description: '记录投递状态、面试时间和复盘', done: summary.applications > 0 }
])
const preparationPercent = computed(() => {
  const done = preparationSteps.value.filter((item) => item.done).length
  return Math.round((done / preparationSteps.value.length) * 100)
})

const quickActions = [
  { label: '上传简历', hint: '准备 AI 分析材料', icon: Upload, path: '/resumes' },
  { label: '新建岗位', hint: '维护目标 JD', icon: Briefcase, path: '/jobs' },
  { label: '开始分析', hint: '生成匹配报告', icon: MagicStick, path: '/analysis/match' },
  { label: '生成面试题', hint: '定制练习题库', icon: QuestionFilled, path: '/interview-questions' },
  { label: '投递记录', hint: '跟踪进展复盘', icon: List, path: '/applications' }
]

async function loadDashboard() {
  loading.value = true
  try {
    const [resumeRes, jobRes, reportRes, appRes]: any[] = await Promise.all([
      getResumeListApi({ pageNum: 1, pageSize: 100 }),
      getJobListApi({ pageNum: 1, pageSize: 100 }),
      getAnalysisReportsApi({ pageNum: 1, pageSize: 100 }),
      getApplicationListApi({ pageNum: 1, pageSize: 100 })
    ])

    summary.resumes = resumeRes.total ?? resumeRes.records?.length ?? 0
    summary.jobs = jobRes.total ?? jobRes.records?.length ?? 0
    summary.reports = reportRes.total ?? reportRes.records?.length ?? 0
    summary.applications = appRes.total ?? appRes.records?.length ?? 0
    reports.value = reportRes.records || []
    applications.value = appRes.records || []
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  if (statusChartRef.value && applications.value.length) {
    echarts.init(statusChartRef.value).setOption({
      color: ['#2563eb', '#16a34a', '#f59e0b', '#dc2626', '#64748b'],
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['52%', '74%'],
          label: { show: false },
          data: statusSummary.value.map((item) => ({
            name: statusLabels[item.status] || item.status,
            value: item.count
          }))
        }
      ]
    })
  }

  if (scoreChartRef.value && reports.value.length) {
    const data = reports.value.slice(0, 8).reverse()
    echarts.init(scoreChartRef.value).setOption({
      color: ['#2563eb'],
      tooltip: { trigger: 'axis' },
      grid: { left: 36, right: 16, top: 24, bottom: 48 },
      xAxis: {
        type: 'category',
        data: data.map((item) => item.companyName || `#${item.reportId}`),
        axisLabel: { interval: 0, rotate: 24 }
      },
      yAxis: { type: 'value', max: 100 },
      series: [
        {
          type: 'line',
          smooth: true,
          symbolSize: 8,
          areaStyle: { opacity: 0.12 },
          data: data.map((item) => Number(item.matchScore || 0))
        }
      ]
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

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
  padding: 26px;
  border: 1px solid var(--color-primary-border);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.12), transparent 46%),
    var(--color-surface);
  box-shadow: var(--shadow-card);
}

.dashboard-hero h2 {
  margin: 8px 0 10px;
  font-size: 26px;
}

.dashboard-hero p {
  max-width: 640px;
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.hero-kicker {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 700;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.action-grid {
  align-items: stretch;
}

.progress-checklist,
.application-list,
.report-list {
  display: grid;
  gap: 12px;
}

.progress-item,
.application-card,
.report-card {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
}

.progress-item {
  justify-content: flex-start;
}

.progress-item .el-icon {
  color: var(--color-text-soft);
}

.progress-item.done .el-icon {
  color: var(--color-success);
}

.progress-item strong,
.application-card strong,
.report-card strong {
  display: block;
  color: var(--color-text);
}

.progress-item span,
.application-card span,
.report-card span {
  display: block;
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 13px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-action {
  display: grid;
  min-height: 86px;
  gap: 6px;
  justify-items: start;
  padding: 14px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
  color: var(--color-text);
  text-align: left;
  cursor: pointer;
}

.quick-action:hover {
  border-color: var(--color-primary-border);
  background: var(--color-primary-soft);
}

.quick-action .el-icon {
  color: var(--color-primary);
  font-size: 20px;
}

.quick-action span {
  font-weight: 700;
}

.quick-action small {
  color: var(--color-text-muted);
}

.report-score {
  display: grid;
  width: 170px;
  gap: 8px;
}

.status-overview {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 180px;
  gap: 14px;
  align-items: center;
}

.chart.compact {
  height: 220px;
}

.status-list {
  display: grid;
  gap: 10px;
}

.status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 900px) {
  .dashboard-hero,
  .progress-item,
  .application-card,
  .report-card,
  .status-overview {
    align-items: stretch;
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }

  .report-score {
    width: 100%;
  }
}
</style>
