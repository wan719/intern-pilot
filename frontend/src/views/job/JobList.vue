<template>
  <PageContainer title="岗位管理" description="按公司、岗位类型或地点查找目标岗位。" width="wide">
    <template #actions>
      <el-button v-if="auth.hasPermission('analysis:read')" @click="viewRecommendations">查看岗位推荐</el-button>
      <Button type="button" class="ip:min-h-11" @click="openCreate"><el-icon aria-hidden="true"><Plus /></el-icon>新建岗位</Button>
    </template>

    <FilterBar class="job-filters" @reset="resetQuery">
      <template #filters>
        <el-input v-model="query.keyword" aria-label="搜索公司或岗位" placeholder="搜索公司或岗位" clearable @keyup.enter="loadJobs" />
        <el-input v-model="query.jobType" aria-label="岗位类型" placeholder="岗位类型" clearable @keyup.enter="loadJobs" />
        <el-input v-model="query.location" aria-label="地点" placeholder="地点" clearable @keyup.enter="loadJobs" />
      </template>
      <template #actions>
        <el-button type="primary" @click="loadJobs">搜索</el-button>
      </template>
    </FilterBar>

    <div class="list-heading">
      <h2>岗位库</h2>
      <span v-if="!loading && !errorText" role="status">{{ jobs.length }} 个岗位</span>
    </div>

    <section v-if="errorText" class="panel state-panel" role="alert">
      <div>
        <strong>目标岗位暂时无法加载</strong>
        <p>{{ errorText }}</p>
      </div>
      <el-button data-job-retry type="primary" @click="loadJobs">重新加载</el-button>
    </section>

    <TableShell
      v-else
      class="job-library"
      :loading="loading"
      :empty="!jobs.length"
      :empty-title="hasAppliedFilters ? '没有符合条件的岗位' : '还没有目标岗位'"
      :empty-hint="hasAppliedFilters ? '调整公司、岗位类型或地点后重试。' : '新增岗位后即可用于 AI 匹配分析。'"
    >
      <template #empty-actions>
        <el-button v-if="hasAppliedFilters" @click="resetQuery">清除筛选</el-button>
        <el-button v-else data-job-empty-action type="primary" :icon="Plus" @click="openCreate">新建岗位</el-button>
      </template>

      <div class="job-card-list" aria-label="目标岗位列表">
        <article v-for="item in jobs" :key="item.jobId" class="job-card">
          <div class="job-card-main">
            <div class="job-card-heading">
              <div>
                <span class="company-name" :title="item.companyName || '未知公司'">{{ item.companyName || '未知公司' }}</span>
                <h3>{{ item.jobTitle || '未命名岗位' }}</h3>
              </div>
              <el-tag v-if="item.jobType" effect="plain">{{ item.jobType }}</el-tag>
            </div>

            <div class="job-meta">
              <span>{{ item.location || '地点未填写' }}</span>
              <span>{{ item.salaryRange || '薪资未填写' }}</span>
              <span>{{ item.sourcePlatform || '来源未填写' }}</span>
              <span>{{ formatDateTime(item.createdAt) }}</span>
            </div>

            <div class="job-jd">
              <p :id="`job-jd-${item.jobId}`" class="job-summary" :data-job-jd="item.jobId">{{ displayedJd(item) }}</p>
              <el-button
                v-if="shouldCollapseJd(item.jdContent)"
                :data-job-jd-toggle="item.jobId"
                class="job-jd-toggle"
                link
                type="primary"
                :aria-expanded="isJdExpanded(item.jobId)"
                :aria-controls="`job-jd-${item.jobId}`"
                @click="toggleJd(item.jobId)"
              >
                {{ isJdExpanded(item.jobId) ? '收起 JD' : '展开 JD' }}
              </el-button>
            </div>

            <div class="skill-tags">
              <el-tag v-for="skill in skillTags(item.skillRequirements)" :key="skill" type="primary" effect="plain">
                {{ skill }}
              </el-tag>
              <span v-if="!skillTags(item.skillRequirements).length" class="empty-skill">暂未填写技能要求</span>
            </div>
          </div>

          <div class="responsive-actions">
            <el-button type="primary" :icon="MagicStick" @click="startAnalysis(item.jobId)">开始分析</el-button>
            <el-button @click="openDetail(item.jobId)">详情</el-button>
            <el-button @click="openEdit(item.jobId)">编辑</el-button>
            <el-button type="danger" plain @click="removeJob(item)">删除</el-button>
          </div>
        </article>
      </div>
    </TableShell>

    <el-dialog v-model="formVisible" :title="form.jobId ? '编辑岗位' : '新建岗位'" :width="formDialogWidth">
      <el-form :model="form" label-position="top">
        <div class="form-grid two">
          <el-form-item label="公司"><el-input v-model="form.companyName" placeholder="例如：腾讯 / 字节跳动" /></el-form-item>
          <el-form-item label="岗位"><el-input v-model="form.jobTitle" placeholder="例如：Java 后端实习生" /></el-form-item>
        </div>
        <div class="form-grid three">
          <el-form-item label="类型">
            <el-input
              v-model="form.jobType"
              placeholder="例如：后端开发 / 前端开发 / 测试 / 产品 / 算法"
            />
          </el-form-item>
          <el-form-item label="地点"><el-input v-model="form.location" placeholder="例如：深圳 / 重庆 / 远程" /></el-form-item>
          <el-form-item label="来源"><el-input v-model="form.sourcePlatform" placeholder="例如：Boss直聘 / 官网" /></el-form-item>
        </div>
        <div class="form-grid two">
          <el-form-item label="薪资"><el-input v-model="form.salaryRange" placeholder="例如：200-300/天" /></el-form-item>
          <el-form-item label="实习周期"><el-input v-model="form.internshipDuration" placeholder="例如：3个月，每周5天" /></el-form-item>
        </div>
        <el-form-item label="岗位链接"><el-input v-model="form.jobUrl" placeholder="粘贴岗位原始链接，便于后续追踪" /></el-form-item>
        <el-form-item label="技能要求">
          <el-input
            v-model="form.skillRequirements"
            type="textarea"
            :rows="3"
            resize="vertical"
            placeholder="可换行输入，例如：\n1. 熟悉 Java/Spring\n2. 了解 MySQL 与 Redis\n3. 具备良好沟通能力"
          />
        </el-form-item>
        <el-form-item label="JD 内容">
          <el-input v-model="form.jdContent" type="textarea" :rows="10" placeholder="粘贴岗位职责、任职要求、加分项等完整 JD 内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveJob">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="岗位详情" :size="detailDrawerSize">
      <div v-if="detail" class="detail-stack">
        <section class="panel flat">
          <div class="panel-header">
            <h3>基础信息</h3>
            <el-button type="primary" link @click="startAnalysis(detail.jobId)">开始 AI 分析</el-button>
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="公司">{{ detail.companyName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="岗位">{{ detail.jobTitle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ detail.jobType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="地点">{{ detail.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="薪资">{{ detail.salaryRange || '-' }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ detail.sourcePlatform || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="panel flat">
          <div class="panel-header">
            <h3>技能要求</h3>
            <span>用于匹配简历中的技能关键词</span>
          </div>
          <div class="skill-tags large">
            <el-tag v-for="skill in skillTags(detail.skillRequirements)" :key="skill" type="primary" effect="plain">
              {{ skill }}
            </el-tag>
            <span v-if="!skillTags(detail.skillRequirements).length" class="empty-skill">暂未填写技能要求</span>
          </div>
        </section>

        <section class="panel flat">
          <div class="panel-header">
            <h3>JD 内容</h3>
            <span>AI 匹配分析的核心岗位输入</span>
          </div>
          <div class="text-preview">{{ detail.jdContent || '暂无 JD 内容' }}</div>
        </section>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { Button } from '@/components/ui/button'
import FilterBar from '@/components/common/FilterBar.vue'
import TableShell from '@/components/common/TableShell.vue'
import router from '@/router'
import { createJobApi, deleteJobApi, getJobDetailApi, getJobListApi, updateJobApi } from '@/api/job'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const jobs = ref<any[]>([])
const detail = ref<any>(null)
const loading = ref(false)
const errorText = ref('')
const saving = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const expandedJobIds = ref(new Set<number>())
const { responsiveDialogWidth, responsiveDrawerSize } = useResponsiveSize()
const formDialogWidth = responsiveDialogWidth('720px')
const detailDrawerSize = responsiveDrawerSize('48%')
const query = reactive({ keyword: '', jobType: '', location: '' })
const emptyForm = () => ({
  jobId: undefined as number | undefined,
  companyName: '',
  jobTitle: '',
  jobType: '',
  location: '',
  sourcePlatform: '',
  jobUrl: '',
  jdContent: '',
  skillRequirements: '',
  salaryRange: '',
  workDaysPerWeek: '',
  internshipDuration: ''
})
const form = reactive(emptyForm())

const hasAppliedFilters = ref(false)
let jobLoadId = 0

async function loadJobs() {
  const loadId = ++jobLoadId
  const filters = { ...query }
  hasAppliedFilters.value = Object.values(filters).some((value) => value.trim() !== '')
  loading.value = true
  errorText.value = ''
  try {
    const res: any = await getJobListApi({ ...filters, pageNum: 1, pageSize: 100 })
    if (loadId !== jobLoadId) return
    const records = await hydrateJobDetails(res.records || [])
    if (loadId === jobLoadId) jobs.value = records
  } catch (e: any) {
    if (loadId === jobLoadId) {
      jobs.value = []
      errorText.value = e?.response?.data?.message || e?.message || '请检查网络连接后重试。'
    }
  } finally {
    if (loadId === jobLoadId) loading.value = false
  }
}

async function hydrateJobDetails(records: any[]) {
  return Promise.all(
    records.map(async (item) => {
      try {
        const detailData = (await getJobDetailApi(item.jobId)) as any
        return { ...item, ...detailData }
      } catch {
        return item
      }
    })
  )
}

function resetQuery() {
  query.keyword = ''
  query.jobType = ''
  query.location = ''
  loadJobs()
}

function resetForm(data = emptyForm()) {
  Object.assign(form, data)
}

function openCreate() {
  resetForm()
  formVisible.value = true
}

async function openEdit(id: number) {
  resetForm((await getJobDetailApi(id)) as any)
  form.jobId = id
  formVisible.value = true
}

async function openDetail(id: number) {
  detail.value = await getJobDetailApi(id)
  detailVisible.value = true
}

async function saveJob() {
  if (!form.companyName || !form.jobTitle || !form.jdContent) {
    ElMessage.warning('请填写公司、岗位和 JD 内容')
    return
  }
  saving.value = true
  try {
    if (form.jobId) await updateJobApi(form.jobId, form)
    else await createJobApi(form)
    ElMessage.success('保存成功')
    formVisible.value = false
    loadJobs()
  } finally {
    saving.value = false
  }
}

async function removeJob(row: any) {
  await ElMessageBox.confirm(
    `确认删除“${row.companyName || '未知公司'} - ${row.jobTitle || '未命名岗位'}”吗？相关分析可能无法继续引用。`,
    '删除岗位',
    { type: 'warning' }
  )
  await deleteJobApi(row.jobId)
  ElMessage.success('已删除')
  loadJobs()
}

function startAnalysis(jobId: number) {
  router.push(`/analysis/match?jobId=${jobId}`)
}

function viewRecommendations() {
  router.push('/job-recommendations')
}

function jdSummary(content?: string) {
  if (!content) {
    return '暂未填写 JD 内容。建议补充岗位职责、任职要求和加分项，AI 匹配会更准确。'
  }
  const text = content.replace(/\s+/g, ' ').trim()
  return text.length > 128 ? `${text.slice(0, 128)}...` : text
}

function shouldCollapseJd(content?: string) {
  return Boolean(content && content.replace(/\s+/g, ' ').trim().length > 128)
}

function isJdExpanded(jobId: number) {
  return expandedJobIds.value.has(jobId)
}

function displayedJd(item: any) {
  return isJdExpanded(item.jobId) ? item.jdContent : jdSummary(item.jdContent)
}

function toggleJd(jobId: number) {
  const next = new Set(expandedJobIds.value)
  if (next.has(jobId)) next.delete(jobId)
  else next.add(jobId)
  expandedJobIds.value = next
}

function skillTags(value?: string) {
  if (!value) {
    return []
  }
  return value
    .split(/[\n,，、;；]/)
    .map((item) => item.replace(/^\d+[.、]\s*/, '').trim())
    .filter(Boolean)
    .slice(0, 8)
}

onMounted(loadJobs)
onBeforeUnmount(() => { jobLoadId += 1 })
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

.job-filters :deep(.filter-bar__filters) {
  display: grid;
  grid-template-columns: minmax(0, 2fr) repeat(2, minmax(0, 1fr));
}

.list-heading {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin: var(--space-5) 0 var(--space-3);
}

.list-heading h2 {
  margin: 0;
  font-size: 20px;
}

.list-heading span {
  color: var(--color-text-muted);
  font-size: 13px;
}

.job-library :deep(.table-shell__content) {
  padding: 0;
}

.job-card {
  display: grid;
  gap: var(--space-4);
  padding: var(--space-4);
  overflow-wrap: anywhere;
}

.job-card + .job-card {
  border-top: 1px solid var(--color-border);
}

.job-card-heading {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.job-card-heading .el-tag {
  flex-shrink: 0;
  height: auto;
  max-width: 40%;
  padding-block: var(--space-1);
  white-space: normal;
}

.company-name {
  display: block;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.job-card h3 {
  margin: 6px 0 0;
  font-size: 18px;
}

.job-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 12px 0;
  color: var(--color-text-soft);
  font-size: 13px;
}

.job-summary {
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.7;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.job-jd {
  margin-bottom: 12px;
}

.job-jd-toggle {
  margin-top: 4px;
  padding-inline: 0;
}

.state-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}

.state-panel p {
  margin: 6px 0 0;
  color: var(--color-text-muted);
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tags.large {
  gap: 10px;
}

.empty-skill {
  color: var(--color-text-soft);
  font-size: 13px;
}

.job-card .responsive-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.job-card .responsive-actions .el-button {
  margin-left: 0;
}

@media (min-width: 1100px) {
  .job-card {
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: start;
  }

  .job-card .responsive-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .job-filters :deep(.filter-bar__filters) {
    flex-basis: auto;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .job-filters :deep(.filter-bar__filters > :first-child) {
    grid-column: 1 / -1;
  }
}

@media (max-width: 520px) {
  .state-panel {
    align-items: stretch;
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  :deep(.app-page-actions),
  .job-filters :deep(.filter-bar__actions),
  .job-card .responsive-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  :deep(.app-page-actions > :only-child) {
    grid-column: 1 / -1;
  }
}
</style>
