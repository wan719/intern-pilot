import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/utils/request'
import { getInterviewQuestionDetailApi } from '@/api/interviewQuestion'

vi.mock('@/utils/request', () => ({
  default: { get: vi.fn() }
}))

describe('interview question request modes', () => {
  beforeEach(() => vi.clearAllMocks())

  it('threads the opt-in silent mode without changing the detail endpoint', () => {
    getInterviewQuestionDetailApi(41, { silentError: true })

    expect(request.get).toHaveBeenCalledWith('/api/interview-questions/41', { silentError: true })
  })

  it('keeps foreground detail reads on the default request behavior', () => {
    getInterviewQuestionDetailApi(41)

    expect(request.get).toHaveBeenCalledWith('/api/interview-questions/41')
  })
})
