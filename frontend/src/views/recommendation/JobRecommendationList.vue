<template>
  <PageContainer title="岗位推荐" description="根据简历和岗位库生成推荐，比较匹配理由与风险。" width="wide">
    <div class="recommendation-grid">
      <section class="panel generator-panel" :aria-busy="optionsLoading">
        <div class="panel-header">
          <div>
            <h2>生成推荐</h2>
            <span>有历史分析时，推荐会参考匹配结果。</span>
          </div>
        </div>

        <el-form :model="form" label-position="top">
          <el-form-item label="简历">
            <el-select v-model="form.resumeId" placeholder="请选择简历" filterable :loading="optionsLoading" :disabled="optionsLoading">
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
                :label="`${item.versionName}${item.isCurrent === 1 ? '（当前）' : ''}`"
                :value="item.versionId"
              />
            </el-select>
          </el-form-item>

          <div class="form-grid two">
            <el-form-item label="推荐数量">
              <el-input-number v-model="form.limit" :min="1" :max="50" />
            </el-form-item>
            <el-form-item label="包含已投递岗位">
              <el-switch v-model="form.includeApplied" />
            </el-form-item>
          </div>

          <p v-if="optionsLoading" class="generator-status" aria-live="polite">正在加载可用简历…</p>
          <div v-else-if="optionsErrorText" data-options-error class="inline-error" role="alert">
            <span>简历选项暂时无法加载：{{ optionsErrorText }}</span>
            <el-button data-options-retry link type="primary" @click="loadOptions">重新加载简历</el-button>
          </div>

          <p v-else-if="!resumes.length" class="generator-status">
            暂无可用简历。<RouterLink to="/resumes">前往简历中心</RouterLink>
          </p>
          <p v-if="generating" class="generator-status" role="status">正在生成推荐，可在 AI 任务中心查看进度。</p>

          <el-button
            type="primary"
            :icon="MagicStick"
            :loading="generating"
            :disabled="optionsLoading || Boolean(optionsErrorText) || !resumes.length"
            @click="generateRecommendation"
          >
            生成推荐
          </el-button>
          <div v-if="generationErrorText" data-generation-error class="inline-error" role="alert">
            <span>{{ generationErrorText }}</span>
            <el-button data-generation-retry link type="primary" :disabled="generating" @click="generateRecommendation">重新生成</el-button>
          </div>
        </el-form>
      </section>

      <section class="recommendation-library" aria-label="岗位推荐批次">
        <div class="history-heading">
          <h2>推荐历史</h2>
          <span v-if="!loading && !historyErrorText" role="status">{{ filteredHistory.length }} 个批次</span>
        </div>
        <FilterBar class="history-filters" @reset="resetLevelFilter">
          <template #filters>
            <el-select v-model="levelFilter" aria-label="按推荐等级筛选">
              <el-option v-for="item in levelFilterOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
          <template #actions>
            <el-button type="primary" @click="loadHistory">刷新</el-button>
          </template>
        </FilterBar>

        <section v-if="historyErrorText" class="panel state-panel" role="alert">
          <div>
            <strong>推荐批次暂时无法加载</strong>
            <p>{{ historyErrorText }}</p>
          </div>
          <el-button data-recommendation-retry type="primary" @click="loadHistory">重新加载</el-button>
        </section>

        <TableShell
          v-else
          :loading="loading"
          :empty="!filteredHistory.length"
          :empty-title="levelFilter === 'all' ? '还没有岗位推荐' : '没有符合等级的推荐批次'"
          :empty-hint="levelFilter === 'all' ? '选择简历后生成首批推荐。' : '清除等级筛选可查看全部批次。'"
        >
          <template #empty-actions>
            <el-button v-if="levelFilter !== 'all'" @click="resetLevelFilter">清除等级筛选</el-button>
            <el-button
              v-else
              data-recommendation-empty-action
              type="primary"
              :icon="MagicStick"
              :disabled="optionsLoading || Boolean(optionsErrorText) || !resumes.length || generating"
              @click="generateRecommendation"
            >
              生成第一批推荐
            </el-button>
          </template>

          <div class="batch-card-list">
            <article v-for="batch in filteredHistory" :key="batch.batchId" class="batch-card">
              <div class="batch-card-main">
                <div class="batch-heading">
                  <div>
                    <h3>{{ batch.title || `推荐批次 #${batch.batchId}` }}</h3>
                  </div>
                  <el-tag :type="batchLevelType(batch)" effect="plain">{{ batchLevelText(batch) }}</el-tag>
                </div>

                <div class="batch-meta">
                  <span>{{ batch.recommendedCount || 0 }} 个推荐岗位</span>
                  <span>{{ batch.jobCount || 0 }} 个候选岗位</span>
                  <span>{{ strategyLabel(batch.strategy) }}</span>
                  <span>{{ formatDateTime(batch.createdAt) }}</span>
                </div>

                <div class="preview-jobs">
                  <div v-for="item in previewItems(batch)" :key="item.itemId || item.jobId" class="preview-job">
                    <div class="preview-job__heading">
                      <strong :title="item.companyName || '未知公司'">{{ item.companyName || '未知公司' }}</strong>
                      <span>{{ item.jobTitle || '未知岗位' }}</span>
                    </div>
                    <div class="preview-job__signals">
                      <span class="preview-job__score">{{ item.recommendationScore || 0 }} 分 · {{ levelLabel(item.recommendationLevel) }}</span>
                      <span>证据：{{ signalText(item.matchedSkills, '待查看') }}</span>
                      <span>风险：{{ signalText(item.missingSkills, '暂无明显风险') }}</span>
                      <span>状态：{{ item.isApplied === 1 ? '已投递' : '待投递' }}</span>
                    </div>
                  </div>
                  <div v-if="batch.previewError" :data-batch-preview-error="batch.batchId" class="preview-job preview-job--error">
                    <span>预览暂不可用，可重试或进入详情查看推荐岗位。</span>
                    <el-button :data-batch-retry="batch.batchId" link type="primary" @click="loadHistory">重试</el-button>
                  </div>
                  <div v-else-if="!previewItems(batch).length" class="preview-job empty">进入详情查看推荐岗位</div>
                </div>
              </div>

              <div class="responsive-actions">
                <el-button type="primary" @click="viewDetail(batch.batchId)">查看详情</el-button>
                <el-button type="danger" plain @click="removeBatch(batch)">删除</el-button>
              </div>
            </article>
          </div>
        </TableShell>
      </section>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import TableShell from '@/components/common/TableShell.vue'
import router from '@/router'
import {
  deleteJobRecommendationApi,
  generateJobRecommendationApi,
  getJobRecommendationDetailApi,
  getJobRecommendationListApi
} from '@/api/jobRecommendation'
import { getResumeListApi } from '@/api/resume'
import { getResumeVersionListApi } from '@/api/resumeVersion'
import { formatDateTime } from '@/utils/format'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'

const aiTaskCenter = useAiTaskCenterStore()
const resumes = ref<any[]>([])
const versions = ref<any[]>([])
const history = ref<any[]>([])
const loading = ref(false)
const generating = ref(false)
const optionsLoading = ref(false)
const optionsErrorText = ref('')
const historyErrorText = ref('')
const generationErrorText = ref('')
const levelFilter = ref('all')

const levelFilterOptions = [
  { label: '全部', value: 'all' },
  { label: '高推荐', value: 'high' },
  { label: '可尝试', value: 'medium' },
  { label: '低优先级', value: 'low' }
]

const form = reactive({
  resumeId: undefined as number | undefined,
  resumeVersionId: undefined as number | undefined,
  includeApplied: false,
  limit: 10
})

const filteredHistory = computed(() => {
  if (levelFilter.value === 'all') return history.value
  return history.value.filter((batch) => {
    const top = topScore(batch)
    if (levelFilter.value === 'high') return top >= 85
    if (levelFilter.value === 'medium') return top >= 70 && top < 85
    return top < 70
  })
})

async function loadOptions() {
  optionsLoading.value = true
  optionsErrorText.value = ''
  try {
    const res: any = await getResumeListApi({ pageNum: 1, pageSize: 100 })
    resumes.value = res.records || []
  } catch (e: any) {
    resumes.value = []
    versions.value = []
    form.resumeId = undefined
    form.resumeVersionId = undefined
    optionsErrorText.value = e?.response?.data?.message || e?.message || '请检查网络连接后重试。'
  } finally {
    optionsLoading.value = false
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

async function loadHistory() {
  loading.value = true
  historyErrorText.value = ''
  try {
    const res: any = await getJobRecommendationListApi({ pageNum: 1, pageSize: 100 })
    history.value = await hydrateBatches(res.records || [])
  } catch (e: any) {
    history.value = []
    historyErrorText.value = e?.message || e?.response?.data?.message || '岗位推荐加载失败，请稍后重试'
    ElMessage.error(historyErrorText.value)
  } finally {
    loading.value = false
  }
}

async function hydrateBatches(records: any[]) {
  return Promise.all(
    records.map(async (batch) => {
      try {
        const detail = (await getJobRecommendationDetailApi(batch.batchId)) as any
        return { ...batch, items: detail.items || [] }
      } catch {
        return { ...batch, previewError: true }
      }
    })
  )
}

async function generateRecommendation() {
  if (generating.value || optionsLoading.value || optionsErrorText.value || !resumes.value.length) return
  if (!form.resumeId) {
    ElMessage.warning('请选择简历')
    return
  }
  generating.value = true
  generationErrorText.value = ''

  const localTaskId = aiTaskCenter.createTask({
    type: 'JOB_RECOMMENDATION',
    title: '岗位推荐',
    message: '正在生成岗位推荐...',
    resumeId: form.resumeId,
    sourcePath: '/job-recommendations'
  })

  try {
    const res: any = await generateJobRecommendationApi(form)
    aiTaskCenter.completeTask(localTaskId, {
      resultId: res.batchId,
      resultPath: `/job-recommendations/${res.batchId}`,
      message: '岗位推荐生成完成'
    })
    ElMessage.success('推荐生成成功')
    await loadHistory()
    viewDetail(res.batchId)
  } catch (e: any) {
    const reason = e?.response?.data?.message || e?.message
    generationErrorText.value = reason ? `推荐生成失败：${reason}` : '推荐生成失败，请稍后重试。'
    aiTaskCenter.failTask(localTaskId, '推荐生成失败')
  } finally {
    generating.value = false
  }
}

function viewDetail(batchId: number) {
  router.push(`/job-recommendations/${batchId}`)
}

async function removeBatch(batch: any) {
  try {
    await ElMessageBox.confirm(
      `确认删除“${batch.title || `推荐批次 #${batch.batchId}`}”吗？删除后无法继续查看这批推荐结果。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteJobRecommendationApi(batch.batchId)
    ElMessage.success('删除成功')
    loadHistory()
  } catch {
    // Error message is already shown by the request interceptor.
  }
}

function previewItems(batch: any) {
  return (batch.items || []).slice(0, 3)
}

function signalText(values: string[] | undefined, fallback: string) {
  return values?.length ? values.slice(0, 3).join('、') : fallback
}

function resetLevelFilter() {
  levelFilter.value = 'all'
}

function topScore(batch: any) {
  return Math.max(0, ...(batch.items || []).map((item: any) => Number(item.recommendationScore || 0)))
}

function batchLevelText(batch: any) {
  const score = topScore(batch)
  if (score >= 85) return '高推荐'
  if (score >= 70) return '可尝试'
  if (score > 0) return '低优先级'
  return '待查看'
}

function batchLevelType(batch: any) {
  const score = topScore(batch)
  if (score >= 85) return 'success'
  if (score >= 70) return 'warning'
  if (score > 0) return 'info'
  return 'primary'
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

function strategyLabel(strategy?: string) {
  return strategy || '综合推荐'
}

watch(() => form.resumeId, loadVersions)

onMounted(() => {
  void loadOptions()
  void loadHistory()
})
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

.generator-panel h2,
.history-heading h2 {
  margin: 0;
  font-size: 20px;
}

.history-heading {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
}

.history-heading span {
  color: var(--color-text-muted);
  font-size: 13px;
}

.history-filters :deep(.filter-bar__filters) {
  flex: 1 1 auto;
}

.history-filters :deep(.el-select) {
  width: min(100%, 180px);
}

.recommendation-grid {
  display: grid;
  grid-template-columns: minmax(250px, 300px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.generator-panel {
  position: sticky;
  top: 96px;
}

.panel-header span {
  color: var(--color-text-soft);
  font-size: 13px;
}

.recommendation-library {
  display: grid;
  gap: 14px;
}

.recommendation-library :deep(.table-shell__content) {
  padding: 0;
}

.batch-card {
  display: grid;
  gap: var(--space-3);
  padding: var(--space-4);
  overflow-wrap: anywhere;
}

.batch-card + .batch-card {
  border-top: 1px solid var(--color-border);
}

.batch-heading {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.batch-heading h3 {
  margin: 6px 0 0;
  font-size: 18px;
}

.batch-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 12px 0;
  color: var(--color-text-soft);
  font-size: 13px;
}

.preview-jobs {
  display: grid;
  gap: 8px;
}

.preview-job {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface-muted);
}

.preview-job strong,
.preview-job__heading span {
  display: block;
  overflow-wrap: anywhere;
}

.preview-job__heading span,
.preview-job__signals,
.preview-job.empty {
  color: var(--color-text-soft);
  font-size: 13px;
}

.preview-job__heading span {
  margin-top: 4px;
}

.preview-job__signals {
  display: grid;
  justify-items: end;
  gap: 3px;
  text-align: right;
}

.preview-job__score {
  color: var(--color-text);
  font-weight: 700;
}

.preview-job--error {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  border-color: color-mix(in srgb, var(--color-warning) 36%, var(--color-border));
  background: color-mix(in srgb, var(--color-warning) 10%, var(--color-surface));
}

.inline-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--color-danger) 30%, var(--color-border));
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--color-danger) 8%, var(--color-surface));
  color: var(--color-danger);
  font-size: 13px;
}

.generator-status {
  margin: 0 0 12px;
  color: var(--color-text-muted);
  font-size: 13px;
}

.state-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.state-panel p {
  margin: 6px 0 0;
  color: var(--color-text-muted);
}

.batch-card .responsive-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.batch-card .responsive-actions .el-button {
  margin-left: 0;
}

@media (max-width: 900px) {
  .recommendation-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .generator-panel {
    position: static;
  }

  .history-filters :deep(.el-select) {
    width: 100%;
  }
}

@media (max-width: 520px) {
  .preview-job {
    grid-template-columns: minmax(0, 1fr);
  }

  .preview-job__signals {
    justify-items: start;
    text-align: left;
  }

  .inline-error,
  .state-panel {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .history-filters :deep(.filter-bar__actions),
  .batch-card .responsive-actions,
  .generator-panel .form-grid.two {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .generator-panel :deep(.el-input-number) {
    width: 100%;
  }
}
</style>
