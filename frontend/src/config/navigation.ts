export type JourneyKey = 'dashboard' | 'resumes' | 'jobs' | 'analysis' | 'interview' | 'applications'

export interface JourneyItem {
  key: JourneyKey
  label: string
  shortLabel: string
  path: string
  mobile: boolean
  matches: (path: string) => boolean
}

export interface AdminNavItem {
  label: string
  path: string
  permission: string
}

export interface AdminNavGroup {
  label: string
  items: AdminNavItem[]
}

const matchesPath = (basePath: string) => (path: string) =>
  path === basePath || path.startsWith(`${basePath}/`)

export const journeyItems: JourneyItem[] = [
  { key: 'dashboard', label: '工作台', shortLabel: '工作台', path: '/dashboard', mobile: true, matches: matchesPath('/dashboard') },
  { key: 'resumes', label: '简历中心', shortLabel: '简历', path: '/resumes', mobile: false, matches: matchesPath('/resumes') },
  {
    key: 'jobs',
    label: '岗位机会',
    shortLabel: '岗位',
    path: '/jobs',
    mobile: true,
    matches: (path) => matchesPath('/jobs')(path) || matchesPath('/job-recommendations')(path)
  },
  { key: 'analysis', label: '匹配分析', shortLabel: '分析', path: '/analysis/match', mobile: true, matches: matchesPath('/analysis') },
  {
    key: 'interview',
    label: '面试准备',
    shortLabel: '面试',
    path: '/interview-questions',
    mobile: false,
    matches: matchesPath('/interview-questions')
  },
  {
    key: 'applications',
    label: '投递追踪',
    shortLabel: '投递',
    path: '/applications',
    mobile: true,
    matches: matchesPath('/applications')
  }
]

export const adminNavGroups: AdminNavGroup[] = [
  { label: '后台概览', items: [{ label: '后台看板', path: '/admin/dashboard', permission: 'admin:dashboard' }] },
  {
    label: '用户与权限',
    items: [
      { label: '用户管理', path: '/admin/users', permission: 'user:read' },
      { label: '角色管理', path: '/admin/roles', permission: 'role:read' },
      { label: '权限管理', path: '/admin/permissions', permission: 'permission:read' }
    ]
  },
  { label: 'AI 知识', items: [{ label: 'RAG 知识库', path: '/admin/rag-knowledge', permission: 'rag:read' }] },
  {
    label: '运营管理',
    items: [
      { label: '操作日志', path: '/admin/operation-logs', permission: 'operation-log:read' },
      { label: '用户反馈', path: '/admin/feedback', permission: 'feedback:read' }
    ]
  }
]

export const resolveJourneyKey = (path: string): JourneyKey | undefined =>
  journeyItems.find((item) => item.matches(path))?.key
