import request from '@/utils/request'

export interface LoginRequest {
  account: string
  password: string
}

export interface RegisterRequest {
  account: string
  accountType: string
  password: string
  confirmPassword: string
  captchaCode: string
  username?: string
  school?: string
  major?: string
  grade?: string
}

export interface CaptchaSendRequest {
  target: string
  type: string
}

export function loginApi(data: LoginRequest) {
  return request.post('/api/auth/login', data)
}

export function registerApi(data: RegisterRequest) {
  return request.post('/api/auth/register', data)
}

export function sendRegisterCaptchaApi(data: CaptchaSendRequest) {
  return request.post('/api/auth/captcha/register', data)
}

export function getCurrentUserApi() {
  return request.get('/api/user/me')
}