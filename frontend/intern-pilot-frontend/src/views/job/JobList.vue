<template>
  <PageContainer title="岗位管理" description="维护目标岗位 JD，后续用于 AI 匹配分析。">
    <template #actions>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建岗位</el-button>
    </template>

    <section class="panel toolbar">
      <el-input v-model="query.keyword" placeholder="搜索公司或岗位" clearable />
      <el-input v-model="query.jobType" placeholder="岗位类型" clearable />
      <el-button type="primary" @click="loadJobs">搜索</el-button>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="jobs">
        <el-table-column prop="companyName" label="公司" min-width="140" />
        <el-table-column prop="jobTitle" label="岗位" min-width="180" />
        <el-table-column prop="jobType" label="类型" width="130" />
        <el-table-column prop="location" label="地点" width="120" />
        <el-table-column prop="sourcePlatform" label="来源" width="120" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.jobId)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row.jobId)">编辑</el-button>
            <el-button link type="danger" @click="removeJob(row.jobId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="formVisible" :title="form.jobId ? '编辑岗位' : '新建岗位'" width="720px">
      <el-form :model="form" label-position="top">
        <div class="form-grid two">
          <el-form-item label="公司"><el-input v-model="form.companyName" /></el-form-item>
          <el-form-item label="岗位"><el-input v-model="form.jobTitle" /></el-form-item>
        </div>
        <div class="form-grid three">
          <el-form-item label="类型">
            <el-input
              v-model="form.jobType"
              placeholder="例如：后端开发 / 前端开发 / 测试 / 产品 / 算法"
            />
          </el-form-item>
          <el-form-item label="地点"><el-input v-model="form.location" /></el-form-item>
          <el-form-item label="来源"><el-input v-model="form.sourcePlatform" /></el-form-item>
        </div>
        <div class="form-grid two">
          <el-form-item label="薪资"><el-input v-model="form.salaryRange" /></el-form-item>
          <el-form-item label="实习周期"><el-input v-model="form.internshipDuration" /></el-form-item>
        </div>
        <el-form-item label="岗位链接"><el-input v-model="form.jobUrl" /></el-form-item>
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
          <el-input v-model="form.jdContent" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveJob">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="岗位详情" size="46%">
      <div v-if="detail" class="detail-stack">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="公司">{{ detail.companyName }}</el-descriptions-item>
          <el-descriptions-item label="岗位">{{ detail.jobTitle }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ detail.location }}</el-descriptions-item>
          <el-descriptions-item label="薪资">{{ detail.salaryRange }}</el-descriptions-item>
        </el-descriptions>
        <div class="text-preview">{{ detail.jdContent }}</div>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { createJobApi, deleteJobApi, getJobDetailApi, getJobListApi, updateJobApi } from '@/api/job'
import { formatDateTime } from '@/utils/format'

const jobs = ref<any[]>([])
const detail = ref<any>(null)
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const query = reactive({ keyword: '', jobType: '' })
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

async function loadJobs() {
  loading.value = true
  try {
    const res: any = await getJobListApi({ ...query, pageNum: 1, pageSize: 100 })
    jobs.value = res.records || []
  } finally {
    loading.value = false
  }
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

async function removeJob(id: number) {
  await ElMessageBox.confirm('确认删除这个岗位吗？', '删除岗位', { type: 'warning' })
  await deleteJobApi(id)
  ElMessage.success('已删除')
  loadJobs()
}

onMounted(loadJobs)
</script>
