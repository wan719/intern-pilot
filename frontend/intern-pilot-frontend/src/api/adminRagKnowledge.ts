import request from '@/utils/request'

export function createRagKnowledgeApi(data: any) {
  return request.post('/api/admin/rag/knowledge', data)
}

export function updateRagKnowledgeApi(documentId: number, data: any) {
  return request.put(`/api/admin/rag/knowledge/${documentId}`, data)
}

export function deleteRagKnowledgeApi(documentId: number) {
  return request.delete(`/api/admin/rag/knowledge/${documentId}`)
}

export function rebuildRagKnowledgeApi(documentId: number) {
  return request.post(`/api/admin/rag/knowledge/${documentId}/rebuild`)
}

export function getRagKnowledgeListApi(params: any) {
  return request.get('/api/admin/rag/knowledge', { params })
}

export function getRagKnowledgeDetailApi(documentId: number) {
  return request.get(`/api/admin/rag/knowledge/${documentId}`)
}

export function searchRagKnowledgeApi(data: any) {
  return request.post('/api/admin/rag/knowledge/search', data)
}