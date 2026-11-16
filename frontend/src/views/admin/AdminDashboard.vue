<template>
  <PageContainer width="wide" title="后台看板" description="查看系统规模、今日动态和运营异常。">
    <div v-if="loadState === 'error'" class="dashboard-error" data-admin-dashboard-error>
      <el-alert title="数据不可用" :description="loadError" type="error" :closable="false" show-icon />
      <el-button type="primary" @click="loadData">重试</el-button>
    </div>

    <div class="dashboard-metrics">
      <AdminMetricGroup class="dashboard-scale" label="系统规模" show-heading>
        <StatCard label="用户总数" :value="metricValue('userCount')" :icon="User" :loading="loading" />
        <StatCard label="简历总数" :value="metricValue('resumeCount')" :icon="Document" :loading="loading" />
        <StatCard label="岗位总数" :value="metricValue('jobCount')" :icon="Briefcase" :loading="loading" />
        <StatCard label="分析报告" :value="metricValue('analysisReportCount')" :icon="DataBoard" :loading="loading" />
        <StatCard label="面试题报告" :value="metricValue('interviewQuestionReportCount')" :icon="QuestionFilled" :loading="loading" />
        <StatCard label="投递记录" :value="metricValue('applicationCount')" :icon="List" :loading="loading" />
      </AdminMetricGroup>
      <AdminMetricGroup class="dashboard-today" label="今日动态" show-heading>
        <StatCard label="今日新增用户" :value="metricValue('todayNewUserCount')" :icon="Plus" :loading="loading" />
        <StatCard
          label="今日失败操作"
          :value="metricValue('failedOperationCount')"
          :icon="Warning"
          :tone="failedCount ? 'warning' : 'primary'"
          :loading="loading"
        />
      </AdminMetricGroup>
    </div>

    <section v-if="loadState === 'idle' || loadState === 'loading'" class="panel dashboard-loading" data-admin-dashboard-loading aria-live="polite">
      <div><strong>正在加载运营数据</strong><span>正在读取系统汇总数据。</span></div>
      <el-skeleton animated :rows="3" />
    </section>

    <div class="admin-grid">
      <section v-if="loadState === 'success'" class="panel admin-panel" data-admin-health-panel>
        <div class="panel-header">
          <div>
            <h2>运营健康度</h2>
            <span>从增长、AI 使用和异常操作判断当前系统状态</span>
          </div>
          <StatusTag class="health-status" :status="healthStatus" :label="healthLabel" />
        </div>
        <div class="health-list">
          <div v-for="item in healthItems" :key="item.label" class="health-item">
            <el-icon><component :is="item.icon" /></el-icon>
            <div>
              <strong>{{ item.label }}</strong>
              <span>{{ item.description }}</span>
            </div>
            <StatusTag :status="item.status" :label="item.value" size="small" />
          </div>
        </div>
      </section>

      <section v-if="loadState === 'success'" class="panel admin-panel" data-admin-activity-panel>
        <div class="panel-header">
          <div>
            <h2>业务活动结构</h2>
            <span>基于现有汇总数据比较核心业务资产规模</span>
          </div>
        </div>
        <div class="activity-list" aria-label="业务活动结构条形图">
          <div v-for="item in activityItems" :key="item.label" class="activity-item">
            <div>
              <strong>{{ item.label }}</strong>
              <span>{{ item.value }}</span>
            </div>
            <div class="activity-track" aria-hidden="true">
              <span :style="{ width: `${activityWidth(item.value)}%` }" />
            </div>
          </div>
        </div>
      </section>

      <section v-if="visibleQuickLinks.length" class="panel admin-panel admin-panel-wide">
        <div class="panel-header">
          <div>
            <h2>后台快捷入口</h2>
          </div>
        </div>
        <div class="quick-admin-grid">
          <button v-for="item in visibleQuickLinks" :key="item.path" class="quick-admin-card" type="button" @click="router.push(item.path)">
            <el-icon><component :is="item.icon" /></el-icon>
            <strong>{{ item.label }}</strong>
            <span>{{ item.description }}</span>
          </button>
        </div>
      </section>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Briefcase, CircleCheck, DataBoard, Document, Files, Key, List, Plus, QuestionFilled, User, Warning
} from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import StatCard from '@/components/common/StatCard.vue'
import AdminMetricGroup from './AdminMetricGroup.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { getAdminDashboardSummaryApi } from '@/api/adminDashboard'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loadState = ref<'idle' | 'loading' | 'success' | 'error'>('idle')
const loadError = ref('')
const summary = ref<any | null>(null)
let active = true
let loadRequestId = 0

const loading = computed(() => loadState.value === 'idle' || loadState.value === 'loading')
const failedCount = computed(() => loadState.value === 'success' ? Number(summary.value?.failedOperationCount || 0) : 0)
const healthStatus = computed(() => (failedCount.value > 0 ? 'PENDING' : 'SUCCESS'))
const healthLabel = computed(() => (failedCount.value > 0 ? '需要关注' : '运行正常'))
const healthItems = computed(() => loadState.value === 'success' ? [
  {
    label: '用户增长', description: '今日新增用户反映注册与用户运营活跃度',
    value: `${summary.value?.todayNewUserCount || 0} 人`,
    status: Number(summary.value?.todayNewUserCount || 0) > 0 ? 'SUCCESS' : 'PENDING', icon: User
  },
  {
    label: 'AI 使用量', description: '分析报告和面试题报告共同体现 AI 能力闭环',
    value: `${Number(summary.value?.analysisReportCount || 0) + Number(summary.value?.interviewQuestionReportCount || 0)} 份`,
    status: 'PROCESSING', icon: DataBoard
  },
  {
    label: '异常操作', description: '失败操作需要进入操作日志进一步排查', value: `${failedCount.value} 次`,
    status: failedCount.value > 0 ? 'FAILED' : 'SUCCESS', icon: failedCount.value > 0 ? Warning : CircleCheck
  }
] : [])
const activityItems = computed(() => loadState.value === 'success' ? [
  { label: '用户资产', value: Number(summary.value?.userCount || 0) },
  { label: '求职材料', value: Number(summary.value?.resumeCount || 0) + Number(summary.value?.jobCount || 0) },
  { label: 'AI 产出', value: Number(summary.value?.analysisReportCount || 0) + Number(summary.value?.interviewQuestionReportCount || 0) },
  { label: '投递跟踪', value: Number(summary.value?.applicationCount || 0) }
] : [])
const activityMax = computed(() => Math.max(1, ...activityItems.value.map((item) => item.value)))
const quickLinks = [
  { label: '用户管理', description: '查看账号、状态与角色分配', path: '/admin/users', permission: 'user:read', icon: User },
  { label: '角色管理', description: '维护角色与权限关系', path: '/admin/roles', permission: 'role:read', icon: Key },
  { label: '权限管理', description: '查看系统权限资源清单', path: '/admin/permissions', permission: 'permission:read', icon: Files },
  { label: '操作日志', description: '排查操作结果与异常原因', path: '/admin/operation-logs', permission: 'operation-log:read', icon: List },
  { label: 'RAG 知识库', description: '维护 AI 分析的知识上下文', path: '/admin/rag-knowledge', permission: 'rag:read', icon: Document }
]
const visibleQuickLinks = computed(() => quickLinks.filter((item) => authStore.hasPermission(item.permission)))

function activityWidth(value: number) {
  return Math.max(value > 0 ? 8 : 0, Math.round((value / activityMax.value) * 100))
}

function metricValue(key: string) {
  if (loadState.value !== 'success') return '—'
  return Number(summary.value?.[key] || 0)
}

async function loadData() {
  const requestId = ++loadRequestId
  loadState.value = 'loading'
  loadError.value = ''
  summary.value = null
  try {
    const res: any = await getAdminDashboardSummaryApi()
    if (!active || requestId !== loadRequestId) return
    summary.value = res || {}
    loadState.value = 'success'
  } catch (error: any) {
    if (!active || requestId !== loadRequestId) return
    summary.value = null
    loadError.value = error?.message || '后台看板数据加载失败，请稍后重试'
    loadState.value = 'error'
  }
}

onMounted(loadData)
onBeforeUnmount(() => {
  active = false
  loadRequestId += 1
})
</script>

<style scoped>
.dashboard-metrics {
  display: grid;
  grid-template-columns: 2fr minmax(0, 1fr);
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.dashboard-scale { --admin-metric-columns: 3; }
.dashboard-today { --admin-metric-columns: 2; }

.dashboard-error { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); margin-bottom: var(--space-4); }
.dashboard-error .el-alert { flex: 1; }
.dashboard-loading { display: grid; gap: var(--space-3); margin-bottom: var(--space-4); }
.dashboard-loading strong, .dashboard-loading span { display: block; }
.dashboard-loading span { margin-top: var(--space-1); color: var(--color-text-muted); }
.admin-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-4); }
.admin-panel { margin: 0; }
.admin-panel-wide { grid-column: 1 / -1; }
.admin-panel h2 { margin: 0; font-size: 17px; }
.health-status { flex: none; white-space: nowrap; }
.health-list, .activity-list { display: grid; gap: var(--space-2); }
.health-item { display: grid; grid-template-columns: 34px minmax(0, 1fr) auto; gap: var(--space-3); align-items: center; padding: var(--space-2); border-bottom: 1px solid var(--color-border-soft); }
.health-item:last-child { border-bottom: 0; }
.health-item .el-icon { color: var(--color-primary); font-size: 20px; }
.health-item strong, .health-item span { display: block; }
.health-item span { margin-top: var(--space-1); color: var(--color-text-muted); font-size: 13px; }
.activity-item > div:first-child { display: flex; justify-content: space-between; gap: var(--space-3); margin-bottom: var(--space-2); }
.activity-item span { color: var(--color-text-muted); font-variant-numeric: tabular-nums; }
.activity-track { height: 9px; overflow: hidden; border-radius: 999px; background: var(--color-surface-muted); }
.activity-track span { display: block; height: 100%; border-radius: inherit; background: var(--color-primary); }
.quick-admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 180px), 1fr));
  gap: var(--space-2);
}

.quick-admin-card {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  align-content: start;
  align-items: center;
  min-height: 68px;
  gap: var(--space-1) var(--space-2);
  padding: var(--space-2);
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-sm);
  background: var(--color-surface-muted);
  color: var(--color-text);
  text-align: left;
  cursor: pointer;
}

.quick-admin-card:hover,
.quick-admin-card:focus-visible {
  border-color: var(--color-primary-border);
  background: var(--color-primary-soft);
}

.quick-admin-card .el-icon {
  color: var(--color-primary-hover);
  font-size: 18px;
}

.quick-admin-card span {
  grid-column: 2;
  color: var(--color-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1000px) {
  .dashboard-metrics,
  .admin-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 520px) {
  .dashboard-error {
    align-items: stretch;
    flex-direction: column;
  }

  .health-item {
    grid-template-columns: 24px minmax(0, 1fr);
  }

  .health-item .status-tag {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
