<template>
  <PageContainer title="" description="查看历史 AI 匹配报告，按最低分筛选。">
    <section class="panel toolbar">
      <el-input-number v-model="query.minScore" :min="0" :max="100" placeholder="最低分" />
      <el-button type="primary" @click="loadReports">筛选</el-button>
      <el-button @click="query.minScore = undefined; loadReports()">重置</el-button>
    </section>

    <section class="panel">
      <el-table v-loading="loading" :data="reports">
        <el-table-column prop="companyName" label="公司" min-width="140" />
        <el-table-column prop="jobTitle" label="岗位" min-width="180" />
        <el-table-column label="分数" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.matchScore" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="matchLevel" label="等级" width="130" />
        <el-table-column label="缓存" width="90">
          <template #default="{ row }">
            <el-tag :type="row.cacheHit ? 'success' : 'info'">{{ row.cacheHit ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.reportId)">详情</el-button>
            <el-button
              v-if="authStore.hasPermission('analysis:delete')"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="detailVisible" title="报告详情" size="52%">
      <div v-if="detail" class="detail-stack">
        <div class="score-row">
          <el-progress type="dashboard" :percentage="detail.matchScore" />
          <div>
            <strong>{{ detail.companyName }} - {{ detail.jobTitle }}</strong>
            <span>{{ detail.matchLevel }} · {{ detail.aiProvider }} / {{ detail.aiModel }}</span>
          </div>
        </div>
        <div class="tag-section">
          <h4>优势</h4>
          <el-tag v-for="item in detail.strengths" :key="item" type="success">{{ item }}</el-tag>
        </div>
        <div class="tag-section">
          <h4>短板</h4>
          <el-tag v-for="item in detail.weaknesses" :key="item" type="warning">{{ item }}</el-tag>
        </div>
        <div class="tag-section">
          <h4>缺失技能</h4>
          <el-tag v-for="item in detail.missingSkills" :key="item" type="danger">{{ item }}</el-tag>
        </div>
        <section class="panel flat">
          <h4>优化建议</h4>
          <ol><li v-for="item in detail.suggestions" :key="item">{{ item }}</li></ol>
        </section>
        <section class="panel flat">
          <h4>面试准备</h4>
          <ol><li v-for="item in detail.interviewTips" :key="item">{{ item }}</li></ol>
        </section>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import PageContainer from '@/components/common/PageContainer.vue'
import { deleteAnalysisReportApi, getAnalysisReportDetailApi, getAnalysisReportsApi } from '@/api/analysis'
import { formatDateTime } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const authStore = useAuthStore()

const reports = ref<any[]>([])
const detail = ref<any>(null)
const detailVisible = ref(false)
const loading = ref(false)
const query = reactive<{ minScore?: number }>({})

async function loadReports() {
  loading.value = true
  try {
    const res: any = await getAnalysisReportsApi({ ...query, pageNum: 1, pageSize: 100 })
    reports.value = res.records || []
  } finally {
    loading.value = false
  }
}

async function openDetail(id: number) {
  detail.value = await getAnalysisReportDetailApi(id)
  detailVisible.value = true
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.companyName} - ${row.jobTitle}」的分析报告吗？删除后不可恢复。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteAnalysisReportApi(row.reportId)
    ElMessage.success('删除成功')
    await loadReports()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(loadReports)
</script>
