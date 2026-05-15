import { createRouter, createWebHistory } from 'vue-router'
import { getToken, removeToken } from '@/utils/token'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', component: () => import('@/views/auth/Login.vue'), meta: { public: true } },
  { path: '/register', component: () => import('@/views/auth/Register.vue'), meta: { public: true } },
  { path: '/403', component: () => import('@/views/error/Forbidden.vue'), meta: { public: true, title: '无权限' } },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'resumes', component: () => import('@/views/resume/ResumeList.vue'), meta: { title: '简历管理' } },
      { path: 'resumes/:resumeId/versions', component: () => import('@/views/resume/ResumeVersionList.vue'), meta: { title: '简历版本管理', permission: 'resume:read' } },
      { path: 'jobs', component: () => import('@/views/job/JobList.vue'), meta: { title: '岗位管理' } },
      { path: 'analysis/match', component: () => import('@/views/analysis/AnalysisMatch.vue'), meta: { title: 'AI 匹配分析' } },
      { path: 'analysis/reports', component: () => import('@/views/analysis/AnalysisReportList.vue'), meta: { title: '分析报告' } },
      { path: 'job-recommendations', component: () => import('@/views/recommendation/JobRecommendationList.vue'), meta: { title: '岗位推荐', permission: 'analysis:read' } },
      { path: 'job-recommendations/:batchId', component: () => import('@/views/recommendation/JobRecommendationDetail.vue'), meta: { title: '推荐详情', permission: 'analysis:read' } },
      { path: 'interview-questions', component: () => import('@/views/interview/InterviewQuestionList.vue'), meta: { title: 'AI 面试题' } },
      { path: 'interview-questions/:id', component: () => import('@/views/interview/InterviewQuestionDetail.vue'), meta: { title: '面试题详情' } },
      { path: 'applications', component: () => import('@/views/application/ApplicationList.vue'), meta: { title: '投递记录' } },
      {
        path: 'admin/dashboard',
        component: () => import('@/views/admin/AdminDashboard.vue'),
        meta: { title: '后台看板', permission: 'admin:dashboard' }
      },
      {
        path: 'admin/users',
        component: () => import('@/views/admin/AdminUserList.vue'),
        meta: { title: '用户管理', permission: 'user:read' }
      },
      {
        path: 'admin/roles',
        component: () => import('@/views/admin/AdminRoleList.vue'),
        meta: { title: '角色管理', permission: 'role:read' }
      },
      {
        path: 'admin/permissions',
        component: () => import('@/views/admin/AdminPermissionList.vue'),
        meta: { title: '权限管理', permission: 'permission:read' }
      },
      {
        path: 'admin/operation-logs',
        component: () => import('@/views/admin/OperationLogList.vue'),
        meta: { title: '操作日志', permission: 'operation-log:read' }
      },
      {
        path: 'admin/rag-knowledge',
        component: () => import('@/views/admin/AdminRagKnowledgeList.vue'),
        meta: { title: 'RAG 知识库', permission: 'rag:read' }
      }
      
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  if (to.meta.public) return true
  if (!getToken()) return '/login'

  const auth = useAuthStore()
  if (!auth.user) {
    try {
      await auth.fetchCurrentUser()
    } catch {
      auth.logout()
      removeToken()
      return '/login'
    }
  }

  const permission = to.meta.permission as string | undefined
  if (!permission) return true

  if (!auth.hasPermission(permission)) {
    return '/403'
  }
  return true
})

export default router
