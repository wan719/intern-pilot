<template>
  <PageContainer title="" description="基于现有列表接口汇总简历、岗位、分析报告和投递进度。">
    <div class="stat-grid">
      <StatCard label="简历数量" :value="summary.resumes" :icon="Document" />
      <StatCard label="岗位数量" :value="summary.jobs" :icon="Briefcase" />
      <StatCard label="分析报告" :value="summary.reports" :icon="Tickets" />
      <StatCard label="投递记录" :value="summary.applications" :icon="List" />
    </div>

    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-header">
          <h3>投递状态</h3>
          <span>按当前记录统计</span>
        </div>
        <div ref="statusChartRef" class="chart"></div>
      </section>
      <section class="panel">
        <div class="panel-header">
          <h3>匹配分分布</h3>
          <span>最近 100 份报告</span>
        </div>
        <div ref="scoreChartRef" class="chart"></div>
      </section>
    </div>

    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-header">
          <h3>最近分析报告</h3>
        </div>
        <el-table :data="reports" height="260">
          <el-table-column prop="companyName" label="公司" min-width="120" />
          <el-table-column prop="jobTitle" label="岗位" min-width="160" />
          <el-table-column prop="matchScore" label="分数" width="80" />
          <el-table-column label="时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </section>
      <section class="panel">
        <div class="panel-header">
          <h3>最近投递</h3>
        </div>
        <el-table :data="applications" height="260">
          <el-table-column prop="companyName" label="公司" min-width="120" />
          <el-table-column prop="jobTitle" label="岗位" min-width="160" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTypes[row.status]">{{ statusLabels[row.status] || row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { Briefcase, Document, List, Tickets } from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import StatCard from '@/components/common/StatCard.vue'
import { getApplicationListApi } from '@/api/application'
import { getAnalysisReportsApi } from '@/api/analysis'
import { getJobListApi } from '@/api/job'
import { getResumeListApi } from '@/api/resume'
import { formatDateTime, statusLabels, statusTypes } from '@/utils/format'

const summary = reactive({ resumes: 0, jobs: 0, reports: 0, applications: 0 })
const reports = ref<any[]>([])
const applications = ref<any[]>([])
const statusChartRef = ref<HTMLDivElement>()
const scoreChartRef = ref<HTMLDivElement>()

async function loadDashboard() {
  const [resumeRes, jobRes, reportRes, appRes]: any[] = await Promise.all([
    getResumeListApi({ pageNum: 1, pageSize: 100 }),
    getJobListApi({ pageNum: 1, pageSize: 100 }),
    getAnalysisReportsApi({ pageNum: 1, pageSize: 100 }),
    getApplicationListApi({ pageNum: 1, pageSize: 100 })
  ])

  summary.resumes = resumeRes.total ?? resumeRes.records?.length ?? 0
  summary.jobs = jobRes.total ?? jobRes.records?.length ?? 0
  summary.reports = reportRes.total ?? reportRes.records?.length ?? 0
  summary.applications = appRes.total ?? appRes.records?.length ?? 0
  reports.value = reportRes.records || []
  applications.value = appRes.records || []
  await nextTick()
  renderCharts()
}

function renderCharts() {
  const statusCounts = applications.value.reduce<Record<string, number>>((acc, item) => {
    acc[item.status] = (acc[item.status] || 0) + 1
    return acc
  }, {})

  echarts.init(statusChartRef.value!).setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        data: Object.entries(statusCounts).map(([key, value]) => ({ name: statusLabels[key] || key, value }))
      }
    ]
  })

  echarts.init(scoreChartRef.value!).setOption({
    tooltip: {},
    xAxis: { type: 'category', data: reports.value.map((item) => item.companyName || `#${item.reportId}`) },
    yAxis: { type: 'value', max: 100 },
    series: [{ type: 'bar', data: reports.value.map((item) => item.matchScore), itemStyle: { color: '#2563eb' } }]
  })
}

onMounted(loadDashboard)
</script>
