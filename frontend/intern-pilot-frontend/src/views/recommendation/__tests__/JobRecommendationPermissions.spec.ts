import { describe, expect, it } from 'vitest'
import router from '@/router'

describe('job recommendation permission contract', () => {
  it('keeps analysis:read on both recommendation routes for the shared guard', () => {
    expect(router.resolve('/job-recommendations').meta.permission).toBe('analysis:read')
    expect(router.resolve('/job-recommendations/31').meta.permission).toBe('analysis:read')
  })
})
