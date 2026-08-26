<template>
  <PageContainer title="" width="wide">
    <template #hero>
      <PageHero
        eyebrow="AI 分析报告"
        title="把匹配结果变成下一步行动"
        description="先看结论与风险，再决定投递、优化简历或进入面试准备。每份报告都保留生成时的岗位与简历上下文。"
      >
        <template #actions>
          <el-button type="primary" :icon="MagicStick" @click="router.push('/analysis/match')">开始 AI 分析</el-button>
        </template>
      </PageHero>
    </template>

    <div class="report-summary-grid">
      <StatCard label="报告总数" :value="reportStats.total" :icon="Files" :loading="loading" />
      <StatCard label="平均匹配分" :value="reportStats.avgScore" :icon="TrendCharts" :loading="loading" />
      <StatCard label="高匹配岗位" :value="reportStats.highCount" :icon="CircleCheck" :loading="loading" />
      <StatCard label="最近分析" :value="reportStats.latestText" :icon="Clock" :loading="loading" />
    </div>

    <FilterBar class="report-filter" @reset="resetQuery">
      <template #filters>
        <el-segmented v-model="scoreFilter" :options="scoreFilterOptions" @change="applyScoreFilter" />
        <label class="score-input">
          <span>最低匹配分</span>
          <el-input-number v-model="query.minScore" :min="0" :max="100" placeholder="不限" />
        </label>
      </template>
      <template #actions>
        <el-button type="primary" @click="loadReports">应用筛选</el-button>
      </template>
    </FilterBar>

    <section class="report-library" aria-label="分析报告列表" :aria-busy="loading">
      <div v-if="loading" class="report-loading" data-report-loading role="status" aria-live="polite">
        <article v-for="index in 2" :key="index" class="report-card report-card--skeleton">
          <el-skeleton :rows="6" animated />
        </article>
        <span class="sr-only">正在加载分析报告</span>
      </div>

      <section v-else-if="loadError" class="report-error" role="alert">
        <div>
          <strong>分析报告暂时无法加载</strong>
          <p>{{ loadError }}</p>
        </div>
        <el-button type="primary" data-report-retry @click="loadReports">重新加载</el-button>
      </section>

      <AppEmpty
        v-else-if="!reports.length"
        title="还没有分析报告"
        description="选择一份简历和目标岗位，生成第一份可解释的 AI 匹配报告。"
        hint="报告会展示匹配等级、优势证据、风险短板和下一步行动。"
      >
        <el-button data-report-empty-action type="primary" :icon="MagicStick" @click="router.push('/analysis/match')">
          开始第一次匹配
        </el-button>
      </AppEmpty>

      <article v-for="item in reports" v-else :key="item.reportId" class="report-card" data-report-card>
        <div class="score-panel" :class="scoreClass(item.matchScore)" :aria-label="`匹配分 ${normalizedScore(item.matchScore)}，${item.matchLevel || scoreLabel(item.matchScore)}`">
          <span>匹配分</span>
          <strong>{{ normalizedScore(item.matchScore) }}</strong>
          <b>{{ item.matchLevel || scoreLabel(item.matchScore) }}</b>
        </div>

        <div class="report-card-main">
          <div class="report-card-heading">
            <div>
              <span class="company-name" :title="item.companyName || '未知公司'">{{ item.companyName || '未知公司' }}</span>
              <h2 :title="item.jobTitle || '未知岗位'">{{ item.jobTitle || '未知岗位' }}</h2>
            </div>
            <div class="report-badges">
              <el-tag :type="scoreTagType(item.matchScore)" effect="plain">{{ deliveryAdvice(item.matchScore) }}</el-tag>
              <el-tag v-if="item.cacheHit" type="info" effect="plain">缓存结果</el-tag>
            </div>
          </div>

          <dl class="report-meta">
            <div>
              <dt>分析简历</dt>
              <dd>{{ item.resumeName || '简历名称未返回' }}</dd>
            </div>
            <div>
              <dt>文本等级</dt>
              <dd>{{ item.matchLevel || scoreLabel(item.matchScore) }}</dd>
            </div>
            <div>
              <dt>生成时间</dt>
              <dd>{{ formatDateTime(item.createdAt) }}</dd>
            </div>
          </dl>

          <div class="insight-preview">
            <AiInsightPanel title="优势证据" tone="strength" data-report-strengths>
              <ul>
                <li v-for="text in previewList(item.strengths, '暂无优势摘要，请进入详情查看完整报告。')" :key="text">{{ text }}</li>
              </ul>
            </AiInsightPanel>
            <AiInsightPanel title="风险短板" tone="risk" data-report-risks>
              <ul>
                <li v-for="text in previewList(item.weaknesses, '暂无明显风险，请进入详情核对完整报告。')" :key="text">{{ text }}</li>
              </ul>
            </AiInsightPanel>
            <AiInsightPanel title="下一步行动" tone="action" data-report-guidance>
              <ol>
                <li v-for="text in previewActions(item)" :key="text">{{ text }}</li>
              </ol>
            </AiInsightPanel>
          </div>
        </div>

        <div class="responsive-actions" aria-label="报告操作">
          <el-button type="primary" @click="openDetail(item.reportId)">查看详情</el-button>
          <el-button @click="goInterviewQuestions(item)">生成面试题</el-button>
          <el-button :icon="Printer" @click="openPrintPage(item.reportId)">导出 PDF</el-button>
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

    <el-drawer v-model="detailVisible" title="分析报告详情" :size="detailDrawerSize">
      <section v-if="detailLoading" class="panel flat" role="status" aria-live="polite">
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
            <div class="detail-meta">
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
            <h2>综合结论</h2>
            <el-tag :type="scoreTagType(detail.matchScore)" effect="plain">{{ deliveryAdvice(detail.matchScore) }}</el-tag>
          </div>
          <p>{{ conclusionText(detail) }}</p>
          <div class="detail-actions">
            <el-button :icon="Printer" @click="openPrintPage(detail.reportId)">导出 PDF</el-button>
            <el-button type="primary" :icon="MagicStick" @click="router.push('/interview-questions')">生成面试题</el-button>
            <el-button @click="router.push('/analysis/match')">重新分析</el-button>
          </div>
        </section>

        <section class="panel flat">
          <div class="panel-header">
            <div>
              <h2>匹配维度</h2>
              <p>基于报告分数、优势短板和缺失技能推导的辅助视图。</p>
            </div>
          </div>
          <div class="dimension-list">
            <div v-for="item in dimensionScores(detail)" :key="item.label" class="dimension-row">
              <span>{{ item.label }}</span>
              <el-progress :percentage="item.score" :stroke-width="10" :color="scoreColor(item.score)" />
            </div>
          </div>
        </section>

        <section class="two-column">
          <AiInsightPanel title="优势" tone="strength">
            <ul>
              <li v-for="item in detail.strengths || []" :key="item">{{ item }}</li>
              <li v-if="!detail.strengths?.length">暂无优势摘要</li>
            </ul>
          </AiInsightPanel>
          <AiInsightPanel title="风险与短板" tone="risk">
            <ul>
              <li v-for="item in detail.weaknesses || []" :key="item">{{ item }}</li>
              <li v-if="!detail.weaknesses?.length">暂无短板摘要</li>
            </ul>
          </AiInsightPanel>
        </section>

        <section class="panel flat">
          <div class="panel-header">
            <div>
              <h2>缺失技能</h2>
              <p>建议优先补充到简历关键词或面试准备清单中。</p>
            </div>
          </div>
          <div class="tag-row">
            <el-tag v-for="item in detail.missingSkills || []" :key="item" type="danger" effect="plain">{{ item }}</el-tag>
            <span v-if="!detail.missingSkills?.length" class="empty-text">暂无明显缺失技能</span>
          </div>
        </section>

        <section class="two-column">
          <AiInsightPanel title="简历优化建议" tone="action">
            <ol>
              <li v-for="item in detail.suggestions || []" :key="item">{{ item }}</li>
              <li v-if="!detail.suggestions?.length">暂无优化建议</li>
            </ol>
          </AiInsightPanel>
          <AiInsightPanel title="面试准备" tone="action">
            <ol>
              <li v-for="item in detail.interviewTips || []" :key="item">{{ item }}</li>
              <li v-if="!detail.interviewTips?.length">暂无面试准备建议</li>
            </ol>
          </AiInsightPanel>
        </section>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { CircleCheck, Clock, Files, MagicStick, Printer, TrendCharts } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import PageContainer from '@/components/common/PageContainer.vue'
import PageHero from '@/components/common/PageHero.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import AiInsightPanel from '@/components/common/AiInsightPanel.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import StatCard from '@/components/common/StatCard.vue'
import router from '@/router'
import { deleteAnalysisReportApi, getAnalysisReportDetailApi, getAnalysisReportsApi } from '@/api/analysis'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const reports = ref<any[]>([])
const detail = ref<any>(null)
const detailVisible = ref(false)
const { responsiveDrawerSize } = useResponsiveSize()
const detailDrawerSize = responsiveDrawerSize('62%', '94%')
const loading = ref(false)
const loadError = ref('')
const detailLoading = ref(false)
const detailError = ref('')
const deletingId = ref<number | null>(null)
const scoreFilter = ref('all')
const query = reactive<{ minScore?: number }>({})
let reportRequestId = 0

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
  const requestId = ++reportRequestId
  loading.value = true
  loadError.value = ''
  try {
    const res: any = await getAnalysisReportsApi({ ...query, pageNum: 1, pageSize: 100 })
    const nextReports = await hydrateReportDetails(res.records || [])
    if (requestId === reportRequestId) reports.value = nextReports
  } catch (error: any) {
    if (requestId !== reportRequestId) return
    reports.value = []
    loadError.value = error?.message || error?.response?.data?.message || '分析报告加载失败，请稍后重试。'
  } finally {
    if (requestId === reportRequestId) loading.value = false
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
  } catch (error: any) {
    detailError.value = error?.message || error?.response?.data?.message || '报告不存在、已被删除，或当前账号没有访问权限。'
  } finally {
    detailLoading.value = false
  }
}

function openReportFromQuery() {
  const reportId = Number(route.query.reportId)
  if (Number.isFinite(reportId) && reportId > 0) openDetail(reportId)
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
  } catch (error: any) {
    ElMessage.error(error?.message || error?.response?.data?.message || '删除失败，请稍后重试')
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

function openPrintPage(reportId: number) {
  const href = router.resolve(`/analysis/reports/${reportId}/print`).href
  window.open(href, '_blank', 'noopener,noreferrer')
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
  if (score >= 80) return '#15803d'
  if (score >= 60) return '#b45309'
  return '#b91c1c'
}

function previewList(items?: string[], fallback = '暂无摘要') {
  const list = (items || []).filter(Boolean).slice(0, 2)
  return list.length ? list : [fallback]
}

function previewActions(report: any) {
  const actions = [...(report.suggestions || []), ...(report.interviewTips || [])].filter(Boolean).slice(0, 2)
  return actions.length ? actions : [deliveryAdvice(report.matchScore), '打开详情查看完整优化建议']
}

function conclusionText(report: any) {
  const score = normalizedScore(report.matchScore)
  const company = report.companyName || '该公司'
  const job = report.jobTitle || '该岗位'
  if (score >= 80) return `与 ${company} 的 ${job} 匹配度较高，建议优先投递，并围绕优势技能和项目经历准备面试表达。`
  if (score >= 60) return `与 ${company} 的 ${job} 具备一定匹配基础，建议先补齐关键短板，再根据岗位要求有选择地投递。`
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

watch(() => route.query.reportId, openReportFromQuery)

onMounted(async () => {
  await loadReports()
  openReportFromQuery()
})
</script>

<style scoped>
.report-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.report-filter {
  margin-bottom: var(--space-5);
}

.score-input {
  display: flex;
  gap: var(--space-2);
  align-items: center;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.report-library,
.report-loading {
  display: grid;
  gap: var(--space-4);
}

.report-error {
  display: flex;
  gap: var(--space-4);
  align-items: center;
  justify-content: space-between;
  padding: var(--space-5);
  border: 1px solid var(--color-danger-border, #fecaca);
  border-radius: var(--radius-card);
  background: var(--color-danger-soft, #fef2f2);
}

.report-error p {
  margin: var(--space-1) 0 0;
  color: var(--color-text-muted);
}

.report-card {
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr) 136px;
  gap: var(--space-5);
  min-width: 0;
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-surface);
}

.report-card--skeleton {
  grid-template-columns: 1fr;
}

.score-panel,
.detail-score {
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: var(--radius-control);
  text-align: center;
}

.score-panel {
  min-height: 124px;
  padding: var(--space-3);
}

.score-panel.high,
.detail-score.high {
  background: #ecfdf3;
  color: #047857;
}

.score-panel.medium,
.detail-score.medium {
  background: #fffbeb;
  color: #92400e;
}

.score-panel.low,
.detail-score.low {
  background: #fef2f2;
  color: #b91c1c;
}

.score-panel strong,
.detail-score strong {
  margin: var(--space-1) 0;
  font-size: 36px;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.score-panel span,
.score-panel b,
.detail-score span {
  font-size: var(--font-size-sm);
}

.report-card-main {
  min-width: 0;
}

.report-card-heading,
.report-hero,
.panel-header {
  display: flex;
  gap: var(--space-4);
  align-items: flex-start;
  justify-content: space-between;
}

.company-name {
  display: block;
  color: var(--color-primary-strong);
  font-size: var(--font-size-sm);
  font-weight: 800;
  overflow-wrap: anywhere;
}

.report-card h2,
.report-hero h2,
.panel-header h2 {
  margin: var(--space-1) 0 0;
  font-size: var(--font-size-xl);
  overflow-wrap: anywhere;
}

.report-badges,
.tag-row,
.detail-actions,
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.report-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
  margin: var(--space-4) 0;
}

.report-meta div {
  min-width: 0;
  padding: var(--space-3);
  border-radius: var(--radius-control);
  background: var(--color-surface-subtle);
}

.report-meta dt,
.report-meta dd {
  margin: 0;
  overflow-wrap: anywhere;
}

.report-meta dt {
  color: var(--color-text-muted);
  font-size: 12px;
}

.report-meta dd {
  margin-top: var(--space-1);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.insight-preview,
.two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.insight-preview :deep(.ai-insight-panel:last-child) {
  grid-column: 1 / -1;
}

.insight-preview ul,
.insight-preview ol,
.two-column ul,
.two-column ol {
  margin: 0;
  padding-left: 18px;
  line-height: 1.7;
}

.responsive-actions {
  display: flex;
  width: 136px;
  flex-direction: column;
  gap: var(--space-2);
}

.responsive-actions .el-button {
  width: 100%;
  margin-left: 0;
}

.detail-stack {
  display: grid;
  gap: var(--space-4);
}

.report-hero {
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-surface);
}

.detail-meta {
  margin-top: var(--space-3);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.detail-score {
  min-width: 128px;
  min-height: 108px;
  padding: var(--space-3);
}

.conclusion-panel p,
.panel-header p {
  margin: var(--space-2) 0 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.detail-actions {
  margin-top: var(--space-4);
}

.dimension-list {
  display: grid;
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.dimension-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: var(--space-3);
  align-items: center;
}

.dimension-row > span,
.empty-text {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 1050px) {
  .report-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .report-card {
    grid-template-columns: 104px minmax(0, 1fr);
  }

  .responsive-actions {
    display: grid;
    grid-column: 1 / -1;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    width: 100%;
  }
}

@media (max-width: 700px) {
  .report-summary-grid,
  .report-card,
  .report-meta,
  .insight-preview,
  .two-column {
    grid-template-columns: 1fr;
  }

  .report-card {
    gap: var(--space-4);
    padding: var(--space-4);
  }

  .score-panel {
    min-height: 96px;
  }

  .insight-preview :deep(.ai-insight-panel:last-child),
  .responsive-actions {
    grid-column: auto;
  }

  .responsive-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .report-card-heading,
  .report-hero,
  .panel-header,
  .report-error {
    align-items: stretch;
    flex-direction: column;
  }

  .score-input {
    align-items: stretch;
    flex-direction: column;
  }

  .dimension-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 420px) {
  .responsive-actions {
    grid-template-columns: 1fr;
  }
}
</style>
