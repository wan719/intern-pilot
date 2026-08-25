<template>
  <PageContainer title="">
    <template #hero>
      <PageHero
        eyebrow="简历中心"
        title="简历版本"
        description="按时间查看原始、手动和 AI 优化版本，让当前投递版本始终清晰可见。"
      >
        <template #actions>
          <el-button @click="router.push('/resumes')">返回简历</el-button>
          <el-button type="primary" :icon="Plus" @click="openCreate">创建版本</el-button>
          <el-button :icon="MagicStick" @click="openOptimize" :disabled="versions.length === 0">AI 优化</el-button>
        </template>
      </PageHero>
    </template>

    <section class="version-content" :aria-busy="loading">
      <div v-if="loading" class="version-loading" aria-live="polite" aria-label="正在加载简历版本">
        <el-skeleton :rows="6" animated />
      </div>

      <div v-else-if="loadError" class="version-error" role="alert">
        <div>
          <strong>版本列表暂时无法加载</strong>
          <span>请稍后重试，当前简历及已有版本不会受到影响。</span>
        </div>
        <el-button data-version-retry type="primary" plain @click="loadData">重新加载</el-button>
      </div>

      <AppEmpty
        v-else-if="!sortedVersions.length"
        title="还没有可管理的简历版本"
        description="创建第一个版本，开始维护针对不同岗位的简历内容。"
        hint="后续可以编辑、对比或使用 AI 生成岗位定制版本。"
      >
        <el-button data-version-empty-action type="primary" @click="openCreate">创建第一个版本</el-button>
      </AppEmpty>

      <ol v-else class="version-timeline" aria-label="简历版本时间线">
        <li
          v-for="row in sortedVersions"
          :key="row.versionId"
          class="version-timeline__item"
          :class="{ 'version-timeline__item--current': row.isCurrent === 1 }"
        >
          <div class="version-timeline__rail" aria-hidden="true">
            <span></span>
          </div>
          <article class="version-card">
            <header class="version-card__header">
              <div class="version-card__title">
                <div class="version-card__tags">
                  <el-tag v-if="row.isCurrent === 1" type="success">当前版本</el-tag>
                  <el-tag effect="plain">{{ versionTypeLabel(row.versionType) }}</el-tag>
                </div>
                <h2 :title="displayVersionName(row)">{{ displayVersionName(row) }}</h2>
                <div class="version-card__identity">
                  <span>版本 #{{ row.versionId }}</span>
                  <time :datetime="row.createdAt">{{ formatDateTime(row.createdAt) }}</time>
                </div>
              </div>
              <div class="version-actions" role="group" :aria-label="`${displayVersionName(row)}操作`">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button link type="primary" :disabled="row.isCurrent === 1" @click="setCurrent(row)">设当前</el-button>
                <el-button link type="warning" @click="prepareCompare(row)">对比</el-button>
                <el-button
                  link
                  type="danger"
                  :disabled="row.isCurrent === 1 || row.versionType === 'ORIGINAL'"
                  @click="removeVersion(row)"
                >删除</el-button>
              </div>
            </header>
            <dl class="version-card__facts">
              <div>
                <dt>目标岗位</dt>
                <dd>{{ row.targetJobTitle ? `${row.targetCompanyName || ''} ${row.targetJobTitle}` : '未指定' }}</dd>
              </div>
              <div>
                <dt>内容摘要</dt>
                <dd>{{ row.contentSummary || '暂无摘要' }}</dd>
              </div>
            </dl>
          </article>
        </li>
      </ol>
    </section>

    <el-dialog v-model="editVisible" :title="editingVersionId ? '编辑版本' : '创建版本'" :width="editDialogWidth">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="版本名称">
          <el-input v-model="editForm.versionName" placeholder="例如：Java 后端实习优化版" />
        </el-form-item>
        <el-form-item v-if="!editingVersionId" label="版本类型">
          <el-select v-model="editForm.versionType">
            <el-option label="手动编辑版本" value="MANUAL" />
            <el-option label="岗位定制版本" value="JOB_TARGETED" />
            <el-option label="导入版本" value="IMPORTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本内容">
          <el-input v-model="editForm.content" type="textarea" :rows="14" placeholder="粘贴或编辑简历文本" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveVersion">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="版本详情" :size="detailDrawerSize">
      <div v-if="detail" class="detail-stack">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="版本名称">{{ displayVersionName(detail) }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ versionTypeLabel(detail.versionType) }}</el-descriptions-item>
          <el-descriptions-item label="目标岗位">{{ detail.targetJobTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前">{{ detail.isCurrent === 1 ? '是' : '否' }}</el-descriptions-item>
        </el-descriptions>
        <el-input v-model="detail.content" type="textarea" :rows="18" readonly />
      </div>
    </el-drawer>

    <el-dialog v-model="compareVisible" title="版本对比" :width="compareDialogWidth">
      <el-form class="compare-picker" label-position="top">
        <el-form-item label="旧版本">
          <el-select v-model="compareForm.oldVersionId" filterable>
            <el-option v-for="item in versions" :key="item.versionId" :label="displayVersionName(item)" :value="item.versionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="新版本">
          <el-select v-model="compareForm.newVersionId" filterable>
            <el-option v-for="item in versions" :key="item.versionId" :label="displayVersionName(item)" :value="item.versionId" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="loadCompare">开始对比</el-button>
      </el-form>

      <div v-if="compareResult" class="diff-grid">
        <section>
          <h3>新增内容 {{ compareResult.addedCount }}</h3>
          <el-tag v-for="line in compareResult.addedLines" :key="line" class="diff-line" type="success">{{ line }}</el-tag>
        </section>
        <section>
          <h3>删除内容 {{ compareResult.removedCount }}</h3>
          <el-tag v-for="line in compareResult.removedLines" :key="line" class="diff-line" type="danger">{{ line }}</el-tag>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="optimizeVisible" title="AI 优化简历版本" :width="optimizeDialogWidth">
      <el-alert class="dialog-alert" type="warning" :closable="false" title="AI 优化结果仅供参考，投递前请自行核对真实性。" />
      <el-form :model="optimizeForm" label-position="top">
        <el-form-item label="来源版本">
          <el-select v-model="optimizeForm.sourceVersionId" filterable>
            <el-option v-for="item in versions" :key="item.versionId" :label="displayVersionName(item)" :value="item.versionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标岗位">
          <el-select v-model="optimizeForm.targetJobId" filterable>
            <el-option v-for="item in jobs" :key="item.jobId" :label="`${item.companyName} - ${item.jobTitle}`" :value="item.jobId" />
          </el-select>
        </el-form-item>
        <el-form-item label="AI 分析报告">
          <el-select v-model="optimizeForm.aiReportId" clearable filterable>
            <el-option v-for="item in filteredAnalysisReports" :key="item.reportId" :label="`${item.companyName || '未知公司'} - ${item.jobTitle || '未知岗位'} - ${item.matchScore || 0}分`" :value="item.reportId" />
          </el-select>
        </el-form-item>
        <el-form-item label="新版本名称">
          <el-input v-model="optimizeForm.versionName" placeholder="例如：腾讯 Java 后端定制版" />
        </el-form-item>
        <el-form-item label="额外优化要求">
          <el-input v-model="optimizeForm.extraRequirement" type="textarea" :rows="4" placeholder="例如：突出 Spring Boot、Redis、MySQL 项目经验" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="optimizeVisible = false">取消</el-button>
        <el-button type="primary" :loading="optimizing" @click="optimizeVersion">生成版本</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import PageHero from '@/components/common/PageHero.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import { getJobListApi } from '@/api/job'
import { getAnalysisReportsApi } from '@/api/analysis'
import {
  compareResumeVersionsApi,
  createResumeVersionApi,
  deleteResumeVersionApi,
  getResumeVersionDetailApi,
  getResumeVersionListApi,
  optimizeResumeVersionApi,
  setCurrentResumeVersionApi,
  updateResumeVersionApi
} from '@/api/resumeVersion'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'
import { useAiTaskCenterStore } from '@/stores/aiTaskCenter'

const route = useRoute()
const router = useRouter()
const resumeId = Number(route.params.resumeId)
const aiTaskCenter = useAiTaskCenterStore()

const loading = ref(false)
const loadError = ref(false)
const saving = ref(false)
const optimizing = ref(false)
const versions = ref<any[]>([])
const jobs = ref<any[]>([])
const analysisReports = ref<any[]>([])
const editVisible = ref(false)
const detailVisible = ref(false)
const compareVisible = ref(false)
const optimizeVisible = ref(false)
const { responsiveDialogWidth, responsiveDrawerSize } = useResponsiveSize()
const editDialogWidth = responsiveDialogWidth('720px')
const compareDialogWidth = responsiveDialogWidth('880px')
const optimizeDialogWidth = responsiveDialogWidth('620px')
const detailDrawerSize = responsiveDrawerSize('52%')
const detail = ref<any>(null)
const compareResult = ref<any>(null)
const editingVersionId = ref<number>()

const editForm = reactive({ versionName: '', versionType: 'MANUAL', content: '' })
const compareForm = reactive<{ oldVersionId?: number; newVersionId?: number }>({})
const optimizeForm = reactive<any>({})

const filteredAnalysisReports = computed(() => {
  return analysisReports.value.filter((item) => {
    const sameResume = item.resumeId === resumeId
    const sameJob = !optimizeForm.targetJobId || item.jobId === optimizeForm.targetJobId
    return sameResume && sameJob
  })
})

const sortedVersions = computed(() => {
  return [...versions.value].sort((left, right) => {
    const currentDifference = Number(right.isCurrent === 1) - Number(left.isCurrent === 1)
    if (currentDifference !== 0) return currentDifference
    return String(right.createdAt || '').localeCompare(String(left.createdAt || ''))
  })
})

async function loadData() {
  loading.value = true
  loadError.value = false
  try {
    const [versionRes, jobRes, analysisRes]: any[] = await Promise.all([
      getResumeVersionListApi(resumeId),
      getJobListApi({ pageNum: 1, pageSize: 100 }),
      getAnalysisReportsApi({ resumeId, pageNum: 1, pageSize: 100 })
    ])
    versions.value = versionRes || []
    jobs.value = jobRes.records || []
    analysisReports.value = analysisRes.records || []
  } catch {
    versions.value = []
    jobs.value = []
    analysisReports.value = []
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function openVersionFromQuery() {
  const versionId = Number(route.query.versionId)
  if (!Number.isFinite(versionId) || versionId <= 0) return
  const row = versions.value.find((item) => item.versionId === versionId)
  if (row) {
    openDetail(row)
  }
}

function openCreate() {
  editingVersionId.value = undefined
  editForm.versionName = ''
  editForm.versionType = 'MANUAL'
  editForm.content = ''
  editVisible.value = true
}

async function openDetail(row: any) {
  detail.value = await getResumeVersionDetailApi(resumeId, row.versionId)
  detailVisible.value = true
}

async function openEdit(row: any) {
  const current: any = await getResumeVersionDetailApi(resumeId, row.versionId)
  editingVersionId.value = row.versionId
  editForm.versionName = displayVersionName(current)
  editForm.versionType = current.versionType
  editForm.content = current.content
  editVisible.value = true
}

async function saveVersion() {
  if (!editForm.versionName || !editForm.content) {
    ElMessage.warning('请填写版本名称和内容')
    return
  }
  saving.value = true
  try {
    if (editingVersionId.value) {
      await updateResumeVersionApi(resumeId, editingVersionId.value, {
        versionName: editForm.versionName,
        content: editForm.content
      })
    } else {
      await createResumeVersionApi(resumeId, { ...editForm })
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function setCurrent(row: any) {
  await setCurrentResumeVersionApi(resumeId, row.versionId)
  ElMessage.success('已设置当前版本')
  loadData()
}

async function removeVersion(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除「${displayVersionName(row)}」？`, '删除版本', { type: 'warning' })
  } catch {
    return
  }
  await deleteResumeVersionApi(resumeId, row.versionId)
  ElMessage.success('删除成功')
  loadData()
}

function prepareCompare(row: any) {
  compareForm.oldVersionId = versions.value.find((item) => item.versionId !== row.versionId)?.versionId
  compareForm.newVersionId = row.versionId
  compareResult.value = null
  compareVisible.value = true
}

async function loadCompare() {
  if (!compareForm.oldVersionId || !compareForm.newVersionId || compareForm.oldVersionId === compareForm.newVersionId) {
    ElMessage.warning('请选择两个不同版本')
    return
  }
  compareResult.value = await compareResumeVersionsApi(resumeId, compareForm.oldVersionId, compareForm.newVersionId)
}

function openOptimize() {
  optimizeForm.sourceVersionId = versions.value.find((item) => item.isCurrent === 1)?.versionId || versions.value[0]?.versionId
  optimizeForm.targetJobId = jobs.value[0]?.jobId
  optimizeForm.aiReportId = undefined
  optimizeForm.versionName = ''
  optimizeForm.extraRequirement = ''
  optimizeVisible.value = true
}

async function optimizeVersion() {
  if (!optimizeForm.sourceVersionId || !optimizeForm.targetJobId) {
    ElMessage.warning('请选择来源版本和目标岗位')
    return
  }
  optimizing.value = true
  const localTaskId = aiTaskCenter.createTask({
    type: 'RESUME_OPTIMIZE',
    title: 'AI 简历版本优化',
    message: '正在调用 AI 生成优化后的简历版本...',
    status: 'CALLING_AI',
    progress: 35,
    sourcePath: `/resumes/${resumeId}/versions`
  })
  try {
    const result: any = await optimizeResumeVersionApi(resumeId, { ...optimizeForm })
    const versionId = result?.versionId
    aiTaskCenter.completeTask(localTaskId, {
      resultId: versionId,
      resultPath: versionId ? `/resumes/${resumeId}/versions?versionId=${versionId}` : `/resumes/${resumeId}/versions`,
      message: 'AI 优化版本已生成，可查看新版本内容'
    })
    ElMessage.success('AI 优化版本已生成')
    optimizeVisible.value = false
    await loadData()
    openVersionFromQuery()
  } catch (e: any) {
    aiTaskCenter.failTask(localTaskId, e?.message || e?.response?.data?.message || 'AI 优化版本生成失败')
    throw e
  } finally {
    optimizing.value = false
  }
}

function displayVersionName(row: any) {
  if (!row) {
    return '-'
  }
  if (String(row.versionType || '').toUpperCase() === 'ORIGINAL' && isLikelyMojibake(row.versionName)) {
    return '原始版本'
  }
  return row.versionName || versionTypeLabel(row.versionType)
}

function versionTypeLabel(type?: string) {
  const labels: Record<string, string> = {
    ORIGINAL: '原始版本',
    MANUAL: '手动编辑',
    JOB_TARGETED: '岗位定制',
    IMPORTED: '导入版本',
    AI_OPTIMIZED: 'AI 优化'
  }
  return labels[String(type || '').toUpperCase()] || type || '-'
}

function isLikelyMojibake(value?: string) {
  if (!value) {
    return false
  }
  return /[鐟鐢闂濞婵閭绠鍘宀椾綅]/.test(value)
}

watch(
  () => route.query.versionId,
  () => openVersionFromQuery()
)

onMounted(async () => {
  await loadData()
  openVersionFromQuery()
})
</script>

<style scoped>
.version-content {
  min-height: 320px;
}

.version-loading,
.version-error,
.version-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.version-loading {
  padding: var(--space-5);
}

.version-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  border-color: color-mix(in srgb, var(--color-danger) 28%, var(--color-border));
  background: color-mix(in srgb, var(--color-danger) 6%, var(--color-surface));
}

.version-error div {
  display: grid;
  gap: var(--space-1);
}

.version-error strong {
  color: var(--color-text);
}

.version-error span {
  color: var(--color-text-muted);
  font-size: 13px;
}

.version-timeline {
  display: grid;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.version-timeline__item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: var(--space-3);
}

.version-timeline__rail {
  display: flex;
  position: relative;
  justify-content: center;
}

.version-timeline__rail::after {
  position: absolute;
  top: 24px;
  bottom: 0;
  width: 2px;
  background: var(--color-border);
  content: '';
}

.version-timeline__item:last-child .version-timeline__rail::after {
  display: none;
}

.version-timeline__rail span {
  width: 14px;
  height: 14px;
  margin-top: var(--space-5);
  border: 3px solid var(--color-surface);
  border-radius: 50%;
  background: var(--color-border-strong);
  box-shadow: 0 0 0 2px var(--color-border);
  z-index: 1;
}

.version-timeline__item--current .version-timeline__rail span {
  background: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary-border);
}

.version-card {
  min-width: 0;
  margin-bottom: var(--space-4);
  padding: var(--space-4);
}

.version-timeline__item--current .version-card {
  border-color: var(--color-primary-border);
}

.version-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.version-card__title {
  min-width: 0;
}

.version-card__tags,
.version-card__identity,
.version-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
}

.version-card__title h2 {
  margin: var(--space-2) 0;
  color: var(--color-text);
  font-size: 18px;
  overflow-wrap: anywhere;
}

.version-card__identity {
  color: var(--color-text-muted);
  font-size: 13px;
}

.version-card__identity span:first-child {
  font-variant-numeric: tabular-nums;
}

.version-actions {
  flex: 0 0 auto;
  justify-content: flex-end;
}

.version-card__facts {
  display: grid;
  grid-template-columns: minmax(180px, 0.65fr) minmax(0, 1.35fr);
  gap: var(--space-4);
  margin: var(--space-4) 0 0;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

.version-card__facts div {
  min-width: 0;
}

.version-card__facts dt {
  margin-bottom: var(--space-1);
  color: var(--color-text-muted);
  font-size: 12px;
}

.version-card__facts dd {
  margin: 0;
  color: var(--color-text);
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.detail-stack {
  display: grid;
  gap: var(--space-4);
}

.compare-picker {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: var(--space-3);
  align-items: end;
}

.diff-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
  margin-top: var(--space-5);
}

.diff-line {
  display: flex;
  width: fit-content;
  max-width: 100%;
  height: auto;
  margin: 0 0 8px;
  white-space: normal;
}

.dialog-alert {
  margin-bottom: var(--space-3);
}

@media (max-width: 900px) {
  .version-card__header,
  .version-error {
    align-items: stretch;
    flex-direction: column;
  }

  .version-actions {
    justify-content: flex-start;
  }

  .version-card__facts,
  .compare-picker,
  .diff-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .version-timeline__item {
    grid-template-columns: 18px minmax(0, 1fr);
    gap: var(--space-2);
  }

  .version-card {
    padding: var(--space-3);
  }

  .version-actions {
    gap: var(--space-1);
  }
}
</style>
