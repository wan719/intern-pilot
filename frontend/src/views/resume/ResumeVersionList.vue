<template>
  <PageContainer class="version-workspace" title="简历版本" width="wide">
    <template #hero>
      <RouterLink class="version-back" to="/resumes">
        <el-icon aria-hidden="true"><ArrowLeft /></el-icon>
        返回简历
      </RouterLink>
      <PageHero class="version-heading" title="简历版本" description="管理当前简历版本，查看和对比历次修改。">
        <template #actions>
          <el-button :icon="MagicStick" @click="openOptimize" :disabled="versions.length === 0">AI 优化</el-button>
          <Button class="version-create-action" type="button" @click="openCreate">
            <el-icon aria-hidden="true"><Plus /></el-icon>
            创建版本
          </Button>
        </template>
      </PageHero>
    </template>

    <div class="version-columns" :class="{ 'version-columns--split': isWide && versions.length && !loadError }">
      <div class="version-history">
        <section v-if="!loading && !loadError && versions.length" class="version-current" aria-labelledby="version-current-heading">
          <span class="version-current__icon" aria-hidden="true"><el-icon><Document /></el-icon></span>
          <div>
            <h2 id="version-current-heading">当前版本</h2>
            <strong>{{ currentVersion ? displayVersionName(currentVersion) : '尚未设置' }}</strong>
          </div>
          <div v-if="currentVersion" class="version-current__meta">
            <span>{{ versionTypeLabel(currentVersion.versionType) }} · 版本 #{{ currentVersion.versionId }}</span>
            <time v-if="currentVersion.createdAt" :datetime="currentVersion.createdAt">{{ formatDateTime(currentVersion.createdAt) }}</time>
          </div>
        </section>

        <section class="version-content" aria-labelledby="version-history-heading" :aria-busy="loading">
          <header class="version-history-heading">
            <h2 id="version-history-heading">版本历史</h2>
            <span v-if="!loading && !loadError">{{ versions.length }} 个版本</span>
          </header>
          <div v-if="loading" class="version-loading" aria-live="polite" aria-label="正在加载简历版本">
            <el-skeleton :rows="6" animated />
          </div>

          <div v-else-if="loadError" class="version-error" role="alert">
            <div>
              <strong>版本列表暂时无法加载</strong>
              <span>请稍后重试。</span>
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

          <ol v-else class="version-timeline" role="list" aria-label="简历版本时间线">
            <li
              v-for="row in sortedVersions"
              :key="row.versionId"
              class="version-timeline__item"
              :class="{
                'version-timeline__item--current': row.isCurrent === 1,
                'version-timeline__item--selected': isWide && selectedVersion?.versionId === row.versionId
              }"
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
                    <h3>{{ displayVersionName(row) }}</h3>
                    <div class="version-card__identity">
                      <span>版本 #{{ row.versionId }}</span>
                      <time :datetime="row.createdAt">{{ formatDateTime(row.createdAt) }}</time>
                    </div>
                  </div>
                  <div class="version-actions" role="group" :aria-label="`${displayVersionName(row)}操作`">
                    <el-button
                      link
                      type="primary"
                      :aria-controls="isWide ? 'version-preview-panel' : undefined"
                      :aria-pressed="isWide ? selectedVersion?.versionId === row.versionId : undefined"
                      @click="openDetail(row)"
                    >详情</el-button>
                    <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                    <el-button link type="primary" :disabled="row.isCurrent === 1" @click="setCurrent(row)">设当前</el-button>
                    <el-button link type="primary" @click="prepareCompare(row)">对比</el-button>
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
      </div>
      <aside
        v-if="isWide && versions.length && !loadError"
        id="version-preview-panel"
        class="version-detail"
        aria-label="版本详情"
      >
        <h2>版本详情</h2>
        <ResumeVersionPreview v-bind="preview" @retry="retryDetail" />
      </aside>
    </div>

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

    <el-drawer v-if="!isWide" v-model="detailVisible" title="版本详情" :size="detailDrawerSize">
      <ResumeVersionPreview v-bind="preview" @retry="retryDetail" />
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
        <el-button type="primary" :loading="comparing" @click="loadCompare">开始对比</el-button>
      </el-form>

      <p v-if="comparing" class="compare-status" role="status">正在对比版本…</p>
      <div v-else-if="compareError" class="compare-status compare-status--error" role="alert">
        <span>对比失败，请重试。</span>
        <el-button @click="loadCompare">重新对比</el-button>
      </div>
      <div v-else-if="compareResult" class="diff-grid" aria-live="polite">
        <section class="diff-panel diff-panel--added">
          <h3>新增内容 <span>{{ compareResult.addedCount }} 行</span></h3>
          <ul v-if="compareResult.addedLines.length" role="list">
            <li v-for="(line, index) in compareResult.addedLines" :key="index" class="diff-line">
              <span aria-hidden="true">+</span>{{ line }}
            </li>
          </ul>
          <p v-else>没有新增内容</p>
        </section>
        <section class="diff-panel diff-panel--removed">
          <h3>移除内容 <span>{{ compareResult.removedCount }} 行</span></h3>
          <ul v-if="compareResult.removedLines.length" role="list">
            <li v-for="(line, index) in compareResult.removedLines" :key="index" class="diff-line">
              <span aria-hidden="true">−</span>{{ line }}
            </li>
          </ul>
          <p v-else>没有移除内容</p>
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
import { RouterLink, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Document, MagicStick, Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import PageHero from '@/components/common/PageHero.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import { Button } from '@/components/ui/button'
import ResumeVersionPreview from './ResumeVersionPreview.vue'
import '@/styles/resume-utilities.css'
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
const { isMobile, isTablet, responsiveDialogWidth, responsiveDrawerSize } = useResponsiveSize()
const isWide = computed(() => !isMobile.value && !isTablet.value)
const editDialogWidth = responsiveDialogWidth('min(720px, calc(100vw - 28px))')
const compareDialogWidth = responsiveDialogWidth('min(880px, calc(100vw - 28px))')
const optimizeDialogWidth = responsiveDialogWidth('620px')
const detailDrawerSize = responsiveDrawerSize('52%')
const detail = ref<any>(null)
const selectedVersion = ref<{ versionId: number }>()
const detailLoading = ref(false)
const detailError = ref(false)
let detailRequest = 0
const comparing = ref(false)
const compareError = ref(false)
interface VersionComparison {
  addedCount: number
  removedCount: number
  addedLines: string[]
  removedLines: string[]
}

const compareResult = ref<VersionComparison | null>(null)
let compareRequest = 0
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

const currentVersion = computed(() => versions.value.find((item) => item.isCurrent === 1))

const sortedVersions = computed(() => {
  return [...versions.value].sort((left, right) => {
    const currentDifference = Number(right.isCurrent === 1) - Number(left.isCurrent === 1)
    if (currentDifference !== 0) return currentDifference
    return String(right.createdAt || '').localeCompare(String(left.createdAt || ''))
  })
})

const preview = computed(() => {
  const row = detail.value
  const summary = versions.value.find((item) => item.versionId === selectedVersion.value?.versionId)
  return {
    name: row ? displayVersionName(row) : undefined,
    type: row ? versionTypeLabel(row.versionType) : undefined,
    jobTitle: row?.targetJobTitle,
    current: (summary || row)?.isCurrent === 1,
    content: row?.content,
    loading: detailLoading.value,
    failed: detailError.value
  }
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
    if (selectedVersion.value && !versions.value.some((row) => row.versionId === selectedVersion.value?.versionId)) {
      detailRequest++
      selectedVersion.value = undefined
      detail.value = null
      detailLoading.value = false
      detailError.value = false
      detailVisible.value = false
    }
    if (!selectedVersion.value && !openVersionFromQuery()) loadDefaultDetail()
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
  if (!Number.isFinite(versionId) || versionId <= 0) return false
  const row = versions.value.find((item) => item.versionId === versionId)
  if (row) {
    void openDetail(row)
    return true
  }
  return false
}

function loadDefaultDetail() {
  const first = currentVersion.value || sortedVersions.value[0]
  if (isWide.value && !selectedVersion.value && first) void openDetail(first)
}

function openCreate() {
  editingVersionId.value = undefined
  editForm.versionName = ''
  editForm.versionType = 'MANUAL'
  editForm.content = ''
  editVisible.value = true
}

async function openDetail(row: { versionId: number }) {
  const request = ++detailRequest
  selectedVersion.value = row
  detail.value = null
  detailError.value = false
  detailLoading.value = true
  if (!isWide.value) detailVisible.value = true
  try {
    const result = await getResumeVersionDetailApi(resumeId, row.versionId)
    if (request === detailRequest) detail.value = result
  } catch {
    if (request === detailRequest) detailError.value = true
  } finally {
    if (request === detailRequest) detailLoading.value = false
  }
}

function retryDetail() {
  if (selectedVersion.value) void openDetail(selectedVersion.value)
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
    if (editingVersionId.value && selectedVersion.value?.versionId === editingVersionId.value && (isWide.value || detailVisible.value)) {
      retryDetail()
    }
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
  compareVisible.value = true
}

async function loadCompare() {
  if (!compareForm.oldVersionId || !compareForm.newVersionId || compareForm.oldVersionId === compareForm.newVersionId) {
    ElMessage.warning('请选择两个不同版本')
    return
  }
  const request = ++compareRequest
  comparing.value = true
  compareResult.value = null
  compareError.value = false
  try {
    const result = await compareResumeVersionsApi(resumeId, compareForm.oldVersionId, compareForm.newVersionId)
    if (request === compareRequest) compareResult.value = result as unknown as VersionComparison
  } catch {
    if (request === compareRequest) compareError.value = true
  } finally {
    if (request === compareRequest) comparing.value = false
  }
}

watch(
  [compareVisible, () => compareForm.oldVersionId, () => compareForm.newVersionId],
  () => {
    // A response only belongs to the selections and dialog that launched it.
    compareRequest++
    comparing.value = false
    compareResult.value = null
    compareError.value = false
  },
  { flush: 'sync' }
)

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

watch(isWide, (wide) => {
  if (!wide) return
  detailVisible.value = false
  loadDefaultDetail()
})

onMounted(loadData)
</script>

<style scoped>
.version-workspace {
  background: var(--color-bg);
  color: var(--color-text);
}

.version-columns {
  display: grid;
  gap: var(--space-5);
  align-items: start;
}

.version-columns--split {
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
}

.version-history,
.version-detail {
  min-width: 0;
}

.version-detail {
  min-height: 320px;
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.version-detail h2 {
  margin: 0 0 var(--space-4);
  font-size: 20px;
}

.version-columns--split .version-current {
  grid-template-columns: auto minmax(0, 1fr);
}

.version-columns--split .version-current__meta {
  grid-column: 2;
}

.version-columns--split .version-card__header {
  flex-wrap: wrap;
}

.version-columns--split .version-actions {
  justify-content: flex-start;
}

.version-back {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  gap: var(--space-1);
  margin-bottom: var(--space-2);
  color: var(--color-primary-hover);
  font-size: 13px;
  font-weight: 600;
}

.version-back:hover {
  text-decoration: underline;
}

.version-heading {
  align-items: flex-end;
  padding: 0;
  border: 0;
  background: transparent;
}

.version-heading :deep(h1) {
  font-size: clamp(28px, 4vw, 40px);
  line-height: 1.2;
}

.version-heading :deep(.page-hero__actions) {
  justify-content: flex-start;
}

.version-heading :deep(.el-button) {
  min-height: 44px;
  margin-left: 0;
}

.version-create-action {
  min-height: 44px;
  border: 0;
  cursor: pointer;
}

.version-back:focus-visible,
.version-create-action:focus-visible,
.version-heading :deep(.el-button:focus-visible),
.version-actions :deep(.el-button:focus-visible) {
  outline: 2px solid var(--color-primary-hover);
  outline-offset: 2px;
}

.version-current {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-primary-border);
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--color-primary-soft) 40%, var(--color-surface));
}

.version-current__icon {
  display: grid;
  width: 36px;
  height: 40px;
  place-items: center;
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  color: var(--color-primary-hover);
}

.version-current h2 {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 500;
}

.version-current strong {
  display: block;
  margin-top: 2px;
  font-size: 16px;
  overflow-wrap: anywhere;
}

.version-current__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1) var(--space-3);
  color: var(--color-text-muted);
  font-size: 13px;
}

.version-history-heading {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.version-history-heading h2 {
  margin: 0;
  font-size: 20px;
}

.version-history-heading span {
  color: var(--color-text-muted);
  font-size: 13px;
}

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

.version-timeline__item--selected .version-card {
  border-color: var(--color-primary-hover);
  background: color-mix(in srgb, var(--color-primary-soft) 18%, var(--color-surface));
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

.version-card__title h3 {
  margin: var(--space-2) 0 var(--space-1);
  color: var(--color-text);
  font-size: 17px;
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

.version-actions :deep(.el-button) {
  min-width: 40px;
  min-height: 40px;
  margin-left: 0;
  padding-inline: 4px;
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

.compare-picker {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
  gap: var(--space-3);
  align-items: end;
}

.compare-picker :deep(.el-form-item) {
  margin-bottom: 0;
}

.compare-status {
  margin-top: var(--space-4);
  color: var(--color-text-muted);
}

.compare-status--error {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-danger);
}

.diff-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  margin-top: var(--space-5);
}

.diff-panel {
  --diff-accent: var(--color-primary-hover);
  --diff-tint: var(--color-primary-soft);
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text);
}

.diff-panel--removed {
  --diff-accent: var(--color-danger);
  --diff-tint: color-mix(in srgb, var(--color-danger) 8%, var(--color-surface));
}

.diff-panel h3 {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-1) var(--space-2);
  margin: 0;
  padding: var(--space-2) var(--space-3);
  background: var(--diff-tint);
  font-size: 14px;
}

.diff-panel h3 span,
.diff-panel p {
  color: var(--color-text-muted);
  font-size: 13px;
}

.diff-panel ul,
.diff-panel p {
  margin: 0;
  padding: 0;
  list-style: none;
}

.diff-panel p,
.diff-line {
  padding: var(--space-2) var(--space-3);
}

.diff-line {
  display: grid;
  grid-template-columns: 1em minmax(0, 1fr);
  gap: var(--space-1);
  border-top: 1px solid var(--color-border);
  line-height: 1.6;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.diff-line > span {
  color: var(--diff-accent);
  font-weight: 700;
}

.dialog-alert {
  margin-bottom: var(--space-3);
}

@media (max-width: 940px) {
  .version-heading,
  .version-card__header,
  .version-error {
    align-items: stretch;
    flex-direction: column;
  }

  .version-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .version-current {
    grid-template-columns: auto minmax(0, 1fr);
    padding-inline: var(--space-3);
  }

  .version-current__meta {
    grid-column: 2;
  }

  .version-card__facts,
  .compare-picker,
  .diff-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .version-back,
  .version-actions :deep(.el-button),
  .compare-picker :deep(.el-button) {
    min-height: 44px;
  }

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
