import { AI_CHAT_COPY } from '@/shared/constants/ai-chat.js'

// AI 客服通用常量与纯函数，避免视图层承担过多业务细节。
export const AI_CHAT_SESSION_STORAGE_KEY = 'waytrip_ai_session_id'

/**
 * 构建首次进入聊天面板时的欢迎消息。
 *
 * @returns {object}
 */
export function buildWelcomeMessage() {
  return {
    id: 'welcome',
    role: 'assistant',
    content: AI_CHAT_COPY.welcome,
    citations: [],
    toolCalls: [],
    suggestions: AI_CHAT_COPY.welcomeSuggestions,
    feedbackEnabled: false,
    feedbackStatus: ''
  }
}

/**
 * 构建用户消息。
 *
 * @param {string} content 消息内容
 * @returns {object}
 */
export function buildUserMessage(content) {
  return {
    id: `user_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    role: 'user',
    content,
    citations: [],
    toolCalls: [],
    suggestions: [],
    feedbackEnabled: false,
    feedbackStatus: ''
  }
}

/**
 * 构建等待 AI 流式返回时的助手消息骨架。
 *
 * @returns {object}
 */
export function buildAssistantStreamingMessage() {
  return {
    id: `pending_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    role: 'assistant',
    content: '',
    citations: [],
    toolCalls: [],
    suggestions: [],
    feedbackEnabled: false,
    feedbackStatus: '',
    pending: true,
    createdAt: Date.now(),
    scenario: ''
  }
}

/**
 * 将后端返回的 AI 响应归一成前端消息结构。
 *
 * @param {object} payload 后端响应数据
 * @returns {object}
 */
export function buildAssistantMessage(payload) {
  return {
    id: payload?.messageId || `assistant_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    role: 'assistant',
    content: normalizeText(payload?.reply, '抱歉，本次回复为空，请换个问法或稍后再试。'),
    citations: Array.isArray(payload?.citations) ? payload.citations : [],
    toolCalls: Array.isArray(payload?.toolCalls) ? payload.toolCalls : [],
    suggestions: normalizeSuggestions(payload?.suggestions),
    feedbackEnabled: Boolean(payload?.feedbackEnabled),
    feedbackStatus: '',
    createdAt: payload?.createdAt || Date.now(),
    scenario: payload?.scenario || ''
  }
}

/**
 * 将流式开始事件写入助手消息骨架。
 *
 * @param {object} message 当前助手消息
 * @param {object} payload 开始事件
 */
export function applyAssistantStartEvent(message, payload) {
  if (!message) return
  if (payload?.messageId) {
    message.id = payload.messageId
  }
  if (payload?.createdAt) {
    message.createdAt = payload.createdAt
  }
  if (payload?.scenario) {
    message.scenario = payload.scenario
  }
}

/**
 * 将流式增量文本追加到当前助手消息。
 *
 * @param {object} message 当前助手消息
 * @param {object} payload 增量事件
 */
export function appendAssistantDelta(message, payload) {
  if (!message) return
  const delta = typeof payload?.delta === 'string' ? payload.delta : ''
  if (!delta) return
  message.content += delta
}

/**
 * 使用流式结束事件补齐助手消息元信息。
 *
 * @param {object} message 当前助手消息
 * @param {object} payload 结束事件
 */
export function finalizeAssistantMessage(message, payload) {
  if (!message) return
  message.id = payload?.messageId || message.id
  message.content = normalizeText(payload?.reply, message.content || '抱歉，本次回复为空，请换个问法或稍后再试。')
  message.citations = Array.isArray(payload?.citations) ? payload.citations : []
  message.toolCalls = Array.isArray(payload?.toolCalls) ? payload.toolCalls : []
  message.suggestions = normalizeSuggestions(payload?.suggestions)
  message.feedbackEnabled = Boolean(payload?.feedbackEnabled)
  message.feedbackStatus = ''
  message.createdAt = payload?.createdAt || message.createdAt || Date.now()
  message.scenario = payload?.scenario || message.scenario || ''
  message.pending = false
}

/**
 * 统一把请求异常转成用户可见文案。
 *
 * @param {any} error 异常对象
 * @returns {string}
 */
export function resolveAiErrorMessage(error) {
  if (typeof error?.message === 'string' && error.message.trim()) {
    return error.message.trim()
  }
  const status = error?.response?.status
  if (status === 401 || status === 403) {
    return '当前登录状态无法使用 AI 客服，请重新登录后再试。'
  }
  if (error?.code === 'ECONNABORTED' || /timeout/i.test(error?.message || '')) {
    return 'AI 响应超时，请稍后重试。'
  }
  if (!error?.response) {
    return '网络连接异常，请检查网络后再试。'
  }
  // 其余异常统一收敛为繁忙提示，避免把后端细节直接暴露给用户。
  return 'AI 服务暂时繁忙，请稍后再试。'
}

/**
 * 从本地缓存读取 AI 会话 ID。
 *
 * @returns {string}
 */
export function readCachedAiSessionId() {
  const value = localStorage.getItem(AI_CHAT_SESSION_STORAGE_KEY)
  return typeof value === 'string' && value.trim() ? value.trim() : ''
}

/**
 * 缓存当前 AI 会话 ID。
 *
 * @param {string} sessionId 会话 ID
 */
export function cacheAiSessionId(sessionId) {
  if (!sessionId) return
  localStorage.setItem(AI_CHAT_SESSION_STORAGE_KEY, sessionId)
}

/**
 * 清理本地缓存中的 AI 会话 ID。
 */
export function clearCachedAiSessionId() {
  localStorage.removeItem(AI_CHAT_SESSION_STORAGE_KEY)
}

/**
 * 归一化建议问题列表，只保留短且有效的字符串项。
 *
 * @param {any} value 原始值
 * @returns {{ text: string, scenarioHint: string, sourcePage: string }[]}
 */
function normalizeSuggestions(value) {
  if (!Array.isArray(value)) return []
  return value
    .map((item) => normalizeSuggestionItem(item))
    .filter((item) => item && item.text)
    .slice(0, 3)
}

/**
 * 归一化单个建议项，兼容历史字符串格式与新的结构化对象格式。
 *
 * @param {any} value 原始建议项
 * @returns {{ text: string, scenarioHint: string, sourcePage: string } | null}
 */
function normalizeSuggestionItem(value) {
  if (typeof value === 'string') {
    const text = value.trim()
    return text ? { text, scenarioHint: '', sourcePage: '' } : null
  }
  if (!value || typeof value !== 'object') {
    return null
  }
  const text = typeof value.text === 'string' ? value.text.trim() : ''
  if (!text) {
    return null
  }
  return {
    text,
    scenarioHint: typeof value.scenarioHint === 'string' ? value.scenarioHint.trim() : '',
    sourcePage: typeof value.sourcePage === 'string' ? value.sourcePage.trim() : ''
  }
}

/**
 * 归一化文本字段，为空时返回兜底文案。
 *
 * @param {any} value 原始值
 * @param {string} fallback 兜底文本
 * @returns {string}
 */
function normalizeText(value, fallback = '') {
  return typeof value === 'string' && value.trim() ? value.trim() : fallback
}
