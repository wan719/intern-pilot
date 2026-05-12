<template>
  <PageContainer title="简历版本管理" description="维护原始、手动和 AI 优化版本，并对比差异。">
    <template #actions>
      <el-button @click="router.push('/resumes')">返回简历</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">创建版本</el-button>
      <el-button :icon="MagicStick" @click="openOptimize" :disabled="versions.length === 0">AI 优化</el-button>
    </template>

    <section class="panel">
      <el-table v-loading="loading" :data="versions">
        <el-table-column prop="versionName" label="版本名称" min-width="180" />
        <el-table-column prop="versionType" label="类型" width="130" />
        <el-table-column label="目标岗位" min-width="180">
          <template #default="{ row }">
            {{ row.targetJobTitle ? `${row.targetCompanyName || ''} ${row.targetJobTitle}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="当前" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isCurrent === 1" type="success">当前</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contentSummary" label="摘要" min-width="260" show-overflow-tooltip />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" :disabled="row.isCurrent === 1" @click="setCurrent(row)">设当前</el-button>
            <el-button link type="warning" @click="prepareCompare(row)">对比</el-button>
            <el-button link type="danger" :disabled="row.isCurrent === 1 || row.versionType === 'ORIGINAL'" @click="removeVersion(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无版本">
            <el-button type="primary" @click="openCreate">创建第一个版本</el-button>
          </el-empty>
        </template>
      </el-table>
    </section>

    <el-dialog v-model="editVisible" :title="editingVersionId ? '编辑版本' : '创建版本'" width="720px">
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

    <el-drawer v-model="detailVisible" title="版本详情" size="52%">
      <div v-if="detail" class="detail-stack">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="版本名称">{{ detail.versionName }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.versionType }}</el-descriptions-item>
          <el-descriptions-item label="目标岗位">{{ detail.targetJobTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前">{{ detail.isCurrent === 1 ? '是' : '否' }}</el-descriptions-item>
        </el-descriptions>
        <el-input v-model="detail.content" type="textarea" :rows="18" readonly />
      </div>
    </el-drawer>

    <el-dialog v-model="compareVisible" title="版本对比" width="880px">
      <el-form class="compare-picker" label-position="top">
        <el-form-item label="旧版本">
          <el-select v-model="compareForm.oldVersionId" filterable>
            <el-option v-for="item in versions" :key="item.versionId" :label="item.versionName" :value="item.versionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="新版本">
          <el-select v-model="compareForm.newVersionId" filterable>
            <el-option v-for="item in versions" :key="item.versionId" :label="item.versionName" :value="item.versionId" />
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

    <el-dialog v-model="optimizeVisible" title="AI 优化简历版本" width="620px">
      <el-alert class="dialog-alert" type="warning" :closable="false" title="AI 优化结果仅供参考，投递前请自行核对真实性。" />
      <el-form :model="optimizeForm" label-position="top">
        <el-form-item label="来源版本">
          <el-select v-model="optimizeForm.sourceVersionId" filterable>
            <el-option v-for="item in versions" :key="item.versionId" :label="item.versionName" :value="item.versionId" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
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

const route = useRoute()
const router = useRouter()
const resumeId = Number(route.params.resumeId)

const loading = ref(false)
const saving = ref(false)
const optimizing = ref(false)
const versions = ref<any[]>([])
const jobs = ref<any[]>([])
const analysisReports = ref<any[]>([])
const editVisible = ref(false)
const detailVisible = ref(false)
const compareVisible = ref(false)
const optimizeVisible = ref(false)
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

async function loadData() {
  loading.value = true
  try {
    const [versionRes, jobRes, analysisRes]: any[] = await Promise.all([
      getResumeVersionListApi(resumeId),
      getJobListApi({ pageNum: 1, pageSize: 100 }),
      getAnalysisReportsApi({ resumeId, pageNum: 1, pageSize: 100 })
    ])
    versions.value = versionRes || []
    jobs.value = jobRes.records || []
    analysisReports.value = analysisRes.records || []
  } finally {
    loading.value = false
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
  editForm.versionName = current.versionName
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
  await ElMessageBox.confirm(`确认删除「${row.versionName}」？`, '删除版本', { type: 'warning' })
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
  try {
    await optimizeResumeVersionApi(resumeId, { ...optimizeForm })
    ElMessage.success('AI 优化版本已生成')
    optimizeVisible.value = false
    await loadData()
  } finally {
    optimizing.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.detail-stack {
  display: grid;
  gap: 16px;
}

.compare-picker {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 12px;
  align-items: end;
}

.diff-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 18px;
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
  margin-bottom: 14px;
}

@media (max-width: 900px) {
  .compare-picker,
  .diff-grid {
    grid-template-columns: 1fr;
  }
}
</style>
