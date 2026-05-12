<template>
  <PageContainer title="岗位推荐" description="选择简历和版本，按技能匹配、历史 AI 分析分数生成可解释的岗位推荐。">
    <div class="recommendation-grid">
      <section class="panel">
        <div class="panel-header">
          <h3>生成推荐</h3>
        </div>

        <el-form :model="form" label-position="top">
          <el-form-item label="简历">
            <el-select v-model="form.resumeId" placeholder="请选择简历" filterable>
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

          <el-button type="primary" :icon="MagicStick" :loading="generating" @click="generateRecommendation">
            生成推荐
          </el-button>
        </el-form>
      </section>

      <section class="panel">
        <div class="panel-header">
          <h3>最近推荐</h3>
          <el-button link type="primary" @click="loadHistory">刷新</el-button>
        </div>

        <el-table v-loading="loading" :data="history">
          <el-table-column prop="title" label="推荐批次" min-width="220" show-overflow-tooltip />
          <el-table-column prop="jobCount" label="岗位数" width="90" />
          <el-table-column prop="recommendedCount" label="推荐数" width="90" />
          <el-table-column prop="strategy" label="策略" width="120" />
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewDetail(row.batchId)">详情</el-button>
              <el-button link type="danger" @click="removeBatch(row.batchId)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import router from '@/router'
import { deleteJobRecommendationApi, generateJobRecommendationApi, getJobRecommendationListApi } from '@/api/jobRecommendation'
import { getResumeListApi } from '@/api/resume'
import { getResumeVersionListApi } from '@/api/resumeVersion'
import { formatDateTime } from '@/utils/format'

const resumes = ref<any[]>([])
const versions = ref<any[]>([])
const history = ref<any[]>([])
const loading = ref(false)
const generating = ref(false)

const form = reactive({
  resumeId: undefined as number | undefined,
  resumeVersionId: undefined as number | undefined,
  includeApplied: false,
  limit: 10
})

async function loadOptions() {
  const res: any = await getResumeListApi({ pageNum: 1, pageSize: 100 })
  resumes.value = res.records || []
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
  try {
    const res: any = await getJobRecommendationListApi({ pageNum: 1, pageSize: 100 })
    history.value = res.records || []
  } catch {
    history.value = []
  } finally {
    loading.value = false
  }
}

async function generateRecommendation() {
  if (!form.resumeId) {
    ElMessage.warning('请选择简历')
    return
  }
  generating.value = true
  try {
    const res: any = await generateJobRecommendationApi(form)
    ElMessage.success('推荐生成成功')
    await loadHistory()
    viewDetail(res.batchId)
  } catch {
    // Error message is already shown by the request interceptor.
  } finally {
    generating.value = false
  }
}

function viewDetail(batchId: number) {
  router.push(`/job-recommendations/${batchId}`)
}

async function removeBatch(batchId: number) {
  await ElMessageBox.confirm('确认删除这条推荐记录？', '删除确认', { type: 'warning' })
  try {
    await deleteJobRecommendationApi(batchId)
    ElMessage.success('删除成功')
    loadHistory()
  } catch {
    // Error message is already shown by the request interceptor.
  }
}

watch(() => form.resumeId, loadVersions)

onMounted(() => {
  loadOptions()
  loadHistory()
})
</script>

<style scoped>
.recommendation-grid {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 18px;
}

@media (max-width: 900px) {
  .recommendation-grid {
    grid-template-columns: 1fr;
  }
}
</style>
