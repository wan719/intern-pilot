import request from '@/utils/request'

export function createResumeVersionApi(resumeId: number, data: any) {
  return request.post(`/api/resumes/${resumeId}/versions`, data)
}

export function getResumeVersionListApi(resumeId: number) {
  return request.get(`/api/resumes/${resumeId}/versions`)
}

export function getResumeVersionDetailApi(resumeId: number, versionId: number) {
  return request.get(`/api/resumes/${resumeId}/versions/${versionId}`)
}

export function updateResumeVersionApi(resumeId: number, versionId: number, data: any) {
  return request.put(`/api/resumes/${resumeId}/versions/${versionId}`, data)
}

export function setCurrentResumeVersionApi(resumeId: number, versionId: number) {
  return request.put(`/api/resumes/${resumeId}/versions/${versionId}/current`)
}

export function deleteResumeVersionApi(resumeId: number, versionId: number) {
  return request.delete(`/api/resumes/${resumeId}/versions/${versionId}`)
}

export function optimizeResumeVersionApi(resumeId: number, data: any) {
  return request.post(`/api/resumes/${resumeId}/versions/optimize`, data)
}

export function compareResumeVersionsApi(
  resumeId: number,
  oldVersionId: number,
  newVersionId: number
) {
  return request.get(`/api/resumes/${resumeId}/versions/compare`, {
    params: {
      oldVersionId,
      newVersionId
    }
  })
}