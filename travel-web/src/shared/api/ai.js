import request from '@/shared/api/client.js'

/**
 * 创建新的 AI 会话。
 *
 * @returns {Promise<any>} 会话创建结果
 */
export const createAiSession = () => request.post('/ai/sessions')

/**
 * 清空指定 AI 会话。
 *
 * @param {string} sessionId 会话 ID
 * @returns {Promise<any>} 清空结果
 */
export const clearAiSession = (sessionId) => request.delete(`/ai/sessions/${sessionId}`)

/**
 * 调用 AI 对话接口。
 *
 * @param {Object} payload 对话请求参数
 * @returns {Promise<any>} AI 对话响应
 */
export const chatWithAi = (payload) => request.post('/ai/chat', payload, { timeout: 60000 })

/**
 * 提交 AI 回复反馈。
 *
 * @param {Object} payload 反馈请求参数
 * @returns {Promise<any>} 反馈结果
 */
export const submitAiFeedback = (payload) => request.post('/ai/feedback', payload)

