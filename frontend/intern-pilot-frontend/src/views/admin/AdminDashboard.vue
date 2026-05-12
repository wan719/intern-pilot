<template>
  <PageContainer title="" description="管理员视角下的系统统计信息。">
    <div class="stat-grid">
      <StatCard label="用户总数" :value="summary.userCount || 0" :icon="User" />
      <StatCard label="今日新增用户" :value="summary.todayNewUserCount || 0" :icon="Plus" />
      <StatCard label="简历总数" :value="summary.resumeCount || 0" :icon="Document" />
      <StatCard label="岗位总数" :value="summary.jobCount || 0" :icon="Briefcase" />
      <StatCard label="分析报告总数" :value="summary.analysisReportCount || 0" :icon="DataBoard" />
      <StatCard label="面试题报告总数" :value="summary.interviewQuestionReportCount || 0" :icon="QuestionFilled" />
      <StatCard label="投递记录总数" :value="summary.applicationCount || 0" :icon="List" />
      <StatCard label="今日失败操作" :value="summary.failedOperationCount || 0" :icon="Warning" />
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Briefcase, DataBoard, Document, List, Plus, QuestionFilled, User, Warning } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import StatCard from '@/components/common/StatCard.vue'
import { getAdminDashboardSummaryApi } from '@/api/adminDashboard'

const summary = ref<any>({})

async function loadData() {
  const res: any = await getAdminDashboardSummaryApi()
  summary.value = res || {}
}

onMounted(loadData)
</script>