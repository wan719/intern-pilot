import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/utils/request'

const originalAdapter = request.defaults.adapter

function failedAdapter(): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig) => Promise.reject({
    config,
    response: {
      config,
      data: { message: '受控详情失败' },
      headers: {},
      status: 503,
      statusText: 'Service Unavailable'
    } satisfies AxiosResponse
  })
}

function businessFailureAdapter(): AxiosAdapter {
  return async (config: InternalAxiosRequestConfig) => ({
    config,
    data: { code: 500, message: '受控业务失败' },
    headers: {},
    status: 200,
    statusText: 'OK'
  })
}

describe('request error presentation', () => {
  beforeEach(() => {
    request.defaults.adapter = failedAdapter()
    vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as any)
  })

  afterEach(() => {
    request.defaults.adapter = originalAdapter
    vi.restoreAllMocks()
  })

  it('keeps foreground request failures globally visible by default', async () => {
    await expect(request.get('/foreground-detail')).rejects.toBeTruthy()

    expect(ElMessage.error).toHaveBeenCalledOnce()
    expect(ElMessage.error).toHaveBeenCalledWith('受控详情失败')
  })

  it('suppresses only explicitly silent background request failures', async () => {
    await expect(request.get('/background-detail', { silentError: true })).rejects.toBeTruthy()

    request.defaults.adapter = businessFailureAdapter()
    await expect(request.get('/background-business-detail', { silentError: true })).rejects.toBeTruthy()

    expect(ElMessage.error).not.toHaveBeenCalled()
  })
})
