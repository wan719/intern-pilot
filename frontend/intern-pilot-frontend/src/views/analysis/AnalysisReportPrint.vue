<template>
  <main class="print-shell">
    <div class="print-toolbar">
      <el-button :icon="Back" @click="router.push('/analysis/reports')">返回报告列表</el-button>
      <el-button type="primary" :icon="Printer" :disabled="loading || !!error" @click="printReport">打印 / 保存 PDF</el-button>
    </div>

    <section v-if="loading" class="print-page">
      <el-skeleton :rows="12" animated />
    </section>

    <section v-else-if="error" class="print-page print-error">
      <el-result icon="error" title="报告加载失败" :sub-title="error">
        <template #extra>
          <el-button type="primary" @click="loadReport">重试</el-button>
          <el-button @click="router.push('/analysis/reports')">返回报告列表</el-button>
        </template>
      </el-result>
    </section>

    <article v-else-if="report" class="print-page">
      <header class="report-header">
        <div>
          <p class="eyebrow">InternPilot AI 分析报告</p>
          <h1>{{ report.companyName || '未知公司' }} - {{ report.jobTitle || '未知岗位' }}</h1>
          <p class="subtitle">基于简历与岗位 JD 的匹配分析、优化建议与面试准备清单。</p>
        </div>
        <div class="score-box" :class="scoreClass(report.matchScore)">
          <strong>{{ normalizedScore(report.matchScore) }}</strong>
          <span>{{ report.matchLevel || scoreLabel(report.matchScore) }}</span>
        </div>
      </header>

      <section class="meta-grid">
        <div>
          <span>简历信息</span>
          <strong>{{ report.resumeName || '未返回简历名称' }}</strong>
          <small>简历 ID：{{ report.resumeId || '-' }} / 版本 ID：{{ report.resumeVersionId || '-' }}</small>
        </div>
        <div>
          <span>岗位信息</span>
          <strong>{{ report.companyName || '未知公司' }} / {{ report.jobTitle || '未知岗位' }}</strong>
          <small>岗位 ID：{{ report.jobId || '-' }}</small>
        </div>
        <div>
          <span>生成时间</span>
          <strong>{{ formatDateTime(report.createdAt) }}</strong>
          <small>{{ report.aiProvider || 'AI' }} / {{ report.aiModel || '模型未返回' }}</small>
        </div>
      </section>

      <section class="report-section">
        <h2>匹配总分</h2>
        <p>{{ conclusionText(report) }}</p>
      </section>

      <section class="report-section">
        <h2>维度得分</h2>
        <div class="dimension-list">
          <div v-for="item in dimensionScores(report)" :key="item.label" class="dimension-row">
            <span>{{ item.label }}</span>
            <div class="bar">
              <i :style="{ width: `${item.score}%`, background: scoreColor(item.score) }" />
            </div>
            <strong>{{ item.score }}</strong>
          </div>
        </div>
      </section>

      <section class="two-column">
        <div class="report-section">
          <h2>优势</h2>
          <ul>
            <li v-for="item in safeList(report.strengths, '暂未返回明确优势。')" :key="item">{{ item }}</li>
          </ul>
        </div>
        <div class="report-section">
          <h2>不足与风险提示</h2>
          <ul>
            <li v-for="item in riskItems" :key="item">{{ item }}</li>
          </ul>
        </div>
      </section>

      <section class="two-column">
        <div class="report-section">
          <h2>简历优化建议</h2>
          <ol>
            <li v-for="item in safeList(report.suggestions, '暂未返回简历优化建议。')" :key="item">{{ item }}</li>
          </ol>
        </div>
        <div class="report-section">
          <h2>面试准备建议</h2>
          <ol>
            <li v-for="item in safeList(report.interviewTips, '暂未返回面试准备建议。')" :key="item">{{ item }}</li>
          </ol>
        </div>
      </section>

      <section class="report-section">
        <h2>总结</h2>
        <p>{{ summaryText }}</p>
      </section>

      <footer class="report-footer">
        <p>免责声明：本报告由 AI 根据当前简历与岗位信息生成，仅作为求职准备参考，不代表企业招聘结论或录用承诺。请结合真实岗位要求、个人经历与人工判断使用。</p>
      </footer>
    </article>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, Printer } from '@element-plus/icons-vue'
import { getAnalysisReportDetailApi } from '@/api/analysis'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const report = ref<any>(null)

const riskItems = computed(() => [
  ...safeList(report.value?.weaknesses, '暂未返回明显短板。'),
  ...safeList(report.value?.missingSkills, '').map((item) => `缺失技能：${item}`).filter(Boolean)
])

const summaryText = computed(() => {
  const strengths = safeList(report.value?.strengths, '').filter(Boolean).slice(0, 2).join('；')
  const weaknesses = safeList(report.value?.weaknesses, '').filter(Boolean).slice(0, 2).join('；')
  return `当前匹配分为 ${normalizedScore(report.value?.matchScore)} 分。优势：${strengths || '暂无明确优势'}。不足：${weaknesses || '暂无明显短板'}。建议优先按报告中的简历优化建议补充关键词、项目成果与岗位相关证据。`
})

async function loadReport() {
  const reportId = Number(route.params.id)
  if (!Number.isFinite(reportId) || reportId <= 0) {
    error.value = '报告 ID 不正确，请从分析报告列表重新进入。'
    return
  }

  loading.value = true
  error.value = ''
  try {
    report.value = await getAnalysisReportDetailApi(reportId)
  } catch (e: any) {
    error.value = e?.message || e?.response?.data?.message || '报告不存在、已被删除，或当前账号没有访问权限。'
  } finally {
    loading.value = false
  }
}

function printReport() {
  window.print()
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

function safeList(items?: string[], fallback = '暂无内容。') {
  const list = Array.isArray(items) ? items.filter(Boolean) : []
  return list.length ? list : fallback ? [fallback] : []
}

function conclusionText(data: any) {
  const score = normalizedScore(data?.matchScore)
  const company = data?.companyName || '该公司'
  const job = data?.jobTitle || '该岗位'
  if (score >= 80) {
    return `与 ${company} 的 ${job} 匹配度较高，建议优先投递，并围绕优势技能和项目经历准备面试表达。`
  }
  if (score >= 60) {
    return `与 ${company} 的 ${job} 具备一定匹配基础，建议先补齐关键短板，再根据岗位要求有选择地投递。`
  }
  return `当前简历与 ${company} 的 ${job} 匹配度偏低，建议优先优化简历关键词、项目描述和缺失技能后再投递。`
}

function dimensionScores(data: any) {
  const base = normalizedScore(data?.matchScore)
  const strengths = data?.strengths?.length || 0
  const weaknesses = data?.weaknesses?.length || 0
  const missing = data?.missingSkills?.length || 0
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

onMounted(loadReport)
</script>

<style scoped>
.print-shell {
  min-height: 100vh;
  padding: 24px;
  background: #eef2f7;
  color: #101828;
}

.print-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  max-width: 920px;
  margin: 0 auto 16px;
}

.print-page {
  width: 210mm;
  max-width: 100%;
  min-height: 297mm;
  margin: 0 auto;
  padding: 18mm;
  overflow: visible;
  background: #fff;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.14);
  overflow-wrap: anywhere;
}

.report-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 112px;
  gap: 18px;
  align-items: start;
  padding-bottom: 18px;
  border-bottom: 2px solid #dbe3ef;
}

.eyebrow {
  margin: 0 0 8px;
  color: #175cd3;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1,
h2,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 8px;
  font-size: 26px;
  line-height: 1.3;
}

h2 {
  margin-bottom: 10px;
  font-size: 16px;
}

.subtitle,
.report-section p,
.report-footer {
  color: #475467;
  line-height: 1.8;
}

.score-box {
  display: grid;
  min-height: 102px;
  place-items: center;
  align-content: center;
  border-radius: 8px;
  text-align: center;
}

.score-box strong {
  font-size: 34px;
  line-height: 1;
}

.score-box span {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 800;
}

.score-box.high {
  background: #ecfdf3;
  color: #047857;
}

.score-box.medium {
  background: #fffbeb;
  color: #b45309;
}

.score-box.low {
  background: #fef2f2;
  color: #dc2626;
}

.meta-grid,
.two-column {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.two-column {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.meta-grid div,
.report-section {
  break-inside: avoid;
  padding: 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.meta-grid span,
.meta-grid small {
  display: block;
  color: #667085;
  font-size: 12px;
}

.meta-grid strong {
  display: block;
  margin: 6px 0;
}

.report-section {
  margin-top: 18px;
}

.report-section ul,
.report-section ol {
  margin: 0;
  padding-left: 20px;
  color: #344054;
  line-height: 1.8;
}

.dimension-list {
  display: grid;
  gap: 10px;
}

.dimension-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 36px;
  gap: 10px;
  align-items: center;
}

.bar {
  height: 9px;
  overflow: hidden;
  border-radius: 99px;
  background: #eef2f7;
}

.bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.report-footer {
  margin-top: 22px;
  padding-top: 14px;
  border-top: 1px solid #dbe3ef;
  font-size: 12px;
}

.print-error {
  display: grid;
  place-items: center;
}

@page {
  size: A4;
  margin: 14mm;
}

@media print {
  :global(html),
  :global(body) {
    width: 100% !important;
    min-width: 0 !important;
    overflow: visible !important;
    background: #fff !important;
    color: #000 !important;
  }

  .print-shell {
    width: 100%;
    min-height: auto;
    padding: 0;
    overflow: visible;
    background: #fff !important;
    color: #000 !important;
  }

  .print-toolbar {
    display: none;
  }

  .print-page {
    width: 100%;
    max-width: none;
    min-height: auto;
    padding: 0;
    overflow: visible;
    background: #fff !important;
    color: #000 !important;
    box-shadow: none;
  }

  .print-page :is(h1, h2, p, li, strong, span, small) {
    color: #000 !important;
  }

  .report-header,
  .report-footer,
  .meta-grid div,
  .report-section {
    border-color: #000 !important;
  }

  .score-box {
    border: 1px solid #000;
    background: #fff !important;
    color: #000 !important;
  }

  .bar {
    border: 1px solid #000;
    background: #fff !important;
  }

  .bar i {
    background: #555 !important;
  }

  .two-column {
    display: block;
    margin-top: 0;
  }

  .report-section,
  .meta-grid div {
    break-inside: avoid-page;
    page-break-inside: avoid;
  }

  h1,
  h2 {
    break-after: avoid-page;
    page-break-after: avoid;
  }

  p,
  li {
    orphans: 3;
    widows: 3;
  }
}

@media screen and (max-width: 760px) {
  .print-shell {
    padding: 12px;
  }

  .print-page {
    padding: 18px;
  }

  .report-header,
  .meta-grid,
  .two-column {
    grid-template-columns: 1fr;
  }
}
</style>
