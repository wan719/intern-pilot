import { describe, expect, it } from 'vitest'
import { journeyItems, resolveJourneyKey } from '@/config/navigation'

describe('journey navigation', () => {
  it('keeps workbench plus five journey stages in order', () => {
    expect(journeyItems.map((item) => item.key)).toEqual([
      'dashboard', 'resumes', 'jobs', 'analysis', 'interview', 'applications'
    ])
  })

  it.each([
    ['/dashboard', 'dashboard'],
    ['/resumes/7/versions', 'resumes'],
    ['/job-recommendations/3', 'jobs'],
    ['/analysis/reports', 'analysis'],
    ['/interview-questions/8', 'interview'],
    ['/applications', 'applications']
  ])('maps %s to %s', (path, key) => {
    expect(resolveJourneyKey(path)).toBe(key)
  })
})
