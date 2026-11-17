import { expect, it } from 'vitest'
import { upcomingInterviews, newestWorkspaceItem } from '../workspaceContent'

it('filters invalid, past and closed interviews before selecting the three soonest', () => {
  const now = new Date(2026, 10, 17, 12).getTime()
  const items = [
    { applicationId: 1, interviewDate: '2026-11-19T10:00:00', status: 'FIRST_INTERVIEW' },
    { applicationId: 2, interviewDate: '2026-11-18T10:00:00', status: 'REJECTED' },
    { applicationId: 3, interviewDate: '2026-11-17T13:00:00' },
    { applicationId: 4, interviewDate: '2026-11-17T11:00:00' },
    { applicationId: 5, interviewDate: '2026-11-18T12:00:00' },
    { applicationId: 6, interviewDate: '2026-11-20T10:00:00' },
    { applicationId: 7, interviewDate: 'invalid' },
    { applicationId: 8, interviewDate: '2026-11-17T15:00:00', status: 'GIVEN_UP' },
  ]
  expect(upcomingInterviews(items, now).map(item => item.applicationId)).toEqual([3, 5, 1])
})

it('chooses a genuinely dated recent item without mutating the source list', () => {
  const items = [{ id: 1, createdAt: '2026-11-10' }, { id: 2, createdAt: '2026-11-16' }, { id: 3 }]
  expect(newestWorkspaceItem(items)?.id).toBe(2)
  expect(newestWorkspaceItem([items[2]!] as typeof items)).toBeUndefined()
  expect(items.map(item => item.id)).toEqual([1, 2, 3])
})
