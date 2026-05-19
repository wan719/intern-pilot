<template>
  <PageContainer title="" description="查看简历与岗位 JD 的 AI 匹配结果，辅助判断投递优先级和简历优化方向。">
    <template #actions>
      <el-button type="primary" :icon="MagicStick" @click="router.push('/analysis/match')">开始 AI 分析</el-button>
    </template>

    <div v-loading="loading" class="report-summary-grid">
      <StatCard label="报告总数" :value="reportStats.total" :icon="Files" />
      <StatCard label="平均匹配分" :value="reportStats.avgScore" :icon="TrendCharts" />
      <StatCard label="高匹配岗位" :value="reportStats.highCount" :icon="CircleCheck" />
      <StatCard label="最近分析" :value="reportStats.latestText" :icon="Clock" />
    </div>

    <section class="panel toolbar">
      <el-segmented v-model="scoreFilter" :options="scoreFilterOptions" @change="applyScoreFilter" />
      <el-input-number v-model="query.minScore" :min="0" :max="100" placeholder="最低分" />
      <el-button type="primary" @click="loadReports">筛选</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </section>

    <section v-loading="loading" class="report-library">
      <el-alert
        v-if="loadError"
        class="report-alert"
        type="error"
        show-icon
        :closable="false"
        :title="loadError"
      >
        <el-button size="small" type="primary" @click="loadReports">重试</el-button>
      </el-alert>

      <AppEmpty
        v-else-if="!reports.length && !loading"
        title="还没有分析报告"
        description="选择一份简历和目标岗位，生成第一份 AI 匹配分析报告。"
        hint="报告会展示匹配分、优势短板、缺失技能和优化建议。"
      >
        <el-button type="primary" :icon="MagicStick" @click="router.push('/analysis/match')">开始 AI 分析</el-button>
      </AppEmpty>

      <article v-for="item in reports" v-else :key="item.reportId" class="report-card">
        <div class="score-panel" :class="scoreClass(item.matchScore)">
          <strong>{{ normalizedScore(item.matchScore) }}</strong>
          <span>{{ scoreLabel(item.matchScore) }}</span>
        </div>

        <div class="report-card-main">
          <div class="report-card-heading">
            <div>
              <span class="company-name">{{ item.companyName || '未知公司' }}</span>
              <h3>{{ item.jobTitle || '未知岗位' }}</h3>
            </div>
            <div class="report-badges">
              <el-tag :type="scoreTagType(item.matchScore)" effect="plain">{{ deliveryAdvice(item.matchScore) }}</el-tag>
              <el-tag v-if="item.cacheHit" type="info" effect="plain">缓存命中</el-tag>
            </div>
          </div>

          <div class="report-meta">
            <span>{{ item.resumeName || '简历名称未返回' }}</span>
            <span>{{ item.matchLevel || scoreLabel(item.matchScore) }}</span>
            <span>{{ formatDateTime(item.createdAt) }}</span>
          </div>

          <div class="insight-preview">
            <div class="insight-box good">
              <strong>优势摘要</strong>
              <ul>
                <li v-for="text in previewList(item.strengths, '暂无优势摘要，请进入详情查看完整报告。')" :key="text">{{ text }}</li>
              </ul>
            </div>
            <div class="insight-box risk">
              <strong>风险短板</strong>
              <ul>
                <li v-for="text in previewList(item.weaknesses, '暂无短板摘要，请进入详情查看完整报告。')" :key="text">{{ text }}</li>
              </ul>
            </div>
          </div>
        </div>

        <div class="report-actions">
          <el-button type="primary" @click="openDetail(item.reportId)">查看详情</el-button>
          <el-button @click="goInterviewQuestions(item)">生成面试题</el-button>
          <el-button
            v-if="authStore.hasPermission('analysis:delete')"
            type="danger"
            plain
            :loading="deletingId === item.reportId"
            @click="handleDelete(item)"
          >
            删除
          </el-button>
        </div>
      </article>
    </section>

    <el-drawer v-model="detailVisible" title="分析报告详情" size="62%">
      <section v-if="detailLoading" class="panel flat">
        <el-skeleton :rows="8" animated />
      </section>

      <section v-else-if="detailError" class="panel flat">
        <el-result icon="error" title="报告加载失败" :sub-title="detailError">
          <template #extra>
            <el-button type="primary" @click="detail?.reportId && openDetail(detail.reportId)">重试</el-button>
          </template>
        </el-result>
      </section>

      <div v-else-if="detail" class="detail-stack">
        <section class="report-hero">
          <div>
            <span class="company-name">{{ detail.companyName || '未知公司' }}</span>
            <h2>{{ detail.jobTitle || '未知岗位' }}</h2>
            <div class="report-meta">
              <span>{{ detail.resumeName || '简历名称未返回' }}</span>
              <span>{{ formatDateTime(detail.createdAt) }}</span>
              <span>{{ detail.aiProvider || 'AI' }} / {{ detail.aiModel || '模型未返回' }}</span>
            </div>
          </div>
          <div class="detail-score" :class="scoreClass(detail.matchScore)">
            <strong>{{ normalizedScore(detail.matchScore) }}</strong>
            <span>{{ detail.matchLevel || scoreLabel(detail.matchScore) }}</span>
          </div>
        </section>

        <section class="panel flat conclusion-panel">
          <div class="panel-header">
            <h3>综合结论</h3>
            <el-tag :type="scoreTagType(detail.matchScore)" effect="plain">{{ deliveryAdvice(detail.matchScore) }}</el-tag>
          </div>
          <p>{{ conclusionText(detail) }}</p>
          <div class="detail-actions">
            <el-button type="primary" :icon="MagicStick" @click="router.push('/interview-questions')">生成面试题</el-button>
            <el-button @click="router.push('/analysis/match')">重新分析</el-button>
          </div>
        </section>

        <section class="panel flat">
          <div class="panel-header">
            <h3>匹配维度</h3>
            <span>基于报告分数、优势短板和缺失技能推导的展示视图</span>
          </div>
          <div class="dimension-list">
            <div v-for="item in dimensionScores(detail)" :key="item.label" class="dimension-row">
              <span>{{ item.label }}</span>
              <el-progress :percentage="item.score" :stroke-width="10" :color="scoreColor(item.score)" />
            </div>
          </div>
        </section>

        <section class="two-column">
          <div class="panel flat insight-detail good">
            <h3>优势</h3>
            <el-tag v-for="item in detail.strengths || []" :key="item" type="success" effect="plain">{{ item }}</el-tag>
            <span v-if="!detail.strengths?.length" class="empty-text">暂无优势摘要</span>
          </div>
          <div class="panel flat insight-detail risk">
            <h3>短板</h3>
            <el-tag v-for="item in detail.weaknesses || []" :key="item" type="warning" effect="plain">{{ item }}</el-tag>
            <span v-if="!detail.weaknesses?.length" class="empty-text">暂无短板摘要</span>
          </div>
        </section>

        <section class="panel flat">
          <div class="panel-header">
            <h3>缺失技能</h3>
            <span>建议优先补充到简历关键词或面试准备清单中</span>
          </div>
          <div class="tag-row">
            <el-tag v-for="item in detail.missingSkills || []" :key="item" type="danger" effect="plain">{{ item }}</el-tag>
            <span v-if="!detail.missingSkills?.length" class="empty-text">暂无明显缺失技能</span>
          </div>
        </section>

        <section class="two-column">
          <div class="panel flat checklist-card">
            <h3>简历优化建议</h3>
            <ol>
              <li v-for="item in detail.suggestions || []" :key="item">{{ item }}</li>
            </ol>
            <span v-if="!detail.suggestions?.length" class="empty-text">暂无优化建议</span>
          </div>
          <div class="panel flat checklist-card">
            <h3>面试准备</h3>
            <ol>
              <li v-for="item in detail.interviewTips || []" :key="item">{{ item }}</li>
            </ol>
            <span v-if="!detail.interviewTips?.length" class="empty-text">暂无面试准备建议</span>
          </div>
        </section>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { CircleCheck, Clock, Files, MagicStick, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import StatCard from '@/components/common/StatCard.vue'
import router from '@/router'
import { deleteAnalysisReportApi, getAnalysisReportDetailApi, getAnalysisReportsApi } from '@/api/analysis'
import { formatDateTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const reports = ref<any[]>([])
const detail = ref<any>(null)
const detailVisible = ref(false)
const loading = ref(false)
const loadError = ref('')
const detailLoading = ref(false)
const detailError = ref('')
const deletingId = ref<number | null>(null)
const scoreFilter = ref('all')
const query = reactive<{ minScore?: number }>({})

const scoreFilterOptions = [
  { label: '全部', value: 'all' },
  { label: '高匹配', value: 'high' },
  { label: '可尝试', value: 'medium' },
  { label: '需优化', value: 'low' }
]

const reportStats = computed(() => {
  const totalScore = reports.value.reduce((sum, item) => sum + normalizedScore(item.matchScore), 0)
  const latest = reports.value[0]?.createdAt
  return {
    total: reports.value.length,
    avgScore: reports.value.length ? Math.round(totalScore / reports.value.length) : 0,
    highCount: reports.value.filter((item) => normalizedScore(item.matchScore) >= 80).length,
    latestText: latest ? formatDateTime(latest).slice(0, 10) : '-'
  }
})

async function loadReports() {
  loading.value = true
  loadError.value = ''
  try {
    const res: any = await getAnalysisReportsApi({ ...query, pageNum: 1, pageSize: 100 })
    reports.value = await hydrateReportDetails(res.records || [])
  } catch (e: any) {
    reports.value = []
    loadError.value = e?.message || e?.response?.data?.message || '分析报告加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

async function hydrateReportDetails(records: any[]) {
  return Promise.all(
    records.map(async (item) => {
      try {
        const detailData = (await getAnalysisReportDetailApi(item.reportId)) as any
        return { ...item, ...detailData }
      } catch {
        return item
      }
    })
  )
}

async function openDetail(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = { reportId: id }
  try {
    detail.value = await getAnalysisReportDetailApi(id)
  } catch (e: any) {
    detailError.value = e?.message || e?.response?.data?.message || '报告不存在、已被删除，或当前账号没有访问权限。'
  } finally {
    detailLoading.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除“${row.companyName || '未知公司'} - ${row.jobTitle || '未知岗位'}”的分析报告吗？删除后不可恢复。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  deletingId.value = row.reportId
  try {
    await deleteAnalysisReportApi(row.reportId)
    ElMessage.success('删除成功')
    await loadReports()
  } catch (e: any) {
    ElMessage.error(e?.message || e?.response?.data?.message || '删除失败，请稍后重试')
  } finally {
    deletingId.value = null
  }
}

function resetQuery() {
  scoreFilter.value = 'all'
  query.minScore = undefined
  loadReports()
}

function applyScoreFilter(value: string) {
  if (value === 'high') query.minScore = 80
  else if (value === 'medium') query.minScore = 60
  else if (value === 'low') query.minScore = 0
  else query.minScore = undefined
  loadReports()
}

function goInterviewQuestions(row: any) {
  router.push(`/interview-questions?reportId=${row.reportId}`)
}

function normalizedScore(value?: number) {
  return Math.max(0, Math.min(100, Number(value || 0)))
}

function scoreLabel(value?: number) {
  const score = normalizedScore(value)
  if (score >= 80) return '高匹配'
  if (score >= 60) return '可尝试'
  return '需优化'
}

function deliveryAdvice(value?: number) {
  const score = normalizedScore(value)
  if (score >= 80) return '建议投递'
  if (score >= 60) return '谨慎投递'
  return '先优化简历'
}

function scoreTagType(value?: number) {
  const score = normalizedScore(value)
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

function scoreClass(value?: number) {
  const score = normalizedScore(value)
  if (score >= 80) return 'high'
  if (score >= 60) return 'medium'
  return 'low'
}

function scoreColor(value?: number) {
  const score = normalizedScore(value)
  if (score >= 80) return '#16a34a'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

function previewList(items?: string[], fallback = '暂无摘要') {
  const list = (items || []).filter(Boolean).slice(0, 3)
  return list.length ? list : [fallback]
}

function conclusionText(report: any) {
  const score = normalizedScore(report.matchScore)
  const company = report.companyName || '该公司'
  const job = report.jobTitle || '该岗位'
  if (score >= 80) {
    return `与 ${company} 的 ${job} 匹配度较高，建议优先投递，并围绕优势技能和项目经历准备面试表达。`
  }
  if (score >= 60) {
    return `与 ${company} 的 ${job} 具备一定匹配基础，建议先补齐关键短板，再根据岗位要求有选择地投递。`
  }
  return `当前简历与 ${company} 的 ${job} 匹配度偏低，建议优先优化简历关键词、项目描述和缺失技能后再投递。`
}

function dimensionScores(report: any) {
  const base = normalizedScore(report.matchScore)
  const strengths = report.strengths?.length || 0
  const weaknesses = report.weaknesses?.length || 0
  const missing = report.missingSkills?.length || 0
  return [
    { label: '技能匹配', score: clamp(base - missing * 5 + strengths * 2) },
    { label: '项目匹配', score: clamp(base + strengths * 3 - weaknesses * 2) },
    { label: '经验匹配', score: clamp(base - weaknesses * 3) },
    { label: '关键词覆盖', score: clamp(base - missing * 6) },
    { label: '综合表达', score: base }
  ]
}

function clamp(value: number) {
  return Math.max(0, Math.min(100, Math.round(value)))
}

onMounted(loadReports)
</script>

<style scoped>
.report-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 20px;
}

.report-alert {
  margin-bottom: 16px;
}

.report-library {
  display: grid;
  gap: 14px;
}

.report-card {
  display: grid;
  grid-template-columns: 116px minmax(0, 1fr) auto;
  gap: 18px;
  padding: 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.score-panel,
.detail-score {
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 8px;
  text-align: center;
}

.score-panel {
  min-height: 116px;
}

.score-panel.high,
.detail-score.high {
  background: #ecfdf3;
  color: #047857;
}

.score-panel.medium,
.detail-score.medium {
  background: #fffbeb;
  color: #b45309;
}

.score-panel.low,
.detail-score.low {
  background: #fef2f2;
  color: #dc2626;
}

.score-panel strong,
.detail-score strong {
  font-size: 34px;
  line-height: 1;
}

.score-panel span,
.detail-score span {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 700;
}

.report-card-heading,
.report-hero,
.panel-header {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  justify-content: space-between;
}

.company-name {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
}

.report-card h3,
.report-hero h2 {
  margin: 6px 0 0;
}

.report-card h3 {
  font-size: 18px;
}

.report-badges,
.report-meta,
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.report-meta {
  margin: 12px 0;
  color: var(--color-text-soft);
  font-size: 13px;
}

.insight-preview,
.two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.insight-box {
  padding: 12px;
  border-radius: 8px;
}

.insight-box.good,
.insight-detail.good {
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
}

.insight-box.risk,
.insight-detail.risk {
  border: 1px solid #fde68a;
  background: #fffbeb;
}

.insight-box strong {
  display: block;
  margin-bottom: 8px;
}

.insight-box ul,
.checklist-card ol {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text-muted);
  line-height: 1.8;
}

.report-actions {
  display: flex;
  width: 132px;
  flex-direction: column;
  gap: 8px;
}

.report-actions .el-button {
  width: 100%;
  margin-left: 0;
}

.detail-stack {
  display: grid;
  gap: 16px;
}

.report-hero {
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.detail-score {
  min-width: 128px;
  min-height: 108px;
  padding: 12px;
}

.conclusion-panel p {
  margin: 12px 0 0;
  color: var(--color-text-muted);
  line-height: 1.8;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.dimension-list {
  display: grid;
  gap: 12px;
}

.dimension-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.dimension-row span {
  color: var(--color-text-muted);
  font-size: 13px;
}

.insight-detail {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-content: flex-start;
}

.insight-detail h3,
.checklist-card h3 {
  width: 100%;
  margin: 0 0 8px;
}

.empty-text {
  color: var(--color-text-soft);
  font-size: 13px;
}

@media (max-width: 900px) {
  .report-summary-grid,
  .report-card,
  .insight-preview,
  .two-column {
    grid-template-columns: 1fr;
  }

  .report-actions {
    width: 100%;
  }

  .report-hero,
  .panel-header {
    flex-direction: column;
  }
}
</style>
