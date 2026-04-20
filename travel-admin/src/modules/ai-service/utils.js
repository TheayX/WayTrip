/**
 * 创建 AI 向量状态默认值，供 AI 模块页面统一复用。
 *
 * @return {Object} 默认状态
 */
export function createEmptyAiVectorStatus() {
  return {
    ragEnabled: false,
    chatProvider: '',
    embeddingProvider: '',
    mixedProviderMode: false,
    chatModel: '',
    embeddingModel: '',
    redisHost: '',
    redisPort: '',
    indexName: '',
    prefix: '',
    modelDimension: null,
    indexDimension: null,
    indexExists: false,
    dimensionMatched: null,
    retrievalReady: false,
    needsRebuild: false,
    warningMessage: '',
    documentCount: 0,
    enabledDocumentCount: 0,
    totalChunkCount: 0,
    pendingChunkCount: 0,
    completedChunkCount: 0,
    failedChunkCount: 0
  }
}

/**
 * 创建 AI 向量维护结果默认值。
 *
 * @return {Object} 默认维护结果
 */
export function createEmptyAiMaintenanceSummary() {
  return {
    clearedVectorCount: 0,
    rebuiltDocumentCount: 0,
    rebuiltChunkCount: 0,
    message: ''
  }
}

/**
 * 统一提取 AI 模块接口错误提示。
 *
 * @param {any} error 异常对象
 * @param {string} fallback 默认提示
 * @return {string} 展示文案
 */
export function extractAiErrorMessage(error, fallback) {
  return error?.response?.data?.message || error?.message || fallback
}

/**
 * 将可选数值格式化为适合页面展示的占位形式。
 *
 * @param {number|string|null|undefined} value 原始值
 * @return {number|string} 展示值
 */
export function displayAiMetric(value) {
  return value ?? value === 0 ? value : '--'
}
