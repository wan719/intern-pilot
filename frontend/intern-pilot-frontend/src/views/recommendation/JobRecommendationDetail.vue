<template>
  <PageContainer :title="detail?.title || '推荐详情'" description="查看推荐分数、推荐等级、匹配技能、缺失技能和推荐理由。">
    <template #actions>
      <el-button @click="router.push('/job-recommendations')">返回列表</el-button>
    </template>

    <section v-if="detail" class="panel">
      <el-descriptions :column="4" border>
        <el-descriptions-item label="岗位数量">{{ detail.jobCount }}</el-descriptions-item>
        <el-descriptions-item label="推荐数量">{{ detail.recommendedCount }}</el-descriptions-item>
        <el-descriptions-item label="推荐策略">{{ detail.strategy }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </section>

    <section v-loading="loading" class="recommendation-list">
      <article v-for="item in items" :key="item.itemId" class="panel recommendation-card">
        <div class="card-head">
          <div>
            <h3>{{ item.companyName }} - {{ item.jobTitle }}</h3>
            <p>{{ item.jobType || '未填写类型' }} · {{ item.location || '未填写地点' }} · {{ item.salaryRange || '薪资面议' }}</p>
          </div>
          <div class="score-box">
            <strong>{{ item.recommendationScore }}</strong>
            <el-tag :type="levelTagType(item.recommendationLevel)">{{ levelLabel(item.recommendationLevel) }}</el-tag>
          </div>
        </div>

        <div class="score-parts">
          <span>技能 {{ item.skillMatchScore ?? '-' }}</span>
          <span>AI {{ item.aiMatchScore ?? '-' }}</span>
          <span>方向 {{ item.jobTypeScore ?? '-' }}</span>
          <el-tag v-if="item.isApplied === 1" type="info">已投递</el-tag>
        </div>

        <div class="tag-section">
          <h4>匹配技能</h4>
          <el-tag v-for="skill in item.matchedSkills" :key="skill" type="success">{{ skill }}</el-tag>
          <span v-if="!item.matchedSkills?.length" class="muted">暂无匹配技能</span>
        </div>

        <div class="tag-section">
          <h4>缺失技能</h4>
          <el-tag v-for="skill in item.missingSkills" :key="skill" type="warning">{{ skill }}</el-tag>
          <span v-if="!item.missingSkills?.length" class="muted">暂无明显缺失技能</span>
        </div>

        <div class="reason-list">
          <h4>推荐理由</h4>
          <ul>
            <li v-for="reason in item.reasons" :key="reason">{{ reason }}</li>
          </ul>
        </div>

        <div class="card-actions">
          <el-button :icon="MagicStick" @click="goAnalysis(item)">AI 分析</el-button>
          <el-button type="primary" :icon="Plus" @click="addApplication(item)">加入投递</el-button>
        </div>
      </article>

      <el-empty v-if="!loading && items.length === 0" description="暂无推荐结果" />
    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Plus } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import PageContainer from '@/components/common/PageContainer.vue'
import router from '@/router'
import { createApplicationApi } from '@/api/application'
import { getJobRecommendationDetailApi } from '@/api/jobRecommendation'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const detail = ref<any>(null)
const loading = ref(false)
const batchId = computed(() => Number(route.params.batchId))
const items = computed(() => detail.value?.items || [])

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getJobRecommendationDetailApi(batchId.value)
  } finally {
    loading.value = false
  }
}

function goAnalysis(item: any) {
  router.push({
    path: '/analysis/match',
    query: {
      resumeId: detail.value.resumeId,
      resumeVersionId: detail.value.resumeVersionId,
      jobId: item.jobId
    }
  })
}

async function addApplication(item: any) {
  await createApplicationApi({
    jobId: item.jobId,
    resumeId: detail.value.resumeId,
    reportId: item.analysisReportId,
    status: 'TO_APPLY',
    priority: item.recommendationScore >= 85 ? 'HIGH' : 'MEDIUM',
    note: `来自岗位推荐批次：${detail.value.title}`
  })
  ElMessage.success('已加入投递记录')
}

function levelLabel(level: string) {
  const labels: Record<string, string> = {
    HIGH: '强烈推荐',
    MEDIUM_HIGH: '较推荐',
    MEDIUM: '一般推荐',
    LOW: '低推荐',
    NOT_RECOMMENDED: '不推荐'
  }
  return labels[level] || level
}

function levelTagType(level: string) {
  if (level === 'HIGH') return 'success'
  if (level === 'MEDIUM_HIGH' || level === 'MEDIUM') return 'warning'
  if (level === 'LOW') return 'info'
  return 'danger'
}

onMounted(loadDetail)
</script>

<style scoped>
.recommendation-list {
  display: grid;
  gap: 16px;
}

.recommendation-card {
  display: grid;
  gap: 14px;
}

.card-head,
.card-actions,
.score-parts {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.card-head h3 {
  margin: 0;
  font-size: 18px;
}

.card-head p {
  margin: 8px 0 0;
  color: #667085;
}

.score-box {
  display: grid;
  justify-items: center;
  gap: 8px;
  min-width: 88px;
}

.score-box strong {
  font-size: 34px;
  line-height: 1;
}

.score-parts {
  justify-content: flex-start;
  color: #667085;
}

.reason-list h4 {
  margin: 0 0 10px;
}

.reason-list ul {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
}

.muted {
  color: #98a2b3;
}

@media (max-width: 700px) {
  .card-head,
  .card-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
