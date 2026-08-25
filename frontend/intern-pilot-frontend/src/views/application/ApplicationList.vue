<template>
  <PageContainer title="投递追踪" description="跟踪岗位投递进度、面试安排和结果反馈，沉淀完整求职流程。">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreate">创建投递</el-button>
    </template>

    <div v-loading="loading" class="application-summary-grid">
      <StatCard label="投递总数" :value="applicationStats.total" :icon="Briefcase" />
      <StatCard label="面试中" :value="applicationStats.interviewing" :icon="Calendar" />
      <StatCard label="Offer 数" :value="applicationStats.offer" :icon="CircleCheck" />
      <StatCard label="待跟进" :value="applicationStats.followUp" :icon="Bell" />
    </div>

    <FilterBar @reset="resetQuery">
      <template #filters>
        <el-input v-model="query.keyword" aria-label="搜索公司或岗位" placeholder="搜索公司或岗位" clearable @keyup.enter="loadApplications" />
        <el-select v-model="query.status" aria-label="按投递状态筛选" placeholder="投递状态" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="priorityFilter" aria-label="按优先级筛选" placeholder="优先级" clearable>
          <el-option label="高" value="HIGH" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="低" value="LOW" />
        </el-select>
      </template>
      <template #actions>
        <el-button type="primary" @click="loadApplications">应用筛选</el-button>
      </template>
    </FilterBar>

    <section v-loading="loading" class="application-board application-track" aria-label="投递进度列表">
      <AppEmpty
        v-if="!filteredApplications.length && !loading"
        title="暂无投递记录"
        description="记录投递状态、面试时间和复盘内容"
        hint="可以从岗位列表、岗位推荐或分析报告开始创建投递记录。"
      >
        <el-button type="primary" :icon="Plus" @click="openCreate">创建投递</el-button>
      </AppEmpty>

      <article
        v-for="item in filteredApplications"
        v-else
        :key="item.applicationId"
        class="application-card"
        role="group"
        :aria-label="`${item.companyName || '未知公司'} ${item.jobTitle || '未知岗位'}投递记录`"
      >
        <div class="stage-panel" :class="stageClass(item.status)" :aria-label="`投递状态：${statusLabels[item.status] || item.status}`">
          <el-icon data-application-status-icon aria-hidden="true"><component :is="stageIcon(item.status)" /></el-icon>
          <strong>{{ stageLabel(item.status) }}</strong>
          <span>{{ priorityLabel(item.priority) }}</span>
        </div>

        <div class="application-main">
          <div class="application-heading">
            <div>
              <span class="company-name">{{ item.companyName || '未知公司' }}</span>
              <h3>{{ item.jobTitle || '未知岗位' }}</h3>
            </div>
            <div class="status-tags">
              <el-tag :type="statusTypes[item.status] || 'info'" effect="plain">{{ statusLabels[item.status] || item.status }}</el-tag>
              <el-tag :type="priorityTagType(item.priority)" effect="plain">{{ priorityLabel(item.priority) }}</el-tag>
            </div>
          </div>

          <div class="application-meta">
            <span>投递：{{ item.applyDate || '暂未填写' }}</span>
            <span>面试：{{ item.interviewDate ? formatDateTime(item.interviewDate) : '暂未安排' }}</span>
            <span>简历：{{ item.resumeName || '未关联' }}</span>
            <span>匹配分：{{ item.matchScore ?? '-' }}</span>
          </div>

          <div class="next-step" data-application-next-action>
            <strong>下一步</strong>
            <p>{{ nextStepText(item) }}</p>
          </div>

          <p class="application-note">{{ item.note || '暂无备注，可补充投递渠道、沟通记录或复盘要点。' }}</p>
        </div>

        <div class="application-actions" role="group" :aria-label="`${item.companyName || '未知公司'}投递操作`">
          <el-button type="primary" @click="openDetail(item.applicationId)">详情</el-button>
          <el-button @click="openStatus(item)">改状态</el-button>
          <el-button @click="openNote(item)">备注</el-button>
          <el-button type="danger" plain @click="removeApplication(item)">删除</el-button>
        </div>
      </article>
    </section>

    <el-dialog v-model="createVisible" title="创建投递记录" :width="createDialogWidth">
      <el-form :model="createForm" label-position="top">
        <el-form-item label="岗位">
          <el-select v-model="createForm.jobId" filterable placeholder="请选择岗位">
            <el-option v-for="item in jobs" :key="item.jobId" :label="`${item.companyName} - ${item.jobTitle}`" :value="item.jobId" />
          </el-select>
        </el-form-item>
        <div class="form-grid two">
          <el-form-item label="简历">
            <el-select v-model="createForm.resumeId" clearable placeholder="可选，关联投递简历">
              <el-option v-for="item in resumes" :key="item.resumeId" :label="item.resumeName || item.originalFileName" :value="item.resumeId" />
            </el-select>
          </el-form-item>
          <el-form-item label="分析报告">
            <el-select v-model="createForm.reportId" clearable placeholder="可选，关联 AI 分析报告">
              <el-option v-for="item in reports" :key="item.reportId" :label="`${item.companyName || '报告'} - ${item.matchScore}`" :value="item.reportId" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid three">
          <el-form-item label="状态">
            <el-select v-model="createForm.status">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级">
            <el-select v-model="createForm.priority">
              <el-option label="高" value="HIGH" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="低" value="LOW" />
            </el-select>
          </el-form-item>
          <el-form-item label="投递日期">
            <el-date-picker v-model="createForm.applyDate" value-format="YYYY-MM-DD" type="date" />
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="createForm.note" type="textarea" :rows="3" placeholder="例如：Boss 直聘沟通中，已约下周一技术面" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="createApplication">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="修改投递状态" :width="statusDialogWidth">
      <el-select v-model="statusForm.status">
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStatus">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="noteVisible" title="备注与复盘" :width="noteDialogWidth">
      <el-form label-position="top">
        <el-form-item label="备注"><el-input v-model="noteForm.note" type="textarea" :rows="3" placeholder="记录投递渠道、HR 沟通和下一步事项" /></el-form-item>
        <el-form-item label="复盘"><el-input v-model="noteForm.review" type="textarea" :rows="4" placeholder="记录笔试、面试反馈和改进点" /></el-form-item>
        <el-form-item label="面试时间">
          <el-date-picker v-model="noteForm.interviewDate" value-format="YYYY-MM-DDTHH:mm:ss" type="datetime" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noteVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNote">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="投递详情" :size="detailDrawerSize">
      <div v-if="detail" class="detail-stack">
        <section class="detail-hero">
          <div>
            <span class="company-name">{{ detail.companyName || '未知公司' }}</span>
            <h2>{{ detail.jobTitle || '未知岗位' }}</h2>
            <div class="application-meta">
              <span>投递：{{ detail.applyDate || '暂未填写' }}</span>
              <span>更新：{{ formatDateTime(detail.updatedAt) }}</span>
            </div>
          </div>
          <el-tag :type="statusTypes[detail.status] || 'info'" effect="plain">{{ statusLabels[detail.status] || detail.status }}</el-tag>
        </section>

        <section class="panel flat">
          <div class="timeline-title">
            <h3>投递时间线</h3>
            <span>{{ nextStepText(detail) }}</span>
          </div>
          <el-steps :active="timelineActive(detail.status)" finish-status="success" align-center>
            <el-step title="待投递" />
            <el-step title="已投递" />
            <el-step title="笔试/面试" />
            <el-step title="结果" />
          </el-steps>
        </section>

        <section class="panel flat">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="状态">{{ statusLabels[detail.status] || detail.status }}</el-descriptions-item>
            <el-descriptions-item label="优先级">{{ priorityLabel(detail.priority) }}</el-descriptions-item>
            <el-descriptions-item label="匹配分">{{ detail.matchScore || '-' }}</el-descriptions-item>
            <el-descriptions-item label="匹配等级">{{ detail.matchLevel || '-' }}</el-descriptions-item>
            <el-descriptions-item label="简历">{{ detail.resumeName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="面试时间">{{ detail.interviewDate ? formatDateTime(detail.interviewDate) : '暂未安排' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="two-column">
          <div class="panel flat text-block"><h4>备注</h4><p>{{ detail.note || '暂无备注' }}</p></div>
          <div class="panel flat text-block"><h4>复盘</h4><p>{{ detail.review || '暂无复盘' }}</p></div>
        </section>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Briefcase, Calendar, CircleCheck, CircleClose, Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import StatCard from '@/components/common/StatCard.vue'
import { createApplicationApi, deleteApplicationApi, getApplicationDetailApi, getApplicationListApi, updateApplicationNoteApi, updateApplicationStatusApi } from '@/api/application'
import { getAnalysisReportsApi } from '@/api/analysis'
import { getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'
import { formatDateTime, statusLabels, statusOptions, statusTypes } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'

const applications = ref<any[]>([])
const jobs = ref<any[]>([])
const resumes = ref<any[]>([])
const reports = ref<any[]>([])
const detail = ref<any>(null)
const loading = ref(false)
const saving = ref(false)
const createVisible = ref(false)
const statusVisible = ref(false)
const noteVisible = ref(false)
const detailVisible = ref(false)
const currentId = ref<number>()
const priorityFilter = ref('')
const query = reactive({ keyword: '', status: '' })
const createForm = reactive<any>({ jobId: undefined, resumeId: undefined, reportId: undefined, status: 'TO_APPLY', priority: 'MEDIUM', applyDate: '', note: '' })
const statusForm = reactive({ status: 'TO_APPLY' })
const noteForm = reactive({ note: '', review: '', interviewDate: '' })
const { responsiveDialogWidth, responsiveDrawerSize } = useResponsiveSize()
const createDialogWidth = responsiveDialogWidth('620px')
const statusDialogWidth = responsiveDialogWidth('360px')
const noteDialogWidth = responsiveDialogWidth('560px')
const detailDrawerSize = responsiveDrawerSize('52%')
let applicationLoadEpoch = 0
let applicationPageActive = true

const filteredApplications = computed(() => {
  if (!priorityFilter.value) return applications.value
  return applications.value.filter((item) => item.priority === priorityFilter.value)
})

const applicationStats = computed(() => ({
  total: applications.value.length,
  interviewing: applications.value.filter((item) => ['WRITTEN_TEST', 'FIRST_INTERVIEW', 'SECOND_INTERVIEW', 'HR_INTERVIEW'].includes(item.status)).length,
  offer: applications.value.filter((item) => item.status === 'OFFER').length,
  followUp: applications.value.filter((item) => ['TO_APPLY', 'APPLIED', 'WRITTEN_TEST', 'FIRST_INTERVIEW'].includes(item.status)).length
}))

async function loadApplications() {
  const epoch = ++applicationLoadEpoch
  const querySnapshot = { keyword: query.keyword, status: query.status, pageNum: 1, pageSize: 100 }
  loading.value = true
  try {
    const res: any = await getApplicationListApi(querySnapshot)
    const hydrated = await hydrateApplications(res.records || [])
    if (applicationPageActive && epoch === applicationLoadEpoch) {
      applications.value = hydrated
    }
  } finally {
    if (applicationPageActive && epoch === applicationLoadEpoch) {
      loading.value = false
    }
  }
}

async function hydrateApplications(records: any[]) {
  return Promise.all(
    records.map(async (item) => {
      try {
        const detailData = (await getApplicationDetailApi(item.applicationId)) as any
        return { ...item, ...detailData }
      } catch {
        return item
      }
    })
  )
}

async function loadOptions() {
  const [jobRes, resumeRes, reportRes]: any[] = await Promise.all([
    getJobListApi({ pageNum: 1, pageSize: 100 }),
    getResumeListApi({ pageNum: 1, pageSize: 100 }),
    getAnalysisReportsApi({ pageNum: 1, pageSize: 100 })
  ])
  jobs.value = jobRes.records || []
  resumes.value = resumeRes.records || []
  reports.value = reportRes.records || []
}

function resetQuery() {
  query.keyword = ''
  query.status = ''
  priorityFilter.value = ''
  loadApplications()
}

function openCreate() {
  Object.assign(createForm, { jobId: undefined, resumeId: undefined, reportId: undefined, status: 'TO_APPLY', priority: 'MEDIUM', applyDate: '', note: '' })
  createVisible.value = true
}

async function createApplication() {
  if (!createForm.jobId) {
    ElMessage.warning('请选择岗位')
    return
  }
  saving.value = true
  try {
    await createApplicationApi(createForm)
    ElMessage.success('创建成功')
    createVisible.value = false
    loadApplications()
  } finally {
    saving.value = false
  }
}

function openStatus(row: any) {
  currentId.value = row.applicationId
  statusForm.status = row.status
  statusVisible.value = true
}

async function saveStatus() {
  await updateApplicationStatusApi(currentId.value!, statusForm)
  ElMessage.success('状态已更新')
  statusVisible.value = false
  loadApplications()
}

function openNote(row: any) {
  currentId.value = row.applicationId
  noteForm.note = row.note || ''
  noteForm.review = row.review || ''
  noteForm.interviewDate = row.interviewDate || ''
  noteVisible.value = true
}

async function saveNote() {
  await updateApplicationNoteApi(currentId.value!, noteForm)
  ElMessage.success('备注已更新')
  noteVisible.value = false
  loadApplications()
}

async function openDetail(id: number) {
  detail.value = await getApplicationDetailApi(id)
  detailVisible.value = true
}

async function removeApplication(row: any) {
  await ElMessageBox.confirm(
    `确认删除“${row.companyName || '未知公司'} - ${row.jobTitle || '未知岗位'}”的投递记录吗？删除后不可恢复。`,
    '删除投递',
    { type: 'warning' }
  )
  await deleteApplicationApi(row.applicationId)
  ElMessage.success('已删除')
  loadApplications()
}

function priorityLabel(priority?: string) {
  const labels: Record<string, string> = { HIGH: '高优先级', MEDIUM: '中优先级', LOW: '低优先级' }
  return labels[priority || ''] || priority || '未设置'
}

function priorityTagType(priority?: string) {
  if (priority === 'HIGH') return 'danger'
  if (priority === 'LOW') return 'info'
  return 'warning'
}

function stageLabel(status?: string) {
  if (status === 'OFFER') return 'Offer'
  if (['FIRST_INTERVIEW', 'SECOND_INTERVIEW', 'HR_INTERVIEW'].includes(status || '')) return '面试中'
  if (status === 'WRITTEN_TEST') return '笔试中'
  if (status === 'APPLIED') return '已投递'
  if (status === 'TO_APPLY') return '待投递'
  if (['REJECTED', 'GIVEN_UP'].includes(status || '')) return '已结束'
  return '跟进中'
}

function stageClass(status?: string) {
  if (status === 'OFFER') return 'success'
  if (['WRITTEN_TEST', 'FIRST_INTERVIEW', 'SECOND_INTERVIEW', 'HR_INTERVIEW'].includes(status || '')) return 'warning'
  if (['REJECTED', 'GIVEN_UP'].includes(status || '')) return 'danger'
  if (status === 'APPLIED') return 'primary'
  return 'info'
}

function stageIcon(status?: string) {
  if (status === 'OFFER') return CircleCheck
  if (['WRITTEN_TEST', 'FIRST_INTERVIEW', 'SECOND_INTERVIEW', 'HR_INTERVIEW'].includes(status || '')) return Calendar
  if (['REJECTED', 'GIVEN_UP'].includes(status || '')) return CircleClose
  if (status === 'APPLIED') return Briefcase
  return Bell
}

function nextStepText(item: any) {
  const status = item.status
  if (status === 'TO_APPLY') return '完善简历和岗位信息后尽快投递。'
  if (status === 'APPLIED') return '关注 HR 回复，必要时 2-3 天后跟进。'
  if (status === 'WRITTEN_TEST') return '准备笔试题和基础知识复盘。'
  if (['FIRST_INTERVIEW', 'SECOND_INTERVIEW', 'HR_INTERVIEW'].includes(status)) return item.interviewDate ? '按面试时间准备项目介绍和追问。' : '补充面试时间并生成针对性面试题。'
  if (status === 'OFFER') return '记录 Offer 信息，准备横向比较和最终选择。'
  return '归档结果并记录复盘。'
}

function timelineActive(status?: string) {
  if (status === 'TO_APPLY') return 1
  if (status === 'APPLIED') return 2
  if (['WRITTEN_TEST', 'FIRST_INTERVIEW', 'SECOND_INTERVIEW', 'HR_INTERVIEW'].includes(status || '')) return 3
  if (['OFFER', 'REJECTED', 'GIVEN_UP'].includes(status || '')) return 4
  return 1
}

onMounted(() => {
  loadApplications()
  loadOptions()
})

onBeforeUnmount(() => {
  applicationPageActive = false
  applicationLoadEpoch += 1
})
</script>

<style scoped>
.application-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 20px;
}

.application-board {
  display: grid;
  gap: 14px;
}

:deep(.filter-bar__filters) {
  grid-template-columns: minmax(240px, 1.5fr) repeat(2, minmax(160px, 0.75fr));
}

.application-card {
  display: grid;
  grid-template-columns: 116px minmax(0, 1fr) auto;
  gap: 18px;
  padding: 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.stage-panel {
  display: grid;
  min-height: 116px;
  place-items: center;
  align-content: center;
  border-radius: 8px;
  text-align: center;
}

.stage-panel .el-icon {
  margin-bottom: 8px;
  font-size: 24px;
}

.stage-panel.success { background: #ecfdf3; color: #047857; }
.stage-panel.warning { background: #fffbeb; color: #b45309; }
.stage-panel.danger { background: #fef2f2; color: #dc2626; }
.stage-panel.primary { background: #eff6ff; color: #1d4ed8; }
.stage-panel.info { background: #f8fafc; color: #475467; }

.stage-panel strong {
  font-size: 20px;
}

.stage-panel span {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 700;
}

.application-heading,
.detail-hero,
.timeline-title {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.company-name {
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
}

.application-heading h3,
.detail-hero h2 {
  margin: 6px 0 0;
}

.status-tags,
.application-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.application-meta {
  margin: 12px 0;
  color: var(--color-text-soft);
  font-size: 13px;
}

.next-step {
  padding: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
}

.next-step strong {
  color: #1d4ed8;
}

.next-step p,
.application-note,
.text-block p {
  margin: 6px 0 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.application-actions {
  display: flex;
  width: 132px;
  flex-direction: column;
  gap: 8px;
}

.application-actions .el-button {
  width: 100%;
  margin-left: 0;
}

.detail-stack {
  display: grid;
  gap: 16px;
}

.detail-hero {
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.timeline-title {
  margin-bottom: 14px;
}

.timeline-title h3 {
  margin: 0;
}

.timeline-title span {
  color: var(--color-text-soft);
  font-size: 13px;
}

.two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.text-block h4 {
  margin: 0;
}

@media (max-width: 900px) {
  .application-summary-grid,
  .application-card,
  .two-column {
    grid-template-columns: 1fr;
  }

  .application-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
  }

  .application-heading,
  .detail-hero,
  .timeline-title {
    flex-direction: column;
  }

  :deep(.filter-bar__filters) {
    grid-template-columns: 1fr;
  }
}
</style>
