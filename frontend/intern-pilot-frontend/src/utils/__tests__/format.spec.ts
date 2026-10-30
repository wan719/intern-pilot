import { describe, expect, it } from 'vitest'
import { formatDateTime } from '@/utils/format'

describe('formatDateTime display fallback (ASE P1 regression)', () => {
  it('preserves the existing display format for valid timestamps', () => {
    expect(formatDateTime('2026-10-28T09:30:00')).toBe('2026-10-28 09:30')
    expect(formatDateTime('2026-10-28')).toBe('2026-10-28 00:00')
  })

  it.each([undefined, '', '   ', 'not-a-date'])(
    'shows a placeholder instead of an invalid timestamp for %s',
    (value) => {
      expect(formatDateTime(value)).toBe('-')
    }
  )
})
