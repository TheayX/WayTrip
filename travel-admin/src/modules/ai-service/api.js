import request from '@/shared/api/request.js'

const AI_KNOWLEDGE_BASE_PATH = '/ai/knowledge'
const AI_RAG_PREVIEW_PATH = '/ai/rag/preview'

export function getAiKnowledgeDocuments() {
  return request.get(`${AI_KNOWLEDGE_BASE_PATH}/documents`)
}

export function getAiKnowledgeDocumentDetail(documentId) {
  return request.get(`${AI_KNOWLEDGE_BASE_PATH}/documents/${documentId}`)
}

export function createAiKnowledgeDocument(data) {
  return request.post(`${AI_KNOWLEDGE_BASE_PATH}/documents`, data)
}

export function updateAiKnowledgeDocument(documentId, data) {
  return request.put(`${AI_KNOWLEDGE_BASE_PATH}/documents/${documentId}`, data)
}

export function updateAiKnowledgeDocumentEnabled(documentId, isEnabled) {
  return request.put(`${AI_KNOWLEDGE_BASE_PATH}/documents/${documentId}/enabled`, { isEnabled })
}

export function rebuildAiKnowledgeDocument(documentId) {
  return request.put(`${AI_KNOWLEDGE_BASE_PATH}/documents/${documentId}/rebuild`)
}

export function rebuildAllAiKnowledge() {
  return request.post(`${AI_KNOWLEDGE_BASE_PATH}/rebuild`)
}

export function getAiVectorIndexStatus() {
  return request.get(`${AI_KNOWLEDGE_BASE_PATH}/vector/status`)
}

export function clearAiVectorIndex() {
  return request.delete(`${AI_KNOWLEDGE_BASE_PATH}/vector`)
}

export function clearAndRebuildAiVectorIndex() {
  return request.post(`${AI_KNOWLEDGE_BASE_PATH}/vector/rebuild`)
}

export function previewAiKnowledge(params) {
  return request.get(AI_RAG_PREVIEW_PATH, {
    params: {
      ...params,
      _ts: Date.now()
    }
  })
}
