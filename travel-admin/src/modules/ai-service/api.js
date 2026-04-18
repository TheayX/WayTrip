import request from '@/shared/api/request.js'

// AI 知识管理接口统一基础路径。
const AI_KNOWLEDGE_BASE_PATH = '/ai/knowledge'

// RAG 命中预览接口路径，单独拆出避免和知识维护接口混淆。
const AI_RAG_PREVIEW_PATH = '/ai/rag/preview'

/**
 * 获取 AI 知识文档列表。
 *
 * @returns {Promise<any>}
 */
export function getAiKnowledgeDocuments() {
  return request.get(`${AI_KNOWLEDGE_BASE_PATH}/documents`)
}

/**
 * 获取单篇知识文档详情。
 *
 * @param {number|string} documentId 文档 ID
 * @returns {Promise<any>}
 */
export function getAiKnowledgeDocumentDetail(documentId) {
  return request.get(`${AI_KNOWLEDGE_BASE_PATH}/documents/${documentId}`)
}

/**
 * 创建知识文档。
 *
 * @param {object} data 文档表单数据
 * @returns {Promise<any>}
 */
export function createAiKnowledgeDocument(data) {
  return request.post(`${AI_KNOWLEDGE_BASE_PATH}/documents`, data)
}

/**
 * 更新知识文档。
 *
 * @param {number|string} documentId 文档 ID
 * @param {object} data 文档表单数据
 * @returns {Promise<any>}
 */
export function updateAiKnowledgeDocument(documentId, data) {
  return request.put(`${AI_KNOWLEDGE_BASE_PATH}/documents/${documentId}`, data)
}

/**
 * 更新知识文档启用状态。
 *
 * @param {number|string} documentId 文档 ID
 * @param {number|boolean} isEnabled 启用状态
 * @returns {Promise<any>}
 */
export function updateAiKnowledgeDocumentEnabled(documentId, isEnabled) {
  return request.put(`${AI_KNOWLEDGE_BASE_PATH}/documents/${documentId}/enabled`, { isEnabled })
}

/**
 * 重建单篇知识文档的分片与向量。
 *
 * @param {number|string} documentId 文档 ID
 * @returns {Promise<any>}
 */
export function rebuildAiKnowledgeDocument(documentId) {
  return request.put(`${AI_KNOWLEDGE_BASE_PATH}/documents/${documentId}/rebuild`)
}

/**
 * 重建全部启用知识。
 *
 * @returns {Promise<any>}
 */
export function rebuildAllAiKnowledge() {
  return request.post(`${AI_KNOWLEDGE_BASE_PATH}/rebuild`)
}

/**
 * 获取当前向量索引状态。
 *
 * @returns {Promise<any>}
 */
export function getAiVectorIndexStatus() {
  return request.get(`${AI_KNOWLEDGE_BASE_PATH}/vector/status`)
}

/**
 * 清空当前向量索引数据。
 *
 * @returns {Promise<any>}
 */
export function clearAiVectorIndex() {
  return request.delete(`${AI_KNOWLEDGE_BASE_PATH}/vector`)
}

/**
 * 清空并重建全部向量数据。
 *
 * @returns {Promise<any>}
 */
export function clearAndRebuildAiVectorIndex() {
  return request.post(`${AI_KNOWLEDGE_BASE_PATH}/vector/rebuild`)
}

/**
 * 预览当前查询在 RAG 链路中的命中情况。
 *
 * @param {object} params 预览参数
 * @returns {Promise<any>}
 */
export function previewAiKnowledge(params) {
  return request.get(AI_RAG_PREVIEW_PATH, {
    params: {
      // 追加时间戳，避免管理端预览被浏览器缓存干扰。
      ...params,
      _ts: Date.now()
    }
  })
}
