<template>
  <PageContainer width="wide" title="后台看板" description="管理员视角下的系统运营概览、风险提示和常用管理入口。">
    <section v-loading="loading" class="admin-hero" aria-label="系统运营总览">
      <div>
        <span class="hero-kicker">InternPilot Admin</span>
        <h2>系统运营总览</h2>
        <p>集中查看用户、简历、岗位、AI 分析和投递数据，快速判断业务活跃度与后台治理风险。</p>
      </div>
      <div class="hero-indicators">
        <div>
          <strong>{{ summary.todayNewUserCount || 0 }}</strong>
          <span>今日新增用户</span>
        </div>
        <div :class="{ danger: failedCount > 0 }">
          <strong>{{ summary.failedOperationCount || 0 }}</strong>
          <span>今日失败操作</span>
        </div>
      </div>
    </section>

    <div class="stat-grid admin-stat-grid" aria-label="后台核心指标">
      <StatCard label="用户总数" :value="summary.userCount || 0" :icon="User" :loading="loading" />
      <StatCard label="简历总数" :value="summary.resumeCount || 0" :icon="Document" :loading="loading" />
      <StatCard label="岗位总数" :value="summary.jobCount || 0" :icon="Briefcase" :loading="loading" />
      <StatCard label="分析报告" :value="summary.analysisReportCount || 0" :icon="DataBoard" :loading="loading" />
      <StatCard label="面试题报告" :value="summary.interviewQuestionReportCount || 0" :icon="QuestionFilled" :loading="loading" />
      <StatCard label="投递记录" :value="summary.applicationCount || 0" :icon="List" :loading="loading" />
      <StatCard label="今日新增用户" :value="summary.todayNewUserCount || 0" :icon="Plus" :loading="loading" />
      <StatCard label="今日失败操作" :value="summary.failedOperationCount || 0" :icon="Warning" :loading="loading" />
    </div>

    <div class="admin-grid">
      <section class="panel admin-panel" data-admin-health-panel>
        <div class="panel-header">
          <div>
            <h2>运营健康度</h2>
            <span>从增长、AI 使用和异常操作判断当前系统状态</span>
          </div>
          <StatusTag :status="healthStatus" :label="healthLabel" />
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

      <section class="panel admin-panel" data-admin-activity-panel>
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

      <section class="panel admin-panel admin-panel-wide">
        <div class="panel-header">
          <div>
            <h2>后台快捷入口</h2>
            <span>沿用现有管理路由进入各治理域</span>
          </div>
        </div>
        <div class="quick-admin-grid">
          <button v-for="item in quickLinks" :key="item.path" class="quick-admin-card" type="button" @click="router.push(item.path)">
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
import { ElMessage } from 'element-plus'
import {
  Briefcase, CircleCheck, DataBoard, Document, Files, Key, List, Plus, QuestionFilled, User, Warning
} from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { getAdminDashboardSummaryApi } from '@/api/adminDashboard'

const router = useRouter()
const loading = ref(false)
const summary = ref<any>({})
let active = true
let loadRequestId = 0

const failedCount = computed(() => Number(summary.value.failedOperationCount || 0))
const healthStatus = computed(() => (failedCount.value > 0 ? 'PENDING' : 'SUCCESS'))
const healthLabel = computed(() => (failedCount.value > 0 ? '需要关注' : '运行正常'))
const healthItems = computed(() => [
  {
    label: '用户增长', description: '今日新增用户反映注册与用户运营活跃度',
    value: `${summary.value.todayNewUserCount || 0} 人`,
    status: Number(summary.value.todayNewUserCount || 0) > 0 ? 'SUCCESS' : 'PENDING', icon: User
  },
  {
    label: 'AI 使用量', description: '分析报告和面试题报告共同体现 AI 能力闭环',
    value: `${Number(summary.value.analysisReportCount || 0) + Number(summary.value.interviewQuestionReportCount || 0)} 份`,
    status: 'PROCESSING', icon: DataBoard
  },
  {
    label: '异常操作', description: '失败操作需要进入操作日志进一步排查', value: `${failedCount.value} 次`,
    status: failedCount.value > 0 ? 'FAILED' : 'SUCCESS', icon: failedCount.value > 0 ? Warning : CircleCheck
  }
])
const activityItems = computed(() => [
  { label: '用户资产', value: Number(summary.value.userCount || 0) },
  { label: '求职材料', value: Number(summary.value.resumeCount || 0) + Number(summary.value.jobCount || 0) },
  { label: 'AI 产出', value: Number(summary.value.analysisReportCount || 0) + Number(summary.value.interviewQuestionReportCount || 0) },
  { label: '投递跟踪', value: Number(summary.value.applicationCount || 0) }
])
const activityMax = computed(() => Math.max(1, ...activityItems.value.map((item) => item.value)))
const quickLinks = [
  { label: '用户管理', description: '查看账号、状态与角色分配', path: '/admin/users', icon: User },
  { label: '角色管理', description: '维护角色与权限关系', path: '/admin/roles', icon: Key },
  { label: '权限管理', description: '查看系统权限资源清单', path: '/admin/permissions', icon: Files },
  { label: '操作日志', description: '排查操作结果与异常原因', path: '/admin/operation-logs', icon: List },
  { label: 'RAG 知识库', description: '维护 AI 分析的知识上下文', path: '/admin/rag-knowledge', icon: Document }
]

function activityWidth(value: number) {
  return Math.max(value > 0 ? 8 : 0, Math.round((value / activityMax.value) * 100))
}

async function loadData() {
  const requestId = ++loadRequestId
  loading.value = true
  try {
    const res: any = await getAdminDashboardSummaryApi()
    if (!active || requestId !== loadRequestId) return
    summary.value = res || {}
  } catch (error: any) {
    if (!active || requestId !== loadRequestId) return
    ElMessage.error(error?.message || '后台看板数据加载失败，请稍后重试')
  } finally {
    if (active && requestId === loadRequestId) loading.value = false
  }
}

onMounted(loadData)
onBeforeUnmount(() => {
  active = false
  loadRequestId += 1
})
</script>

<style scoped>
.admin-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-5);
  margin-bottom: var(--space-4);
  padding: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--color-primary-soft), transparent 48%), var(--color-surface);
}

.hero-kicker { color: var(--color-primary-hover); font-size: 12px; font-weight: 700; letter-spacing: .04em; text-transform: uppercase; }
.admin-hero h2 { margin: var(--space-2) 0; font-size: 24px; }
.admin-hero p { max-width: 680px; margin: 0; color: var(--color-text-muted); line-height: 1.65; }
.hero-indicators { display: grid; grid-template-columns: repeat(2, 112px); gap: var(--space-3); }
.hero-indicators div { padding: var(--space-3); border: 1px solid var(--color-border-soft); border-radius: var(--radius-md); background: var(--color-surface-muted); }
.hero-indicators strong, .hero-indicators span { display: block; }
.hero-indicators strong { font-variant-numeric: tabular-nums; font-size: 25px; }
.hero-indicators span { margin-top: var(--space-1); color: var(--color-text-muted); font-size: 12px; }
.hero-indicators .danger strong { color: var(--color-danger); }
.admin-stat-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.admin-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-4); }
.admin-panel { margin: 0; }
.admin-panel-wide { grid-column: 1 / -1; }
.admin-panel h2 { margin: 0; font-size: 17px; }
.health-list, .activity-list { display: grid; gap: var(--space-3); }
.health-item { display: grid; grid-template-columns: 34px minmax(0, 1fr) auto; gap: var(--space-3); align-items: center; padding: var(--space-3); border: 1px solid var(--color-border-soft); border-radius: var(--radius-md); background: var(--color-surface-muted); }
.health-item .el-icon { color: var(--color-primary); font-size: 20px; }
.health-item strong, .health-item span { display: block; }
.health-item span { margin-top: var(--space-1); color: var(--color-text-muted); font-size: 13px; }
.activity-item > div:first-child { display: flex; justify-content: space-between; gap: var(--space-3); margin-bottom: var(--space-2); }
.activity-item span { color: var(--color-text-muted); font-variant-numeric: tabular-nums; }
.activity-track { height: 9px; overflow: hidden; border-radius: 999px; background: var(--color-surface-muted); }
.activity-track span { display: block; height: 100%; border-radius: inherit; background: var(--color-primary); }
.quick-admin-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: var(--space-3); }
.quick-admin-card { display: grid; min-height: 104px; gap: var(--space-2); justify-items: start; padding: var(--space-3); border: 1px solid var(--color-border-soft); border-radius: var(--radius-md); background: var(--color-surface-muted); color: var(--color-text); text-align: left; cursor: pointer; }
.quick-admin-card:hover, .quick-admin-card:focus-visible { border-color: var(--color-primary-border); background: var(--color-primary-soft); }
.quick-admin-card .el-icon { color: var(--color-primary); font-size: 21px; }
.quick-admin-card span { color: var(--color-text-muted); font-size: 13px; line-height: 1.5; }

@media (max-width: 1100px) {
  .admin-stat-grid, .quick-admin-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 900px) {
  .admin-hero, .admin-grid { grid-template-columns: 1fr; flex-direction: column; align-items: stretch; }
  .admin-panel-wide { grid-column: auto; }
}

@media (max-width: 520px) {
  .admin-stat-grid, .hero-indicators, .quick-admin-grid { grid-template-columns: 1fr; }
  .health-item { grid-template-columns: 28px minmax(0, 1fr); }
  .health-item .status-tag { grid-column: 2; justify-self: start; }
}
</style>
