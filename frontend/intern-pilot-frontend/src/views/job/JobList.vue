<template>
  <PageContainer title="锁定目标岗位" description="维护目标岗位 JD，用于 AI 匹配分析和投递跟踪。">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建岗位</el-button>
    </template>

    <div v-loading="loading" class="job-summary-grid">
      <StatCard label="岗位总数" :value="jobStats.total" :icon="Briefcase" />
      <StatCard label="公司数量" :value="jobStats.companyCount" :icon="OfficeBuilding" />
      <StatCard label="完整 JD" :value="jobStats.completeJd" :icon="DocumentChecked" />
      <StatCard label="待完善" :value="jobStats.incomplete" :icon="WarningFilled" />
    </div>

    <FilterBar @reset="resetQuery">
      <template #filters>
        <el-input v-model="query.keyword" aria-label="搜索公司或岗位" placeholder="搜索公司或岗位" clearable />
        <el-input v-model="query.jobType" aria-label="岗位类型" placeholder="岗位类型" clearable />
        <el-input v-model="query.location" aria-label="地点" placeholder="地点" clearable />
      </template>
      <template #actions>
        <el-button type="primary" @click="loadJobs">搜索</el-button>
      </template>
    </FilterBar>

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
      empty-title="还没有目标岗位"
      empty-hint="新增岗位后即可用于 AI 匹配分析。建议填写公司、岗位、地点、薪资和完整 JD 内容。"
    >
      <template #empty-actions>
        <el-button data-job-empty-action type="primary" :icon="Plus" @click="openCreate">新建岗位</el-button>
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

          <div class="job-actions">
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Briefcase, DocumentChecked, MagicStick, OfficeBuilding, Plus, WarningFilled } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import StatCard from '@/components/common/StatCard.vue'
import TableShell from '@/components/common/TableShell.vue'
import router from '@/router'
import { createJobApi, deleteJobApi, getJobDetailApi, getJobListApi, updateJobApi } from '@/api/job'
import { formatDateTime } from '@/utils/format'
import { useResponsiveSize } from '@/utils/useResponsiveSize'

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

const jobStats = computed(() => {
  const companyNames = new Set(jobs.value.map((item) => item.companyName).filter(Boolean))
  const completeJd = jobs.value.filter((item) => item.jdContent && item.skillRequirements).length
  return {
    total: jobs.value.length,
    companyCount: companyNames.size,
    completeJd,
    incomplete: jobs.value.length - completeJd
  }
})

async function loadJobs() {
  loading.value = true
  errorText.value = ''
  try {
    const res: any = await getJobListApi({ ...query, pageNum: 1, pageSize: 100 })
    const records = res.records || []
    jobs.value = await hydrateJobDetails(records)
  } catch (e: any) {
    jobs.value = []
    errorText.value = e?.message || e?.response?.data?.message || '请检查网络连接后重试。'
  } finally {
    loading.value = false
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
</script>

<style scoped>
.job-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 20px;
}

.job-library {
  margin-top: 18px;
}

.job-card-list {
  display: grid;
  gap: 14px;
}

.job-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  padding: 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.job-card-heading {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
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

.job-actions {
  display: flex;
  width: 132px;
  flex-direction: column;
  gap: 8px;
}

.job-actions .el-button {
  width: 100%;
  margin-left: 0;
}

@media (max-width: 900px) {
  .job-summary-grid,
  .job-card {
    grid-template-columns: 1fr;
  }

  .job-actions {
    width: 100%;
  }

  .state-panel {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
