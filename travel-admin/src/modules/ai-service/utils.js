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
 * 将 AI 预览返回的知识域列表格式化为统一展示文案。
 *
 * @param {string[]|undefined|null} domains 命中知识域列表
 * @param {string|undefined|null} fallbackDomain 兜底知识域
 * @param {Record<string, string>} domainLabels 知识域文案映射
 * @return {string} 页面展示文案
 */
export function formatAiPreviewDomains(domains, fallbackDomain, domainLabels) {
  const labels = Array.isArray(domains)
    ? domains
      .filter(item => typeof item === 'string' && item.trim())
      .map(item => resolveAiDomainLabel(item, domainLabels))
    : []
  if (labels.length > 0) {
    return labels.join(' / ')
  }
  return resolveAiDomainLabel(fallbackDomain, domainLabels)
}

/**
 * 生成 AI 预览的空结果兜底结构，避免多个页面各自拼装字段。
 *
 * @param {object} options 兜底参数
 * @param {string} options.query 查询内容
 * @param {string} options.scenario 场景
 * @param {string} options.domain 默认知识域
 * @return {object} 统一兜底结果
 */
export function buildEmptyAiPreviewResult({ query, scenario, domain }) {
  return {
    query,
    scenario,
    domain: domain || '',
    domains: domain ? [domain] : [],
    hitCount: 0,
    hits: []
  }
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

/**
 * 将单个知识域编码转换为页面展示文案。
 *
 * @param {string|null|undefined} value 原始知识域
 * @param {Record<string, string>} domainLabels 知识域文案映射
 * @return {string} 展示文案
 */
function resolveAiDomainLabel(value, domainLabels) {
  return domainLabels?.[value] || value || '未分类'
}
