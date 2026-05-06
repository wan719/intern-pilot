<template>
  <PageContainer title="AI 匹配分析" description="选择一份简历和一个岗位，生成匹配分数、技能缺口和面试建议。">
    <div class="analysis-grid">
      <section class="panel">
        <div class="panel-header">
          <h3>分析参数</h3>
        </div>
        <el-form :model="form" label-position="top">
          <el-form-item label="选择简历">
            <el-select v-model="form.resumeId" placeholder="请选择简历" filterable>
              <el-option
                v-for="item in resumes"
                :key="item.resumeId"
                :label="item.resumeName || item.originalFileName"
                :value="item.resumeId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="选择岗位">
            <el-select v-model="form.jobId" placeholder="请选择岗位" filterable>
              <el-option
                v-for="item in jobs"
                :key="item.jobId"
                :label="`${item.companyName} - ${item.jobTitle}`"
                :value="item.jobId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="强制重新分析">
            <el-switch v-model="form.forceRefresh" />
          </el-form-item>
          <el-button type="primary" :icon="MagicStick" :loading="loading" @click="analyze">开始分析</el-button>
        </el-form>
      </section>

      <section class="panel result-panel">
        <div class="panel-header">
          <h3>分析结果</h3>
          <el-tag v-if="result" :type="result.cacheHit ? 'success' : 'primary'">
            {{ result.cacheHit ? '缓存命中' : '新分析' }}
          </el-tag>
        </div>
        <template v-if="result">
          <div class="score-row">
            <el-progress type="dashboard" :percentage="result.matchScore" />
            <div>
              <strong>{{ result.matchLevel }}</strong>
              <span>匹配分数 {{ result.matchScore }}</span>
            </div>
          </div>
          <div class="tag-section">
            <h4>优势</h4>
            <el-tag v-for="item in result.strengths" :key="item" type="success">{{ item }}</el-tag>
          </div>
          <div class="tag-section">
            <h4>短板</h4>
            <el-tag v-for="item in result.weaknesses" :key="item" type="warning">{{ item }}</el-tag>
          </div>
          <div class="tag-section">
            <h4>缺失技能</h4>
            <el-tag v-for="item in result.missingSkills" :key="item" type="danger">{{ item }}</el-tag>
          </div>
        </template>
        <el-empty v-else description="选择简历和岗位后开始分析" />
      </section>
    </div>

    <div v-if="result" class="dashboard-grid">
      <section class="panel">
        <div class="panel-header"><h3>优化建议</h3></div>
        <el-timeline>
          <el-timeline-item v-for="item in result.suggestions" :key="item">{{ item }}</el-timeline-item>
        </el-timeline>
      </section>
      <section class="panel">
        <div class="panel-header"><h3>面试准备</h3></div>
        <el-timeline>
          <el-timeline-item v-for="item in result.interviewTips" :key="item">{{ item }}</el-timeline-item>
        </el-timeline>
      </section>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { matchAnalysisApi } from '@/api/analysis'
import { getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'

const resumes = ref<any[]>([])
const jobs = ref<any[]>([])
const result = ref<any>(null)
const loading = ref(false)
const form = reactive({
  resumeId: undefined as number | undefined,
  jobId: undefined as number | undefined,
  forceRefresh: false
})

async function loadOptions() {
  const [resumeRes, jobRes]: any[] = await Promise.all([
    getResumeListApi({ pageNum: 1, pageSize: 100 }),
    getJobListApi({ pageNum: 1, pageSize: 100 })
  ])
  resumes.value = resumeRes.records || []
  jobs.value = jobRes.records || []
}

async function analyze() {
  if (!form.resumeId || !form.jobId) {
    ElMessage.warning('请选择简历和岗位')
    return
  }
  loading.value = true
  try {
    result.value = await matchAnalysisApi(form)
    ElMessage.success('分析完成')
  } finally {
    loading.value = false
  }
}

onMounted(loadOptions)
</script>
