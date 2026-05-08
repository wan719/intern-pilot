import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/token'

const routes = [
  { path: '/login', component: () => import('@/views/auth/Login.vue'), meta: { public: true } },
  { path: '/register', component: () => import('@/views/auth/Register.vue'), meta: { public: true } },
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
      { path: 'applications', component: () => import('@/views/application/ApplicationList.vue'), meta: { title: '投递记录' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  return getToken() ? true : '/login'
})

export default router
