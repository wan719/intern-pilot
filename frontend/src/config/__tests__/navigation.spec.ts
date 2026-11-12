import { describe, expect, it } from 'vitest'
import { adminNavGroups, journeyItems, resolveJourneyKey } from '@/config/navigation'
import router from '@/router'

describe('journey navigation', () => {
  it('keeps workbench plus five journey stages in order', () => {
    expect(journeyItems.map((item) => item.key)).toEqual([
      'dashboard', 'resumes', 'jobs', 'analysis', 'interview', 'applications'
    ])
  })

  it.each([
    ['/dashboard', 'dashboard'],
    ['/resumes/7/versions', 'resumes'],
    ['/jobs', 'jobs'],
    ['/job-recommendations/3', 'jobs'],
    ['/analysis/match', 'analysis'],
    ['/analysis/reports', 'analysis'],
    ['/interview-questions/8', 'interview'],
    ['/applications', 'applications']
  ])('maps %s to %s', (path, key) => {
    expect(resolveJourneyKey(path)).toBe(key)
  })

  it('keeps the four mobile-primary stages separate from More', () => {
    expect(journeyItems.filter((item) => item.mobile).map((item) => item.key)).toEqual([
      'dashboard', 'jobs', 'analysis', 'applications'
    ])
    expect(journeyItems.filter((item) => !item.mobile).map((item) => item.key)).toEqual([
      'resumes', 'interview'
    ])
  })

  it('preserves admin group order with each route and permission pairing', () => {
    expect(adminNavGroups.map((group) => ({
      label: group.label,
      items: group.items.map((item) => [item.path, item.permission])
    }))).toEqual([
      { label: '后台概览', items: [['/admin/dashboard', 'admin:dashboard']] },
      {
        label: '用户与权限',
        items: [
          ['/admin/users', 'user:read'],
          ['/admin/roles', 'role:read'],
          ['/admin/permissions', 'permission:read']
        ]
      },
      { label: 'AI 知识', items: [['/admin/rag-knowledge', 'rag:read']] },
      {
        label: '运营管理',
        items: [
          ['/admin/operation-logs', 'operation-log:read'],
          ['/admin/feedback', 'feedback:read']
        ]
      }
    ])
  })

  it('assigns every journey route its configured journey metadata', () => {
    const expectedJourneys = new Map([
      ['/dashboard', 'dashboard'],
      ['/resumes', 'resumes'],
      ['/resumes/:resumeId/versions', 'resumes'],
      ['/jobs', 'jobs'],
      ['/job-recommendations', 'jobs'],
      ['/job-recommendations/:batchId', 'jobs'],
      ['/analysis/match', 'analysis'],
      ['/analysis/reports', 'analysis'],
      ['/interview-questions', 'interview'],
      ['/interview-questions/:id', 'interview'],
      ['/applications', 'applications']
    ])

    const actualJourneys = new Map(
      router.getRoutes()
        .filter((route) => expectedJourneys.has(route.path))
        .map((route) => [route.path, route.meta.journey])
    )

    expect(actualJourneys).toEqual(expectedJourneys)
  })
})
