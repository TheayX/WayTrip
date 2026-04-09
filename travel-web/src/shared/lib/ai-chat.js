// AI 客服通用常量与纯函数，避免视图层承担过多业务细节。
export const AI_CHAT_SESSION_STORAGE_KEY = 'waytrip_ai_session_id'

export function buildWelcomeMessage() {
  return { role: 'assistant', content: '你好，我是 WayTrip AI 客服。你可以问我景点、攻略、订单相关问题。' }
}

export function createSessionId() {
  // 会话 ID 只要求当前浏览器内唯一，便于串联短期上下文，不承担安全标识职责。
  return `web_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
}

export function isValidSessionId(value) {
  return typeof value === 'string' && /^web_\d+_[a-z0-9]{8}$/i.test(value)
}

export function getReplyContent(response) {
  // 前端统一从业务响应体的 data.reply 取值，避免组件层感知接口包裹结构。
  const reply = response?.data?.reply
  return typeof reply === 'string' ? reply.trim() : ''
}

export function resolveAiErrorMessage(error) {
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

