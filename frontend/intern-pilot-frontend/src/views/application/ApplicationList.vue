<template>
  <PageContainer title="" description="管理岗位投递状态、面试时间、备注和复盘。">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreate">创建投递</el-button>
    </template>

    <section class="panel toolbar">
      <el-input v-model="query.keyword" placeholder="搜索公司或岗位" clearable />
      <el-select v-model="query.status" placeholder="状态" clearable>
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" @click="loadApplications">筛选</el-button>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="applications">
        <el-table-column prop="companyName" label="公司" min-width="140" />
        <el-table-column prop="jobTitle" label="岗位" min-width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTypes[row.status]">{{ statusLabels[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100" />
        <el-table-column label="面试时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.interviewDate) }}</template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.applicationId)">详情</el-button>
            <el-button link type="primary" @click="openStatus(row)">改状态</el-button>
            <el-button link type="primary" @click="openNote(row)">备注</el-button>
            <el-button link type="danger" @click="removeApplication(row.applicationId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="createVisible" title="创建投递记录" width="620px">
      <el-form :model="createForm" label-position="top">
        <el-form-item label="岗位">
          <el-select v-model="createForm.jobId" filterable>
            <el-option v-for="item in jobs" :key="item.jobId" :label="`${item.companyName} - ${item.jobTitle}`" :value="item.jobId" />
          </el-select>
        </el-form-item>
        <div class="form-grid two">
          <el-form-item label="简历">
            <el-select v-model="createForm.resumeId" clearable>
              <el-option v-for="item in resumes" :key="item.resumeId" :label="item.resumeName || item.originalFileName" :value="item.resumeId" />
            </el-select>
          </el-form-item>
          <el-form-item label="分析报告">
            <el-select v-model="createForm.reportId" clearable>
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
          <el-input v-model="createForm.note" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="createApplication">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="修改投递状态" width="360px">
      <el-select v-model="statusForm.status">
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStatus">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="noteVisible" title="备注与复盘" width="560px">
      <el-form label-position="top">
        <el-form-item label="备注"><el-input v-model="noteForm.note" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="复盘"><el-input v-model="noteForm.review" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="面试时间">
          <el-date-picker v-model="noteForm.interviewDate" value-format="YYYY-MM-DDTHH:mm:ss" type="datetime" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noteVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNote">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="投递详情" size="46%">
      <div v-if="detail" class="detail-stack">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="公司">{{ detail.companyName }}</el-descriptions-item>
          <el-descriptions-item label="岗位">{{ detail.jobTitle }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusLabels[detail.status] || detail.status }}</el-descriptions-item>
          <el-descriptions-item label="匹配分">{{ detail.matchScore || '-' }}</el-descriptions-item>
          <el-descriptions-item label="简历">{{ detail.resumeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="面试时间">{{ formatDateTime(detail.interviewDate) }}</el-descriptions-item>
        </el-descriptions>
        <section class="panel flat"><h4>备注</h4><p>{{ detail.note || '-' }}</p></section>
        <section class="panel flat"><h4>复盘</h4><p>{{ detail.review || '-' }}</p></section>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { createApplicationApi, deleteApplicationApi, getApplicationDetailApi, getApplicationListApi, updateApplicationNoteApi, updateApplicationStatusApi } from '@/api/application'
import { getAnalysisReportsApi } from '@/api/analysis'
import { getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'
import { formatDateTime, statusLabels, statusOptions, statusTypes } from '@/utils/format'

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
const query = reactive({ keyword: '', status: '' })
const createForm = reactive<any>({ jobId: undefined, resumeId: undefined, reportId: undefined, status: 'TO_APPLY', priority: 'MEDIUM', applyDate: '', note: '' })
const statusForm = reactive({ status: 'TO_APPLY' })
const noteForm = reactive({ note: '', review: '', interviewDate: '' })

async function loadApplications() {
  loading.value = true
  try {
    const res: any = await getApplicationListApi({ ...query, pageNum: 1, pageSize: 100 })
    applications.value = res.records || []
  } finally {
    loading.value = false
  }
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

async function removeApplication(id: number) {
  await ElMessageBox.confirm('确认删除这条投递记录吗？', '删除投递', { type: 'warning' })
  await deleteApplicationApi(id)
  ElMessage.success('已删除')
  loadApplications()
}

onMounted(() => {
  loadApplications()
  loadOptions()
})
</script>
