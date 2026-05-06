import request from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest extends LoginRequest {
  confirmPassword: string
  email?: string
  school?: string
  major?: string
  grade?: string
}

export function loginApi(data: LoginRequest) {
  return request.post('/api/auth/login', data)
}

export function registerApi(data: RegisterRequest) {
  return request.post('/api/auth/register', data)
}
