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
 * 构建等待 AI 返回时的占位消息。
 *
 * @returns {object}
 */
export function buildAssistantPendingMessage() {
  return {
    id: `pending_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    role: 'assistant',
    content: '',
    citations: [],
    toolCalls: [],
    suggestions: [],
    feedbackEnabled: false,
    feedbackStatus: '',
    pending: true
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
 * @returns {string[]}
 */
function normalizeSuggestions(value) {
  if (!Array.isArray(value)) return []
  return value
    .map((item) => typeof item === 'string' ? item.trim() : '')
    .filter(Boolean)
    .slice(0, 3)
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
