<template>
  <PageContainer :title="detail ? detail.title || '推荐批次 #' + detail.batchId : '推荐详情'">
    <template #actions>
      <el-button @click="router.push('/job-recommendations')">返回列表</el-button>
    </template>

    <section v-if="loading" class="panel detail-loading" aria-busy="true" aria-live="polite">
      <span class="detail-loading__label">正在加载推荐详情</span>
      <el-skeleton :rows="10" animated />
    </section>

    <section v-else-if="errorText" class="panel state-panel" role="alert">
      <el-result icon="error" title="推荐详情加载失败" :sub-title="errorText">
        <template #extra>
          <el-button type="primary" @click="loadDetail">重试</el-button>
          <el-button @click="router.push('/job-recommendations')">返回列表</el-button>
        </template>
      </el-result>
    </section>

    <template v-else-if="detail">
      <section class="result-summary" aria-label="推荐批次摘要">
        <div class="hero-meta">
          <span>{{ detail.recommendedCount || 0 }} / {{ detail.jobCount || 0 }} 个候选岗位获推荐</span>
          <span>{{ detail.strategy || '综合推荐' }}</span>
          <span>{{ formatDateTime(detail.createdAt) }}</span>
        </div>
        <dl class="result-metrics">
          <div><dt>平均推荐分</dt><dd>{{ averageScore }}</dd></div>
          <div><dt>高推荐</dt><dd>{{ scoreStats.high }}</dd></div>
          <div><dt>可尝试</dt><dd>{{ scoreStats.medium }}</dd></div>
          <div><dt>低优先级</dt><dd>{{ scoreStats.low }}</dd></div>
          <div><dt>已投递</dt><dd>{{ scoreStats.applied }}</dd></div>
        </dl>
      </section>

      <section v-loading="loading" class="recommendation-list">
        <AppEmpty
          v-if="items.length === 0"
          title="暂无推荐结果"
          description="这批推荐暂时没有可展示的岗位。"
        >
          <el-button data-detail-empty-action type="primary" @click="router.push('/job-recommendations')">返回推荐列表</el-button>
        </AppEmpty>

        <article v-for="item in items" v-else :key="item.itemId" class="recommendation-card">
          <div class="recommendation-main">
            <div class="card-head">
              <div>
                <span class="company-name" :title="item.companyName || '未知公司'">{{ item.companyName || '未知公司' }}</span>
                <h3>{{ item.jobTitle || '未知岗位' }}</h3>
                <el-tag class="delivery-advice" :type="scoreTagType(item.recommendationScore)" effect="plain">{{ deliveryAdvice(item.recommendationScore) }}</el-tag>
              </div>
              <div class="score-panel" :class="scoreClass(item.recommendationScore)">
                <strong>{{ normalizedScore(item.recommendationScore) }}</strong>
                <span>{{ levelLabel(item.recommendationLevel) }}</span>
              </div>
            </div>

            <div class="job-meta">
              <span>{{ item.jobType || '类型未填写' }}</span>
              <span>{{ item.location || '地点未填写' }}</span>
              <span>{{ item.salaryRange || '薪资面议' }}</span>
              <span>{{ item.sourcePlatform || '来源未填写' }}</span>
              <span>状态：{{ item.isApplied === 1 ? '已投递' : '待投递' }}</span>
            </div>

            <div class="score-parts">
              <div>
                <span>技能匹配</span>
                <el-progress :percentage="normalizedScore(item.skillMatchScore)" :stroke-width="8" />
              </div>
              <div>
                <span>AI 匹配</span>
                <el-progress :percentage="normalizedScore(item.aiMatchScore)" :stroke-width="8" />
              </div>
              <div>
                <span>方向匹配</span>
                <el-progress :percentage="normalizedScore(item.jobTypeScore)" :stroke-width="8" />
              </div>
            </div>

            <div class="insight-grid">
              <AiInsightPanel title="推荐理由" tone="action">
                <ul class="insight-list">
                  <li v-for="reason in item.reasons || []" :key="reason">{{ reason }}</li>
                  <li v-if="!item.reasons?.length">暂无推荐理由，建议重新生成或查看关联分析报告。</li>
                </ul>
              </AiInsightPanel>

              <AiInsightPanel title="匹配证据" tone="strength">
                <div class="tag-row">
                  <el-tag v-for="skill in item.matchedSkills || []" :key="skill" type="success" effect="plain">{{ skill }}</el-tag>
                  <span v-if="!item.matchedSkills?.length" class="muted">暂无匹配技能</span>
                </div>
              </AiInsightPanel>

              <AiInsightPanel title="风险提醒" tone="risk">
                <div class="tag-row">
                  <el-tag v-for="skill in item.missingSkills || []" :key="skill" type="warning" effect="plain">{{ skill }}</el-tag>
                  <span v-if="!item.missingSkills?.length" class="muted">暂无明显风险</span>
                </div>
              </AiInsightPanel>
            </div>

            <details class="action-advice">
              <summary>行动建议</summary>
              <ol>
                <li>根据风险提醒补齐简历关键词和项目描述。</li>
                <li>投递前确认地点、实习周期、薪资和岗位职责。</li>
                <li>进入 AI 分析或面试题页面准备针对性表达。</li>
              </ol>
            </details>

            <div
              v-if="applicationError(item)"
              :data-application-error="item.jobId"
              class="application-error"
              role="alert"
            >
              {{ applicationError(item) }}
            </div>
          </div>

          <div class="card-actions" role="group" :aria-label="`${item.companyName || '未知公司'} ${item.jobTitle || '未知岗位'} 后续行动`">
            <el-button
              v-if="primaryAction(item) === 'application'"
              class="primary-next-action"
              type="primary"
              :icon="Plus"
              :loading="isApplicationPending(item)"
              @click="addApplication(item)"
            >加入投递</el-button>
            <el-button
              v-else-if="primaryAction(item) === 'analysis'"
              class="primary-next-action"
              type="primary"
              :icon="MagicStick"
              @click="goAnalysis(item)"
            >先做 AI 分析</el-button>
            <el-button
              v-else
              class="primary-next-action"
              type="primary"
              @click="goInterviewQuestions(item)"
            >准备面试</el-button>

            <el-button
              v-if="primaryAction(item) !== 'application' && item.isApplied !== 1"
              :icon="Plus"
              :loading="isApplicationPending(item)"
              @click="addApplication(item)"
            >加入投递</el-button>
            <el-button v-if="primaryAction(item) !== 'analysis'" :icon="MagicStick" @click="goAnalysis(item)">AI 分析</el-button>
            <el-button v-if="primaryAction(item) !== 'interview'" @click="goInterviewQuestions(item)">生成面试题</el-button>
          </div>
        </article>
      </section>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Plus } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import AiInsightPanel from '@/components/common/AiInsightPanel.vue'
import router from '@/router'
import { createApplicationApi } from '@/api/application'
import { getJobRecommendationDetailApi } from '@/api/jobRecommendation'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const detail = ref<any>(null)
const loading = ref(false)
const errorText = ref('')
const pendingApplicationJobIds = ref(new Set<number>())
const applicationErrorsByJob = ref(new Map<number, string>())
const applicationIdsByJob = ref(new Map<number, number | string>())
const batchId = computed(() => Number(route.params.batchId))
const items = computed(() => detail.value?.items || [])

const averageScore = computed(() => {
  if (!items.value.length) return 0
  const total = items.value.reduce((sum: number, item: any) => sum + normalizedScore(item.recommendationScore), 0)
  return Math.round(total / items.value.length)
})

const scoreStats = computed(() => ({
  high: items.value.filter((item: any) => normalizedScore(item.recommendationScore) >= 85).length,
  medium: items.value.filter((item: any) => {
    const score = normalizedScore(item.recommendationScore)
    return score >= 70 && score < 85
  }).length,
  low: items.value.filter((item: any) => normalizedScore(item.recommendationScore) < 70).length,
  applied: items.value.filter((item: any) => item.isApplied === 1).length
}))

async function loadDetail() {
  loading.value = true
  errorText.value = ''
  try {
    detail.value = await getJobRecommendationDetailApi(batchId.value)
  } catch (e: any) {
    detail.value = null
    errorText.value = e?.message || e?.response?.data?.message || '推荐批次不存在、已被删除，或当前账号没有访问权限。'
  } finally {
    loading.value = false
  }
}

function goAnalysis(item: any) {
  router.push({
    path: '/analysis/match',
    query: {
      resumeId: detail.value.resumeId,
      resumeVersionId: detail.value.resumeVersionId,
      jobId: item.jobId
    }
  })
}

function goInterviewQuestions(item: any) {
  router.push({
    path: '/interview-questions',
    query: {
      resumeId: detail.value.resumeId,
      resumeVersionId: detail.value.resumeVersionId,
      jobId: item.jobId,
      reportId: item.analysisReportId
    }
  })
}

async function addApplication(item: any) {
  const jobId = Number(item.jobId)
  if (item.isApplied === 1 || pendingApplicationJobIds.value.has(jobId)) return

  setApplicationPending(jobId, true)
  setApplicationError(jobId, '')
  try {
    const result: any = await createApplicationApi({
      jobId: item.jobId,
      resumeId: detail.value.resumeId,
      reportId: item.analysisReportId,
      status: 'TO_APPLY',
      priority: normalizedScore(item.recommendationScore) >= 85 ? 'HIGH' : 'MEDIUM',
      note: `来自岗位推荐批次：${detail.value.title || detail.value.batchId}`
    })
    items.value.forEach((candidate: any) => {
      if (Number(candidate.jobId) !== jobId) return
      candidate.isApplied = 1
      if (result?.applicationId != null) candidate.applicationId = result.applicationId
    })
    if (result?.applicationId != null) {
      const nextIds = new Map(applicationIdsByJob.value)
      nextIds.set(jobId, result.applicationId)
      applicationIdsByJob.value = nextIds
    }
    ElMessage.success('已加入投递记录')
  } catch (e: any) {
    const reason = e?.response?.data?.message || e?.message
    const message = reason ? `投递记录创建失败：${reason}` : '投递记录创建失败，请稍后重试。'
    setApplicationError(jobId, message)
    ElMessage.error(message)
  } finally {
    setApplicationPending(jobId, false)
  }
}

function isApplicationPending(item: any) {
  return pendingApplicationJobIds.value.has(Number(item.jobId))
}

function applicationError(item: any) {
  return applicationErrorsByJob.value.get(Number(item.jobId)) || ''
}

function setApplicationPending(jobId: number, pending: boolean) {
  const nextPending = new Set(pendingApplicationJobIds.value)
  if (pending) nextPending.add(jobId)
  else nextPending.delete(jobId)
  pendingApplicationJobIds.value = nextPending
}

function setApplicationError(jobId: number, message: string) {
  const nextErrors = new Map(applicationErrorsByJob.value)
  if (message) nextErrors.set(jobId, message)
  else nextErrors.delete(jobId)
  applicationErrorsByJob.value = nextErrors
}

function primaryAction(item: any): 'application' | 'analysis' | 'interview' {
  if (item.isApplied === 1) return 'interview'
  if (!item.analysisReportId) return 'analysis'
  return 'application'
}

function normalizedScore(value?: number) {
  return Math.max(0, Math.min(100, Number(value || 0)))
}

function levelLabel(level: string) {
  const labels: Record<string, string> = {
    HIGH: '强烈推荐',
    MEDIUM_HIGH: '较推荐',
    MEDIUM: '一般推荐',
    LOW: '低推荐',
    NOT_RECOMMENDED: '不推荐'
  }
  return labels[level] || level || '未评级'
}

function deliveryAdvice(value?: number) {
  const score = normalizedScore(value)
  if (score >= 85) return '优先投递'
  if (score >= 70) return '可尝试'
  return '低优先级'
}

function scoreTagType(value?: number) {
  const score = normalizedScore(value)
  if (score >= 85) return 'success'
  if (score >= 70) return 'warning'
  return 'info'
}

function scoreClass(value?: number) {
  const score = normalizedScore(value)
  if (score >= 85) return 'high'
  if (score >= 70) return 'medium'
  return 'low'
}

onMounted(loadDetail)
</script>

<style scoped>
:deep(.app-page-header) {
  align-items: flex-end;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
}

:deep(.app-page-title) {
  min-width: 0;
  overflow-wrap: anywhere;
}

:deep(.app-page-title h1) {
  font-size: clamp(26px, 3vw, 36px);
  line-height: 1.2;
}

:deep(.app-page-title p) {
  margin-top: var(--space-2);
}

@media (max-width: 900px) {
  :deep(.app-page-header) {
    align-items: stretch;
  }
}

.detail-loading__label {
  display: block;
  margin-bottom: var(--space-3);
  color: var(--color-text-muted);
}

.result-summary {
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
}

.hero-meta,
.job-meta,
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.hero-meta,
.job-meta,
.result-metrics dt {
  color: var(--color-text-muted);
  font-size: 13px;
}

.result-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2) var(--space-4);
  margin: var(--space-2) 0 0;
}

.result-metrics div {
  display: flex;
  align-items: baseline;
  gap: var(--space-1);
}

.result-metrics dd {
  margin: 0;
  font-weight: 700;
}

.company-name {
  display: block;
  color: var(--color-primary-hover);
  font-size: 13px;
  font-weight: 700;
}

.card-head h3 {
  margin: var(--space-1) 0 0;
}

.job-meta,
.delivery-advice {
  margin-top: var(--space-2);
}

.recommendation-list {
  display: grid;
  gap: 16px;
}

.recommendation-card {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  overflow-wrap: anywhere;
}

.score-panel {
  display: grid;
  flex-shrink: 0;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-3);
  place-items: center;
  border-radius: var(--radius-sm);
  text-align: center;
}

.score-panel.high {
  background: var(--color-primary-soft);
  color: var(--color-primary-hover);
}

.score-panel.medium {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning-dark-2);
}

.score-panel.low {
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
}

.score-panel strong {
  font-size: 26px;
  line-height: 1;
}

.score-panel span {
  font-size: 12px;
  font-weight: 700;
}

.card-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.score-parts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.score-parts span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-text-soft);
  font-size: 13px;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.action-advice {
  margin-top: var(--space-3);
  border-radius: var(--radius-sm);
  padding: var(--space-2) var(--space-3);
  background: var(--color-surface-muted);
}

.action-advice summary {
  cursor: pointer;
  font-weight: 700;
}

.action-advice[open] summary {
  margin-bottom: var(--space-2);
}

.action-advice ol {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text-muted);
  line-height: 1.8;
}

.insight-list {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.application-error {
  margin-top: 14px;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--color-danger) 30%, var(--color-border));
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--color-danger) 8%, var(--color-surface));
  color: var(--color-danger);
  font-size: 13px;
}

.muted {
  color: var(--color-text-soft);
  font-size: 13px;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.card-actions .el-button {
  margin-left: 0;
}

@media (max-width: 640px) {
  .score-parts,
  .insight-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .recommendation-card .card-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .recommendation-card .primary-next-action {
    grid-column: 1 / -1;
  }
}
</style>
