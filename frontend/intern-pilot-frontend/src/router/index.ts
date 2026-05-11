import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/token'
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
      { path: 'jobs', component: () => import('@/views/job/JobList.vue'), meta: { title: '岗位管理' } },
      { path: 'analysis/match', component: () => import('@/views/analysis/AnalysisMatch.vue'), meta: { title: 'AI 匹配分析' } },
      { path: 'analysis/reports', component: () => import('@/views/analysis/AnalysisReportList.vue'), meta: { title: '分析报告' } },
      { path: 'interview-questions', component: () => import('@/views/interview/InterviewQuestionList.vue'), meta: { title: 'AI 面试题' } },
      { path: 'interview-questions/:id', component: () => import('@/views/interview/InterviewQuestionDetail.vue'), meta: { title: '面试题详情' } },
      { path: 'applications', component: () => import('@/views/application/ApplicationList.vue'), meta: { title: '投递记录' } },
      {
        path: 'admin/dashboard',
        component: () => import('@/views/admin/AdminDashboard.vue'),
        meta: { title: '后台看板', permission: 'dashboard:admin:read' }
      },
      {
        path: 'admin/users',
        component: () => import('@/views/admin/AdminUserList.vue'),
        meta: { title: '用户管理', permission: 'admin:user:read' }
      },
      {
        path: 'admin/roles',
        component: () => import('@/views/admin/AdminRoleList.vue'),
        meta: { title: '角色管理', permission: 'admin:role:read' }
      },
      {
        path: 'admin/permissions',
        component: () => import('@/views/admin/AdminPermissionList.vue'),
        meta: { title: '权限管理', permission: 'admin:permission:read' }
      },
      {
        path: 'admin/operation-logs',
        component: () => import('@/views/admin/OperationLogList.vue'),
        meta: { title: '操作日志', permission: 'system:log:read' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  if (!getToken()) return '/login'

  const permission = to.meta.permission as string | undefined
  if (!permission) return true

  const auth = useAuthStore()
  const permissions: string[] = auth.user?.permissions || []
  if (!permissions.includes(permission)) {
    return '/403'
  }
  return true
})

export default router
