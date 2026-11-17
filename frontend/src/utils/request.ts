import axios, { type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken, removeToken } from '@/utils/token'

declare module 'axios' {
  export interface AxiosRequestConfig {
    silentError?: boolean
  }

  export interface InternalAxiosRequestConfig {
    silentError?: boolean
  }
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 60000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function handleUnauthorized(config?: InternalAxiosRequestConfig) {
  const authorization = config?.headers.Authorization
  if (!authorization) return false

  const token = getToken()
  if (!token || authorization !== `Bearer ${token}`) return true

  removeToken()
  void router.push('/login')
  return false
}

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body?.code !== 200) {
      if (body?.code === 401 && handleUnauthorized(response.config)) return Promise.reject(body)
      if (!response.config.silentError) ElMessage.error(getFriendlyErrorMessage(body?.message, body?.code))
      return Promise.reject(body)
    }
    return body.data
  },
  (error) => {
    const status = error.response?.status
    if (status === 401 && handleUnauthorized(error.config)) return Promise.reject(error)
    if (!error.config?.silentError) ElMessage.error(getFriendlyErrorMessage(error.response?.data?.message, status))
    return Promise.reject(error)
  }
)

function getFriendlyErrorMessage(message?: string, status?: number | string) {
  if (message && !/unknown error|request failed|network error/i.test(message)) {
    return message
  }
  const code = Number(status)
  if (code === 400) return '请求参数不完整，请检查表单内容后重试'
  if (code === 401) return '登录状态已过期，请重新登录'
  if (code === 403) return '当前账号没有权限执行该操作'
  if (code === 404) return '请求的数据不存在或已被删除'
  if (code >= 500) return '服务暂时不可用，请稍后重试'
  return '网络请求失败，请检查连接后重试'
}

export default request
