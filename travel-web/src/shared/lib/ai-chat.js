// AI 客服通用常量与纯函数，避免视图层承担过多业务细节。
export const AI_CHAT_SESSION_STORAGE_KEY = 'waytrip_ai_session_id'

export function buildWelcomeMessage() {
  return {
    id: 'welcome',
    role: 'assistant',
    content: '你好，我是 WayTrip AI 助手。你可以问我景点推荐、行程规划、订单咨询和平台规则问题。',
    citations: [],
    toolCalls: [],
    suggestions: ['帮我规划周末一日游', '看看我适合哪些景点', '查询我的订单问题'],
    feedbackEnabled: false,
    feedbackStatus: ''
  }
}

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

export function readCachedAiSessionId() {
  const value = localStorage.getItem(AI_CHAT_SESSION_STORAGE_KEY)
  return typeof value === 'string' && value.trim() ? value.trim() : ''
}

export function cacheAiSessionId(sessionId) {
  if (!sessionId) return
  localStorage.setItem(AI_CHAT_SESSION_STORAGE_KEY, sessionId)
}

export function clearCachedAiSessionId() {
  localStorage.removeItem(AI_CHAT_SESSION_STORAGE_KEY)
}

function normalizeSuggestions(value) {
  if (!Array.isArray(value)) return []
  return value
    .map((item) => typeof item === 'string' ? item.trim() : '')
    .filter(Boolean)
    .slice(0, 3)
}

function normalizeText(value, fallback = '') {
  return typeof value === 'string' && value.trim() ? value.trim() : fallback
}

