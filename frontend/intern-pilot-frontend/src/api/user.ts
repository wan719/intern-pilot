import request from '@/utils/request'

export interface UpdateProfileRequest {
  nickname?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export function getCurrentUserApi() {
  return request.get('/api/user/me')
}

export function getUserProfileApi() {
  return request.get('/api/user/profile')
}

export function updateUserProfileApi(data: UpdateProfileRequest) {
  return request.put('/api/user/profile', data)
}

export function changePasswordApi(data: ChangePasswordRequest) {
  return request.put('/api/user/password', data)
}
