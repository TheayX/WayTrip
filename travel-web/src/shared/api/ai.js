import request from '@/shared/api/client.js'

// AI 会话接口基础路径。
const AI_SESSION_BASE_PATH = '/ai/sessions'

// AI 对话接口路径。
const AI_CHAT_PATH = '/ai/chat'

// AI 反馈接口路径。
const AI_FEEDBACK_PATH = '/ai/feedback'

/**
 * 创建新的 AI 会话。
 *
 * @returns {Promise<any>} 会话创建结果
 */
export const createAiSession = () => request.post(AI_SESSION_BASE_PATH)

/**
 * 清空指定 AI 会话。
 *
 * @param {string} sessionId 会话 ID
 * @returns {Promise<any>} 清空结果
 */
export const clearAiSession = (sessionId) => request.delete(`${AI_SESSION_BASE_PATH}/${sessionId}`)

/**
 * 调用 AI 对话接口。
 *
 * @param {Object} payload 对话请求参数
 * @returns {Promise<any>} AI 对话响应
 */
export const chatWithAi = (payload) => request.post(AI_CHAT_PATH, payload, {
  // AI 对话链路可能包含 RAG 和工具调用，超时预算需要明显高于普通接口。
  timeout: 60000
})

/**
 * 提交 AI 回复反馈。
 *
 * @param {Object} payload 反馈请求参数
 * @returns {Promise<any>} 反馈结果
 */
export const submitAiFeedback = (payload) => request.post(AI_FEEDBACK_PATH, payload)

