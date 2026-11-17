import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/utils/request'
import router from '@/router'
import { getToken, removeToken, setToken } from '@/utils/token'

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
    removeToken()
    request.defaults.adapter = failedAdapter()
    vi.spyOn(ElMessage, 'error').mockImplementation(() => undefined as any)
  })

  afterEach(() => {
    removeToken()
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

  it.each([false, true])('ignores late unauthorized redirects across logout and relogin (business response: %s)', async (businessResponse) => {
    const push = vi.spyOn(router, 'push').mockResolvedValue(undefined)
    for (const nextToken of [null, 'replacement-session']) {
      setToken('previous-session')
      request.defaults.adapter = async (config) => {
        expect(config.headers.Authorization).toBe('Bearer previous-session')
        if (nextToken) setToken(nextToken)
        else removeToken()
        const response = { config, data: { code: 401 }, headers: {}, status: businessResponse ? 200 : 401, statusText: '' }
        if (businessResponse) return response
        throw { config, response }
      }
      await expect(request.get('/pending-request')).rejects.toBeTruthy()
      expect(getToken()).toBe(nextToken)
    }
    expect(push).not.toHaveBeenCalled()
    expect(ElMessage.error).not.toHaveBeenCalled()
  })

  it('passes authenticated Blob responses through without unwrapping JSON', async () => {
    setToken('file-reader')
    const file = new Blob(['%PDF'], { type: 'application/pdf' })
    request.defaults.adapter = async (config) => {
      expect(config.headers.Authorization).toBe('Bearer file-reader')
      return { config, data: file, headers: { 'content-type': 'application/pdf' }, status: 200, statusText: 'OK' }
    }
    await expect(request.get('/api/resumes/7/file', { responseType: 'blob', silentError: true })).resolves.toBe(file)
  })

  it('still redirects an expired current session to login', async () => {
    const push = vi.spyOn(router, 'push').mockResolvedValue(undefined)
    setToken('expired-session')
    request.defaults.adapter = async (config) => {
      throw { config, response: { config, status: 401, data: {} } }
    }
    await expect(request.get('/restricted')).rejects.toBeTruthy()
    expect(getToken()).toBeNull()
    expect(push).toHaveBeenCalledWith('/login')
  })
})
