<template>
  <PageContainer title="后台看板" description="管理员视角下的系统运营概览、风险提示和常用管理入口。">
    <section v-loading="loading" class="admin-hero">
      <div>
        <span class="hero-kicker">InternPilot Admin</span>
        <h2>系统运营总览</h2>
        <p>集中查看用户、简历、岗位、AI 分析和投递数据，答辩演示时能快速说明平台闭环和后台治理能力。</p>
      </div>
      <div class="hero-indicators">
        <div>
          <strong>{{ summary.todayNewUserCount || 0 }}</strong>
          <span>今日新增用户</span>
        </div>
        <div :class="{ danger: Number(summary.failedOperationCount || 0) > 0 }">
          <strong>{{ summary.failedOperationCount || 0 }}</strong>
          <span>今日失败操作</span>
        </div>
      </div>
    </section>

    <div v-loading="loading" class="stat-grid">
      <StatCard label="用户总数" :value="summary.userCount || 0" :icon="User" />
      <StatCard label="简历总数" :value="summary.resumeCount || 0" :icon="Document" />
      <StatCard label="岗位总数" :value="summary.jobCount || 0" :icon="Briefcase" />
      <StatCard label="分析报告" :value="summary.analysisReportCount || 0" :icon="DataBoard" />
      <StatCard label="面试题报告" :value="summary.interviewQuestionReportCount || 0" :icon="QuestionFilled" />
      <StatCard label="投递记录" :value="summary.applicationCount || 0" :icon="List" />
      <StatCard label="今日新增用户" :value="summary.todayNewUserCount || 0" :icon="Plus" />
      <StatCard label="今日失败操作" :value="summary.failedOperationCount || 0" :icon="Warning" />
    </div>

    <div class="admin-grid">
      <section class="panel">
        <div class="panel-header">
          <div>
            <h3>运营健康度</h3>
            <span>用少量关键指标帮助判断系统是否稳定</span>
          </div>
          <el-tag :type="healthType" effect="plain">{{ healthLabel }}</el-tag>
        </div>
        <div class="health-list">
          <div v-for="item in healthItems" :key="item.label" class="health-item">
            <el-icon><component :is="item.icon" /></el-icon>
            <div>
              <strong>{{ item.label }}</strong>
              <span>{{ item.description }}</span>
            </div>
            <el-tag :type="item.type" effect="plain">{{ item.value }}</el-tag>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h3>后台快捷入口</h3>
            <span>进入管理后台后，用这里快速切换核心管理页面</span>
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
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Briefcase,
  CircleCheck,
  DataBoard,
  Document,
  Files,
  Key,
  List,
  Plus,
  QuestionFilled,
  User,
  Warning
} from '@element-plus/icons-vue'
import PageContainer from '@/components/common/PageContainer.vue'
import StatCard from '@/components/common/StatCard.vue'
import { getAdminDashboardSummaryApi } from '@/api/adminDashboard'

const router = useRouter()
const loading = ref(false)
const summary = ref<any>({})

const failedCount = computed(() => Number(summary.value.failedOperationCount || 0))
const healthType = computed(() => (failedCount.value > 0 ? 'warning' : 'success'))
const healthLabel = computed(() => (failedCount.value > 0 ? '需要关注' : '运行正常'))

const healthItems = computed(() => [
  {
    label: '用户增长',
    description: '今日新增用户可用于演示注册与用户运营数据',
    value: `${summary.value.todayNewUserCount || 0} 人`,
    type: Number(summary.value.todayNewUserCount || 0) > 0 ? 'success' : 'info',
    icon: User
  },
  {
    label: 'AI 使用量',
    description: '分析报告和面试题报告体现 AI 能力闭环',
    value: `${Number(summary.value.analysisReportCount || 0) + Number(summary.value.interviewQuestionReportCount || 0)} 份`,
    type: 'primary',
    icon: DataBoard
  },
  {
    label: '异常操作',
    description: '失败操作应进入操作日志进一步排查',
    value: `${failedCount.value} 次`,
    type: failedCount.value > 0 ? 'danger' : 'success',
    icon: failedCount.value > 0 ? Warning : CircleCheck
  }
])

const quickLinks = [
  { label: '用户管理', description: '查看账号、状态与角色分配', path: '/admin/users', icon: User },
  { label: '角色管理', description: '维护角色与权限关系', path: '/admin/roles', icon: Key },
  { label: '权限管理', description: '查看系统权限资源清单', path: '/admin/permissions', icon: Files },
  { label: '操作日志', description: '排查操作结果与异常原因', path: '/admin/operation-logs', icon: List },
  { label: 'RAG 知识库', description: '维护 AI 分析的知识上下文', path: '/admin/rag-knowledge', icon: Document }
]

async function loadData() {
  loading.value = true
  try {
    const res: any = await getAdminDashboardSummaryApi()
    summary.value = res || {}
  } catch (error: any) {
    ElMessage.error(error?.message || '后台看板数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.admin-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
  padding: 26px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.1), transparent 46%),
    var(--color-surface);
  box-shadow: var(--shadow-card);
}

.hero-kicker {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.admin-hero h2 {
  margin: 8px 0 10px;
  font-size: 26px;
}

.admin-hero p {
  max-width: 660px;
  margin: 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.hero-indicators {
  display: grid;
  grid-template-columns: repeat(2, 112px);
  gap: 12px;
}

.hero-indicators div {
  padding: 16px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
}

.hero-indicators strong,
.hero-indicators span {
  display: block;
}

.hero-indicators strong {
  font-size: 26px;
}

.hero-indicators span {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 12px;
}

.hero-indicators .danger strong {
  color: var(--color-danger);
}

.admin-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 18px;
}

.health-list {
  display: grid;
  gap: 12px;
}

.health-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
}

.health-item .el-icon {
  color: var(--color-primary);
  font-size: 20px;
}

.health-item strong,
.health-item span {
  display: block;
}

.health-item span {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 13px;
}

.quick-admin-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-admin-card {
  display: grid;
  min-height: 104px;
  gap: 7px;
  justify-items: start;
  padding: 14px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface-muted);
  color: var(--color-text);
  text-align: left;
  cursor: pointer;
}

.quick-admin-card:hover {
  border-color: var(--color-primary-border);
  background: var(--color-primary-soft);
}

.quick-admin-card .el-icon {
  color: var(--color-primary);
  font-size: 21px;
}

.quick-admin-card strong {
  font-size: 15px;
}

.quick-admin-card span {
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.5;
}

@media (max-width: 900px) {
  .admin-hero,
  .admin-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }

  .hero-indicators,
  .quick-admin-grid {
    grid-template-columns: 1fr;
  }
}
</style>
